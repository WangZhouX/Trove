package io.trove.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.trove.backend.entity.RedemptionReceiptDO;

/**
 * 为 {@code redemption_receipt} 表中的已最终确认的兑换凭证声明 MyBatis-Plus 映射接口。
 * Declares the MyBatis-Plus mapper for finalized redemption receipts in {@code redemption_receipt}.
 */
public interface RedemptionReceiptMapper extends BaseMapper<RedemptionReceiptDO> {
}
