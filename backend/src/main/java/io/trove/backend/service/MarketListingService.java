package io.trove.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.trove.backend.entity.MarketListingDO;

/**
 * 声明固定价托管挂单投影的基础持久化服务契约。
 * Declares the base persistence service contract for fixed-price escrow listing projections.
 */
public interface MarketListingService extends IService<MarketListingDO> {
}
