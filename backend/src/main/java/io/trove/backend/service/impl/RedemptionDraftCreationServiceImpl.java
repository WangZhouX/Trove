package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.trove.backend.api.RedemptionDraftCreationApi;
import io.trove.backend.api.RedemptionDraftCreationException;
import io.trove.backend.entity.ChainAssetDO;
import io.trove.backend.entity.MarketListingDO;
import io.trove.backend.entity.RedemptionDraftDO;
import io.trove.backend.entity.RedemptionReceiptDO;
import io.trove.backend.mapper.ChainAssetMapper;
import io.trove.backend.mapper.CustodyItemMapper;
import io.trove.backend.mapper.MarketListingMapper;
import io.trove.backend.mapper.RedemptionDraftMapper;
import io.trove.backend.mapper.RedemptionReceiptMapper;
import io.trove.backend.service.FinalizedTokenOwnershipService;
import io.trove.backend.service.RedemptionDraftCreationService;
import io.trove.backend.service.WalletSessionService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import static io.trove.backend.api.RedemptionDraftCreationApi.MAX_ADDRESS_CHARACTERS;
import static io.trove.backend.api.RedemptionDraftCreationApi.MAX_NAME_CHARACTERS;
import static io.trove.backend.api.RedemptionDraftCreationApi.MAX_PHONE_CHARACTERS;
import static io.trove.backend.api.RedemptionDraftCreationException.Error.ASSET_INELIGIBLE;
import static io.trove.backend.api.RedemptionDraftCreationException.Error.ASSET_NOT_OWNED;
import static io.trove.backend.api.RedemptionDraftCreationException.Error.AUTHENTICATION_REQUIRED;
import static io.trove.backend.api.RedemptionDraftCreationException.Error.CONCURRENT_UPDATE;
import static io.trove.backend.api.RedemptionDraftCreationException.Error.DRAFT_SIGNING;
import static io.trove.backend.api.RedemptionDraftCreationException.Error.INVALID_REQUEST;
import static io.trove.backend.api.RedemptionDraftCreationException.Error.OWNERSHIP_UNAVAILABLE;

/**
 * 保存私有兑换草稿，使用数据库约束和回滚重试处理同时提交。
 * Saves private redemption drafts, using database constraints and rollback before retry on competing writes.
 */
@Service
public class RedemptionDraftCreationServiceImpl implements RedemptionDraftCreationService {

    /** 新建待签名草稿的期限，以及写入冲突的最大尝试次数。 / New draft lifetime and maximum attempts for contention. */
    private static final Duration DRAFT_LIFETIME = Duration.ofMinutes(30);
    private static final int SAVE_ATTEMPTS = 3;

    /** 草稿数据库状态。 / Stored draft statuses. */
    private static final String DRAFT = "DRAFT";
    private static final String SIGNING = "SIGNING";
    private static final String EXPIRED = "EXPIRED";

    /** 已验证的 Mint 投影和仓库库存。 / Verified mint projection and physical inventory. */
    private final ChainAssetMapper assets;
    private final CustodyItemMapper custody;

    /** 有效的成交挂单和已经确认的兑换凭证。 / Active marketplace listings and finalized redemption receipts. */
    private final MarketListingMapper listings;
    private final RedemptionReceiptMapper receipts;

    /** 兑换草稿写入、链上持有核验和收件人资料加密。 / Draft writes, chain ownership checks and delivery detail encryption. */
    private final RedemptionDraftMapper drafts;
    private final FinalizedTokenOwnershipService ownership;
    private final RedemptionRecipientCipher recipientCipher;

    /** 每次尝试独立提交或回滚的数据库事务。 / Database transaction that commits or rolls back each attempt independently. */
    private final TransactionTemplate draftTransaction;

