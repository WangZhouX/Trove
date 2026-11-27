package io.trove.backend.api;

/**
 * 仓管员核对单笔履约任务收件资料的响应结构。
 * Response types for a warehouse operator checking a single shipment's delivery details.
 */
public final class WarehouseShipmentRecipientApi {

    /** 仅定义接口数据类型。 / Only defines API data types. */
    private WarehouseShipmentRecipientApi() {
    }

    /**
     * 来自已确认兑换草稿的收件资料，仅单笔授权响应可见。
     * Details from a confirmed redemption draft, visible only in an authorized single-task response.
     *
     * @param recipientName 收件人姓名 / recipient name
     * @param recipientPhone 收件人电话 / recipient contact phone
     * @param recipientAddress 收件地址 / delivery address
     */
    public record Recipient(String recipientName, String recipientPhone, String recipientAddress) {

        /**
         * 为调试用途生成摘要，隐藏原始收件资料。
         * Produces a debug summary with every delivery detail redacted.
         *
         * @return 固定的脱敏摘要 / fixed redacted summary
         */
        @Override
        public String toString() {
            return "ShipmentRecipient[redacted]";
        }
    }
}
