package io.trove.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.trove.backend.entity.ShipmentDO;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 为 {@code shipment} 表中的兑换发货任务与履约状态声明 MyBatis-Plus 映射接口。
 * Declares the MyBatis-Plus mapper for redemption shipment tasks and fulfillment status in {@code shipment}.
 */
@Mapper
public interface ShipmentMapper extends BaseMapper<ShipmentDO> {

    /**
     * 查询仓库任务队列，网络和稳定排序覆盖每一页。 / Query the warehouse queue with network scoping and stable ordering.
     *
     * @param network 会话绑定的网络 / network bound to the session
     * @param status 可选的精确状态 / optional exact status
     * @param page 从 1 开始的页码 / one-based page number
     * @param size 单页条数 / page size
     * @return 匹配的仓库任务 / matching warehouse tasks
     */
    default Page<ShipmentDO> selectQueue(String network, String status, int page, int size) {
        return selectPage(new Page<>(page, size), Wrappers.lambdaQuery(ShipmentDO.class)
                .eq(ShipmentDO::getNetwork, network)
                .eq(status != null, ShipmentDO::getStatus, status)
                .orderByDesc(ShipmentDO::getCreatedAt, ShipmentDO::getShipmentId));
    }

    /**
     * 按同一网络的一组已确认实物编号关联任务。 / Find tasks for a set of finalized item IDs on the same network.
     *
     * @param network 会话绑定的网络 / network bound to the session
     * @param itemIds 非空的一组已验证实物编号 / nonempty set of verified item IDs
     * @return 对应发货任务 / associated shipment tasks
     */
    default List<ShipmentDO> selectForItems(String network, Collection<UUID> itemIds) {
        return selectList(Wrappers.lambdaQuery(ShipmentDO.class)
                .eq(ShipmentDO::getNetwork, network)
                .in(ShipmentDO::getItemId, itemIds));
    }

    /**
     * 仓库只能定位当前网络的任务。 / Resolve warehouse tasks on the current network only.
     *
     * @param network 会话绑定的网络 / network bound to the session
     * @param shipmentId 发货任务编号 / shipment task identifier
     * @return 匹配的发货任务，未知或其他网络时为空 / matching task, null if unknown or on another network
     */
    default ShipmentDO selectForNetwork(String network, UUID shipmentId) {
        return selectOne(Wrappers.lambdaQuery(ShipmentDO.class)
                .eq(ShipmentDO::getShipmentId, shipmentId)
                .eq(ShipmentDO::getNetwork, network));
    }

    /**
     * 按读到的状态和更新时间推进任务，更新失败时由业务层检查并发重试。
     * Advance the task from the observed state and revision time; the service handles concurrent retries.
     *
     * @param current 读取到的版本 / observed task revision
     * @param nextStatus 下一个任务状态 / next task state
     * @param carrierCode 首次登记发货的承运商 / carrier code for the first dispatch
     * @param trackingNumber 首次登记发货的运单号 / tracking number for the first dispatch
     * @param shippedAt 首次登记发货时间，否则为空 / initial dispatch time, otherwise null
     * @param deliveredAt 此次登记的签收时间，否则为空 / delivery time recorded by this action, otherwise null
     * @param updatedAt 本操作时间 / timestamp of the action
     * @return 更新的任务行数 / affected shipment count
     */
    default int advance(ShipmentDO current, String nextStatus, String carrierCode, String trackingNumber,
                        OffsetDateTime shippedAt, OffsetDateTime deliveredAt, OffsetDateTime updatedAt) {
        return update(null, Wrappers.lambdaUpdate(ShipmentDO.class)
                .eq(ShipmentDO::getShipmentId, current.shipmentId())
                .eq(ShipmentDO::getNetwork, current.network())
                .eq(ShipmentDO::getStatus, current.status())
                .eq(ShipmentDO::getUpdatedAt, current.updatedAt())
                .set(ShipmentDO::getStatus, nextStatus)
                .set(ShipmentDO::getUpdatedAt, updatedAt)
                // 异常或签收保留原始运单和出库时间。 / Preserve original tracking and dispatch on exceptions and delivery.
                .set(shippedAt != null, ShipmentDO::getCarrierCode, carrierCode)
                .set(shippedAt != null, ShipmentDO::getTrackingNumber, trackingNumber)
                .set(shippedAt != null, ShipmentDO::getShippedAt, shippedAt)
                .set(deliveredAt != null, ShipmentDO::getDeliveredAt, deliveredAt));
    }
}
