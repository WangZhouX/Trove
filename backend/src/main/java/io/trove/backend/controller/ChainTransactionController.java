package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.trove.backend.api.ChainTransactionApi;
import io.trove.backend.api.ChainTransactionException;
import io.trove.backend.service.ChainEventService;
import io.trove.backend.service.WalletSessionService;
import io.trove.backend.utils.SolanaWalletUtil;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供钱包可见的最终确认交易回执查询。
 * Provides finalized transaction receipt lookups within a wallet's visibility.
 */
@RestController
@RequestMapping("/api/v1/chain/transactions")
@Tag(name = "链上交易进度")
public class ChainTransactionController {

    /** 查询已索引回执的服务。 / Service for looking up indexed receipts. */
    private final ChainEventService events;

    /**
     * 创建交易回执控制器。 / Create the transaction receipt controller.
     *
     * @param events 链上事件与回执服务 / chain event and receipt service
     */
    public ChainTransactionController(ChainEventService events) {
        this.events = events;
    }

    /**
     * 供「仓库工作台」「藏品详情」和「兑换进度」将来在拿到签名后查询本地索引回执。
     * 返回与当前钱包关联的最终确认事件，页面可按事件类型、区块槽、区块时间展示。
     * Provides a future indexed receipt lookup for Warehouse Workbench, Item Details and Redemption Progress after a
     * signature becomes available. The UI can show finalized event types, slots and block times related to the caller.
     *
     * @param identity 已认证的钱包身份及其绑定网络 / authenticated wallet identity and bound network
     * @param signature Base58 编码的 Solana 交易签名 / Base58-encoded Solana transaction signature
     * @return 仅包含本钱包可见事件的最终确认回执 / finalized receipt containing only events visible to this wallet
     * @throws ChainTransactionException 签名格式无效或当前钱包查不到索引回执 / if the signature is malformed or no visible
     *         receipt is indexed for the wallet
     */
    @GetMapping("/{signature}")
    @Operation(summary = "查询已索引的链上交易回执", security = @SecurityRequirement(name = "walletBearer"))
    @ApiResponse(responseCode = "200", description = "当前钱包可见的最终确认交易回执")
    @ApiResponse(responseCode = "400", description = "交易签名格式不正确")
    @ApiResponse(responseCode = "404", description = "当前钱包无可查的已索引回执")
    public ChainTransactionApi.Receipt getReceipt(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity,
            @PathVariable("signature") String signature
    ) {
        if (!SolanaWalletUtil.isTransactionSignature(signature)) {
            throw new ChainTransactionException(ChainTransactionException.Error.INVALID_TRANSACTION_SIGNATURE);
        }

        // 无可见投影统一返回 404，避免暴露其他钱包的记录或猜测实时确认结果。
        // Use the same 404 for absent visibility evidence, avoiding disclosure or guesses about live confirmation.
        return events.findReceipt(identity.network(), identity.walletAddress(), signature)
                .orElseThrow(() -> new ChainTransactionException(
                        ChainTransactionException.Error.CHAIN_TRANSACTION_NOT_AVAILABLE));
    }
}
