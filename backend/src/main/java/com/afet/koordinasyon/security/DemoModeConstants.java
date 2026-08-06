package com.afet.koordinasyon.security;

/** Sabitler: "Giriş Yapmadan Admin Olarak Siteyi Gez" (salt okunur demo admin) özelliği. */
public final class DemoModeConstants {

    private DemoModeConstants() {
    }

    public static final String DEMO_ADMIN_USERNAME = "demo_admin";
    public static final String DEMO_ADMIN_EMAIL = "demo.admin@demo.local";
    public static final String DEMO_ADMIN_PHONE = "+905559990000";

    public static final String RESTRICTED_ERROR_CODE = "DEMO_MODE_RESTRICTED";
    public static final String RESTRICTED_MESSAGE = "Bu işlem demo modunda kullanılamaz.";
}
