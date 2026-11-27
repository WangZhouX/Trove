package io.trove.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.trove.backend.entity.ConsignmentDO;

/**
 * 声明寄存申请与验收记录的基础持久化服务契约。
 * Declares the base persistence service contract for consignment applications and inspection records.
 */
public interface ConsignmentService extends IService<ConsignmentDO> {
}
