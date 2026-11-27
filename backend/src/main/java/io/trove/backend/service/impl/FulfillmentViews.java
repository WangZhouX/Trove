package io.trove.backend.service.impl;

import io.trove.backend.api.FulfillmentApi;
import io.trove.backend.api.FulfillmentException;
import io.trove.backend.entity.ShipmentDO;

/** 兑换和仓库共用的分页边界与履约视图。 / Paging bounds and shipment views shared by redemption and warehouse fulfillment. */
final class FulfillmentViews {

    private FulfillmentViews() {
    }

    /**
     * 为兑换和仓库队列应用相同的分页上限。 / Applies the same pagination limit to redemption and warehouse queues.
     *
     * @param page 从 1 开始的页码 / one-based page number
     * @param size 每页 1 至 100 条 / page size from 1 to 100
     * @throws FulfillmentException 分页超出范围 / pagination is out of range
     */
    static void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new FulfillmentException(FulfillmentException.Error.INVALID_FULFILLMENT_REQUEST);
        }
    }

    /**
     * 生成不含收件资料的进度输出。调用者必须已经验证访问范围。
     * Builds a progress view without recipient details. The caller must already verify access scope.
     *
     * @param shipment 已授权的任务记录 / authorized task record
     * @return 可返回给当前访问者的任务进度 / task progress visible to the current caller
     */
    static FulfillmentApi.ShipmentProgress shipment(ShipmentDO shipment) {
        return new FulfillmentApi.ShipmentProgress(shipment.shipmentId(), shipment.itemId(),
                FulfillmentApi.ShipmentStatus.valueOf(shipment.status()), shipment.carrierCode(),
                shipment.trackingNumber(), shipment.createdAt(), shipment.shippedAt(), shipment.deliveredAt(),
                shipment.updatedAt());
    }
}
