package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.entity.CustodyItemDO;
import io.trove.backend.mapper.CustodyItemMapper;
import io.trove.backend.service.CustodyItemService;

/**
 * 将仓储实物及出库状态的基础服务与 {@link CustodyItemMapper} 关联。
 * Connects the base service for physical custody items and outbound status to {@link CustodyItemMapper}.
 */
public class CustodyItemServiceImpl extends ServiceImpl<CustodyItemMapper, CustodyItemDO>
        implements CustodyItemService {
}
