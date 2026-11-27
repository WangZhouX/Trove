package io.trove.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.trove.backend.api.MarketApi;
import io.trove.backend.service.MarketplaceQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 为「藏品详情」公开核验过的实物资料、当前挂单和已确认成交。
 * Exposes verified physical-item details, current indexed listings and finalized sales for Item Detail.
 */
@RestController
@RequestMapping("/api/v1/items")
@Tag(name = "藏品详情")
public class ItemController {

    /** 读取市场和藏品索引。 / Reads marketplace and collectible projections. */
    private final MarketplaceQueryService marketplace;

    /**
     * 注入藏品查询服务。 / Inject the collectible query service.
     *
     * @param marketplace 提供公开详情和成交记录 / service providing public details and sales
     */
    public ItemController(MarketplaceQueryService marketplace) {
        this.marketplace = marketplace;
    }

    /**
     * 预计点击「市场」或「我的收藏」的藏品后调用。展示公开品相、Mint 和当前报价。
     * For opening an item from Market or My Collection to display the public condition, mint and indexed quote.
     *
     * @param network 页面所选 localnet 或 devnet / localnet or devnet selected by the page
     * @param itemId 仓库实物 UUID / physical collectible UUID
     * @return 公开藏品详情，未挂售时 activeListing 为空 / public item detail, with null activeListing when unlisted
     */
    @GetMapping("/{itemId}")
    @Operation(summary = "查询藏品详情和当前报价")
    public MarketApi.ItemDetail getItem(@RequestParam("network") String network,
                                       @PathVariable("itemId") UUID itemId) {
        return marketplace.getItem(network, itemId);
    }

    /**
     * 预计打开「藏品详情」或翻动「成交记录」时调用，供页面区分首售和转售并链接链上交易。
     * For opening Item Detail or paging its Sales History, distinguishing primary sales from resales and linking transactions.
     *
     * @param network 页面所选 localnet 或 devnet / localnet or devnet selected by the page
     * @param itemId 仓库实物 UUID / physical collectible UUID
     * @param page 从 1 开始的页码 / one-based page number
     * @param size 每页最多 100 条 / page size up to 100
     * @return 按成交序号倒序排列的记录页 / sale history page ordered by descending sale number
     */
    @GetMapping("/{itemId}/sales")
    @Operation(summary = "分页查询藏品成交记录")
    public MarketApi.PageResult<MarketApi.Sale> listSales(
            @RequestParam("network") String network,
            @PathVariable("itemId") UUID itemId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return marketplace.listSales(network, itemId, page, size);
    }
}
