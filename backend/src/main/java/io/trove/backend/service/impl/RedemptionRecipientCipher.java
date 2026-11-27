package io.trove.backend.service.impl;

import io.trove.backend.api.RedemptionDraftCreationApi;
import io.trove.backend.api.RedemptionDraftCreationException;
import io.trove.backend.api.WarehouseShipmentRecipientApi;
import io.trove.backend.api.WarehouseShipmentRecipientException;
import io.trove.backend.entity.RedemptionDraftDO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

import static io.trove.backend.api.RedemptionDraftCreationApi.MAX_ADDRESS_CHARACTERS;
import static io.trove.backend.api.RedemptionDraftCreationApi.MAX_NAME_CHARACTERS;
import static io.trove.backend.api.RedemptionDraftCreationApi.MAX_PHONE_CHARACTERS;
import static io.trove.backend.api.RedemptionDraftCreationException.Error.RECIPIENT_PROTECTION_UNAVAILABLE;
import static io.trove.backend.api.WarehouseShipmentRecipientException.Error.RECIPIENT_UNAVAILABLE;

/**
 * 在收件资料入库前生成与草稿身份绑定的认证加密封装。
 * Creates an authenticated envelope bound to a draft's identity before storing delivery details.
 */
@Component
public class RedemptionRecipientCipher {

    /** 当前加密格式和密钥版本的标识字节。 / Identifier byte for the current envelope format and key version. */
    private static final byte VERSION = 1;

    /** AES-256 密钥长度及 GCM nonce、认证标签长度。 / AES-256 key size and GCM nonce and authentication tag lengths. */
    private static final int KEY_BYTES = 32;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    /** 封装的头部长度和可解密的字节上限。 / Header length and maximum envelope byte count. */
    private static final int HEADER_BYTES = 1 + NONCE_BYTES;
    private static final int MAX_ENVELOPE_BYTES = HEADER_BYTES + TAG_BITS / Byte.SIZE + 3 * Integer.BYTES
            + 4 * (MAX_NAME_CHARACTERS + MAX_PHONE_CHARACTERS + MAX_ADDRESS_CHARACTERS);

    /** 每次加密生成独立 nonce 的安全随机源。 / Secure random source that generates a fresh nonce per encryption. */
    private final SecureRandom random = new SecureRandom();

    /** 由服务端配置的 AES 密钥，缺失或无效时为空。 / Server-configured AES key, null when unavailable or invalid. */
    private final SecretKey key;

    /**
     * 从服务端环境读取唯一的版本 1 AES-256 密钥。缺少配置时应用仍可运行，保存草稿会失败。
     * Reads the version 1 AES-256 key from the server environment. With no key, startup is allowed and draft saves fail.
     *
     * @param configuredKey 标准 Base64 表示的 32 字节密钥 / standard Base64 encoding of the 32-byte key
     */
    public RedemptionRecipientCipher(
            @Value("${TROVE_REDEMPTION_RECIPIENT_KEY_BASE64:}") String configuredKey) {
        this.key = readKey(configuredKey);
    }

