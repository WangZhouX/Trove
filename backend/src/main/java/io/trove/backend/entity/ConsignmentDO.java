package io.trove.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 钱包寄存申报、收货验收、铸造结果及拒收退件记录。
 * Wallet consignment submissions, receiving and inspection, minting results and rejection returns.
 */
@TableName("consignment")
public record ConsignmentDO() {
}
