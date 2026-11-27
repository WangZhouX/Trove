package io.trove.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.trove.backend.entity.RedemptionReceiptDO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.UUID;

/**
 * 为 {@code redemption_receipt} 表中的已最终确认的兑换凭证声明 MyBatis-Plus 映射接口。
 * Declares the MyBatis-Plus mapper for finalized redemption receipts in {@code redemption_receipt}.
 */
@Mapper
public interface RedemptionReceiptMapper extends BaseMapper<RedemptionReceiptDO> {

    /**
     * 批量关联当前页草稿的最终确认凭证。
     * Resolve finalized receipts for the drafts on the current page.
     *
     * @param network 会话绑定的网络 / network bound to the session
     * @param wallet 兑换人的认证钱包 / authenticated redeemer wallet
     * @param draftIds 非空的当前页草稿编号 / nonempty draft IDs on the current page
     * @return 该钱包持有的已确认凭证 / finalized receipts owned by the wallet
     */
    default List<RedemptionReceiptDO> selectForDrafts(String network, String wallet, List<UUID> draftIds) {
        return selectList(Wrappers.lambdaQuery(RedemptionReceiptDO.class)
                .eq(RedemptionReceiptDO::getNetwork, network)
                .eq(RedemptionReceiptDO::getRedeemerWallet, wallet)
                .in(RedemptionReceiptDO::getDraftId, draftIds));
    }
}
