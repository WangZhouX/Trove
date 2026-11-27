package io.trove.backend.service;

import io.trove.backend.api.WarehouseShipmentRecipientApi;
import io.trove.backend.api.WarehouseShipmentRecipientException;

import java.util.UUID;

/**
 * 声明单笔仓库履约任务的收件资料查询。
 * Declares the recipient lookup for an individual warehouse fulfillment task.
 */
public interface WarehouseShipmentRecipientService {

    /**
     * 仓管员读取当前网络中尚未签收的单笔任务的已确认收件资料。
     * Allows an operator to read confirmed delivery details for one undelivered shipment on the session network.
     *
     * @param identity 会话中的仓管钱包和链上网络 / warehouse wallet and chain network from the session
     * @param shipmentId 目标发货任务编号 / target shipment ID
     * @return 已认证解密的收件人、电话和地址 / authenticated decrypted recipient, phone and address
     * @throws WarehouseShipmentRecipientException 授权、任务资格或解密检查未通过时 / on authorization, eligibility
     *         or decryption failure
     */
    WarehouseShipmentRecipientApi.Recipient get(WalletSessionService.Identity identity, UUID shipmentId);
}
