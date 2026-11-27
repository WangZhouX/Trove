package io.trove.backend.service;

import com.baomidou.mybatisplus.spring.service.IService;
import io.trove.backend.api.FulfillmentApi;
import io.trove.backend.entity.ShipmentDO;

import java.util.UUID;

/**
 * 声明兑换发货任务与履约状态的基础持久化服务契约。
 * Declares the base persistence service contract for redemption shipment tasks and fulfillment status.
 */
public interface ShipmentService extends IService<ShipmentDO> {

    /**
     * 查询已获授权的仓库发货队列。 / Lists the warehouse's authorized shipment queue.
     *
     * @param identity 库管员会话及网络 / operator session and network
     * @param status 可选状态筛选 / optional status filter
     * @param page 从 1 开始的页码 / one-based page number
     * @param size 单页条数 / page size
     * @return 当前网络的发货任务摘要 / shipment summaries on the current network
     */
    FulfillmentApi.PageResult<FulfillmentApi.ShipmentProgress> listForWarehouse(
            WalletSessionService.Identity identity, FulfillmentApi.ShipmentStatus status, int page, int size);

    /**
     * 在同一事务内登记一次履约操作及必要的出库。 / Records one fulfillment action and related dispatch in one transaction.
     *
     * @param identity 库管员会话及网络 / operator session and network
     * @param shipmentId 已创建的发货任务 UUID / UUID of an existing shipment
     * @param request 携带稳定幂等键的请求 / request with a stable idempotency key
     * @return 原事件与当前任务进度 / original event and current shipment progress
     */
    FulfillmentApi.EventResult recordEvent(WalletSessionService.Identity identity, UUID shipmentId,
                                           FulfillmentApi.RecordEventRequest request);
}
