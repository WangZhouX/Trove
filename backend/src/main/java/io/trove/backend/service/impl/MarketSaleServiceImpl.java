package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.entity.MarketSaleDO;
import io.trove.backend.mapper.MarketSaleMapper;
import io.trove.backend.service.MarketSaleService;

/**
 * 将已最终确认的市场成交记录的基础服务与 {@link MarketSaleMapper} 关联。
 * Connects the base service for finalized marketplace sales to {@link MarketSaleMapper}.
 */
public class MarketSaleServiceImpl extends ServiceImpl<MarketSaleMapper, MarketSaleDO>
        implements MarketSaleService {
}
