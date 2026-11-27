package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.trove.backend.api.WarehouseShipmentRecipientApi;
import io.trove.backend.api.WarehouseShipmentRecipientException;
import io.trove.backend.entity.RedemptionDraftDO;
import io.trove.backend.entity.RedemptionReceiptDO;
import io.trove.backend.entity.ShipmentDO;
import io.trove.backend.mapper.RedemptionDraftMapper;
import io.trove.backend.mapper.RedemptionReceiptMapper;
import io.trove.backend.mapper.ShipmentMapper;
import io.trove.backend.service.WalletAccountRoleService;
import io.trove.backend.service.WalletSessionService;
import io.trove.backend.service.WarehouseShipmentRecipientService;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

import static io.trove.backend.api.WarehouseShipmentRecipientException.Error.AUTHENTICATION_REQUIRED;
import static io.trove.backend.api.WarehouseShipmentRecipientException.Error.OPERATOR_REQUIRED;
import static io.trove.backend.api.WarehouseShipmentRecipientException.Error.RECIPIENT_UNAVAILABLE;
import static io.trove.backend.api.WarehouseShipmentRecipientException.Error.SHIPMENT_NOT_FOUND;

/**
 * 只通过当前网络中尚未签收的履约任务读取其已确认草稿。
 * Reads confirmed draft details only through an undelivered fulfillment task on the session network.
 */
@Service
public class WarehouseShipmentRecipientServiceImpl implements WarehouseShipmentRecipientService {

    /** 可进行发货或物流异常处理的任务状态。 / Task states that can require dispatch or exception handling. */
    private static final Set<String> AVAILABLE_STATES = Set.of("READY", "SHIPPED", "EXCEPTION");

    /** 发货任务、最终兑换凭证和私有兑换草稿的表映射。 / Mappers for shipments, finalized receipts and private drafts. */
    private final ShipmentMapper shipments;
    private final RedemptionReceiptMapper receipts;
    private final RedemptionDraftMapper drafts;

    /** 按草稿绑定解密密文。 / Decrypts ciphertext with its draft binding. */
    private final RedemptionRecipientCipher cipher;

    /**
     * 注入按任务查单、核对兑换凭证、关联草稿和解密的依赖。
     * Injects the shipment lookup, receipt lookup, bound draft lookup and decryption dependencies.
     *
     * @param shipments 发货任务 Mapper / shipment mapper
     * @param receipts 最终确认兑换凭证 Mapper / finalized redemption receipt mapper
     * @param drafts 私有草稿 Mapper / private draft mapper
     * @param cipher 草稿认证加密组件 / authenticated draft cipher
     */
    public WarehouseShipmentRecipientServiceImpl(
            ShipmentMapper shipments, RedemptionReceiptMapper receipts, RedemptionDraftMapper drafts,
            RedemptionRecipientCipher cipher) {
        this.shipments = shipments;
        this.receipts = receipts;
        this.drafts = drafts;
        this.cipher = cipher;
    }

    /** {@inheritDoc} */
    @Override
    public WarehouseShipmentRecipientApi.Recipient get(WalletSessionService.Identity identity, UUID shipmentId) {
        if (identity == null) {
            throw new WarehouseShipmentRecipientException(AUTHENTICATION_REQUIRED);
        }
        if (!identity.roles().contains(WalletAccountRoleService.WAREHOUSE_OPERATOR)) {
            throw new WarehouseShipmentRecipientException(OPERATOR_REQUIRED);
        }
        var shipment = requireUndelivered(identity.network(), shipmentId);

        // 从最终确认凭证反查精确的四元绑定，拒绝未确认草稿或来自其他钱包或网络的资料。
        // Follow the finalized receipt's exact four-part binding and reject an unconfirmed or differently scoped draft.
        var receipt = receipts.selectOne(Wrappers.lambdaQuery(RedemptionReceiptDO.class)
                .eq(RedemptionReceiptDO::getNetwork, shipment.network())
                .eq(RedemptionReceiptDO::getItemId, shipment.itemId()));
        if (receipt == null) {
            throw new WarehouseShipmentRecipientException(RECIPIENT_UNAVAILABLE);
        }
        var draft = drafts.selectOne(Wrappers.lambdaQuery(RedemptionDraftDO.class)
                .eq(RedemptionDraftDO::getDraftId, receipt.draftId())
                .eq(RedemptionDraftDO::getNetwork, receipt.network())
                .eq(RedemptionDraftDO::getItemId, receipt.itemId())
                .eq(RedemptionDraftDO::getRedeemerWallet, receipt.redeemerWallet())
                .eq(RedemptionDraftDO::getStatus, "CONFIRMED"));
        if (draft == null) {
            throw new WarehouseShipmentRecipientException(RECIPIENT_UNAVAILABLE);
        }
        var details = cipher.decrypt(draft);

        // 单独读取当前任务以观察刚刚提交的签收，避免在并发签收完成后再返回收件资料。
        // Re-read the current task to observe a just-committed delivery before disclosing recipient details.
        requireUndelivered(identity.network(), shipmentId);
        return details;
    }

    /**
     * 查询当前网络尚未完成签收的任务。签收、其他网络及不存在均视为无可读取的任务。
     * Looks up an undelivered task on the current network. Delivered, cross-network and missing tasks are unavailable.
     *
     * @param network 会话绑定的链上网络 / chain network bound to the session
     * @param shipmentId 发货任务编号 / shipment ID
     * @return 当前可核对的任务 / readable shipment task
     * @throws WarehouseShipmentRecipientException 任务不存在或已经签收时 / if the task is missing or delivered
     */
    private ShipmentDO requireUndelivered(String network, UUID shipmentId) {
        var shipment = shipments.selectForNetwork(network, shipmentId);
        if (shipment == null || shipment.deliveredAt() != null || !AVAILABLE_STATES.contains(shipment.status())) {
            throw new WarehouseShipmentRecipientException(SHIPMENT_NOT_FOUND);
        }
        return shipment;
    }
}
