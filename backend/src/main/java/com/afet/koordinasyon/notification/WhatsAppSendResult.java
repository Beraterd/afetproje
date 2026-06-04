package com.afet.koordinasyon.notification;

/**
 * WhatsApp Cloud API gönderim sonucu.
 * providerMessageId: Meta tarafından dönen wamid (başarılı ise).
 */
public record WhatsAppSendResult(boolean success, String providerMessageId, String errorMessage) {

    public static WhatsAppSendResult ok(String providerMessageId) {
        return new WhatsAppSendResult(true, providerMessageId, null);
    }

    public static WhatsAppSendResult fail(String errorMessage) {
        return new WhatsAppSendResult(false, null, errorMessage);
    }
}
