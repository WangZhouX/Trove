package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.trove.backend.api.MarketApi;
import io.trove.backend.service.MarketplaceQueryService;
import io.trove.backend.service.WalletSessionService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 在「我的收藏」读取当前钱包直接持有及索引显示正在托管的藏品。
 * Loads directly held collectibles and indexed escrow for the current wallet in My Collection.
 */
@RestController
@RequestMapping("/api/v1/collection")
@Tag(name = "我的收藏")
public class CollectionController {

    /** 收藏查询服务。 / Collection query service. */
    private final MarketplaceQueryService marketplace;

    /**
     * 注入收藏查询服务。 / Inject the collection query service.
     *
     * @param marketplace 合并链上持有与索引托管的服务 / service combining on-chain holdings with indexed escrow
     */
    public CollectionController(MarketplaceQueryService marketplace) {
        this.marketplace = marketplace;
    }

    /**
     * 预计在「我的收藏」登录后打开、切换钱包重新登录或交易确认后刷新时调用。
     * For loading My Collection after sign-in, signing in as a different wallet, or refreshing after transaction confirmation.
     *
     * @param identity 会话绑定的钱包与网络 / wallet and network bound to the authenticated session
     * @return 收藏列表和核对钱包持有时的最终确认槽 / collectibles and the finalized slot used to check wallet holdings
     */
    @GetMapping("/me")
    @Operation(summary = "查询当前钱包的收藏", security = @SecurityRequirement(name = "walletBearer"))
    public ResponseEntity<MarketApi.Collection> getCollection(
            @Parameter(hidden = true) @AuthenticationPrincipal WalletSessionService.Identity identity) {
        // 钱包切换后必须查询新会话的数据。 / Wallet changes must fetch data for the new session.
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(marketplace.getCollection(identity));
    }
}
