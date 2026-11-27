package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 寄存提交的照片引用，由后端控制访问。
 * References to submitted consignment photos with backend-controlled access.
 */
@TableName("consignment_photo")
public record ConsignmentPhotoDO() {
}
