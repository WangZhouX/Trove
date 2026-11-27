package io.trove.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.trove.backend.entity.ChainSyncCursorDO;

/**
 * 为 {@code chain_sync_cursor} 表中的按网络和程序保存的链上扫描游标声明 MyBatis-Plus 映射接口。
 * Declares the MyBatis-Plus mapper for on-chain scan cursors scoped to a network and program in {@code chain_sync_cursor}.
 */
public interface ChainSyncCursorMapper extends BaseMapper<ChainSyncCursorDO> {
}
