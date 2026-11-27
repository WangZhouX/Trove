package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 为「市场」页面加载和筛选固定价挂单保留入口。
 * Reserves entry points for loading and filtering fixed-price listings on the Market page.
 */
@RestController
@RequestMapping("/api/v1/market")
@Tag(name = "藏品市场")
public class MarketController {
}
