package com.afet.koordinasyon.notification;

public record SmsSendResult(
        boolean success,
        String providerMessageId,
        String errorMessage
) {
    public static SmsSendResult ok(String providerMessageId) {
        return new SmsSendResult(true, providerMessageId, null);
    }

    public static SmsSendResult fail(String errorMessage) {
        return new SmsSendResult(false, null, errorMessage);
    }
}
