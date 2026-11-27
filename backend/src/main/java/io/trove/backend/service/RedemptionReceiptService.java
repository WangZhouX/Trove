package io.trove.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.trove.backend.entity.RedemptionReceiptDO;

/**
 * 声明已最终确认的兑换凭证的基础持久化服务契约。
 * Declares the base persistence service contract for finalized redemption receipts.
 */
public interface RedemptionReceiptService extends IService<RedemptionReceiptDO> {
}
