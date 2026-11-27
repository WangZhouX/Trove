package io.trove.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.trove.backend.entity.CustodyItemDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 为 {@code custody_item} 表中的仓储实物及出库状态声明 MyBatis-Plus 映射接口。
 * Declares the MyBatis-Plus mapper for physical custody items and outbound status in {@code custody_item}.
 */
@Mapper
public interface CustodyItemMapper extends BaseMapper<CustodyItemDO> {
}
