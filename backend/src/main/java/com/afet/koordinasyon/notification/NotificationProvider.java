package com.afet.koordinasyon.notification;

import com.afet.koordinasyon.domain.enums.NotificationChannel;

public interface NotificationProvider {

    NotificationChannel getChannel();

    /**
     * Bildirimi gönderir.
     * @throws NotificationException gönderim başarısız olursa
     */
    void send(String to, String subject, String body);
}
