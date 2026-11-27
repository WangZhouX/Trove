package io.trove.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.trove.backend.api.ConsignmentPhotoApi;
import io.trove.backend.entity.ConsignmentPhotoDO;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * 声明寄存照片引用的基础持久化服务契约。
 * Declares the base persistence service contract for consignment photo references.
 */
public interface ConsignmentPhotoService extends IService<ConsignmentPhotoDO> {

    /**
     * 当前寄存人为待寄入单据补传一张私有照片。 / The current depositor appends one private photo to an awaiting-arrival consignment.
     *
     * @param identity 已认证的寄存人 / authenticated depositor
     * @param consignmentId 寄存单编号 / consignment ID
     * @param source 需要限长读取的原始 HTTP 请求体 / raw HTTP request body to read with a limit
     * @param contentLength HTTP 声明的字节数，未知为 -1 / declared byte count, or -1 if unknown
     * @return 首次成功写入的照片条目 / newly stored photo entry
     */
    ConsignmentPhotoApi.Photo upload(WalletSessionService.Identity identity, UUID consignmentId,
                                    InputStream source, long contentLength);

    /**
     * 查询当前调用方能访问的寄存照片。 / Lists photos for the consignment accessible to the caller.
     *
     * @param identity 寄存人或库管员会话 / depositor or warehouse operator session
     * @param consignmentId 寄存单编号 / consignment ID
     * @param warehouse 仅仓库路由传入 true，并核验库管员权限 / true only from warehouse routes, requiring the operator role
     * @return 按展示顺序排列的条目，不含内部存储键 / display-ordered entries without their storage keys
     */
    List<ConsignmentPhotoApi.Photo> list(WalletSessionService.Identity identity, UUID consignmentId, boolean warehouse);

    /**
     * 读取当前调用方能查看的一张私有照片。 / Reads one private photo authorized for the current caller.
     *
     * @param identity 寄存人或库管员会话 / depositor or warehouse operator session
     * @param consignmentId 寄存单编号 / consignment ID
     * @param photoId 该寄存单内的照片编号 / photo ID within the consignment
     * @param warehouse 仅仓库路由传入 true，并核验库管员权限 / true only from warehouse routes, requiring the operator role
     * @return 限长的私有图像与已校验的 MIME / bounded private image with its verified MIME
     */
    ConsignmentPhotoApi.Content read(WalletSessionService.Identity identity, UUID consignmentId, UUID photoId,
                                    boolean warehouse);
}
