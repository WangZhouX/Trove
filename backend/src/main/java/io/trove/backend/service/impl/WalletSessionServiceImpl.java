package io.trove.backend.service.impl;

import io.trove.backend.service.WalletAccountRoleService;
import io.trove.backend.service.WalletAuthService;
import io.trove.backend.service.WalletSessionService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 在单实例进程内保存已验证的短期会话，重启后要求重新签名。
 * Stores verified short-lived sessions in a single backend process, requiring a fresh signature after restart.
 */
@Service
public class WalletSessionServiceImpl implements WalletSessionService {

    /** 当前会话的绝对有效期以及 HTTP Bearer 前缀。 / Absolute session lifetime and HTTP bearer prefix. */
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);
    private static final String BEARER = "Bearer ";

    /** 只存凭证的 SHA-256 摘要，认证时从数据库获取角色。 / Stores SHA-256 token digests and loads roles from the database. */
    private final ConcurrentMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final WalletAccountRoleService roles;

    /**
     * 使用钱包角色表作为授权来源。 / Uses the wallet role table as the authorization source.
     *
     * @param roles 数据库角色查询服务 / database role query service
     */
    public WalletSessionServiceImpl(WalletAccountRoleService roles) {
        this.roles = roles;
    }

    /** {@inheritDoc} */
    @Override
    public SignedIn issue(WalletAuthService.VerifiedWallet verifiedWallet) {
        OffsetDateTime issuedAt = OffsetDateTime.now(ZoneOffset.UTC);
        Session session = new Session(verifiedWallet, issuedAt.plus(SESSION_TTL));
        Identity identity = identity(session);

        // 签发会话时移除过期记录，并防止碰撞覆盖现有有效会话。
        // Clear expired records at issuance and never overwrite an existing session on a token collision.
        sessions.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(issuedAt));
        String token;
        do {
            token = WalletAuthSecrets.randomValue();
        } while (sessions.putIfAbsent(key(token), session) != null);
        return new SignedIn(token, identity);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Identity> authenticate(String authorization, String origin) {
        String tokenKey = authorizationKey(authorization);
        if (tokenKey == null) {
            return Optional.empty();
        }
        Session session = sessions.get(tokenKey);
        if (session == null) {
            return Optional.empty();
        }
        if (!session.expiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            sessions.remove(tokenKey, session);
            return Optional.empty();
        }
        // 跨来源浏览器请求必须保持登录时的 Origin。直接访问时未发送 Origin 的请求仍须有会话凭证。
        // Cross-origin browser requests must match the sign-in origin. Direct requests with no Origin still require a token.
        if (origin != null && !session.verifiedWallet().origin().equals(origin)) {
            return Optional.empty();
        }
        return Optional.of(identity(session));
    }

    /** {@inheritDoc} */
    @Override
    public void revoke(String authorization) {
        String tokenKey = authorizationKey(authorization);
        if (tokenKey != null) {
            sessions.remove(tokenKey);
        }
    }

    /**
     * 使用会话地址查询角色，构造本次请求的身份。 / Resolve roles by session address for the current request identity.
     *
     * @param session 内存中已验证且未过期的会话 / verified unexpired session from memory
     * @return 当前角色与已验证身份 / verified identity with current roles
     */
    private Identity identity(Session session) {
        WalletAuthService.VerifiedWallet wallet = session.verifiedWallet();
        return new Identity(wallet.walletAddress(), wallet.network(), roles.rolesFor(wallet.walletAddress()),
                session.expiresAt());
    }

    /**
     * 解析请求中格式明确的 Bearer 凭证并生成查询键。 / Parse a well-formed bearer credential and derive its lookup key.
     *
     * @param authorization HTTP Authorization 头，可以为空 / nullable HTTP Authorization header
     * @return 存储键或格式不匹配时的空值 / storage key, or null if the format does not match
     */
    private static String authorizationKey(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, BEARER, 0, BEARER.length())) {
            return null;
        }
        String token = authorization.substring(BEARER.length());
        return WalletAuthSecrets.hasExpectedFormat(token) ? key(token) : null;
    }

    /**
     * 以 SHA-256 摘要作为会话索引。 / Index sessions by the SHA-256 token digest.
     *
     * @param token 已校验格式的原始会话凭证 / raw session credential with a valid format
     * @return 摘要的十六进制文本 / hex-encoded digest
     */
    private static String key(String token) {
        return HexFormat.of().formatHex(WalletAuthSecrets.digest(token));
    }

    /**
     * 保存在进程内的会话数据。 / Session data kept in process memory.
     *
     * @param verifiedWallet 签名确定的身份和网络范围 / identity and network scope established by signature verification
     * @param expiresAt UTC 到期时间 / UTC expiration time
     */
    private record Session(WalletAuthService.VerifiedWallet verifiedWallet, OffsetDateTime expiresAt) {
    }
}
