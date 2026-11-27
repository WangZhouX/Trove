package io.trove.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.trove.backend.entity.ConsignmentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 为 {@code consignment} 表中的寄存申请与验收记录声明 MyBatis-Plus 映射接口。
 * Declares the MyBatis-Plus mapper for consignment applications and inspection records in {@code consignment}.
 */
@Mapper
public interface ConsignmentMapper extends BaseMapper<ConsignmentDO> {
}