    /**
     * 认证加密收件资料，并把版本、草稿及所属网络、藏品、钱包绑定到密文。
     * Authenticates and encrypts delivery details, binding the envelope version and draft, network, asset and wallet.
     *
     * @param draftId 资料所属草稿 UUID / UUID of the draft that owns these details
     * @param network 草稿的认证链上网络 / authenticated network of the draft
     * @param itemId 草稿关联的实物 UUID / physical item UUID for the draft
     * @param wallet 草稿所属钱包公钥 / public key of the wallet that owns the draft
     * @param request 已校验的收件资料 / validated delivery details
     * @return 版本字节、随机 nonce、密文与认证标签 / version byte, random nonce, ciphertext and authentication tag
     * @throws RedemptionDraftCreationException 无法安全保存资料时 / when the data cannot be protected
     */
    byte[] encrypt(UUID draftId, String network, UUID itemId, String wallet,
                   RedemptionDraftCreationApi.CreateRequest request) {
        if (key == null) {
            throw new RedemptionDraftCreationException(RECIPIENT_PROTECTION_UNAVAILABLE);
        }

        // 每一版草稿资料都重新取 nonce，更新同一草稿也不会复用。
        // Use a new nonce for each set of details, including updates to the same draft.
        byte[] nonce = new byte[NONCE_BYTES];
        random.nextBytes(nonce);
        byte[] plaintext = encode(request);
        try {
            // 认证附加数据绑定密文归属，替换封装版本或把密文搬到另一草稿都会破坏认证。
            // AAD authenticates the envelope version and ownership, so transplanting the envelope fails authentication.
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            bind(cipher, draftId, network, itemId, wallet);
            byte[] sealed = cipher.doFinal(plaintext);
            return ByteBuffer.allocate(1 + nonce.length + sealed.length).put(VERSION).put(nonce).put(sealed).array();
        } catch (GeneralSecurityException exception) {
            // 为请求方提供固定错误，避免序列化密码学异常或输入。
            // Use fixed feedback and keep cryptographic exceptions and inputs out of the response.
            throw new RedemptionDraftCreationException(RECIPIENT_PROTECTION_UNAVAILABLE);
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    /**
     * 仅供已确认仓库任务解密其草稿中的收件资料。
     * Decrypts a draft's delivery details for a verified warehouse task.
     *
     * @param draft 仓库任务关联的已确认草稿与完整绑定 / confirmed draft and complete binding for the shipment
     * @return 经认证的收件资料 / authenticated recipient details
     * @throws WarehouseShipmentRecipientException 缺少密钥或密文无法认证时 / when the key is missing or authentication fails
     */
    WarehouseShipmentRecipientApi.Recipient decrypt(RedemptionDraftDO draft) {
        // 在密码学解析前检查版本和长度，任何错误统一为不包含内部原因的响应。
        // Check version and size before parsing and use the same opaque failure for any decryption error.
        byte[] envelope = draft.recipientCiphertext();
        if (key == null || envelope == null || envelope.length <= HEADER_BYTES + TAG_BITS / Byte.SIZE
                || envelope.length > MAX_ENVELOPE_BYTES || envelope[0] != VERSION) {
            throw new WarehouseShipmentRecipientException(RECIPIENT_UNAVAILABLE);
        }
        try {
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, envelope, 1, NONCE_BYTES));
            bind(cipher, draft.draftId(), draft.network(), draft.itemId(), draft.redeemerWallet());
            // 先验证 GCM 标签，只有认证后的数据才能被解析为字符串。
            // Authenticate the GCM tag before parsing any plaintext into strings.
            byte[] plaintext = cipher.doFinal(envelope, HEADER_BYTES, envelope.length - HEADER_BYTES);
            try {
                return decode(plaintext);
            } finally {
                Arrays.fill(plaintext, (byte) 0);
            }
        } catch (GeneralSecurityException | CharacterCodingException exception) {
            throw new WarehouseShipmentRecipientException(RECIPIENT_UNAVAILABLE);
        }
    }

