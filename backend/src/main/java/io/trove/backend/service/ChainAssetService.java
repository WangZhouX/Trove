package io.trove.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.trove.backend.entity.ChainAssetDO;

/**
 * 声明实物和 NFT 的链上绑定投影的基础持久化服务契约。
 * Declares the base persistence service contract for on-chain binding projections for physical items and NFTs.
 */
public interface ChainAssetService extends IService<ChainAssetDO> {
}
