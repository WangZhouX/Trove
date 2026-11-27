package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigInteger;
import java.util.UUID;

/**
 * 经最终确认的 Trove 成交记录，每笔挂单最多对应一次成交。
 * Finalized Trove sales, with at most one sale for each listing.
 *
 * <p>复合主键为 {@code (network, item_id, listing_no)}。
 * The composite primary key is {@code (network, item_id, listing_no)}.
 *
 * @param network 成交所属的 Solana 网络。
 *        Solana network of the sale.
 * @param itemId 被购买藏品的实物 UUID。
 *        Physical item UUID of the purchased asset.
 * @param listingNo 本次交易成交并关闭的挂单编号。
 *        Listing number filled and closed by this trade.
 * @param saleNo Trove 程序记录的该藏品成交序号，从 1 开始。
 *        One-based sale sequence for this item recorded by the Trove program.
 * @param saleType 由成交序号生成，首次成交为 PRIMARY，其后为 RESALE。
 *        Derived from the sale sequence, PRIMARY for the first sale and RESALE thereafter.
 * @param listingRevision 成交时买家确认并由程序校验的挂单版本。
 *        Listing version accepted by the buyer and verified by the program at purchase.
 * @param sellerWallet 链上收到成交款的卖家钱包公钥。
 *        Seller wallet address that received the on-chain payment.
 * @param buyerWallet 链上收到 NFT 的买家钱包公钥。
 *        Buyer wallet address that received the NFT on-chain.
 * @param priceLamports 成交支付额，以整数 lamports 计，不含网络手续费。
 *        Purchase amount in integer lamports, excluding network transaction fees.
 * @param txSignature 最终确认的成交交易签名。
 *        Finalized purchase transaction signature.
 * @param eventIndex 成交事件在该交易内的序号，从 0 开始。
 *        Zero-based ordinal of the sale event within the transaction.
 */
@TableName("market_sale")
public record MarketSaleDO(
        @TableField(value = "network", updateStrategy = FieldStrategy.NEVER) String network,
        @TableField(value = "item_id", updateStrategy = FieldStrategy.NEVER) UUID itemId,
        @TableField(value = "listing_no", updateStrategy = FieldStrategy.NEVER) BigInteger listingNo,
        @TableField("sale_no") BigInteger saleNo,
        @TableField("sale_type") String saleType,
        @TableField("listing_revision") BigInteger listingRevision,
        @TableField("seller_wallet") String sellerWallet,
        @TableField("buyer_wallet") String buyerWallet,
        @TableField("price_lamports") BigInteger priceLamports,
        @TableField("tx_signature") String txSignature,
        @TableField("event_index") int eventIndex
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

    /**
     * 供 MyBatis-Plus Lambda 查询定位成交卖家钱包的 Solana 公钥。
     * Supports MyBatis-Plus Lambda queries that match the Solana public key of the seller in the sale.
     *
     * @return 成交卖家钱包的 Solana 公钥 / Solana public key of the seller in the sale
     */
    public String getSellerWallet() {
        return sellerWallet;
    }

    /**
     * 供 MyBatis-Plus Lambda 查询定位成交买家钱包的 Solana 公钥。
     * Supports MyBatis-Plus Lambda queries that match the Solana public key of the buyer in the sale.
     *
     * @return 成交买家钱包的 Solana 公钥 / Solana public key of the buyer in the sale
     */
    public String getBuyerWallet() {
        return buyerWallet;
    }

    /**
     * 供 MyBatis-Plus Lambda 查询定位最终确认的成交交易签名。
     * Supports MyBatis-Plus Lambda queries that match the signature of the finalized sale transaction.
     *
     * @return 最终确认的成交交易签名 / signature of the finalized sale transaction
     */
    public String getTxSignature() {
        return txSignature;
    }

    /**
     * 支持 MyBatis-Plus Lambda 条件。 / Provides the getter used in MyBatis-Plus Lambda conditions.
     *
     * @return 链上分配的成交顺序号，用于查询成交历史 / on-chain sale sequence number used to order sale history
     */
    public BigInteger getSaleNo() {
        return saleNo;
    }
}
