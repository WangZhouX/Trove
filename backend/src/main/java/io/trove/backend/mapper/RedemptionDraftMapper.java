package io.trove.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.trove.backend.entity.RedemptionDraftDO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

/**
 * 为 {@code redemption_draft} 表中的私有兑换草稿声明 MyBatis-Plus 映射接口。
 * Declares the MyBatis-Plus mapper for private redemption drafts in {@code redemption_draft}.
 */
@Mapper
public interface RedemptionDraftMapper extends BaseMapper<RedemptionDraftDO> {

    /**
     * 按认证网络和钱包查询兑换草稿，时间相同时由 UUID 确定顺序。
     * Query drafts by authenticated network and wallet, using UUID for ties in creation time.
     *
     * @param network 会话绑定的网络 / network bound to the session
     * @param wallet 兑换人的认证钱包 / authenticated redeemer wallet
     * @param page 从 1 开始的页码 / one-based page number
     * @param size 单页条数 / page size
     * @return 有归属约束的草稿分页 / page of drafts scoped to its owner
     */
    default Page<RedemptionDraftDO> selectForRedeemer(String network, String wallet, int page, int size) {
        return selectPage(new Page<>(page, size), Wrappers.lambdaQuery(RedemptionDraftDO.class)
                .eq(RedemptionDraftDO::getNetwork, network)
                .eq(RedemptionDraftDO::getRedeemerWallet, wallet)
                .orderByDesc(RedemptionDraftDO::getCreatedAt, RedemptionDraftDO::getDraftId));
    }
}
