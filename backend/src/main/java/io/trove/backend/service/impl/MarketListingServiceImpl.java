package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.entity.MarketListingDO;
import io.trove.backend.mapper.MarketListingMapper;
import io.trove.backend.service.MarketListingService;

/**
 * 将固定价托管挂单投影的基础服务与 {@link MarketListingMapper} 关联。
 * Connects the base service for fixed-price escrow listing projections to {@link MarketListingMapper}.
 */
public class MarketListingServiceImpl extends ServiceImpl<MarketListingMapper, MarketListingDO>
        implements MarketListingService {
}
