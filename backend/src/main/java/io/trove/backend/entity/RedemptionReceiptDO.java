package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 已最终确认的链上兑换凭证投影，也是创建发货任务的依据。
 * Projection of a finalized on-chain redemption receipt and prerequisite for shipment creation.
 *
 * <p>复合主键为 {@code (network, item_id)}。
 * The composite primary key is {@code (network, item_id)}.
 *
 * @param network 兑换发生的 Solana 网络。
 *        Solana network on which redemption occurred.
 * @param itemId 已兑换的实体藏品 UUID，每个网络只能登记一次。
 *        Redeemed physical collectible UUID, recorded only once per network.
 * @param draftId 链上凭证包含的随机草稿引用，绑定网络、藏品和兑换人。
 *        Random public draft reference from the on-chain receipt, bound to the network, asset, and redeemer.
 * @param redeemerWallet 链上核验的兑换人钱包公钥，与关联草稿相同。
 *        On-chain-verified redeemer wallet address matching the associated draft.
 * @param mintAddress 在兑换交易中销毁的 NFT Mint 公钥。
 *        NFT Mint address burned by the redemption transaction.
 * @param receiptAddress 链上一次性兑换凭证的 PDA 地址。
 *        PDA address of the on-chain one-time redemption receipt.
 * @param txSignature 经验证的兑换交易签名。
 *        Verified redemption transaction signature.
 * @param eventIndex 指定交易中兑换事件的确定性序号。
 *        Deterministic index of the redemption event within the transaction.
 * @param verifiedAt 后端确认并持久化最终兑换结果的时间。
 *        Timestamp when the backend verified and persisted the finalized redemption.
 */
@TableName("redemption_receipt")
public record RedemptionReceiptDO(
        @TableField(value = "network", updateStrategy = FieldStrategy.NEVER) String network,
        @TableField(value = "item_id", updateStrategy = FieldStrategy.NEVER) UUID itemId,
        @TableField("draft_id") UUID draftId,
        @TableField("redeemer_wallet") String redeemerWallet,
        @TableField("mint_address") String mintAddress,
        @TableField("receipt_address") String receiptAddress,
        @TableField("tx_signature") String txSignature,
        @TableField("event_index") int eventIndex,
        @TableField("verified_at") OffsetDateTime verifiedAt
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
}
