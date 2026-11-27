package io.trove.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.trove.backend.entity.ChainAssetDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 为 {@code chain_asset} 表中的实物和 NFT 的链上绑定投影声明 MyBatis-Plus 映射接口。
 * Declares the MyBatis-Plus mapper for on-chain binding projections for physical items and NFTs in {@code chain_asset}.
 */
@Mapper
public interface ChainAssetMapper extends BaseMapper<ChainAssetDO> {
}
