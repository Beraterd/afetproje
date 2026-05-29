package com.afet.koordinasyon.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.sms")
@Getter
@Setter
public class SmsProperties {

    private boolean enabled = false;
    private boolean testMode = true;
    private String testRecipient = "";
    private String apiKey = "";
    private String apiSecret = "";
    private String sender = "AFETKOORD";
    private String provider = "iletimerkezi";
    private int otpExpirySeconds = 300;
    private int maxOtpPerHour = 3;
    private int maxVerifyAttempts = 5;
    private String apiUrl = "https://api.iletimerkezi.com/v1/send-sms/json";
}
