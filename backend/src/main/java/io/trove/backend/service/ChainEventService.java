package io.trove.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.trove.backend.api.ChainTransactionApi;
import io.trove.backend.entity.ChainEventDO;

import java.util.Optional;

/**
 * 保存最终确认的链上事件，并提供受钱包范围约束的交易回执查询。
 * Persists finalized chain events and provides wallet-scoped transaction receipt lookups.
 */
public interface ChainEventService extends IService<ChainEventDO> {

    /**
     * 按已认证网络、钱包和交易签名，查询本地已索引的可见事件。
     * Reads locally indexed events visible to the authenticated wallet, restricted to its network and the signature.
     *
     * @param network 会话绑定的 Solana 网络 / Solana network bound to the session
     * @param walletAddress 已认证钱包的 Base58 公钥 / Base58 public key of the authenticated wallet
     * @param signature 已校验格式的 Base58 交易签名 / format-validated Base58 transaction signature
     * @return 可见的最终确认回执，缺少可见索引时为空 / visible finalized receipt, or empty when the visible index is absent
     */
    Optional<ChainTransactionApi.Receipt> findReceipt(String network, String walletAddress, String signature);
}
