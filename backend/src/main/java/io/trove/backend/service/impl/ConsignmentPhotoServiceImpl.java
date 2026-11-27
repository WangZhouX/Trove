package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.api.ConsignmentPhotoApi;
import io.trove.backend.api.ConsignmentPhotoException;
import io.trove.backend.entity.ConsignmentDO;
import io.trove.backend.entity.ConsignmentPhotoDO;
import io.trove.backend.mapper.ConsignmentMapper;
import io.trove.backend.mapper.ConsignmentPhotoMapper;
import io.trove.backend.service.ConsignmentPhotoService;
import io.trove.backend.service.WalletAccountRoleService;
import io.trove.backend.service.WalletSessionService;
import io.trove.backend.utils.ConsignmentPhotoImageUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/** 受理私有照片并限制其归属、上传状态和访问权限。 / Accepts private photos with scoped access and upload-state enforcement. */
@Service
public class ConsignmentPhotoServiceImpl extends ServiceImpl<ConsignmentPhotoMapper, ConsignmentPhotoDO>
        implements ConsignmentPhotoService {

    /** 同一张寄存单允许上传的照片数量。 / Number of photos accepted for one consignment. */
    private static final int MAX_PHOTOS = 8;

    /** 唯一允许寄存人补传照片的状态。 / Only state that permits the depositor to append photos. */
    private static final String PENDING_ARRIVAL = "PENDING_ARRIVAL";

    /** 寄存归属与收货竞争通过寄存单行校验。 / Validates consignment ownership and coordinates with receiving. */
    private final ConsignmentMapper consignments;

    /** 为已授权图像提供后端私有的文件存储。 / Supplies backend-private storage for authorized images. */
    private final PrivatePhotoStorage storage;

    /** 解码完成后才开启写事务，避免上传期间占用数据库锁。 / Starts writes after decoding so uploads do not hold database locks. */
    private final TransactionTemplate writes;

    /**
     * 注入寄存单、私有文件存储和写事务。 / Injects consignment, private storage and write transactions.
     *
     * @param consignments 寄存单 Mapper / consignment mapper
     * @param storage 本机私有照片存储 / local private-photo storage
     * @param transactions 管理 PostgreSQL 连接的事务管理器 / transaction manager for PostgreSQL
     */
    public ConsignmentPhotoServiceImpl(ConsignmentMapper consignments, PrivatePhotoStorage storage,
                                       PlatformTransactionManager transactions) {
        this.consignments = consignments;
        this.storage = storage;
        this.writes = new TransactionTemplate(transactions);
    }

    /** {@inheritDoc} */
    @Override
    public ConsignmentPhotoApi.Photo upload(WalletSessionService.Identity identity, UUID consignmentId,
                                           InputStream source, long contentLength) {
        var current = requireScopedConsignment(identity, consignmentId, false);
        if (!PENDING_ARRIVAL.equals(current.status())) {
            throw uploadClosed();
        }

        // 文件键仅由服务端生成。读请求体前验证存储状态。
        // The server alone generates the file key. Validate storage before reading the body.
        var photoId = UUID.randomUUID();
        var path = storage.path(photoId);
        var image = ConsignmentPhotoImageUtil.readUpload(source, contentLength);
        return writes.execute(transaction -> storePhoto(identity, consignmentId, photoId, path, image));
    }

    /** {@inheritDoc} */
    @Override
    public List<ConsignmentPhotoApi.Photo> list(WalletSessionService.Identity identity, UUID consignmentId,
                                                boolean warehouse) {
        requireScopedConsignment(identity, consignmentId, warehouse);
        return baseMapper.selectForConsignment(consignmentId).stream().map(ConsignmentPhotoServiceImpl::view).toList();
    }

    /** {@inheritDoc} */
    @Override
    public ConsignmentPhotoApi.Content read(WalletSessionService.Identity identity, UUID consignmentId, UUID photoId,
                                           boolean warehouse) {
        requireScopedConsignment(identity, consignmentId, warehouse);
        var photo = baseMapper.selectForConsignment(consignmentId, photoId);
        if (photo == null) {
            throw notFound();
        }
        // 不接受数据库以外的路径，且只允许当前 UUID 对应的固定键与上传支持的媒体类型。
        // Never accept a client path; the stored key must match this UUID and the type must be supported.
        if (!photoId.toString().equals(photo.storageKey())
                || !("image/jpeg".equals(photo.contentType()) || "image/png".equals(photo.contentType()))) {
            throw new ConsignmentPhotoException(ConsignmentPhotoException.Error.PHOTO_STORAGE_UNAVAILABLE);
        }
        return new ConsignmentPhotoApi.Content(photo.contentType(), storage.read(storage.path(photoId)));
    }

    /**
     * 序列化同一寄存单的照片上传和仓库收货，再写入照片条目和内容。
     * Serializes uploads and warehouse receiving on the same consignment, then saves the reference and bytes.
     *
     * @param identity 已认证的寄存人 / authenticated depositor
     * @param consignmentId 当前寄存单编号 / current consignment ID
     * @param photoId 本次新生成的照片编号 / newly generated photo ID
     * @param path 本次生成的私有路径 / generated private path for this upload
     * @param image 已校验的 MIME 与内容 / verified MIME and content
     * @return 新建照片的对外条目 / public-facing reference of the newly stored photo
     * @throws ConsignmentPhotoException 收货已推进、上传超限或存储失败 / receiving advanced, upload capacity was exceeded, or write failed
     */
    private ConsignmentPhotoApi.Photo storePhoto(WalletSessionService.Identity identity, UUID consignmentId,
                                                 UUID photoId, Path path,
                                                 ConsignmentPhotoImageUtil.VerifiedImage image) {
        // 同值赋值只用于获取 PostgreSQL 行锁并重新确认寄存状态，不修改业务更新时间。
        // The same-value update acquires the PostgreSQL row lock and rechecks the state without modifying the workflow timestamp.
        var locked = consignments.update(null, Wrappers.lambdaUpdate(ConsignmentDO.class)
                .eq(ConsignmentDO::getConsignmentId, consignmentId)
                .eq(ConsignmentDO::getDepositorWallet, identity.walletAddress())
                .eq(ConsignmentDO::getStatus, PENDING_ARRIVAL)
                .set(ConsignmentDO::getStatus, PENDING_ARRIVAL));
        if (locked != 1) {
            throw uploadClosed();
        }

        var existing = baseMapper.selectForConsignment(consignmentId);
        var next = existing.isEmpty() ? 0 : existing.getLast().sortOrder() + 1;
        if (existing.size() >= MAX_PHOTOS || next > Short.MAX_VALUE) {
            throw new ConsignmentPhotoException(ConsignmentPhotoException.Error.PHOTO_LIMIT_REACHED);
        }

        // 先登记数据库条目。文件创建失败将抛出异常，条目随事务回滚。
        // Insert the reference first. A failed file write throws and rolls the row back with the transaction.
        var row = new ConsignmentPhotoDO(photoId, consignmentId, photoId.toString(), image.contentType(),
                (short) next, OffsetDateTime.now(ZoneOffset.UTC));
        baseMapper.insert(row);
        storage.write(path, image.bytes());
        registerRollback(path);
        return view(row);
    }

    /**
     * 若已写文件的数据库事务明确回滚，删除这一次创建的对象。
     * Removes the object created by this upload if the database transaction definitely rolls back.
     *
     * @param path 已成功创建的私有路径 / successfully created private file path
     */
    private void registerRollback(Path path) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            /**
             * 只在明确回滚时清理，提交结果未知时保留文件供核对。
             * Cleans up only a confirmed rollback and preserves the file if the commit outcome is unknown.
             *
             * @param status 数据库事务完成状态 / database transaction completion state
             */
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    storage.removeRolledBack(path);
                }
            }
        });
    }

    /**
     * 核验寄存人归属或从仓库入口校验库管员角色。寄存单在铸造前以钱包归属查询。
     * Verifies depositor ownership or the warehouse operator role. Before minting, consignment ownership is wallet-scoped.
     *
     * @param identity 当前认证会话 / current authenticated session
     * @param consignmentId 请求中的寄存单编号 / requested consignment ID
     * @param warehouse 是否由仓库入口调用 / whether called by a warehouse route
     * @return 可访问的寄存单 / accessible consignment
     * @throws ConsignmentPhotoException 未授权或寄存单不可见 / access denied or the consignment is not visible
     */
    private ConsignmentDO requireScopedConsignment(WalletSessionService.Identity identity, UUID consignmentId,
                                                   boolean warehouse) {
        if (warehouse && !identity.roles().contains(WalletAccountRoleService.WAREHOUSE_OPERATOR)) {
            throw new ConsignmentPhotoException(ConsignmentPhotoException.Error.WAREHOUSE_ROLE_REQUIRED);
        }
        var row = consignments.selectOne(Wrappers.lambdaQuery(ConsignmentDO.class)
                .eq(ConsignmentDO::getConsignmentId, consignmentId)
                .eq(!warehouse, ConsignmentDO::getDepositorWallet, identity.walletAddress()));
        if (row == null) {
            throw notFound();
        }
        return row;
    }

    /**
     * 输出对外可用的照片信息，只提供照片编号和展示字段。
     * Builds a client photo reference from the photo ID and display fields.
     *
     * @param row 已经按寄存归属过滤的照片引用 / photo reference scoped to the consignment
     * @return 用于照片列表的条目 / item for the photo list
     */
    private static ConsignmentPhotoApi.Photo view(ConsignmentPhotoDO row) {
        return new ConsignmentPhotoApi.Photo(row.photoId(), row.contentType(), row.sortOrder(), row.uploadedAt());
    }

    /** @return 寄存单或照片不可见的安全错误 / safe error when the consignment or photo is not visible */
    private static ConsignmentPhotoException notFound() {
        return new ConsignmentPhotoException(ConsignmentPhotoException.Error.PHOTO_NOT_FOUND);
    }

    /** @return 待寄入状态已结束的错误 / error when awaiting-arrival status is no longer available */
    private static ConsignmentPhotoException uploadClosed() {
        return new ConsignmentPhotoException(ConsignmentPhotoException.Error.PHOTO_UPLOAD_CLOSED);
    }
}
