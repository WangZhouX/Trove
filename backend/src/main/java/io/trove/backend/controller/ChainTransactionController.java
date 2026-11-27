package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 为钱包签署铸造、市场或兑换交易后查询确认状态保留入口。
 * Reserves an entry point for checking confirmation after a wallet signs a mint, market or redemption transaction.
 */
@RestController
@RequestMapping("/api/v1/chain/transactions")
@Tag(name = "链上交易进度")
public class ChainTransactionController {
}
