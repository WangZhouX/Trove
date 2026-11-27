package io.trove.backend.api;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 定义收藏页兑换草稿的写入请求和无收件资料的响应。
 * Defines the collection page's draft submission and its response without delivery details.
 */
public final class RedemptionDraftCreationApi {

    /** 收件字段的最大 Unicode 码点数，写入与解密共用。 / Maximum recipient field lengths in Unicode code points for writes and reads. */
    public static final int MAX_NAME_CHARACTERS = 120;
    public static final int MAX_PHONE_CHARACTERS = 40;
    public static final int MAX_ADDRESS_CHARACTERS = 500;

    /** 仅提供兑换草稿数据类型。 / Only provides redemption-draft data types. */
    private RedemptionDraftCreationApi() {
    }

    /**
     * 兑换表单发送的私有收件资料。藏品与钱包分别来自 URL 和已认证会话。
     * Private delivery details from the redemption form. The URL and authenticated session supply the item and wallet.
     *
     * @param recipientName 非空收件人姓名，最多 120 字符 / nonblank recipient name, up to 120 characters
     * @param recipientPhone 非空联系电话，最多 40 字符 / nonblank contact phone, up to 40 characters
     * @param recipientAddress 非空收件地址，最多 500 字符 / nonblank delivery address, up to 500 characters
     */
    public record CreateRequest(String recipientName, String recipientPhone, String recipientAddress) {

        /**
         * 供调试使用的请求摘要，隐藏全部收件资料。
         * Returns a debug summary with every delivery detail redacted.
         *
         * @return 固定的脱敏文本 / fixed redacted text
         */
        @Override
        public String toString() {
            return "RedemptionDraftCreateRequest[redacted]";
        }
    }

    /**
     * 保存完成后向持有人返回的私有草稿摘要。
     * Draft summary returned to the holder after saving.
     *
     * @param draftId 稳定草稿 UUID / stable draft UUID
     * @param network 会话绑定的链上网络 / chain network bound to the session
     * @param itemId 待兑换的实物 UUID / UUID of the item to be redeemed
     * @param status 草稿状态，保存成功为 DRAFT / draft state, DRAFT on a successful save
     * @param expiresAt 草稿的 UTC 到期时间 / UTC expiration time of the draft
     */
    public record Draft(UUID draftId, String network, UUID itemId, String status, OffsetDateTime expiresAt) {
    }

    /**
     * 保存结果与 HTTP 状态所需的创建标记。
     * Saved draft and the creation flag used for the HTTP status.
     *
     * @param draft 持有人可见的草稿摘要 / draft summary visible to the holder
     * @param created 是否生成新的草稿 UUID / whether a new draft UUID was created
     */
    public record Saved(Draft draft, boolean created) {
    }
}
