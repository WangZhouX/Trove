package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 寄存提交的照片引用，由后端控制访问。
 * References to submitted consignment photos with backend-controlled access.
 *
 * @param photoId 后端生成的 UUID 照片编号。
 *        Server-generated UUID photo identifier.
 * @param consignmentId 照片所属寄存单编号。
 *        Consignment identifier associated with the photo.
 * @param storageKey 受访问控制的存储对象键。
 *        Storage object key subject to backend access controls.
 * @param contentType 上传校验通过的图像 MIME 类型。
 *        Image MIME type verified during upload.
 * @param sortOrder 寄存单内从零开始的展示顺序。
 *        Zero-based display position within the consignment.
 * @param uploadedAt 照片引用入库时间，API 以 UTC 返回。
 *        Time the photo reference was stored, exposed in UTC by the API.
 */
@TableName("consignment_photo")
public record ConsignmentPhotoDO(
        @TableId(value = "photo_id", type = IdType.INPUT) UUID photoId,
        @TableField("consignment_id") UUID consignmentId,
        @TableField("storage_key") String storageKey,
        @TableField("content_type") String contentType,
        @TableField("sort_order") short sortOrder,
        @TableField("uploaded_at") OffsetDateTime uploadedAt
) {

    /**
     * 供 MyBatis-Plus Lambda 查询读取 {@code photo_id} 主键列。
     * Exposes the primary-key column {@code photo_id} for MyBatis-Plus Lambda queries.
     *
     * @return 主键列的当前值。 The current primary-key value.
     */
    public UUID getPhotoId() {
        return photoId;
    }
}
