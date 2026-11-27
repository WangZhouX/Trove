package io.trove.backend.api;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 仅返回已授权寄存照片的信息。 / Carries details for an authorized consignment photo. */
public final class ConsignmentPhotoApi {

    private ConsignmentPhotoApi() {
    }

    /**
     * 用于寄存详情和仓库验真的私有照片条目。 / Private photo entry used by consignment details and warehouse inspection.
     *
     * @param photoId 服务端生成的照片编号 / server-generated photo ID
     * @param contentType 校验后的 JPEG 或 PNG MIME 类型 / verified JPEG or PNG MIME type
     * @param sortOrder 寄存单内从 0 开始的展示顺序 / zero-based display order within the consignment
     * @param uploadedAt 上传完成的 UTC 时间 / UTC upload time
     */
    public record Photo(UUID photoId, String contentType, short sortOrder, OffsetDateTime uploadedAt) {
    }

    /**
     * 经归属或仓库权限验证后读取的照片内容。 / Photo content loaded after ownership or warehouse authorization.
     *
     * @param contentType 服务端校验保存的图像 MIME / image MIME verified and saved by the server
     * @param bytes 最多 5 MiB 的私有照片字节 / up to 5 MiB of private image bytes
     */
    public record Content(String contentType, byte[] bytes) {
    }
}
