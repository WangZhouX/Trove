package io.trove.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.trove.backend.entity.MarketSaleDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 为 {@code market_sale} 表中的已最终确认的市场成交记录声明 MyBatis-Plus 映射接口。
 * Declares the MyBatis-Plus mapper for finalized marketplace sales in {@code market_sale}.
 */
@Mapper
public interface MarketSaleMapper extends BaseMapper<MarketSaleDO> {
}
