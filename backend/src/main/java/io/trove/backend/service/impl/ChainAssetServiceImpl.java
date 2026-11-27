package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.entity.ChainAssetDO;
import io.trove.backend.mapper.ChainAssetMapper;
import io.trove.backend.service.ChainAssetService;

/**
 * 将实物和 NFT 的链上绑定投影的基础服务与 {@link ChainAssetMapper} 关联。
 * Connects the base service for on-chain binding projections for physical items and NFTs to {@link ChainAssetMapper}.
 */
public class ChainAssetServiceImpl extends ServiceImpl<ChainAssetMapper, ChainAssetDO>
        implements ChainAssetService {
}
