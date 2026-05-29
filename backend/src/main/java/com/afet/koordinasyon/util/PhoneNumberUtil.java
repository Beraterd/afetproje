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
