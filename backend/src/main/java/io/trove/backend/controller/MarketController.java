package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.trove.backend.api.MarketApi;
import io.trove.backend.service.MarketplaceQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 为「市场」页面公开已索引的固定价挂单分页。
 * Exposes indexed fixed-price listing pages for the Market page.
 */
@RestController
@RequestMapping("/api/v1/market")
@Tag(name = "藏品市场")
public class MarketController {

    /** 市场和藏品查询服务。 / Marketplace and collectible query service. */
    private final MarketplaceQueryService marketplace;

    /**
     * 注入市场查询服务。 / Inject the market query service.
     *
     * @param marketplace 读取市场索引的服务 / service for reading the marketplace index
     */
    public MarketController(MarketplaceQueryService marketplace) {
        this.marketplace = marketplace;
    }

    /**
     * 预计在打开「市场」、切换「全部／收藏卡／潮玩」、调整价格排序或翻页时调用。
     * For opening Market, switching its all/cards/art toys filter, changing price order, or turning the page.
     *
     * @param network 页面所选 localnet 或 devnet / localnet or devnet selected by the page
     * @param category 可选 CARD 或 TOY，省略时读取所有品类 / optional CARD or TOY, omit to include all categories
     * @param sort 最先更新、最低价格或最高价格在前 / order by newest update, lowest price, or highest price first
     * @param page 从 1 开始的页码 / one-based page number
     * @param size 每页最多 100 条 / page size up to 100
     * @return 含标题、封面和精确报价的挂单页 / listing page with titles, covers and exact quotes
     */
    @GetMapping("/listings")
    @Operation(summary = "查询市场在售藏品")
    public MarketApi.PageResult<MarketApi.Listing> listListings(
            @Parameter(description = "localnet 或 devnet") @RequestParam("network") String network,
            @RequestParam(value = "category", required = false) MarketApi.Category category,
            @RequestParam(value = "sort", defaultValue = "NEWEST") MarketApi.Sort sort,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return marketplace.listMarket(network, category, sort, page, size);
    }
}
