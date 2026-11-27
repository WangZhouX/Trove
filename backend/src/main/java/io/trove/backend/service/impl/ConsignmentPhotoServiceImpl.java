package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.entity.ConsignmentPhotoDO;
import io.trove.backend.mapper.ConsignmentPhotoMapper;
import io.trove.backend.service.ConsignmentPhotoService;

/**
 * 将寄存照片引用的基础服务与 {@link ConsignmentPhotoMapper} 关联。
 * Connects the base service for consignment photo references to {@link ConsignmentPhotoMapper}.
 */
public class ConsignmentPhotoServiceImpl extends ServiceImpl<ConsignmentPhotoMapper, ConsignmentPhotoDO>
        implements ConsignmentPhotoService {
}
