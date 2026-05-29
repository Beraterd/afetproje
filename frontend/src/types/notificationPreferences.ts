export interface NotificationPreferences {
    emailTaskNotificationsEnabled: boolean;
    emailDamageNotificationsEnabled: boolean;
    emailEarthquakeNotificationsEnabled: boolean;
    emailTeamNotificationsEnabled: boolean;
    emailAidNotificationsEnabled: boolean;
    emailSystemNotificationsEnabled: boolean;
}

export interface UpdateNotificationPreferencesRequest {
    emailTaskNotificationsEnabled?: boolean;
    emailDamageNotificationsEnabled?: boolean;
    emailEarthquakeNotificationsEnabled?: boolean;
    emailTeamNotificationsEnabled?: boolean;
    emailAidNotificationsEnabled?: boolean;
    emailSystemNotificationsEnabled?: boolean;
}
