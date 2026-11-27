package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import io.trove.backend.api.ConsignmentApi;
import io.trove.backend.api.ConsignmentException;
import io.trove.backend.entity.ConsignmentDO;
import io.trove.backend.entity.CustodyItemDO;
import io.trove.backend.mapper.ConsignmentMapper;
import io.trove.backend.mapper.CustodyItemMapper;
import io.trove.backend.service.ConsignmentService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.trove.backend.api.ConsignmentException.Error.INVALID_REQUEST;
import static io.trove.backend.api.ConsignmentException.Error.NOT_FOUND;
import static io.trove.backend.api.ConsignmentException.Error.PHYSICAL_IDENTITY_CONFLICT;
import static io.trove.backend.api.ConsignmentException.Error.REQUEST_ID_CONFLICT;
import static io.trove.backend.api.ConsignmentException.Error.STATUS_CONFLICT;

/**
 * 管理寄存申请、验收状态与在库实物的事务边界。
 * Manages consignments, inspection transitions and the custody registration transaction.
 */
@Service
public class ConsignmentServiceImpl extends ServiceImpl<ConsignmentMapper, ConsignmentDO>
        implements ConsignmentService {

    /** 受理的藏品分类与凭据类型。 / Supported categories and types of physical evidence. */
    private static final List<String> CATEGORIES = List.of("CARD", "TOY");
    private static final List<String> IDENTITY_KINDS = List.of("GRADING_CERTIFICATE", "ITEM_SERIAL");

    /** 判重时合并 Unicode 空白，保留原申报字段用于展示。 / Collapse Unicode whitespace for deduplication and retain submitted display values. */
    private static final Pattern IDENTITY_WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    /** 分页上限及在库实物的初始状态。 / Page size limit and initial custody state. */
    private static final int MAX_PAGE_SIZE = 100;
    private static final String IN_VAULT = "IN_VAULT";

    /** 关联寄存单和永久实物编号的在库记录。 / Custody records binding a consignment and its permanent identity. */
    private final CustodyItemMapper custodyItems;

    /**
     * 注入验收及查询用的在库 Mapper。 / Inject the custody mapper used by inspection and queries.
     * @param custodyItems 在库实物 Mapper / custody item mapper
     */
    public ConsignmentServiceImpl(CustodyItemMapper custodyItems) {
        this.custodyItems = custodyItems;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ConsignmentApi.Submission submit(String walletAddress, ConsignmentApi.CreateRequest request) {
        ConsignmentApi.CreateRequest validated = validateCreate(request);
        ConsignmentDO existing = findRequest(walletAddress, validated.clientRequestId());
        if (existing != null) {
            return replay(existing, validated);
        }

        String identity = physicalIdentity(validated);
        if (custodyItems.exists(Wrappers.lambdaQuery(CustodyItemDO.class)
                .eq(CustodyItemDO::getPhysicalIdentity, identity))) {
            return replayOrConflict(walletAddress, validated);
        }

        OffsetDateTime createdAt = now();
        ConsignmentDO row = new ConsignmentDO(UUID.randomUUID(), walletAddress, validated.clientRequestId(),
                validated.category(), validated.submittedTitle(), validated.submittedCondition(), validated.identityKind(),
                validated.identityIssuer(), validated.identityNumber(), identity, validated.inboundCarrier(),
                validated.inboundTrackingNumber(), Status.PENDING_ARRIVAL.name(), null, null, null, null, null, null,
                null, createdAt, createdAt);
        try {
            baseMapper.insert(row);
        } catch (DuplicateKeyException exception) {
            // PostgreSQL 拒绝重复编号后，使用独立语句查询并发请求是否已提交。
            // After PostgreSQL rejects a duplicate, an independent statement can read the committed concurrent request.
            return replayOrConflict(walletAddress, validated);
        }
        return new ConsignmentApi.Submission(toDetail(row, null), true);
    }

    /** {@inheritDoc} */
    @Override
    public ConsignmentApi.PageResult listMine(String walletAddress, String status, int page, int size) {
        return listPage(Objects.requireNonNull(walletAddress, "authenticated depositor wallet"), status, page, size);
    }

    /** {@inheritDoc} */
    @Override
    public ConsignmentApi.Detail getMine(String walletAddress, UUID id) {
        // 所属钱包和单据编号同时进入查询，其他钱包的单据同样返回 404。
        // Scope the lookup by both wallet and ID so another wallet's consignment also returns 404.
        ConsignmentDO row = baseMapper.selectOne(Wrappers.lambdaQuery(ConsignmentDO.class)
                .eq(ConsignmentDO::getConsignmentId, id)
                .eq(ConsignmentDO::getDepositorWallet, walletAddress));
        if (row == null) {
            throw new ConsignmentException(NOT_FOUND);
        }
        return detailFor(row);
    }

    /** {@inheritDoc} */
    @Override
    public ConsignmentApi.PageResult listForWarehouse(String status, int page, int size) {
        return listPage(null, status, page, size);
    }

    /** {@inheritDoc} */
    @Override
    public ConsignmentApi.Detail getForWarehouse(UUID id) {
        return detailFor(requireConsignment(id));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ConsignmentApi.Detail receive(String actor, UUID id) {
        ConsignmentDO row = requireStatus(id, Status.PENDING_ARRIVAL);
        OffsetDateTime receivedAt = transitionTime(row);
        advance(transition(row, Status.RECEIVED, receivedAt)
                .set(ConsignmentDO::getReceivedAt, receivedAt)
                .set(ConsignmentDO::getReceivedByWallet, actor));
        return toDetail(requireConsignment(id), null);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ConsignmentApi.Detail approve(String actor, UUID id, ConsignmentApi.ApproveRequest request) {
        if (request == null) {
            throw new ConsignmentException(INVALID_REQUEST);
        }
        String title = requiredText(request.displayTitle(), 120, "核验名称");
        String condition = requiredText(request.verifiedCondition(), 120, "核验品相");
        String location = requiredText(request.warehouseLocation(), 100, "库位");
        ConsignmentDO row = requireStatus(id, Status.RECEIVED);
        OffsetDateTime reviewedAt = transitionTime(row);

        // 先竞争已收货状态，再在同一事务内创建实物档案。任一步失败均回滚验收。
        // Claim the received state before inserting the custody record in the same rollback boundary.
        advance(transition(row, Status.APPROVED_PENDING_MINT, reviewedAt)
                .set(ConsignmentDO::getReviewedAt, reviewedAt)
                .set(ConsignmentDO::getReviewedByWallet, actor));
        UUID itemId = UUID.randomUUID();
        try {
            custodyItems.insert(new CustodyItemDO(itemId, id, row.physicalIdentity(), title, condition, null,
                    location, IN_VAULT, reviewedAt, null, reviewedAt));
        } catch (DuplicateKeyException exception) {
            throw new ConsignmentException(PHYSICAL_IDENTITY_CONFLICT);
        }
        return toDetail(requireConsignment(id), itemId);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ConsignmentApi.Detail reject(String actor, UUID id, ConsignmentApi.RejectRequest request) {
        String reason = requiredText(request == null ? null : request.rejectionReason(), 500, "拒收原因");
        ConsignmentDO row = requireStatus(id, Status.RECEIVED);
        OffsetDateTime reviewedAt = transitionTime(row);
        advance(transition(row, Status.REJECTED_PENDING_RETURN, reviewedAt)
                .set(ConsignmentDO::getReviewedAt, reviewedAt)
                .set(ConsignmentDO::getReviewedByWallet, actor)
                .set(ConsignmentDO::getRejectionReason, reason));
        return toDetail(requireConsignment(id), null);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ConsignmentApi.Detail recordReturn(String actor, UUID id, ConsignmentApi.ReturnRequest request) {
        // 当前退件表结构记录时间和运单，发起者来自已授权会话。
        // The schema records the return time and tracking number, and the actor comes from the authorized session.
        Objects.requireNonNull(actor, "authorized warehouse wallet");
        String tracking = optionalText(request == null ? null : request.returnTrackingNumber(), 120, "退件运单号");
        ConsignmentDO row = requireStatus(id, Status.REJECTED_PENDING_RETURN);
        OffsetDateTime returnedAt = transitionTime(row);
        advance(transition(row, Status.RETURNED, returnedAt)
                .set(ConsignmentDO::getReturnedAt, returnedAt)
                .set(ConsignmentDO::getReturnTrackingNumber, tracking));
        return toDetail(requireConsignment(id), null);
    }

    /**
     * 统一查询分页，并批量补齐已验收单据的实物 UUID。 / Query a page and load approved custody IDs in one batch.
     * @param walletAddress 本人查询的钱包，仓库全量查询时为空 / owner wallet, null for an authorized warehouse query
     * @param status 可选状态过滤 / optional status filter
     * @param page 从 1 开始的页号 / one-based page number
     * @param size 每页条数 / entries per page
     * @return 分页结果，按创建时间和单据编号稳定排序 / page ordered consistently by creation time and ID
     */
    private ConsignmentApi.PageResult listPage(String walletAddress, String status, int page, int size) {
        if (page < 1 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ConsignmentException(INVALID_REQUEST, "页码从 1 开始，每页条数必须在 1 至 100 之间。");
        }
        Status filter = statusFilter(status);
        var query = Wrappers.lambdaQuery(ConsignmentDO.class)
                .eq(walletAddress != null, ConsignmentDO::getDepositorWallet, walletAddress)
                .eq(filter != null, ConsignmentDO::getStatus, filter == null ? null : filter.name())
                .orderBy(true, walletAddress == null, ConsignmentDO::getCreatedAt)
                .orderByAsc(ConsignmentDO::getConsignmentId);
        Page<ConsignmentDO> result = baseMapper.selectPage(new Page<>(page, size), query);
        List<ConsignmentDO> rows = result.getRecords();
        Map<UUID, UUID> itemIds = loadItemIds(rows);
        var items = rows.stream().map(row -> toDetail(row, itemIds.get(row.consignmentId()))).toList();
        return new ConsignmentApi.PageResult(items, page, size, result.getTotal());
    }

    /**
     * 只为本页已验收单据查询关联标识，避免逐单查询。 / Fetch custody IDs only for accepted rows on this page to avoid per-row lookups.
     * @param rows 当前页单据 / current page of consignments
     * @return 寄存编号到实物编号的映射 / mapping from consignment UUID to custody item UUID
     */
    private Map<UUID, UUID> loadItemIds(List<ConsignmentDO> rows) {
        List<UUID> ids = rows.stream().filter(ConsignmentServiceImpl::hasCustody).map(ConsignmentDO::consignmentId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return custodyItems.selectList(Wrappers.lambdaQuery(CustodyItemDO.class)
                        .in(CustodyItemDO::getConsignmentId, ids)).stream()
                .collect(Collectors.toMap(CustodyItemDO::consignmentId, CustodyItemDO::itemId));
    }

    /**
     * 解析可选状态过滤，拒绝未知状态。 / Parse an optional filter and reject unknown states.
     * @param status 请求中的状态 / requested state
     * @return 状态枚举，无过滤时为空 / state, null for no filter
     */
    private static Status statusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return Status.valueOf(status.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new ConsignmentException(INVALID_REQUEST, "寄存状态无效。");
        }
    }

    /**
     * 在提交前校验并规范化输入，保证请求比较和持久化使用同一组值。 / Validate and canonicalize input consistently for replay comparison and persistence.
     * @param request 客户端寄存申请 / client consignment application
     * @return 校验后的申请内容 / validated application values
     */
    private static ConsignmentApi.CreateRequest validateCreate(ConsignmentApi.CreateRequest request) {
        if (request == null || request.clientRequestId() == null) {
            throw new ConsignmentException(INVALID_REQUEST, "请提供寄存申请请求编号。");
        }
        String category = requiredText(request.category(), 16, "藏品类别").toUpperCase(Locale.ROOT);
        String kind = requiredText(request.identityKind(), 24, "凭据类型").toUpperCase(Locale.ROOT);
        if (!CATEGORIES.contains(category) || !IDENTITY_KINDS.contains(kind)) {
            throw new ConsignmentException(INVALID_REQUEST, "藏品类别或凭据类型无效。");
        }
        return new ConsignmentApi.CreateRequest(request.clientRequestId(), category,
                requiredText(request.submittedTitle(), 120, "藏品名称"),
                requiredText(request.submittedCondition(), 120, "申报品相"), kind,
                requiredText(request.identityIssuer(), 100, "签发机构或厂商"),
                requiredText(request.identityNumber(), 120, "证书或序列号"),
                optionalText(request.inboundCarrier(), 80, "寄入承运商"),
                optionalText(request.inboundTrackingNumber(), 120, "寄入运单号"));
    }

    /**
     * 从类型、机构和编号构造永久判重键。 / Build the permanent deduplication key from type, issuer, and identifier.
     * @param request 校验过的申报记录 / validated application
     * @return 保留标点和前导零的组合键 / composite key preserving punctuation and leading zeros
     */
    private static String physicalIdentity(ConsignmentApi.CreateRequest request) {
        String issuer = normalizeIdentityPart(request.identityIssuer());
        String number = normalizeIdentityPart(request.identityNumber());
        if (issuer.isEmpty() || number.isEmpty()) {
            throw new ConsignmentException(INVALID_REQUEST, "签发机构或厂商及证书或序列号不能为空。");
        }

        // 记录机构的长度，使机构和编号包含冒号时也能保持一一对应。
        // Prefix the issuer length to prevent ambiguous concatenation when it or the number contains a colon.
        String identity = "%s:%d:%s:%s".formatted(request.identityKind(), issuer.length(), issuer, number);
        if (identity.codePointCount(0, identity.length()) > 280) {
            throw new ConsignmentException(INVALID_REQUEST, "规范化后的实物编号过长。");
        }
        return identity;
    }

    /**
     * 统一 Unicode 表示、大小写及多余空白。 / Canonicalize Unicode representation, case, and redundant whitespace.
     * @param value 机构或编号的申报值 / submitted issuer or identifier
     * @return 判重组件 / normalized deduplication component
     */
    private static String normalizeIdentityPart(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        return IDENTITY_WHITESPACE.matcher(normalized).replaceAll(" ").strip();
    }

    /**
     * 校验必填文本及其数据库字段上限。 / Validate required text within its database column length.
     * @param value 输入文本 / input text
     * @param maxLength Unicode 字符数上限 / maximum Unicode code points
     * @param label 对用户显示的字段名 / user-facing field label
     * @return 去除首尾空白的输入 / stripped input value
     */
    private static String requiredText(String value, int maxLength, String label) {
        String text = optionalText(value, maxLength, label);
        if (text == null) {
            throw new ConsignmentException(INVALID_REQUEST, label + "不能为空。");
        }
        return text;
    }

    /**
     * 处理可选文本并检查 PostgreSQL 可表示的字符与长度。 / Normalize optional text and enforce PostgreSQL character and length constraints.
     * @param value 可空输入 / optional input
     * @param maxLength Unicode 字符数上限 / maximum Unicode code points
     * @param label 对用户显示的字段名 / user-facing field label
     * @return 去除首尾空白的文本，未填时为空 / stripped text or null when omitted
     */
    private static String optionalText(String value, int maxLength, String label) {
        if (value == null) {
            return null;
        }
        String text = value.strip();
        if (text.isEmpty() || IDENTITY_WHITESPACE.matcher(text).matches()) {
            return null;
        }
        if (text.codePointCount(0, text.length()) > maxLength
                || text.codePoints().anyMatch(point -> point == 0
                || point >= Character.MIN_SURROGATE && point <= Character.MAX_SURROGATE)) {
            throw new ConsignmentException(INVALID_REQUEST, label + "长度或字符无效。");
        }
        return text;
    }

    /**
     * 查找同一钱包的重试记录，包含已退回的历史单据。 / Find a retry for the same wallet, including returned records.
     * @param walletAddress 会话钱包 / authenticated wallet
     * @param clientRequestId 客户端请求 UUID / client request UUID
     * @return 已存在的单据，无记录时为空 / existing consignment or null
     */
    private ConsignmentDO findRequest(String walletAddress, UUID clientRequestId) {
        return baseMapper.selectOne(Wrappers.lambdaQuery(ConsignmentDO.class)
                .eq(ConsignmentDO::getDepositorWallet, walletAddress)
                .eq(ConsignmentDO::getClientRequestId, clientRequestId));
    }

    /**
     * 实物已被占用时再次确认是否为同一钱包的并发重试。 / Check whether a held identity belongs to the same concurrent wallet request.
     * @param walletAddress 会话钱包 / authenticated wallet
     * @param request 校验后的申请内容 / validated application
     * @return 现有单据，仅在原样重试时返回 / existing consignment for an identical retry
     */
    private ConsignmentApi.Submission replayOrConflict(String walletAddress, ConsignmentApi.CreateRequest request) {
        ConsignmentDO concurrent = findRequest(walletAddress, request.clientRequestId());
        if (concurrent == null) {
            throw new ConsignmentException(PHYSICAL_IDENTITY_CONFLICT);
        }
        return replay(concurrent, request);
    }

    /**
     * 相同请求返回现有单据，内容变化时要求新的客户端请求编号。 / Replays unchanged requests and rejects changed payloads using the same request ID.
     * @param existing 已持久化的原单 / stored original consignment
     * @param request 新请求中已校验的数据 / validated retry payload
     * @return 已有寄存单 / prior consignment
     */
    private ConsignmentApi.Submission replay(ConsignmentDO existing, ConsignmentApi.CreateRequest request) {
        var original = new ConsignmentApi.CreateRequest(existing.clientRequestId(), existing.category(),
                existing.submittedTitle(), existing.submittedCondition(), existing.identityKind(), existing.identityIssuer(),
                existing.identityNumber(), existing.inboundCarrier(), existing.inboundTrackingNumber());
        if (!original.equals(request)) {
            throw new ConsignmentException(REQUEST_ID_CONFLICT);
        }
        return new ConsignmentApi.Submission(detailFor(existing), false);
    }

    /**
     * 定位单据，供仓库查询或流转使用。 / Loads a consignment for warehouse views or transitions.
     * @param id 寄存单 UUID / consignment UUID
     * @return 已存在的单据 / existing consignment
     */
    private ConsignmentDO requireConsignment(UUID id) {
        ConsignmentDO row = baseMapper.selectById(id);
        if (row == null) {
            throw new ConsignmentException(NOT_FOUND);
        }
        return row;
    }

    /**
     * 拒绝与单据当前状态不匹配的操作。 / Reject an action that does not match the current state.
     * @param id 寄存单 UUID / consignment UUID
     * @param expected 允许执行操作的状态 / state required by the action
     * @return 已处于期望状态的单据 / consignment in the expected state
     */
    private ConsignmentDO requireStatus(UUID id, Status expected) {
        ConsignmentDO row = requireConsignment(id);
        if (!expected.name().equals(row.status())) {
            throw new ConsignmentException(STATUS_CONFLICT);
        }
        return row;
    }

    /**
     * 根据读取到的源状态构造条件更新。 / Build a conditional update using the observed source state.
     * @param row 操作前的单据 / pre-action consignment
     * @param next 目标状态 / target state
     * @param at 更新的 UTC 时间 / UTC update time
     * @return 已含状态和更新时间的 Lambda 更新条件 / Lambda update applying status and timestamp
     */
    private static LambdaUpdateWrapper<ConsignmentDO> transition(ConsignmentDO row, Status next, OffsetDateTime at) {
        return Wrappers.lambdaUpdate(ConsignmentDO.class)
                .eq(ConsignmentDO::getConsignmentId, row.consignmentId())
                .eq(ConsignmentDO::getStatus, row.status())
                .set(ConsignmentDO::getStatus, next.name())
                .set(ConsignmentDO::getUpdatedAt, at);
    }

    /**
     * 应用状态条件，确保并发操作只能推进一次。 / Apply the status condition so concurrent actions advance the row only once.
     * @param update 带源状态条件及各业务字段的更新 / update with source state predicate and business fields
     */
    private void advance(LambdaUpdateWrapper<ConsignmentDO> update) {
        if (baseMapper.update(update) != 1) {
            throw new ConsignmentException(STATUS_CONFLICT);
        }
    }

    /**
     * 已验收的寄存单具备在库记录。 / An approved consignment has a custody record.
     * @param row 寄存单 / consignment row
     * @return 是否已通过验收 / whether inspection has approved the consignment
     */
    private static boolean hasCustody(ConsignmentDO row) {
        return Status.APPROVED_PENDING_MINT.name().equals(row.status()) || Status.MINTED.name().equals(row.status());
    }

    /**
     * 查询单笔寄存单的实物关联并转换成详情。 / Read the custody association and convert one consignment to detail.
     * @param row 寄存单 / consignment row
     * @return 受权限约束的页面详情 / access-controlled detail for the page
     */
    private ConsignmentApi.Detail detailFor(ConsignmentDO row) {
        CustodyItemDO custody = hasCustody(row) ? custodyItems.selectOne(Wrappers.lambdaQuery(CustodyItemDO.class)
                .eq(CustodyItemDO::getConsignmentId, row.consignmentId())) : null;
        return toDetail(row, custody == null ? null : custody.itemId());
    }

    /**
     * 只返回页面所需的寄存信息和已验收实物编号。 / Project just the consignment fields needed by the page and the accepted item ID.
     * @param row 寄存单 / consignment row
     * @param itemId 验收后的实物 UUID，可空 / nullable custody item UUID after approval
     * @return 寄存详情 / consignment detail
     */
    private static ConsignmentApi.Detail toDetail(ConsignmentDO row, UUID itemId) {
        return new ConsignmentApi.Detail(row.consignmentId(), row.depositorWallet(), row.category(), row.submittedTitle(),
                row.submittedCondition(), row.identityKind(), row.identityIssuer(), row.identityNumber(),
                row.inboundCarrier(), row.inboundTrackingNumber(), row.status(), row.receivedAt(), row.reviewedAt(),
                row.rejectionReason(), row.returnedAt(), row.returnTrackingNumber(), row.createdAt(), row.updatedAt(), itemId);
    }

    /**
     * 保证记录时间不会早于前序事件，即使系统时钟回拨。 / Keep timestamps no earlier than preceding events if the host clock shifts.
     * @param row 操作前的寄存单 / pre-action consignment row
     * @return 以 UTC 表示的流转时间 / UTC transition time
     */
    private static OffsetDateTime transitionTime(ConsignmentDO row) {
        return Stream.of(now(), row.updatedAt(), row.receivedAt(), row.reviewedAt())
                .filter(Objects::nonNull)
                .max(Comparator.comparing(OffsetDateTime::toInstant)).orElseThrow().withOffsetSameInstant(ZoneOffset.UTC);
    }

    /** @return PostgreSQL 微秒精度的当前 UTC 时间 / current UTC time at PostgreSQL microsecond precision */
    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    /** 寄存单使用的数据库状态代码。 / Consignment states persisted in PostgreSQL. */
    private enum Status {
        PENDING_ARRIVAL, RECEIVED, APPROVED_PENDING_MINT, MINTED, REJECTED_PENDING_RETURN, RETURNED
    }
}
