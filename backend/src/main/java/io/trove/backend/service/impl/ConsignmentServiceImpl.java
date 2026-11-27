package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.entity.ConsignmentDO;
import io.trove.backend.mapper.ConsignmentMapper;
import io.trove.backend.service.ConsignmentService;

/**
 * 将寄存申请与验收记录的基础服务与 {@link ConsignmentMapper} 关联。
 * Connects the base service for consignment applications and inspection records to {@link ConsignmentMapper}.
 */
public class ConsignmentServiceImpl extends ServiceImpl<ConsignmentMapper, ConsignmentDO>
        implements ConsignmentService {
}
