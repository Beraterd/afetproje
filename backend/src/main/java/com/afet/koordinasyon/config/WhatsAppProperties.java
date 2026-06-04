package com.afet.koordinasyon.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * WhatsApp Business Cloud API yapılandırması.
 * Prefix: app.whatsapp (application.yml içindeki app.whatsapp bloğu ile birebir eşleşir).
 * <p>
 * NOT: accessToken hassas bir değerdir; hiçbir log satırına yazılmamalıdır.
 */
@Component
@ConfigurationProperties(prefix = "app.whatsapp")
@Getter
@Setter
public class WhatsAppProperties {

    /** Tüm WhatsApp gönderimini açar/kapatır. false ise sessizce atlanır. */
    private boolean enabled = false;

    /** true ise tüm mesajlar testRecipient numarasına gönderilir. */
    private boolean testMode = true;

    /** testMode=true iken kullanılan tek alıcı numarası (E.164, örn. +905551112233). */
    private String testRecipient = "";

    /** Meta WhatsApp telefon numarası kimliği (Phone Number ID). */
    private String phoneNumberId = "";

    /** Meta Graph API erişim token'ı (Bearer). Asla loglanmaz. */
    private String accessToken = "";

    /** Graph API sürümü. */
    private String apiVersion = "v20.0";

    /** Graph API ana adresi. */
    private String baseUrl = "https://graph.facebook.com";

    /** Deprem bildirimi için onaylı WhatsApp template adı. Boşsa text fallback kullanılır. */
    private String earthquakeTemplateName = "";

    /** Yakına durum mesajı için onaylı WhatsApp template adı. Boşsa text fallback kullanılır. */
    private String statusTemplateName = "";

    /** Template dil kodu. */
    private String languageCode = "tr";
}
