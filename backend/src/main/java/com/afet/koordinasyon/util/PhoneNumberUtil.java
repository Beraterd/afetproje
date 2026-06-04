package com.afet.koordinasyon.util;

public final class PhoneNumberUtil {

    private PhoneNumberUtil() {}

    /**
     * Normalizes a Turkish phone number to E.164 format without the '+' prefix.
     * 05XXXXXXXXX → 905XXXXXXXXX
     * 5XXXXXXXXX  → 905XXXXXXXXX
     * +905XXXXXXXX → 905XXXXXXXXX
     * 905XXXXXXXX → 905XXXXXXXXX (already normalized)
     */
    public static String normalize(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("90") && digits.length() == 12) {
            return digits;
        }
        if (digits.startsWith("0") && digits.length() == 11) {
            return "9" + digits;
        }
        if (digits.length() == 10) {
            return "90" + digits;
        }
        return digits;
    }

    /**
     * Normalizes to full E.164 format with the leading '+' (e.g. +905551234567).
     * This is the canonical DB storage format for user phones.
     * Returns null for null input.
     */
    public static String toE164(String phone) {
        String normalized = normalize(phone);
        if (normalized == null || normalized.isBlank()) return normalized;
        return "+" + normalized;
    }

    /**
     * E.164 formatından '+' işaretini kaldırarak Meta WhatsApp Cloud API'nin beklediği
     * sade rakam formatını döner (905XXXXXXXXX). Geçersiz/boş girişte normalize çıktısını döner.
     */
    public static String toWhatsAppRecipient(String phone) {
        return normalize(phone);
    }

    /**
     * Türkiye cep telefonu numarasının geçerliliğini kontrol eder.
     * Kabul edilen formatlar (normalize sonrası 905XXXXXXXXX olmalı):
     * +905XXXXXXXXX, 905XXXXXXXXX, 05XXXXXXXXX, 5XXXXXXXXX.
     */
    public static boolean isValidTurkishMobile(String phone) {
        String digits = normalize(phone);
        return digits != null
                && digits.length() == 12
                && digits.startsWith("90")
                && digits.charAt(2) == '5';
    }

    /**
     * Masks a phone number so only the last 2 digits are visible.
     * 905551234567 → 05** *** **67
     */
    public static String mask(String phone) {
        if (phone == null || phone.isBlank()) return "****";
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 2) return "****";
        String last2 = digits.substring(digits.length() - 2);
        return "05** *** **" + last2;
    }
}
