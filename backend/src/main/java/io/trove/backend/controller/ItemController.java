package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 为藏品详情页查询公开资料、当前挂单和历史成交保留入口。
 * Reserves entry points for loading public item data, the current listing and sale history on the item detail page.
 */
@RestController
@RequestMapping("/api/v1/items")
@Tag(name = "藏品详情")
public class ItemController {
}