    /**
     * 注入藏品查询、当前钱包核验和草稿持久化依赖。
     * Injects asset queries, the current ownership verifier and draft persistence dependencies.
     *
     * @param assets 已确认的 NFT 投影 / finalized NFT projection
     * @param custody 仓库库存 / physical inventory
     * @param listings 用于阻断兑换的托管挂单 / escrowed listings that block redemption
     * @param receipts 防止已兑换实物再次申请的凭证 / receipts that prevent another application for a redeemed item
     * @param drafts 草稿持久化 / draft persistence
     * @param ownership 最终确认后的链上持有查询 / finalized chain ownership lookup
     * @param recipientCipher 收件资料认证加密组件 / authenticated encryption for delivery details
     * @param transactions PostgreSQL 事务管理器 / PostgreSQL transaction manager
     */
    public RedemptionDraftCreationServiceImpl(
            ChainAssetMapper assets, CustodyItemMapper custody, MarketListingMapper listings,
            RedemptionReceiptMapper receipts, RedemptionDraftMapper drafts, FinalizedTokenOwnershipService ownership,
            RedemptionRecipientCipher recipientCipher, PlatformTransactionManager transactions) {
        this.assets = assets;
        this.custody = custody;
        this.listings = listings;
        this.receipts = receipts;
        this.drafts = drafts;
        this.ownership = ownership;
        this.recipientCipher = recipientCipher;
        this.draftTransaction = new TransactionTemplate(transactions);
        // PostgreSQL 发生唯一冲突后误用同一个事务会继续失败，重试必须从新事务读取。
        // PostgreSQL aborts a transaction on a unique conflict, so a retry must read in a new transaction.
        this.draftTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** {@inheritDoc} */
    @Override
    public RedemptionDraftCreationApi.Saved save(
            WalletSessionService.Identity identity, UUID itemId, RedemptionDraftCreationApi.CreateRequest request) {
        if (identity == null) {
            throw new RedemptionDraftCreationException(AUTHENTICATION_REQUIRED);
        }
        // 先校验资料和投影，再在数据库事务外执行 RPC，避免持有数据库连接等待网络。
        // Validate the fields and projection before calling RPC outside the write transaction.
        var validated = validate(request);
        var asset = assets.selectOne(Wrappers.lambdaQuery(ChainAssetDO.class)
                .eq(ChainAssetDO::getNetwork, identity.network()).eq(ChainAssetDO::getItemId, itemId));
        if (asset == null) {
            throw new RedemptionDraftCreationException(ASSET_NOT_OWNED);
        }
        requireEligible(asset);
        requireOwnership(identity, asset);

        // 并发插入时由数据库的活动草稿唯一索引选择写入者。回滚后查询已提交的草稿再更新。
        // The partial unique index selects the winning insert; after rollback a retry can see the committed draft.
        for (int attempt = 1; attempt <= SAVE_ATTEMPTS; attempt++) {
            try {
                return Objects.requireNonNull(draftTransaction.execute(
                        transaction -> saveAttempt(identity, asset, validated)));
            } catch (DuplicateKeyException | DraftContention exception) {
                if (attempt == SAVE_ATTEMPTS) {
                    throw new RedemptionDraftCreationException(CONCURRENT_UPDATE);
                }
            }
        }
        throw new IllegalStateException("Redemption draft retry loop exhausted.");
    }

    /**
     * 检查库存、活动挂单与最终确认的兑换凭证。
     * Checks custody, active listings and finalized redemption receipts.
     *
     * @param asset 会话网络中的藏品投影 / asset projection for the session network
     * @throws RedemptionDraftCreationException 藏品不能进入兑换时 / when the asset cannot enter redemption
     */
    private void requireEligible(ChainAssetDO asset) {
        var item = custody.selectById(asset.itemId());
        if (item == null || !"IN_VAULT".equals(item.state())
                || listings.exists(Wrappers.lambdaQuery(MarketListingDO.class)
                        .eq(MarketListingDO::getNetwork, asset.network())
                        .eq(MarketListingDO::getItemId, asset.itemId()).eq(MarketListingDO::getStatus, "ACTIVE"))
                || receipts.exists(Wrappers.lambdaQuery(RedemptionReceiptDO.class)
                        .eq(RedemptionReceiptDO::getNetwork, asset.network())
                        .eq(RedemptionReceiptDO::getItemId, asset.itemId()))) {
            throw new RedemptionDraftCreationException(ASSET_INELIGIBLE);
        }
    }

    /**
     * 用会话钱包的最终确认 SPL 持仓核对目标 Mint。
     * Checks the target mint against the session wallet's finalized SPL holdings.
     *
     * @param identity 钱包及链上网络 / wallet and chain network
     * @param asset 已确认的藏品 Mint 投影 / finalized mint projection
     * @throws RedemptionDraftCreationException 当前钱包未持有或无法可靠核验时 / when the wallet does not hold the
     *         mint or ownership cannot be reliably verified
     */
    private void requireOwnership(WalletSessionService.Identity identity, ChainAssetDO asset) {
        try {
            // 初始铸造接收人不能证明当前所有权，外部转移后的持有人由 RPC 结果决定。
            // The original mint recipient cannot prove current ownership, including after external transfers.
            var snapshot = ownership.ownedMints(identity.network(), identity.walletAddress());
            if (!snapshot.mintAddresses().contains(asset.mintAddress())) {
                throw new RedemptionDraftCreationException(ASSET_NOT_OWNED);
            }
        } catch (FinalizedTokenOwnershipService.Unavailable exception) {
            throw new RedemptionDraftCreationException(OWNERSHIP_UNAVAILABLE);
        }
    }

    /**
     * 在一个事务内编辑有效草稿，或关闭过期草稿并创建新的草稿。
     * Edits a live draft or closes an expired draft and creates a new one in a single transaction.
     *
     * @param identity 本次已通过验证的钱包与网络 / verified wallet and network for this request
     * @param asset 对应的已确认证明 / matching finalized asset projection
     * @param request 已验证的私有资料 / validated private delivery details
     * @return 保存完成的草稿摘要 / saved draft summary
     * @throws DraftContention 竞争状态变更需要回滚重试时 / when a competing state transition requires rollback and retry
     */
    private RedemptionDraftCreationApi.Saved saveAttempt(
            WalletSessionService.Identity identity, ChainAssetDO asset, RedemptionDraftCreationApi.CreateRequest request) {
        // 再次检查本地投影，以便与刚同步的挂单或兑换结果一致。签名前仍需重新核验实时链上状态。
        // Recheck local projections against just-indexed state. The signing workflow must independently revalidate chain state.
        requireEligible(asset);
        // 与数据库微秒精度对齐，保持返回的时间和存储值一致。
        // Match PostgreSQL microsecond precision so returned and persisted times agree.
        var now = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
        var current = drafts.selectOne(Wrappers.lambdaQuery(RedemptionDraftDO.class)
                .eq(RedemptionDraftDO::getNetwork, identity.network())
                .eq(RedemptionDraftDO::getItemId, asset.itemId())
                .eq(RedemptionDraftDO::getRedeemerWallet, identity.walletAddress())
                .in(RedemptionDraftDO::getStatus, DRAFT, SIGNING));
        if (current != null) {
            // 已协签的草稿即使超过应用期限，也必须由未来的链上核对流程决定能否释放。
            // Only the signing reconciliation workflow may release a signed draft, even after its application deadline.
            if (SIGNING.equals(current.status())) {
                throw new RedemptionDraftCreationException(DRAFT_SIGNING);
            }
            if (current.expiresAt().isAfter(now)) {
                return update(current, now, request);
            }
            expire(current, now);
        }

        // 新草稿用独立 UUID 绑定密文与到期时间，失败时旧草稿的过期写入一起回滚。
        // Bind new details and the deadline to a new UUID; a failed insert also rolls back the previous draft's expiration.
        var id = UUID.randomUUID();
        var expiresAt = now.plus(DRAFT_LIFETIME);
        byte[] sealed = recipientCipher.encrypt(id, identity.network(), asset.itemId(), identity.walletAddress(), request);
        var draft = new RedemptionDraftDO(id, identity.network(), asset.itemId(), identity.walletAddress(), DRAFT,
                sealed, null, null, null, expiresAt, now, now);
        drafts.insert(draft);
        return new RedemptionDraftCreationApi.Saved(summary(draft), true);
    }

    /**
     * 替换当前 DRAFT 的密文，保留其 UUID 和原截止时间。
     * Replaces ciphertext on a DRAFT and retains the draft ID and original deadline.
     *
     * @param current 已通过归属校验的可编辑草稿 / editable draft scoped to its owner
     * @param now 本次保存使用的 UTC 时间 / UTC time used for the current save
     * @param request 新的收件资料 / new delivery details
     * @return 已更新的原草稿摘要 / summary of the updated draft
     * @throws DraftContention 更新被状态变更抢先提交时 / when a competing transition commits first
     */
    private RedemptionDraftCreationApi.Saved update(RedemptionDraftDO current, OffsetDateTime now,
                                                    RedemptionDraftCreationApi.CreateRequest request) {
        byte[] sealed = recipientCipher.encrypt(current.draftId(), current.network(), current.itemId(),
                current.redeemerWallet(), request);
        // 原状态和更新时间进入条件，防止签名或其他更新期间覆盖草稿。
        // Match the previous state and update time to avoid overwriting signing or another concurrent change.
        int updated = drafts.update(Wrappers.lambdaUpdate(RedemptionDraftDO.class)
                .eq(RedemptionDraftDO::getDraftId, current.draftId()).eq(RedemptionDraftDO::getStatus, DRAFT)
                .eq(RedemptionDraftDO::getUpdatedAt, current.updatedAt())
                .set(RedemptionDraftDO::getRecipientCiphertext, sealed).set(RedemptionDraftDO::getUpdatedAt, now));
        if (updated != 1) {
            throw new DraftContention();
        }
        return new RedemptionDraftCreationApi.Saved(summary(current), false);
    }

    /**
     * 关闭仍处于 DRAFT 状态的过期草稿，并清除其私有资料。
     * Closes an expired draft that remains in DRAFT and clears its private delivery details.
     *
     * @param current 候选过期草稿 / candidate expired draft
     * @param now UTC 到期判定时点 / UTC instant used for the expiration check
     * @throws DraftContention 状态或更新时间已变更时 / when another transaction changed the state or update time
     */
    private void expire(RedemptionDraftDO current, OffsetDateTime now) {
        int expired = drafts.update(Wrappers.lambdaUpdate(RedemptionDraftDO.class)
                .eq(RedemptionDraftDO::getDraftId, current.draftId()).eq(RedemptionDraftDO::getStatus, DRAFT)
                .eq(RedemptionDraftDO::getUpdatedAt, current.updatedAt()).le(RedemptionDraftDO::getExpiresAt, now)
                .set(RedemptionDraftDO::getStatus, EXPIRED).set(RedemptionDraftDO::getRecipientCiphertext, null)
                .set(RedemptionDraftDO::getUpdatedAt, now));
        if (expired != 1) {
            throw new DraftContention();
        }
    }

    /**
     * 映射已保存草稿的安全摘要。
     * Maps the saved draft to a safe summary.
     *
     * @param draft 已持久化的草稿 / persisted draft
     * @return 不含私有字段或密文的摘要 / summary without private fields or ciphertext
     */
    private static RedemptionDraftCreationApi.Draft summary(RedemptionDraftDO draft) {
        return new RedemptionDraftCreationApi.Draft(draft.draftId(), draft.network(), draft.itemId(),
                draft.status(), draft.expiresAt());
    }

    /**
     * 校验兑换请求中的资料，保持输入仅存在于加密前的服务调用内。
     * Validates delivery details while keeping them inside the request path until encryption.
     *
     * @param request 页面提交的收件资料 / delivery details submitted by the page
     * @return 已移除首尾空白的资料 / details with boundary whitespace removed
     * @throws RedemptionDraftCreationException 必填字段缺失或过长时 / when required fields are missing or too long
     */
    private static RedemptionDraftCreationApi.CreateRequest validate(RedemptionDraftCreationApi.CreateRequest request) {
        if (request == null) {
            throw new RedemptionDraftCreationException(INVALID_REQUEST);
        }
        return new RedemptionDraftCreationApi.CreateRequest(
                requiredText(request.recipientName(), MAX_NAME_CHARACTERS),
                requiredText(request.recipientPhone(), MAX_PHONE_CHARACTERS),
                requiredText(request.recipientAddress(), MAX_ADDRESS_CHARACTERS));
    }

    /**
     * 校验单个必填字段，错误中只保留固定提示。
     * Validates one required field without embedding private values in errors.
     *
     * @param value 原始字段值 / original field value
     * @param maxCharacters 字符数上限，以 Unicode 码点计 / upper limit in Unicode code points
     * @return 规范化后的非空字段 / normalized nonblank value
     * @throws RedemptionDraftCreationException 字符数、编码或空值不符合要求时 / when length, encoding or nullability is invalid
     */
    private static String requiredText(String value, int maxCharacters) {
        if (value == null) {
            throw new RedemptionDraftCreationException(INVALID_REQUEST);
        }
        var stripped = value.strip();
        if (stripped.isEmpty() || stripped.codePointCount(0, stripped.length()) > maxCharacters
                || stripped.codePoints().anyMatch(character -> character == 0
                        || character >= Character.MIN_SURROGATE && character <= Character.MAX_SURROGATE)) {
            throw new RedemptionDraftCreationException(INVALID_REQUEST);
        }
        return stripped;
    }

    /** 内部重试标记，当前写入已被并发状态变更抢先。 / Internal retry marker for a competing committed state change. */
    private static final class DraftContention extends RuntimeException {
    }
}
