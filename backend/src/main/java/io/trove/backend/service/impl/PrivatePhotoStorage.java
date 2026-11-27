package io.trove.backend.service.impl;

import io.trove.backend.api.ConsignmentPhotoException;
import io.trove.backend.utils.ConsignmentPhotoImageUtil;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.UUID;

/** 保存只可经后端授权读取的照片文件。 / Stores photos that are readable only through authorized backend routes. */
@Component
class PrivatePhotoStorage {

    /** 运维为服务用户预置的独立目录路径，可空。 / Dedicated directory provisioned for the service user, or empty. */
    private final String directory;

    /**
     * 记录部署方的私有目录配置。 / Stores the deployment's private directory configuration.
     *
     * @param directory 空值禁用存储，启用时须为绝对 POSIX 目录 / empty disables storage; enable with an absolute POSIX directory
     */
    PrivatePhotoStorage(@Value("${TROVE_PRIVATE_PHOTO_DIR:}") String directory) {
        this.directory = directory;
    }

    /**
     * 检查目录权限和边界，再生成只包含服务端 UUID 的目标路径。
     * Checks directory permissions and boundaries before deriving the path using only the server-generated UUID.
     *
     * @param photoId 后端生成的 UUID / server-generated UUID
     * @return 私有目录内的绝对文件路径 / absolute file path within the private directory
     * @throws ConsignmentPhotoException 存储未设置或配置不安全 / storage is absent or unsafe
     */
    Path path(UUID photoId) {
        try {
            var configured = Path.of(directory);
            if (!configured.isAbsolute()) {
                throw unavailable();
            }
            var root = configured.toRealPath();
            // 仅使用运维明确配置的专用目录，禁止经过符号链接并要求仅所有者访问。
            // Use the explicitly configured dedicated directory, disallow symlinks and require owner-only access.
            if (!root.equals(configured.normalize()) || !Files.isDirectory(root)
                    || !Files.getPosixFilePermissions(root, LinkOption.NOFOLLOW_LINKS)
                    .equals(PosixFilePermissions.fromString("rwx------")) || !Files.isWritable(root)) {
                throw unavailable();
            }
            return root.resolve(photoId.toString());
        } catch (IOException | InvalidPathException | UnsupportedOperationException | SecurityException e) {
            throw unavailable();
        }
    }

    /**
     * 只创建新文件，任何失败均清理已创建的残留。
     * Creates a new file exclusively and cleans up any partial file on failure.
     *
     * @param path 由服务端编号派生的私有路径 / private path derived from a server-owned identifier
     * @param bytes 已验证且限制大小的内容 / validated and size-bounded contents
     * @throws ConsignmentPhotoException 写入失败或对象已存在 / write failed or the object already exists
     */
    void write(Path path, byte[] bytes) {
        var created = false;
        try (var output = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS)) {
            created = true;
            output.write(bytes);
        } catch (IOException | SecurityException e) {
            if (created) {
                removeRolledBack(path);
            }
            throw unavailable();
        }
    }

    /**
     * 安全读回限长文件，禁止通过照片文件符号链接。
     * Reads a size-bounded file without following a symlink in place of the photo.
     *
     * @param path 当前调用已鉴权的照片路径 / photo path already authorized for the current caller
     * @return 已存储的原始图像 / original stored image
     * @throws ConsignmentPhotoException 文件丢失、超过限制或不可访问 / file is missing, too large, or otherwise inaccessible
     */
    byte[] read(Path path) {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw unavailable();
        }
        try (var input = Files.newInputStream(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            var bytes = input.readNBytes(ConsignmentPhotoImageUtil.MAX_BYTES + 1);
            if (bytes.length == 0 || bytes.length > ConsignmentPhotoImageUtil.MAX_BYTES) {
                throw unavailable();
            }
            return bytes;
        } catch (IOException | SecurityException e) {
            throw unavailable();
        }
    }

    /**
     * 清理此次事务写入但未入库的对象，不记录图片路径或内容。
     * Removes the object from a rolled-back write without logging the image path or contents.
     *
     * @param path 此次上传创建的私有对象 / private object created by this upload
     */
    void removeRolledBack(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException | SecurityException e) {
            LoggerFactory.getLogger(PrivatePhotoStorage.class).warn(
                    "Could not remove a private consignment photo from a failed upload");
        }
    }

    /** @return 隐藏存储细节的可重试错误 / retriable error that withholds storage details */
    private static ConsignmentPhotoException unavailable() {
        return new ConsignmentPhotoException(ConsignmentPhotoException.Error.PHOTO_STORAGE_UNAVAILABLE);
    }
}
