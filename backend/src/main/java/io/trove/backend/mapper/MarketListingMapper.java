package io.trove.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.trove.backend.entity.MarketListingDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 为 {@code market_listing} 表中的固定价托管挂单投影声明 MyBatis-Plus 映射接口。
 * Declares the MyBatis-Plus mapper for fixed-price escrow listing projections in {@code market_listing}.
 */
@Mapper
public interface MarketListingMapper extends BaseMapper<MarketListingDO> {
}
