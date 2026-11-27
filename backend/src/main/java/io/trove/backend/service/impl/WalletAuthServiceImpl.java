package io.trove.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.trove.backend.api.WalletAuthException;
import io.trove.backend.config.WalletOriginPolicy;
import io.trove.backend.entity.WalletAccountDO;
import io.trove.backend.entity.WalletLoginChallengeDO;
import io.trove.backend.mapper.WalletAccountMapper;
import io.trove.backend.mapper.WalletLoginChallengeMapper;
import io.trove.backend.service.WalletAuthService;
import io.trove.backend.utils.SolanaWalletUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * 使用现有钱包及挑战表实现一次性签名认证。 / Verifies one-time signatures using the wallet and challenge tables.
 */
@Service
public class WalletAuthServiceImpl implements WalletAuthService {

    /** Phantom 提示和提交签名的有效时间。 / Time allowed to approve and submit a Phantom signature. */
    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);

    /** 钱包身份表、挑战表，以及服务端允许的前端来源。 / Wallet table, challenge table, and frontend origin policy. */
    private final WalletAccountMapper walletAccounts;
    private final WalletLoginChallengeMapper challenges;
    private final WalletOriginPolicy origins;

    /**
     * 装配钱包挑战的数据库和来源检查依赖。 / Wire persistence and origin checks for sign-in challenges.
     *
     * @param walletAccounts 钱包身份表 Mapper / wallet identity mapper
     * @param challenges 登录挑战表 Mapper / login challenge mapper
     * @param origins 精确的前端来源校验 / strict frontend origin policy
     */
    public WalletAuthServiceImpl(WalletAccountMapper walletAccounts, WalletLoginChallengeMapper challenges,
                                 WalletOriginPolicy origins) {
        this.walletAccounts = walletAccounts;
        this.challenges = challenges;
        this.origins = origins;
    }

    /**
     * {@inheritDoc}
     * 首次登记使用独立的数据库语句。 / First-time registration runs in independent database statements.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Challenge issueChallenge(String walletAddress, String network, String origin) {
        origins.requireAllowed(origin);
        validateWalletAndNetwork(walletAddress, network);
        ensureAccount(walletAddress);

        // PostgreSQL 的微秒精度在业务层保持一致，签名文本重新构造时得到相同时间。
        // Match PostgreSQL microsecond precision so reconstruction yields identical signed times.
        OffsetDateTime issuedAt = now();
        String nonce = WalletAuthSecrets.randomValue();
        WalletLoginChallengeDO challenge = new WalletLoginChallengeDO(UUID.randomUUID(), walletAddress, network,
                origin, WalletAuthSecrets.digest(nonce), issuedAt, issuedAt.plus(CHALLENGE_TTL), null);
        challenges.insert(challenge);
        return new Challenge(challenge.challengeId(), nonce, message(challenge, nonce), challenge.expiresAt());
    }

    /**
     * {@inheritDoc}
     * 成功消费之前校验签名，写入失败统一拒绝。 / Verify the signature before attempting atomic consumption.
     */
    @Override
    @Transactional
    public VerifiedWallet verifyChallenge(UUID challengeId, String walletAddress, String network, String origin,
                                          String nonce, String signature) {
        origins.requireAllowed(origin);
        validateWalletAndNetwork(walletAddress, network);
        if (challengeId == null || !WalletAuthSecrets.hasExpectedFormat(nonce) || signature == null) {
            throw new WalletAuthException(WalletAuthException.Error.INVALID_REQUEST);
        }

        WalletLoginChallengeDO challenge = challenges.selectById(challengeId);
        if (challenge == null || challenge.consumedAt() != null || !challenge.expiresAt().isAfter(now())
                || !walletAddress.equals(challenge.walletAddress()) || !network.equals(challenge.network())
                || !origin.equals(challenge.domain())
                || !MessageDigest.isEqual(challenge.nonceHash(), WalletAuthSecrets.digest(nonce))
                || !SolanaWalletUtil.verifies(walletAddress, message(challenge, nonce), signature)) {
            throw new WalletAuthException(WalletAuthException.Error.CHALLENGE_REJECTED);
        }

        OffsetDateTime verifiedAt = now();
        // PostgreSQL 在锁等待后重新检查条件，并发提交的同一挑战只更新一次。
        // PostgreSQL rechecks this predicate after lock waits, allowing only one concurrent consumption.
        int consumed = challenges.update(Wrappers.lambdaUpdate(WalletLoginChallengeDO.class)
                .eq(WalletLoginChallengeDO::getChallengeId, challengeId)
                .isNull(WalletLoginChallengeDO::getConsumedAt)
                .gt(WalletLoginChallengeDO::getExpiresAt, verifiedAt)
                .set(WalletLoginChallengeDO::getConsumedAt, verifiedAt));
        if (consumed != 1) {
            throw new WalletAuthException(WalletAuthException.Error.CHALLENGE_REJECTED);
        }
        recordSignIn(walletAddress, verifiedAt);
        return new VerifiedWallet(walletAddress, network, origin);
    }

    /**
     * 预备空的钱包身份行以满足挑战表外键，保留既有账户属性。
     * Ensure that wallet identity exists for the challenge foreign key and preserve existing properties.
     *
     * @param walletAddress 已校验格式的钱包公钥 / wallet address with validated format
     */
    private void ensureAccount(String walletAddress) {
        if (walletAccounts.selectById(walletAddress) != null) {
            return;
        }
        try {
            walletAccounts.insert(new WalletAccountDO(walletAddress, now(), null, null));
        } catch (DuplicateKeyException exception) {
            // 并发首次请求可能抢先登记。单独的 Mapper 调用可重新读取已提交的记录。
            // A concurrent first request may insert the account first. A separate mapper call sees its committed row.
            if (walletAccounts.selectById(walletAddress) == null) {
                throw exception;
            }
        }
    }

    /**
     * 设置首次成功签名时间，并保持最近验证时间单调更新。 / Record first verification and keep last verification monotonic.
     *
     * @param walletAddress 成功验证的钱包 / successfully verified wallet
     * @param verifiedAt UTC 验证时间 / UTC verification time
     */
    private void recordSignIn(String walletAddress, OffsetDateTime verifiedAt) {
        int first = walletAccounts.update(Wrappers.lambdaUpdate(WalletAccountDO.class)
                .eq(WalletAccountDO::getWalletAddress, walletAddress)
                .isNull(WalletAccountDO::getFirstVerifiedAt)
                .set(WalletAccountDO::getFirstVerifiedAt, verifiedAt)
                .set(WalletAccountDO::getLastVerifiedAt, verifiedAt));
        if (first == 0) {
            walletAccounts.update(Wrappers.lambdaUpdate(WalletAccountDO.class)
                    .eq(WalletAccountDO::getWalletAddress, walletAddress)
                    .lt(WalletAccountDO::getLastVerifiedAt, verifiedAt)
                    .set(WalletAccountDO::getLastVerifiedAt, verifiedAt));
        }
    }

    /**
     * 验证钱包及当前支持的演示网络。 / Validate the wallet and supported demo networks.
     *
     * @param walletAddress Base58 公钥 / Base58 public key
     * @param network API 网络代号 / API network identifier
     * @throws WalletAuthException 钱包或网络无效 / if the wallet or network is invalid
     */
    private static void validateWalletAndNetwork(String walletAddress, String network) {
        if (!SolanaWalletUtil.isAddress(walletAddress) || !("devnet".equals(network) || "localnet".equals(network))) {
            throw new WalletAuthException(WalletAuthException.Error.INVALID_REQUEST);
        }
    }

    /**
     * 只用持久化挑战和校验过的 nonce 构造固定换行的登录文本。
     * Build the message with fixed line endings from the stored challenge and verified nonce.
     *
     * @param challenge 数据库中的挑战内容或即将写入的对应记录 / stored challenge or exact record about to be stored
     * @param nonce 已签发且与摘要匹配的随机值 / issued nonce matching the stored digest
     * @return 带签名域、网络和有效期的 UTF-8 文本 / text binding the signing origin, network and expiration
     */
    private static String message(WalletLoginChallengeDO challenge, String nonce) {
        return String.join("\n", "Trove wallet sign-in", "",
                "Sign in to Trove. This message does not submit a transaction.", "",
                "Origin: " + challenge.domain(), "Wallet: " + challenge.walletAddress(),
                "Network: " + challenge.network(), "Challenge: " + challenge.challengeId(), "Nonce: " + nonce,
                "Issued at: " + challenge.issuedAt().toInstant(), "Expires at: " + challenge.expiresAt().toInstant());
    }

    /** @return 与 PostgreSQL 精度一致的当前 UTC 时间 / current UTC time at PostgreSQL precision */
    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }
}
