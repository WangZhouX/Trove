package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 为收藏者提交兑换草稿、准备签名及在「兑换进度」查询记录保留入口。
 * Reserves entry points for creating a redemption draft, preparing its signature and checking records on Redemption Progress.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "实物兑换")
public class RedemptionController {
}
