package io.trove.backend.utils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;

/**
 * 校验 Solana 公钥格式并验证钱包桌面或浏览器签出的 Ed25519 登录消息。
 * Checks Solana public-key formatting and verifies Ed25519 sign-in messages signed by a wallet.
 */
public final class SolanaWalletUtil {

    // Solana 地址使用 Bitcoin Base58 字母表和 32 字节原始公钥。
    // Solana addresses use the Bitcoin Base58 alphabet and a 32-byte raw public key.
    private static final String BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final BigInteger BASE58_RADIX = BigInteger.valueOf(58);
    private static final int PUBLIC_KEY_BYTES = 32;

    // Ed25519 签名为 64 字节，标准 Base64 最长 88 个字符，含填充。
    // An Ed25519 signature is 64 bytes and at most 88 standard Base64 characters with padding.
    private static final String ED25519 = "Ed25519";
    private static final int SIGNATURE_BYTES = 64;
    private static final int MAX_BASE64_SIGNATURE_LENGTH = 88;

    // RFC 8410 SubjectPublicKeyInfo 的固定头部包含 Ed25519 OID 和 32 字节 BIT STRING 标记。
    // The fixed RFC 8410 SubjectPublicKeyInfo prefix identifies Ed25519 and a 32-byte BIT STRING.
    private static final byte[] ED25519_KEY_PREFIX = HexFormat.of().parseHex("302a300506032b6570032100");

    // 小阶点的规范 y 坐标采用 32 字节小端表示，取值参考 libsodium 的 ge25519_has_small_order。
    // Canonical 32-byte little-endian y coordinates of small-order points, from libsodium ge25519_has_small_order.
    private static final Set<String> SMALL_ORDER_Y = Set.of(
            "0000000000000000000000000000000000000000000000000000000000000000",
            "0100000000000000000000000000000000000000000000000000000000000000",
            "26e8958fc2b227b045c3f489f2ef98f0d5dfac05d3c63339b13802886d53fc05",
            "c7176a703d4dd84fba3c0b760d10670f2a2053fa2c39ccc64ec7fd7792ac037a",
            "ecffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f"
    );

    /**
     * 此类只提供无状态方法。
     * Keeps this utility static and stateless.
     */
    private SolanaWalletUtil() {
    }

    /**
     * 校验输入是否为规范的 32 字节 Solana Base58 地址。
     * Tests whether the input is the canonical Base58 encoding of a 32-byte Solana address.
     *
     * @param address Solana 公钥的大小写敏感 Base58 字符串 / case-sensitive Base58 Solana public key
     * @return 地址格式正确时为 true / true when the address has valid formatting
     */
    public static boolean isAddress(String address) {
        return decodeAddress(address) != null;
    }

    /**
     * 对服务端重建的 UTF-8 消息验证钱包签名。
     * Verifies the wallet signature against the UTF-8 message rebuilt by the server.
     *
     * @param address 预期签名钱包的 Base58 公钥 / Base58 public key of the expected signer
     * @param message 按签发规则重建的原始登录消息 / exact sign-in message rebuilt from the issued challenge
     * @param signature 钱包签名的标准 Base64 编码 / standard Base64 encoding of the wallet signature
     * @return 地址、签名和消息匹配时为 true / true when the address, signature, and message match
     * @throws IllegalStateException Java 加密提供者缺少 Ed25519 / if the Java provider lacks Ed25519
     */
    public static boolean verifies(String address, String message, String signature) {
        // 校验地址位宽与编码长度，避免把无效输入交给加密提供者。
        // Validate the key width and encoded length before passing the inputs to the provider.
        byte[] publicKey = decodeAddress(address);
        if (publicKey == null || message == null || signature == null
                || signature.length() > MAX_BASE64_SIGNATURE_LENGTH || isSmallOrder(publicKey)) {
            return false;
        }

        try {
            // Java 标准 Base64 解码器会拒绝 URL-safe 字母表和空白字符。
            // Java's standard Base64 decoder rejects the URL-safe alphabet and whitespace.
            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            if (signatureBytes.length != SIGNATURE_BYTES) {
                return false;
            }

            // 用 X.509 头部把 Solana 原始公钥转换为 JDK 能读取的公开密钥规范。
            // Wrap the Solana raw public key in the X.509 header understood by the JDK.
            byte[] encodedKey = Arrays.copyOf(ED25519_KEY_PREFIX, ED25519_KEY_PREFIX.length + PUBLIC_KEY_BYTES);
            System.arraycopy(publicKey, 0, encodedKey, ED25519_KEY_PREFIX.length, PUBLIC_KEY_BYTES);

            Signature verifier = Signature.getInstance(ED25519);
            verifier.initVerify(KeyFactory.getInstance(ED25519).generatePublic(new X509EncodedKeySpec(encodedKey)));
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(signatureBytes);
        } catch (IllegalArgumentException | InvalidKeySpecException | InvalidKeyException | SignatureException exception) {
            return false;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("The Java security provider does not support Ed25519", exception);
        }
    }