    /**
     * 在加密和解密中使用完全相同的版本及草稿附加认证数据。
     * Uses identical version and draft-associated authentication data for encryption and decryption.
     *
     * @param cipher 初始化完成的 AES-GCM 实例 / initialized AES-GCM instance
     * @param draftId 接收或读取资料的草稿 UUID / UUID of the draft receiving or reading the details
     * @param network 草稿关联的链上网络 / chain network of the draft
     * @param itemId 草稿关联的实物 UUID / physical item UUID of the draft
     * @param wallet 草稿的原始兑换钱包 / original redeemer wallet for the draft
     */
    private static void bind(Cipher cipher, UUID draftId, String network, UUID itemId, String wallet) {
        cipher.updateAAD(new byte[] {VERSION});
        cipher.updateAAD(("trove-redemption-recipient\0" + network + "\0" + itemId + "\0" + draftId + "\0" + wallet)
                .getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 读取三段经过认证的 UTF-8 字段，并拒绝额外数据。
     * Reads the three authenticated UTF-8 fields and rejects trailing data.
     *
     * @param plaintext 已认证的密文解封结果 / plaintext from the authenticated envelope
     * @return 可返回给授权仓管员的收件资料 / details for the authorized warehouse operator
     * @throws CharacterCodingException 字节不是有效的 UTF-8 时 / if bytes are invalid UTF-8
     */
    private static WarehouseShipmentRecipientApi.Recipient decode(byte[] plaintext) throws CharacterCodingException {
        var fields = ByteBuffer.wrap(plaintext);
        String recipientName = decodeField(fields, MAX_NAME_CHARACTERS);
        String recipientPhone = decodeField(fields, MAX_PHONE_CHARACTERS);
        String recipientAddress = decodeField(fields, MAX_ADDRESS_CHARACTERS);
        if (fields.hasRemaining()) {
            throw new WarehouseShipmentRecipientException(RECIPIENT_UNAVAILABLE);
        }
        return new WarehouseShipmentRecipientApi.Recipient(recipientName, recipientPhone, recipientAddress);
    }

    /**
     * 读取单段长度前缀，并检查 UTF-8 文本和字符数。
     * Reads a length-prefixed field and checks its UTF-8 text and character count.
     *
     * @param fields 位于下一字段长度前缀的缓冲区 / buffer positioned at the next field's length
     * @param maxCharacters 当前字段允许的最大码点数 / maximum code points allowed for this field
     * @return 字段文本 / decoded field text
     * @throws CharacterCodingException 字节不是有效的 UTF-8 时 / if bytes are invalid UTF-8
     */
    private static String decodeField(ByteBuffer fields, int maxCharacters) throws CharacterCodingException {
        if (fields.remaining() < Integer.BYTES) {
            throw new WarehouseShipmentRecipientException(RECIPIENT_UNAVAILABLE);
        }
        // 一个 UTF-8 码点最多四字节；在构造字符串前检查长度边界。
        // A UTF-8 code point uses at most four bytes, so check length before constructing the string.
        int length = fields.getInt();
        if (length <= 0 || length > 4 * maxCharacters || length > fields.remaining()) {
            throw new WarehouseShipmentRecipientException(RECIPIENT_UNAVAILABLE);
        }
        String decoded = StandardCharsets.UTF_8.newDecoder()
                .decode(fields.slice(fields.position(), length)).toString();
        fields.position(fields.position() + length);
        if (decoded.isBlank() || decoded.indexOf('\0') >= 0
                || decoded.codePointCount(0, decoded.length()) > maxCharacters) {
            throw new WarehouseShipmentRecipientException(RECIPIENT_UNAVAILABLE);
        }
        return decoded;
    }

    /**
     * 将配置解码成指定长度的 AES 密钥，避免在异常中传播配置值。
     * Converts the configuration to an AES key of the required size without exposing the configured value in errors.
     *
     * @param configuredKey 可选标准 Base64 服务端配置 / optional standard Base64 server configuration
     * @return 长度符合要求的 AES 密钥；无效时返回空 / sized AES key, or null for invalid configuration
     */
    private static SecretKey readKey(String configuredKey) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(configuredKey.strip());
        } catch (IllegalArgumentException exception) {
            return null;
        }
        try {
            return bytes.length == KEY_BYTES ? new SecretKeySpec(bytes, "AES") : null;
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    /**
     * 以 4 字节长度前缀保存姓名、电话、地址三个 UTF-8 字段。
     * Encodes the name, phone and address as three UTF-8 fields with four-byte length prefixes.
     *
     * @param request 待保护的验证后收件资料 / validated delivery details to protect
     * @return 待加密的字节数组，由调用方清理 / plaintext bytes, cleared by the caller
     */
    private static byte[] encode(RedemptionDraftCreationApi.CreateRequest request) {
        // 各字段的字节长度独立存储，可正确处理地址中的分隔符和多行文本。
        // Separate byte lengths preserve separators and line breaks in addresses.
        byte[] name = request.recipientName().getBytes(StandardCharsets.UTF_8);
        byte[] phone = request.recipientPhone().getBytes(StandardCharsets.UTF_8);
        byte[] address = request.recipientAddress().getBytes(StandardCharsets.UTF_8);
        try {
            return ByteBuffer.allocate(3 * Integer.BYTES + name.length + phone.length + address.length)
                    .putInt(name.length).put(name).putInt(phone.length).put(phone)
                    .putInt(address.length).put(address).array();
        } finally {
            Arrays.fill(name, (byte) 0);
            Arrays.fill(phone, (byte) 0);
            Arrays.fill(address, (byte) 0);
        }
    }
}
