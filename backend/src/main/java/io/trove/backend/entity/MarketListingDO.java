package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 链上固定价托管挂单的查询投影，更新报价会推进同一挂单的版本。
 * Query projection of fixed-price on-chain escrow listings, with repricing advancing the version of the same listing.
 *
 * <p>复合主键为 {@code (network, item_id, listing_no)}。
 * The composite primary key is {@code (network, item_id, listing_no)}.
 *
 * @param network 该挂单所属的 Solana 网络。
 *        Solana network of the listing.
 * @param itemId 对应链上藏品的实物 UUID。
 *        Physical item UUID of the listed asset.
 * @param listingNo 链上分配的正整数挂单编号，重新挂售时使用新编号。
 *        Positive on-chain listing number, renewed when the item is relisted.
 * @param listingAddress 该挂单对应的链上 Listing PDA。
 *        On-chain Listing PDA for this listing.
 * @param sellerWallet 链上挂单记录的卖家钱包公钥。
 *        Seller wallet address recorded on-chain.
 * @param escrowTokenAccount 挂单期间托管 NFT 的链上代币账户公钥。
 *        Address of the on-chain token account that holds the NFT in escrow while listed.
 * @param revision 当前已同步的链上挂单版本，用于识别旧报价。
 *        Indexed on-chain listing version used to identify stale quotes.
 * @param priceLamports 挂单固定报价，以整数 lamports 计。
 *        Fixed asking price in integer lamports.
 * @param status 挂单状态为有效、已售出或已撤销。
 *        Listing status is ACTIVE, SOLD, or CANCELLED.
 * @param openTxSignature 创建该挂单的最终确认交易签名。
 *        Finalized transaction signature that opened the listing.
 * @param openEventIndex 创建挂单的交易内事件序号，从 0 开始。
 *        Zero-based event ordinal of the listing creation within its transaction.
 * @param lastTxSignature 最近一次应用到该挂单的最终确认交易签名。
 *        Most recent finalized transaction signature applied to this listing.
 * @param lastEventIndex 最近一次应用到该挂单的交易内事件序号，从 0 开始。
 *        Zero-based ordinal of the most recent event applied to this listing within its transaction.
 * @param updatedAt 最近应用链上变更的数据库时间。
 *        Database time at which the latest on-chain change was applied.
 */
@TableName("market_listing")
public record MarketListingDO(
        @TableField(value = "network", updateStrategy = FieldStrategy.NEVER) String network,
        @TableField(value = "item_id", updateStrategy = FieldStrategy.NEVER) UUID itemId,
        @TableField(value = "listing_no", updateStrategy = FieldStrategy.NEVER) BigInteger listingNo,
        @TableField("listing_address") String listingAddress,
        @TableField("seller_wallet") String sellerWallet,
        @TableField("escrow_token_account") String escrowTokenAccount,
        @TableField("revision") BigInteger revision,
        @TableField("price_lamports") BigInteger priceLamports,
        @TableField("status") String status,
        @TableField("open_tx_signature") String openTxSignature,
        @TableField("open_event_index") int openEventIndex,
        @TableField("last_tx_signature") String lastTxSignature,
        @TableField("last_event_index") int lastEventIndex,
        @TableField("updated_at") OffsetDateTime updatedAt
) {

    /**
     * 供 MyBatis-Plus Lambda 查询读取 {@code network} 主键列。
     * Exposes the primary-key column {@code network} for MyBatis-Plus Lambda queries.
     *
     * @return 主键列的当前值。 The current primary-key value.
     */
    public String getNetwork() {
        return network;
    }

    /**
     * 供 MyBatis-Plus Lambda 查询读取 {@code item_id} 主键列。
     * Exposes the primary-key column {@code item_id} for MyBatis-Plus Lambda queries.
     *
     * @return 主键列的当前值。 The current primary-key value.
     */
    public UUID getItemId() {
        return itemId;
    }

    /**
     * 供 MyBatis-Plus Lambda 查询读取 {@code listing_no} 主键列。
     * Exposes the primary-key column {@code listing_no} for MyBatis-Plus Lambda queries.
     *
     * @return 主键列的当前值。 The current primary-key value.
     */
    public BigInteger getListingNo() {
        return listingNo;
    }
}
