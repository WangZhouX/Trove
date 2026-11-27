package io.trove.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.trove.backend.entity.CustodyItemDO;

/**
 * 声明仓储实物及出库状态的基础持久化服务契约。
 * Declares the base persistence service contract for physical custody items and outbound status.
 */
public interface CustodyItemService extends IService<CustodyItemDO> {
}
