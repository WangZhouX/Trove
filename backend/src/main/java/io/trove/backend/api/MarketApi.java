package io.trove.backend.api;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 市场、藏品详情与钱包收藏的公开字段和分页结构。
 * Public fields and pagination for the marketplace, item details and wallet collections.
 */
public final class MarketApi {

    /** 仅通过嵌套接口类型引用。 / Use only the nested API types. */
    private MarketApi() {
    }

    /**
     * 市场与成交历史的分页响应。 / Page of listings or historical sales.
     *
     * @param items 按查询顺序排列的数据 / results in query order
     * @param page 从 1 开始的页码 / one-based page number
     * @param size 请求的每页数量 / requested page size
     * @param total 过滤后记录总数 / count after applying filters
     * @param <T> 页面行类型 / page row type
     */
    public record PageResult<T>(List<T> items, int page, int size, long total) {
    }

    /**
     * 当前有效挂单，可用于显示价格和核对买家确认的报价。
     * Active listing with display fields and the exact quote the buyer can confirm.
     *
     * @param itemId 仓库实物的稳定 UUID / stable UUID of the physical item
     * @param category CARD 收藏卡或 TOY 潮玩 / CARD for cards or TOY for art toys
     * @param title 仓库核验的公开名称 / warehouse-verified public title
     * @param verifiedCondition 核验后的公开品相 / verified public condition
     * @param coverUri 审核后的封面，未设置时为空 / approved cover, null when unset
     * @param mintAddress NFT Mint 的 Base58 公钥 / Base58 public key of the NFT mint
     * @param listingNo 链上挂单编号，无符号整数的十进制文本 / on-chain listing number as an unsigned decimal string
     * @param listingAddress 挂单 PDA 的 Base58 公钥 / Base58 public key of the listing PDA
     * @param sellerWallet 卖家的 Base58 钱包地址 / seller's Base58 wallet address
     * @param revision 链上报价版本，无符号整数的十进制文本 / on-chain quote revision as an unsigned decimal string
     * @param priceLamports 整数 lamports 十进制文本 / price in integer lamports as a decimal string
     * @param updatedAt 挂单投影最近更新时间 / time of the latest indexed listing update
     */
    public record Listing(UUID itemId, String category, String title, String verifiedCondition, String coverUri,
                          String mintAddress, String listingNo, String listingAddress, String sellerWallet,
                          String revision, String priceLamports, OffsetDateTime updatedAt) {
    }

    /**
     * 公开实物与 NFT 资料，附当前已索引的有效挂单。
     * Public item and NFT information, with the current indexed active listing.
     *
     * @param itemId 仓库实物的稳定 UUID / stable UUID of the physical item
     * @param category CARD 收藏卡或 TOY 潮玩 / CARD for cards or TOY for art toys
     * @param title 仓库核验的公开名称 / warehouse-verified public title
     * @param verifiedCondition 核验后的公开品相 / verified public condition
     * @param coverUri 审核后的封面，未设置时为空 / approved cover, null when unset
     * @param mintAddress NFT Mint 的 Base58 公钥 / Base58 public key of the NFT mint
     * @param itemRecordAddress 链上实物记录 PDA / on-chain item record PDA
     * @param collectionMintAddress Trove 集合的 Mint 公钥 / mint public key of the Trove collection
     * @param custodyState 仓库实物状态，IN_VAULT 或 SHIPPED_OUT / physical custody state, IN_VAULT or SHIPPED_OUT
     * @param activeListing 已索引的有效报价，无挂单时为空 / indexed active quote, null when unlisted
     */
    public record ItemDetail(UUID itemId, String category, String title, String verifiedCondition, String coverUri,
                             String mintAddress, String itemRecordAddress, String collectionMintAddress,
                             String custodyState, Listing activeListing) {
    }

    /**
     * 一笔已最终确认的藏品成交。 / A finalized collectible sale.
     *
     * @param listingNo 成交对应的挂单编号十进制文本 / listing number as a decimal string
     * @param saleNo 从 1 开始的成交序号十进制文本 / one-based sale sequence as a decimal string
     * @param saleType PRIMARY 首次成交或 RESALE 转售 / PRIMARY for the first sale or RESALE thereafter
     * @param listingRevision 成交确认的报价版本十进制文本 / confirmed quote revision as a decimal string
     * @param sellerWallet 卖家的链上钱包公钥 / seller's on-chain wallet public key
     * @param buyerWallet 买家的链上钱包公钥 / buyer's on-chain wallet public key
     * @param priceLamports 整数 lamports 十进制文本，不计网络费 / integer lamports as a decimal string, excluding network fees
     * @param signature 最终确认的链上交易签名 / finalized on-chain transaction signature
     * @param eventIndex 交易中从 0 开始的事件序号 / zero-based event index within the transaction
     * @param blockTime RPC 返回的区块生产时间，未提供时为空 / RPC block production time, null if unavailable
     */
    public record Sale(String listingNo, String saleNo, String saleType, String listingRevision,
                       String sellerWallet, String buyerWallet, String priceLamports, String signature,
                       int eventIndex, OffsetDateTime blockTime) {
    }

    /**
     * 当前钱包的直接持有与索引托管记录。 / Direct wallet holdings and indexed escrow records.
     *
     * @param items 钱包持有或作为卖家托管的 Trove 藏品 / Trove items directly held or in this seller's indexed escrow
     * @param finalizedSlot 直接持有查询使用的最终确认区块槽十进制文本 / finalized slot for direct holdings as a decimal string
     */
    public record Collection(List<CollectionItem> items, String finalizedSlot) {
    }

    /**
     * 当前钱包的一条收藏记录。 / One item in the current wallet's collection.
     *
     * @param itemId 实物 UUID / physical-item UUID
     * @param category CARD 收藏卡或 TOY 潮玩 / CARD for cards or TOY for art toys
     * @param title 仓库核验的公开名称 / warehouse-verified public title
     * @param verifiedCondition 核验后的公开品相 / verified public condition
     * @param coverUri 审核后的封面，未设置时为空 / approved cover, null when unset
     * @param mintAddress NFT Mint 的 Base58 公钥 / Base58 public key of the NFT mint
     * @param holding HELD 表示链上直接持有，LISTED 表示索引显示卖家托管 /
     *                HELD means direct on-chain ownership, LISTED means indexed seller escrow
     * @param activeListing LISTED 对应的有效挂单，HELD 时为空 / indexed active listing for LISTED, null for HELD
     */
    public record CollectionItem(UUID itemId, String category, String title, String verifiedCondition, String coverUri,
                                 String mintAddress, Holding holding, Listing activeListing) {
    }

    /** 市场排序规则。 / Marketplace sorting options. */
    public enum Sort {
        NEWEST, PRICE_ASC, PRICE_DESC
    }

    /**
     * 市场藏品品类。全部品类由省略筛选参数表示。
     * Marketplace categories. Omit the filter to include all categories.
     */
    public enum Category {
        CARD, TOY
    }

    /** 收藏记录的持有来源。 / Source of a collection holding. */
    public enum Holding {
        HELD, LISTED
    }
}
