package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 为「我的收藏」在钱包连接后加载持有和托管藏品保留入口。
 * Reserves an entry point for loading held and escrowed collectibles on My Collection after a wallet connects.
 */
@RestController
@RequestMapping("/api/v1/collection")
@Tag(name = "我的收藏")
public class CollectionController {
}
