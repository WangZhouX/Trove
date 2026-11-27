package io.trove.backend.service;

import java.util.Set;

/**
 * 查询钱包在 Solana 上已最终确认的标准 SPL Token 持有记录。
 * Reads finalized standard SPL Token holdings for a Solana wallet.
 */
public interface FinalizedTokenOwnershipService {

    /**
     * 查询钱包持有的、余额为一且精度为零的 Mint，供收藏页与 Trove 库存交叉核对。
     * Reads Mint addresses with a balance of one and zero decimals for matching against Trove inventory.
     *
     * @param network 会话绑定的 devnet 或 localnet / devnet or localnet bound to the wallet session
     * @param walletAddress 会话已验证的 Solana 钱包公钥 / verified Solana wallet public key from the session
     * @return 查询的最终确认区块槽和 Mint 地址集合 / finalized context slot and set of Mint addresses
     * @throws Unavailable RPC 无可用结果或结果格式无效 / if RPC could not produce a valid result
     */
    Ownership ownedMints(String network, String walletAddress);

    /**
     * 一次 RPC 响应内的最终确认钱包持有结果。 / Finalized wallet holdings from one RPC response.
     *
     * @param finalizedSlot 查询时 RPC 使用的区块槽 / slot used by the RPC node for this query
     * @param mintAddresses 符合条件的 Mint 公钥集合 / set of qualifying Mint public keys
     */
    record Ownership(long finalizedSlot, Set<String> mintAddresses) {
    }

    /**
     * 最终确认读取失败，业务接口应提示稍后重试。 / Finalized reads failed and can be retried.
     */
    final class Unavailable extends RuntimeException {

        /**
         * 创建可对上游故障进行统一处理的异常。 / Creates a typed error for handling upstream failures.
         */
        public Unavailable() {
            super("Solana finalized ownership is unavailable");
        }
    }
}