    /**
     * 判断 Ed25519 公钥是否为小阶点，以保证签名证明钱包控制权。
     * Identifies small-order Ed25519 keys so verification can establish wallet ownership.
     *
     * @param publicKey 32 字节压缩公钥 / 32-byte compressed public key
     * @return 公钥为小阶点时为 true / true if the key is a small-order point
     */
    private static boolean isSmallOrder(byte[] publicKey) {
        // 高位编码 x 坐标的奇偶性，y 坐标同时覆盖其两种取值。
        // The highest bit encodes x parity, and the y coordinate covers both choices.
        byte[] coordinate = publicKey.clone();
        coordinate[PUBLIC_KEY_BYTES - 1] &= 0x7f;
        return SMALL_ORDER_Y.contains(HexFormat.of().formatHex(coordinate));
    }

    /**
     * 将 Base58 文本解码为正好 32 字节，保留前导零。
     * Decodes Base58 into exactly 32 bytes, preserving leading zeros.
     *
     * @param address 待解析的公钥文本 / encoded public key to decode
     * @return 原始公钥，格式不合法时为 null / the raw public key, or null for malformed input
     */
    private static byte[] decodeAddress(String address) {
        if (address == null || address.length() < 32 || address.length() > 44) {
            return null;
        }

        // Base58 中每个前导字符 1 表示一个前导零字节，不参与整数位宽计算。
        // Each leading Base58 digit 1 encodes a leading zero byte outside the integer magnitude.
        int leadingZeros = 0;
        while (leadingZeros < address.length() && address.charAt(leadingZeros) == '1') {
            leadingZeros++;
        }

        // 还原其余字符表示的无符号整数。
        // Recover the unsigned integer represented by the remaining digits.
        BigInteger integer = BigInteger.ZERO;
        for (int index = leadingZeros; index < address.length(); index++) {
            int digit = BASE58_ALPHABET.indexOf(address.charAt(index));
            if (digit < 0) {
                return null;
            }
            integer = integer.multiply(BASE58_RADIX).add(BigInteger.valueOf(digit));
        }

        // BigInteger 可能附加符号字节，将其移除后校验编码明确表示 32 字节。
        // Remove BigInteger's optional sign byte and verify that the encoded value represents exactly 32 bytes.
        byte[] magnitude = integer.toByteArray();
        int magnitudeStart = magnitude[0] == 0 ? 1 : 0;
        int magnitudeLength = magnitude.length - magnitudeStart;
        if (leadingZeros + magnitudeLength != PUBLIC_KEY_BYTES) {
            return null;
        }

        byte[] publicKey = new byte[PUBLIC_KEY_BYTES];
        System.arraycopy(magnitude, magnitudeStart, publicKey, leadingZeros, magnitudeLength);
        return publicKey;
    }
}
