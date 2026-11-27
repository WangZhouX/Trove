package io.trove.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.trove.backend.entity.ChainSyncCursorDO;

/**
 * 声明按网络和程序保存的链上扫描游标的基础持久化服务契约。
 * Declares the base persistence service contract for on-chain scan cursors scoped to a network and program.
 */
public interface ChainSyncCursorService extends IService<ChainSyncCursorDO> {
}
