package io.trove.backend.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * 为挑战 nonce 和会话凭证生成及计算摘要。 / Generates and hashes challenge nonces and session credentials.
 */
final class WalletAuthSecrets {

    /** 256 位熵及其无填充 Base64url 表示。 / 256 bits of entropy and its unpadded Base64url representation. */
    private static final int ENTROPY_BYTES = 32;
    private static final Pattern ENCODED_SECRET = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final SecureRandom RANDOM = new SecureRandom();

    private WalletAuthSecrets() {
    }

    /** @return 无填充 Base64url 随机值 / random unpadded Base64url value */
    static String randomValue() {
        byte[] bytes = new byte[ENTROPY_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 在计算摘要前校验预期长度和字符范围。 / Check length and alphabet before hashing an incoming credential.
     *
     * @param value 收到的 nonce 或会话凭证 / supplied nonce or session credential
     * @return 格式是否匹配 / whether the format matches
     */
    static boolean hasExpectedFormat(String value) {
        return value != null && value.length() == 43 && ENCODED_SECRET.matcher(value).matches();
    }

    /**
     * 计算原始 Base64url 文本的 SHA-256 字节摘要。 / Hash the exact Base64url text with SHA-256.
     *
     * @param value 已验证格式的随机值 / random value with a validated format
     * @return 32 字节摘要 / 32-byte digest
     * @throws IllegalStateException JDK 缺少 SHA-256 / if the JDK does not provide SHA-256
     */
    static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required for wallet authentication", exception);
        }
    }
}
