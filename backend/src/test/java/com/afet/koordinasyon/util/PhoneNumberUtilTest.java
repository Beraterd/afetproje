package com.afet.koordinasyon.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumberUtilTest {

    @ParameterizedTest
    @CsvSource({
            "+905395471062, +905395471062",
            "905395471062,  +905395471062",
            "05395471062,   +905395471062",
            "5395471062,    +905395471062",
            "+90 539 547 10 62, +905395471062",
            "0539 547 10 62,    +905395471062"
    })
    @DisplayName("toE164: tüm kabul edilen TR formatları +905XXXXXXXXX'e normalize edilir")
    void toE164_normalizesAllAcceptedFormats(String input, String expected) {
        assertThat(PhoneNumberUtil.toE164(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"+905395471062", "905395471062", "05395471062", "5395471062"})
    @DisplayName("isValidTurkishMobile: geçerli TR cep numaraları true döner")
    void isValid_acceptsValidNumbers(String input) {
        assertThat(PhoneNumberUtil.isValidTurkishMobile(input)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "abc", "02125551234", "90212555123", "5395471", "0539547106212345"})
    @DisplayName("isValidTurkishMobile: geçersiz/cep-olmayan numaralar false döner")
    void isValid_rejectsInvalidNumbers(String input) {
        assertThat(PhoneNumberUtil.isValidTurkishMobile(input)).isFalse();
    }

    @Test
    @DisplayName("isValidTurkishMobile: null ve boş false döner")
    void isValid_nullOrBlank() {
        assertThat(PhoneNumberUtil.isValidTurkishMobile(null)).isFalse();
        assertThat(PhoneNumberUtil.isValidTurkishMobile("")).isFalse();
        assertThat(PhoneNumberUtil.isValidTurkishMobile("   ")).isFalse();
    }

    @Test
    @DisplayName("toWhatsAppRecipient: Meta için + işaretsiz format döner")
    void toWhatsAppRecipient_stripsPlus() {
        assertThat(PhoneNumberUtil.toWhatsAppRecipient("+905395471062")).isEqualTo("905395471062");
        assertThat(PhoneNumberUtil.toWhatsAppRecipient("05395471062")).isEqualTo("905395471062");
    }

    @Test
    @DisplayName("toE164: null girişte null döner")
    void toE164_nullHandling() {
        assertThat(PhoneNumberUtil.toE164(null)).isNull();
    }

    @Test
    @DisplayName("mask: yalnızca son 2 hane görünür")
    void mask_hidesDigits() {
        assertThat(PhoneNumberUtil.mask("+905395471062")).isEqualTo("05** *** **62");
        assertThat(PhoneNumberUtil.mask(null)).isEqualTo("****");
    }
}
