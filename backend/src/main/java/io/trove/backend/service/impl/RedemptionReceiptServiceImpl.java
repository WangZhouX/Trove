package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.entity.RedemptionReceiptDO;
import io.trove.backend.mapper.RedemptionReceiptMapper;
import io.trove.backend.service.RedemptionReceiptService;

/**
 * 将已最终确认的兑换凭证的基础服务与 {@link RedemptionReceiptMapper} 关联。
 * Connects the base service for finalized redemption receipts to {@link RedemptionReceiptMapper}.
 */
public class RedemptionReceiptServiceImpl extends ServiceImpl<RedemptionReceiptMapper, RedemptionReceiptDO>
        implements RedemptionReceiptService {
}
