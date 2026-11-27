package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.entity.ChainSyncCursorDO;
import io.trove.backend.mapper.ChainSyncCursorMapper;
import io.trove.backend.service.ChainSyncCursorService;

/**
 * 将按网络和程序保存的链上扫描游标的基础服务与 {@link ChainSyncCursorMapper} 关联。
 * Connects the base service for on-chain scan cursors scoped to a network and program to {@link ChainSyncCursorMapper}.
 */
public class ChainSyncCursorServiceImpl extends ServiceImpl<ChainSyncCursorMapper, ChainSyncCursorDO>
        implements ChainSyncCursorService {
}
