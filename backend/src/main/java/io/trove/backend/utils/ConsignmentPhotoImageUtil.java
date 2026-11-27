package io.trove.backend.utils;

import io.trove.backend.api.ConsignmentPhotoException;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/** 限制上传字节和图像像素，并验证 JDK 可完整解码的照片。 / Bounds upload bytes and pixels and verifies photos the JDK can decode in full. */
public final class ConsignmentPhotoImageUtil {

    /** 照片内容的字节上限，5 MiB。 / Photo content limit in bytes, 5 MiB. */
    public static final int MAX_BYTES = 5 * 1024 * 1024;

    /** 最长宽高和总像素，限制解码内存使用。 / Maximum dimensions and pixel count to bound decode memory. */
    private static final int MAX_DIMENSION = 4096;
    private static final long MAX_PIXELS = 12L * 1024 * 1024;

    /** PNG 文件的固定签名和带 CRC 的终结块。 / Fixed PNG signature and terminal chunk with CRC. */
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    private static final byte[] PNG_END = {0, 0, 0, 0, 0x49, 0x45, 0x4e, 0x44, (byte) 0xae, 0x42, 0x60, (byte) 0x82};

    private ConsignmentPhotoImageUtil() {
    }

    /**
     * 读取请求内容并独立核对格式、图像边界和完整解码。
     * Reads the upload and independently checks format, image bounds and full decoding.
     *
     * @param input 当前 HTTP 请求体，调用方管理其寿命 / current HTTP request body, owned by the caller
     * @param contentLength HTTP 声明的大小，未知时为 -1 / declared HTTP length, -1 if unknown
     * @return 校验过的 MIME 和原始私有图像字节 / verified MIME and original private image bytes
     * @throws ConsignmentPhotoException 请求无效、内容过大或格式不受支持 / unreadable, oversized or unsupported upload
     */
    public static VerifiedImage readUpload(InputStream input, long contentLength) {
        if (contentLength > MAX_BYTES) {
            throw new ConsignmentPhotoException(ConsignmentPhotoException.Error.PHOTO_TOO_LARGE);
        }

        // 即使缺少 Content-Length 或使用分块传输，也最多读取上限加一字节。
        // Read at most one byte beyond the limit even for chunked bodies without a Content-Length.
        final byte[] bytes;
        try {
            bytes = input.readNBytes(MAX_BYTES + 1);
        } catch (IOException e) {
            throw new ConsignmentPhotoException(ConsignmentPhotoException.Error.INVALID_PHOTO_REQUEST);
        }
        if (bytes.length > MAX_BYTES) {
            throw new ConsignmentPhotoException(ConsignmentPhotoException.Error.PHOTO_TOO_LARGE);
        }
        var contentType = detect(bytes);
        verifyDecode(bytes, contentType);
        return new VerifiedImage(contentType, bytes);
    }

    /**
     * 检查支持格式的文件头和结束标记，随后还要通过图像解码。
     * Checks file signatures and terminators for supported formats before image decoding.
     *
     * @param bytes 拟验证的图像字节 / candidate image bytes
     * @return JPEG 或 PNG 的 MIME 类型 / JPEG or PNG MIME type
     * @throws ConsignmentPhotoException 格式或边界不符合要求 / unsupported format or invalid boundary
     */
    private static String detect(byte[] bytes) {
        var length = bytes.length;
        if (length >= PNG_SIGNATURE.length + PNG_END.length
                && Arrays.equals(bytes, 0, PNG_SIGNATURE.length, PNG_SIGNATURE, 0, PNG_SIGNATURE.length)
                && Arrays.equals(bytes, length - PNG_END.length, length, PNG_END, 0, PNG_END.length)) {
            return "image/png";
        }
        if (length >= 4 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8
                && bytes[length - 2] == (byte) 0xff && bytes[length - 1] == (byte) 0xd9) {
            return "image/jpeg";
        }
        throw new ConsignmentPhotoException(ConsignmentPhotoException.Error.INVALID_PHOTO);
    }

    /**
     * 先读取宽高再尝试完整解码，避免损坏图像或超限像素入库。
     * Reads dimensions before decoding to reject corrupted images and excessive pixel counts.
     *
     * @param bytes 请求中的图像内容 / uploaded image contents
     * @param contentType 已从边界字节确认的 MIME / MIME identified from boundary bytes
     * @throws ConsignmentPhotoException 不能完整解码或像素超限 / image cannot be decoded or exceeds pixel limits
     */
    private static void verifyDecode(byte[] bytes, String contentType) {
        var readers = ImageIO.getImageReadersByMIMEType(contentType);
        if (!readers.hasNext()) {
            throw new ConsignmentPhotoException(ConsignmentPhotoException.Error.INVALID_PHOTO);
        }
        var reader = readers.next();
        // 使用内存流，避免 JDK 在系统临时目录额外保留照片。
        // Keep the image input in memory so the JDK does not create a second copy in system temp storage.
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            reader.setInput(input, true, true);
            var width = reader.getWidth(0);
            var height = reader.getHeight(0);
            if (width < 1 || height < 1 || width > MAX_DIMENSION || height > MAX_DIMENSION
                    || (long) width * height > MAX_PIXELS) {
                throw new ConsignmentPhotoException(ConsignmentPhotoException.Error.PHOTO_TOO_LARGE);
            }
            // 拒绝带解码告警的图像，例如只读出部分像素的损坏 JPEG。
            // Reject decoding warnings, such as corrupted JPEGs from which only some pixels can be read.
            var warning = new AtomicBoolean();
            reader.addIIOReadWarningListener((source, message) -> warning.set(true));
            if (reader.read(0) == null || warning.get()) {
                throw new ConsignmentPhotoException(ConsignmentPhotoException.Error.INVALID_PHOTO);
            }
        } catch (ConsignmentPhotoException e) {
            throw e;
        } catch (IOException | IllegalArgumentException | IndexOutOfBoundsException e) {
            throw new ConsignmentPhotoException(ConsignmentPhotoException.Error.INVALID_PHOTO);
        } finally {
            reader.dispose();
        }
    }

    /**
     * 已通过解码和上限检查的私有图像。 / Private image after decode and size validation.
     *
     * @param contentType 校验出的 MIME / verified MIME type
     * @param bytes 原始图像数据 / original image bytes
     */
    public record VerifiedImage(String contentType, byte[] bytes) {
    }
}
