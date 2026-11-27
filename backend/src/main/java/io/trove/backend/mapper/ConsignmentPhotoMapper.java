package io.trove.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.trove.backend.entity.ConsignmentPhotoDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.UUID;

/**
 * 为 {@code consignment_photo} 表中的寄存照片引用声明 MyBatis-Plus 映射接口。
 * Declares the MyBatis-Plus mapper for consignment photo references in {@code consignment_photo}.
 */
@Mapper
public interface ConsignmentPhotoMapper extends BaseMapper<ConsignmentPhotoDO> {

    /**
     * 按展示顺序读取已授权的寄存单照片条目。 / Lists photo references in display order for an authorized consignment.
     *
     * @param consignmentId 服务层已鉴权的寄存单编号 / consignment ID authorized by the service
     * @return 同一寄存单的照片条目 / references for the same consignment
     */
    default List<ConsignmentPhotoDO> selectForConsignment(UUID consignmentId) {
        return selectList(Wrappers.lambdaQuery(ConsignmentPhotoDO.class)
                .eq(ConsignmentPhotoDO::getConsignmentId, consignmentId)
                .orderByAsc(ConsignmentPhotoDO::getSortOrder));
    }

    /**
     * 使用寄存单和照片编号查找同一条照片引用。 / Looks up a photo using both its authorized consignment and photo IDs.
     *
     * @param consignmentId 服务层已鉴权的寄存单编号 / consignment ID authorized by the service
     * @param photoId 照片编号 / photo ID
     * @return 匹配行，编号不存在或不归此单时为空 / matching row, null when unknown or attached to another consignment
     */
    default ConsignmentPhotoDO selectForConsignment(UUID consignmentId, UUID photoId) {
        return selectOne(Wrappers.lambdaQuery(ConsignmentPhotoDO.class)
                .eq(ConsignmentPhotoDO::getConsignmentId, consignmentId)
                .eq(ConsignmentPhotoDO::getPhotoId, photoId));
    }
}
