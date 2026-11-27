package io.trove.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.trove.backend.entity.MarketSaleDO;

/**
 * 声明已最终确认的市场成交记录的基础持久化服务契约。
 * Declares the base persistence service contract for finalized marketplace sales.
 */
public interface MarketSaleService extends IService<MarketSaleDO> {
}
