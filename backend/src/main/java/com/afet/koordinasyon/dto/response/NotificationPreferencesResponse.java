package com.afet.koordinasyon.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationPreferencesResponse {
    private boolean emailTaskNotificationsEnabled;
    private boolean emailDamageNotificationsEnabled;
    private boolean emailEarthquakeNotificationsEnabled;
    private boolean emailTeamNotificationsEnabled;
    private boolean emailAidNotificationsEnabled;
    private boolean emailSystemNotificationsEnabled;
}
