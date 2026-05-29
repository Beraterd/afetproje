package com.afet.koordinasyon.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateNotificationPreferencesRequest {
    private Boolean emailTaskNotificationsEnabled;
    private Boolean emailDamageNotificationsEnabled;
    private Boolean emailEarthquakeNotificationsEnabled;
    private Boolean emailTeamNotificationsEnabled;
    private Boolean emailAidNotificationsEnabled;
    private Boolean emailSystemNotificationsEnabled;
}
