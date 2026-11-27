package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.api.FulfillmentApi;
import io.trove.backend.api.FulfillmentException;
import io.trove.backend.entity.CustodyItemDO;
import io.trove.backend.entity.ShipmentDO;
import io.trove.backend.entity.ShipmentEventDO;
import io.trove.backend.mapper.CustodyItemMapper;
import io.trove.backend.mapper.ShipmentEventMapper;
import io.trove.backend.mapper.ShipmentMapper;
import io.trove.backend.service.ShipmentService;
import io.trove.backend.service.WalletAccountRoleService;
import io.trove.backend.service.WalletSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 管理最终确认兑换后的仓库进度，统一维护任务状态、在库记录和幂等事件。
 * Manages fulfillment after finalized redemptions and maintains shipment state, custody and idempotent events together.
 */
@Service
public class ShipmentServiceImpl extends ServiceImpl<ShipmentMapper, ShipmentDO> implements ShipmentService {

    /** 幂等键、承运商和非敏感原因使用的 ASCII 格式。 / ASCII format for retry keys, carriers and non-sensitive reasons. */
    private static final Pattern CODE = Pattern.compile("[A-Za-z0-9._:-]+");

    /** 运单号允许演示编号和常见物流格式，长度不能超过数据库限制。 / Tracking format accepts demo and common carrier IDs within the database limit. */
    private static final Pattern TRACKING = Pattern.compile("[A-Za-z0-9][A-Za-z0-9 ._-]{0,127}");

    /** 在库实物状态。 / State of an item still held by the warehouse. */
    private static final String IN_VAULT = "IN_VAULT";

    /** 实物已首次交付给承运商。 / State after first dispatch to a carrier. */
    private static final String SHIPPED_OUT = "SHIPPED_OUT";

    /** 事件键和首次记录时间由事件表保存。 / Stores action keys and original action timestamps. */
    private final ShipmentEventMapper events;

    /** 首次发货同步更新实物保管事实。 / Updates physical custody on initial dispatch. */
    private final CustodyItemMapper custody;

