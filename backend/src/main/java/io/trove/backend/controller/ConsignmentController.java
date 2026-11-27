package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 为「寄存」页面提交寄存申请、查看我的申请与验收进度保留入口。
 * Reserves entry points for submitting consignments and viewing the user's applications and inspection progress on the Consign page.
 */
@RestController
@RequestMapping("/api/v1/consignments")
@Tag(name = "藏品寄存")
public class ConsignmentController {
}
