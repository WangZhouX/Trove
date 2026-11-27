package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 持有人私有兑换草稿及加密收件资料，进入签名后冻结资料。
 * Holder-private redemption draft and encrypted shipping details, frozen after entering signing.
 *
 * @param draftId 服务端生成的随机 UUID，同时作为唯一公开草稿引用。
 *        Server-generated random UUID also used as the unique public draft reference.
 * @param network 藏品所属的 Solana 网络，由同一藏品外键确定。
 *        Solana network of the asset, bound by the asset foreign key.
 * @param itemId 要兑换的稳定实体藏品 UUID。
 *        Stable UUID of the collectible being redeemed.
 * @param redeemerWallet 提交草稿并签名的持有人钱包公钥。
 *        Public wallet address of the holder who creates and signs the draft.
 * @param status 草稿状态，含待签名、签名中、确认成功、执行失败、取消和过期。
 *        Draft status covering editable, signing, confirmed, failed, cancelled, and expired.
 * @param recipientCiphertext 服务端认证加密封装，内含收件人、电话和地址，含解密所需版本引用，履约及保留要求结束后可清除。
 *        Authenticated encrypted envelope containing recipient name, phone, and address with key-version
 *        information, nullable after fulfillment and retention allow removal.
 * @param submittedTxSignature 客户端回传的交易签名，仅用于 RPC 查询，可空表示尚未知。
 *        Client-reported transaction signature used only for RPC lookup, nullable when unknown.
 * @param signingBlockhash 协签交易使用的近期块哈希，未进入签名时可空。
 *        Recent blockhash of the partially signed transaction, null before signing begins.
 * @param lastValidBlockHeight 对应块哈希的最后有效区块高度，无符号整数，未进入签名时可空。
 *        Last valid block height for the blockhash as an unsigned integer, null before signing begins.
 * @param expiresAt 草稿审核授权的应用截止时刻，过期签名还需核对链上交易和块哈希。
 *        Application deadline for draft authorization, with on-chain transaction and blockhash verification
 *        still required for signed drafts.
 * @param createdAt 创建草稿的数据库时间。
 *        Database timestamp when the draft was created.
 * @param updatedAt 应用最后修改草稿的时间。
 *        Application timestamp of the latest draft update.
 */
@TableName("redemption_draft")
public record RedemptionDraftDO(
        @TableId(value = "draft_id", type = IdType.INPUT) UUID draftId,
        @TableField("network") String network,
        @TableField("item_id") UUID itemId,
        @TableField("redeemer_wallet") String redeemerWallet,
        @TableField("status") String status,
        @TableField("recipient_ciphertext") byte[] recipientCiphertext,
        @TableField("submitted_tx_signature") String submittedTxSignature,
        @TableField("signing_blockhash") String signingBlockhash,
        @TableField("last_valid_block_height") BigInteger lastValidBlockHeight,
        @TableField("expires_at") OffsetDateTime expiresAt,
        @TableField("created_at") OffsetDateTime createdAt,
        @TableField("updated_at") OffsetDateTime updatedAt
) {

    /**
     * 供 MyBatis-Plus 查询草稿对应的实物。
     * Exposes the physical item column for MyBatis-Plus queries.
     *
     * @return 实物 UUID / physical item UUID
     */
    public UUID getItemId() {
        return itemId;
    }

    /**
     * 供 MyBatis-Plus 比较和推进草稿状态。
     * Exposes the state for MyBatis-Plus queries and transitions.
     *
     * @return 草稿状态 / draft status
     */
    public String getStatus() {
        return status;
    }

    /**
     * 供 MyBatis-Plus 在编辑和过期流程写入或清除密文。
     * Exposes the encrypted column for edits and expiration in MyBatis-Plus operations.
     *
     * @return 加密的收件资料，清除后为空 / encrypted delivery details, null once cleared
     */
    public byte[] getRecipientCiphertext() {
        return recipientCiphertext;
    }

    /**
     * 供 MyBatis-Plus 比较未签名草稿是否过期。
     * Exposes the deadline for MyBatis-Plus comparisons on unsigned drafts.
     *
     * @return 草稿的应用期限 / application expiration time
     */
    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    /**
     * 供 MyBatis-Plus 检查草稿是否被并发修改。
     * Exposes the last write time for concurrent change checks in MyBatis-Plus.
     *
     * @return 草稿最后修改时间 / last draft update time
     */
    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** @return 关联链上网络 / bound chain network */
    public String getNetwork() {
        return network;
    }
    /** @return 当前兑换人钱包公钥 / current redeemer wallet public key */
    public String getRedeemerWallet() {
        return redeemerWallet;
    }
    /** @return 草稿创建时间 / draft creation time */
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 供 MyBatis-Plus Lambda 查询读取 {@code draft_id} 主键列。
     * Exposes the primary-key column {@code draft_id} for MyBatis-Plus Lambda queries.
     *
     * @return 主键列的当前值。 The current primary-key value.
     */
    public UUID getDraftId() {
        return draftId;
    }
}
