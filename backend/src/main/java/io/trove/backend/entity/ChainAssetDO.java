package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.UUID;

/**
 * 最终确认后形成的 Trove 实物与 NFT 的链上绑定投影，烧毁 NFT 后仍保留。
 * Finalized projection of a Trove physical item NFT binding, retained after the NFT is burned.
 *
 * <p>复合主键为 {@code (network, item_id)}。
 * The composite primary key is {@code (network, item_id)}.
 *
 * @param network 该 NFT 所属的 Solana 网络。
 *        Solana network of the NFT.
 * @param itemId 仓库为实物登记的稳定 UUID。
 *        Stable UUID assigned to the physical item by the warehouse.
 * @param mintAddress NFT 的 Mint 公钥，在所属网络唯一。
 *        NFT mint address, unique within its network.
 * @param itemRecordAddress 记录实物和 Mint 绑定关系的 Trove ItemRecord PDA。
 *        Trove ItemRecord PDA that binds the physical item to the mint.
 * @param collectionMintAddress 铸造登记时验证通过的 Trove 集合 Mint 公钥。
 *        Trove collection mint address verified at registration and minting.
 * @param mintedToWallet 首次铸造的接收钱包公钥，用于追踪铸造来源。
 *        Wallet address that received the NFT at mint time, retained for provenance.
 * @param mintTxSignature 创建绑定的最终确认铸造交易签名。
 *        Finalized mint transaction signature that established the binding.
 * @param mintEventIndex 该铸造交易内的事件序号，从 0 开始。
 *        Zero-based ordinal of the mint event within the transaction.
 */
@TableName("chain_asset")
public record ChainAssetDO(
        @TableField(value = "network", updateStrategy = FieldStrategy.NEVER) String network,
        @TableField(value = "item_id", updateStrategy = FieldStrategy.NEVER) UUID itemId,
        @TableField("mint_address") String mintAddress,
        @TableField("item_record_address") String itemRecordAddress,
        @TableField("collection_mint_address") String collectionMintAddress,
        @TableField("minted_to_wallet") String mintedToWallet,
        @TableField("mint_tx_signature") String mintTxSignature,
        @TableField("mint_event_index") int mintEventIndex
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