    /**
     * 注入履约依赖。 / Injects fulfillment dependencies.
     *
     * @param events 发货事件 Mapper / shipment event mapper
     * @param custody 实物档案 Mapper / physical custody mapper
     */
    public ShipmentServiceImpl(ShipmentEventMapper events, CustodyItemMapper custody) {
        this.events = events;
        this.custody = custody;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public FulfillmentApi.PageResult<FulfillmentApi.ShipmentProgress> listForWarehouse(
            WalletSessionService.Identity identity, FulfillmentApi.ShipmentStatus status, int page, int size) {
        requireOperator(identity);
        FulfillmentViews.validatePage(page, size);
        var tasks = baseMapper.selectQueue(identity.network(), status == null ? null : status.name(), page, size);
        var items = tasks.getRecords().stream().map(FulfillmentViews::shipment).toList();
        return new FulfillmentApi.PageResult<>(items, page, size, tasks.getTotal());
    }

    /**
     * {@inheritDoc}
     *
     * <p>只处理数据库中已关联最终确认兑换凭证的任务。任一写入失败会回滚整个操作。
     * Operates on tasks already linked to a finalized redemption receipt; a failed write rolls back the entire action.
     */
    @Override
    @Transactional
    public FulfillmentApi.EventResult recordEvent(WalletSessionService.Identity identity, UUID shipmentId,
                                                  FulfillmentApi.RecordEventRequest request) {
        requireOperator(identity);
        validateRequest(request);
        // 先查经过授权的任务，再读取该任务的幂等记录。 / Authorize the scoped task before reading its idempotency record.
        var current = requireShipment(identity.network(), shipmentId);
        var existing = events.selectByKey(shipmentId, request.eventKey());
        if (existing != null) {
            return replay(identity, request, existing);
        }

        var next = nextStatus(current, request.type());
        // 以数据库的微秒精度严格推进更新时间，作为并发状态比较的版本。
        // Advance at database microsecond precision to keep the update timestamp usable as a concurrency revision.
        var now = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
        var occurredAt = now.isAfter(current.updatedAt()) ? now : current.updatedAt().plusNanos(1_000);
        var shippedAt = request.type() == FulfillmentApi.EventType.SHIPPED ? occurredAt : null;
        var deliveredAt = request.type() == FulfillmentApi.EventType.DELIVERED ? occurredAt : null;

        // 条件更新只允许当前版本成功，避免并发操作覆盖已经登记的结果。
        // Compare-and-set the observed revision so concurrent work cannot overwrite an action already recorded.
        if (baseMapper.advance(current, next.name(), request.carrierCode(), request.trackingNumber(),
                shippedAt, deliveredAt, occurredAt) != 1) {
            var concurrent = events.selectByKey(shipmentId, request.eventKey());
            if (concurrent != null) {
                return replay(identity, request, concurrent);
            }
            throw new FulfillmentException(FulfillmentException.Error.SHIPMENT_STATUS_CONFLICT);
        }

        if (shippedAt != null) {
            dispatchCustody(current.itemId(), shippedAt);
        }
        // 首次任务变化及事件在同一事务提交，不向重复请求分配新的登记时间。
        // Commit the first task transition with its action and preserve the timestamp on replays.
        var recorded = new ShipmentEventDO(shipmentId, request.eventKey(), request.type().name(),
                identity.walletAddress(), request.reasonCode(), occurredAt);
        events.insert(recorded);
        var updated = requireShipment(identity.network(), shipmentId);
        return result(updated, recorded);
    }

    /**
     * 查找属于当前网络的履约任务。 / Looks up the fulfillment task on the current network.
     *
     * @param network 会话绑定的网络 / network bound to the session
     * @param shipmentId 任务 UUID / task UUID
     * @return 当前网络中的任务 / task on the current network
     * @throws FulfillmentException 任务不存在或属于其他网络 / task does not exist or belongs to another network
     */
    private ShipmentDO requireShipment(String network, UUID shipmentId) {
        var shipment = baseMapper.selectForNetwork(network, shipmentId);
        if (shipment == null) {
            throw new FulfillmentException(FulfillmentException.Error.SHIPMENT_NOT_FOUND);
        }
        return shipment;
    }

    /**
     * 履约服务也检查本次读取到的库管员授权。 / The fulfillment service also checks the currently resolved warehouse role.
     *
     * @param identity 当前钱包会话 / current wallet session
     * @throws FulfillmentException 没有库管员权限 / warehouse access is missing
     */
    private static void requireOperator(WalletSessionService.Identity identity) {
        if (!identity.roles().contains(WalletAccountRoleService.WAREHOUSE_OPERATOR)) {
            throw new FulfillmentException(FulfillmentException.Error.WAREHOUSE_ROLE_REQUIRED);
        }
    }

    /**
     * 验证幂等键及每类履约事件的必填字段。 / Validates the idempotency key and each event type's required fields.
     *
     * @param request 客户端履约操作 / fulfillment action submitted by the client
     * @throws FulfillmentException 有缺失或不适用的字段 / fields are missing or do not apply to the event type
     */
    private static void validateRequest(FulfillmentApi.RecordEventRequest request) {
        if (request == null || request.type() == null || !isCode(request.eventKey(), 128)) {
            throw new FulfillmentException(FulfillmentException.Error.INVALID_FULFILLMENT_REQUEST);
        }
        var dispatch = request.type() == FulfillmentApi.EventType.SHIPPED;
        var exception = request.type() == FulfillmentApi.EventType.EXCEPTION;
        // 每种事件只接受对应的字段，便于在重试时验证操作含义未变。
        // Accept only fields that belong to an action so replays can verify the same operation was requested.
        var shippingValid = dispatch
                ? isCode(request.carrierCode(), 32) && request.trackingNumber() != null
                    && TRACKING.matcher(request.trackingNumber()).matches()
                : request.carrierCode() == null && request.trackingNumber() == null;
        var reasonValid = exception ? isCode(request.reasonCode(), 64) : request.reasonCode() == null;
        if (!shippingValid || !reasonValid) {
            throw new FulfillmentException(FulfillmentException.Error.INVALID_FULFILLMENT_REQUEST);
        }
    }

    /**
     * 校验需要公开格式的标识值。 / Validates the format of a code or identifier.
     *
     * @param value 输入字符串 / supplied string
     * @param maxLength 对应字段的最大字符数 / maximum characters for the field
     * @return 是否为长度可接受的非空标识 / whether the identifier is nonempty and within the limit
     */
    private static boolean isCode(String value, int maxLength) {
        return value != null && value.length() <= maxLength && CODE.matcher(value).matches();
    }

    /**
     * 从当前状态确定本次事件的允许转换。 / Determines the permitted transition from the current state.
     *
     * @param current 已授权任务的当前版本 / current revision of the authorized task
     * @param type 要登记的事件 / action to record
     * @return 事件完成后的任务状态 / task status after the action
     * @throws FulfillmentException 当前状态不允许此事件 / current status does not permit the action
     */
    private static FulfillmentApi.ShipmentStatus nextStatus(ShipmentDO current, FulfillmentApi.EventType type) {
        var status = FulfillmentApi.ShipmentStatus.valueOf(current.status());
        return switch (type) {
            case SHIPPED -> {
                requireStatus(status == FulfillmentApi.ShipmentStatus.READY);
                yield FulfillmentApi.ShipmentStatus.SHIPPED;
            }
            case DELIVERED -> {
                requireStatus(status == FulfillmentApi.ShipmentStatus.SHIPPED);
                yield FulfillmentApi.ShipmentStatus.DELIVERED;
            }
            case EXCEPTION -> {
                requireStatus(status == FulfillmentApi.ShipmentStatus.READY || status == FulfillmentApi.ShipmentStatus.SHIPPED);
                yield FulfillmentApi.ShipmentStatus.EXCEPTION;
            }
            case EXCEPTION_RESOLVED -> {
                requireStatus(status == FulfillmentApi.ShipmentStatus.EXCEPTION);
                // 处理异常只恢复原发货进度，已出库时保留原运单。 / Restore dispatch progress and retain original tracking after shipment.
                yield current.shippedAt() == null ? FulfillmentApi.ShipmentStatus.READY : FulfillmentApi.ShipmentStatus.SHIPPED;
            }
        };
    }

    /**
     * 将不允许的状态转换映射为稳定的冲突错误。 / Maps a forbidden transition to a stable conflict error.
     *
     * @param allowed 当前状态是否满足事件的必要条件 / whether current state meets the action precondition
     * @throws FulfillmentException 不允许在当前状态登记 / action is invalid in the current state
     */
    private static void requireStatus(boolean allowed) {
        if (!allowed) {
            throw new FulfillmentException(FulfillmentException.Error.SHIPMENT_STATUS_CONFLICT);
        }
    }

    /**
     * 在发货事务中首次标记实物出库。 / Marks the initial custody dispatch in the shipment transaction.
     *
     * @param itemId 经任务关联并受限制的实物 UUID / physical item UUID bound to the scoped task
     * @param occurredAt 本次发货登记的 UTC 时间 / UTC dispatch timestamp
     * @throws FulfillmentException 实物不存在或无法从在库状态出库 / item is missing or unavailable for dispatch
     */
    private void dispatchCustody(UUID itemId, OffsetDateTime occurredAt) {
        // 以 IN_VAULT 作为出库条件，失败时前面已推进的任务一并回滚。
        // Dispatch only from IN_VAULT; a failure rolls back the earlier shipment update.
        var updated = custody.update(null, Wrappers.lambdaUpdate(CustodyItemDO.class)
                .eq(CustodyItemDO::getItemId, itemId)
                .eq(CustodyItemDO::getState, IN_VAULT)
                .set(CustodyItemDO::getState, SHIPPED_OUT)
                .set(CustodyItemDO::getShippedOutAt, occurredAt)
                .set(CustodyItemDO::getUpdatedAt, occurredAt));
        if (updated != 1) {
            throw new FulfillmentException(FulfillmentException.Error.CUSTODY_STATE_CONFLICT);
        }
    }

    /**
     * 幂等重试复用原事件，检查相同仓管员、事件类型和参数。 / Replays the original event only for the same operator, type and inputs.
     *
     * @param identity 已授权的仓管员 / authorized operator
     * @param request 当前重试输入 / current retry request
     * @param existing 已变更任务的原始事件 / original persisted event for the task
     * @return 最新任务进度和相同的原事件 / latest progress and the same original event
     * @throws FulfillmentException 重试键先前已用于不同操作 / the retry key is already bound to a different action
     */
    private FulfillmentApi.EventResult replay(WalletSessionService.Identity identity, FulfillmentApi.RecordEventRequest request,
                                              ShipmentEventDO existing) {
        // 任务 UUID 已验证归属。使用单主键读重新取状态，避免会话内复用较早的队列查询结果。
        // The task scope has already been verified. Refresh by primary key to avoid reusing the earlier session-cached lookup.
        var latest = baseMapper.selectById(existing.shipmentId());
        if (latest == null || !latest.network().equals(identity.network())) {
            throw new FulfillmentException(FulfillmentException.Error.SHIPMENT_NOT_FOUND);
        }
        var identical = existing.eventType().equals(request.type().name())
                && Objects.equals(existing.actorWallet(), identity.walletAddress())
                && Objects.equals(existing.reasonCode(), request.reasonCode());
        if (request.type() == FulfillmentApi.EventType.SHIPPED) {
            identical = identical && Objects.equals(latest.carrierCode(), request.carrierCode())
                    && Objects.equals(latest.trackingNumber(), request.trackingNumber());
        }
        if (!identical) {
            throw new FulfillmentException(FulfillmentException.Error.SHIPMENT_EVENT_CONFLICT);
        }
        return result(latest, existing);
    }

    /**
     * 将记录后的状态映射为页面结果。 / Maps the recorded result for the page.
     *
     * @param shipment 已授权的任务进度 / authorized shipment progress
     * @param event 此次或首次重试记录 / new or original retried event
     * @return 当前进度和稳定的事件标识 / current progress and stable event identity
     */
    private static FulfillmentApi.EventResult result(ShipmentDO shipment, ShipmentEventDO event) {
        return new FulfillmentApi.EventResult(FulfillmentViews.shipment(shipment), event.eventKey(),
                FulfillmentApi.EventType.valueOf(event.eventType()), event.occurredAt());
    }
}
