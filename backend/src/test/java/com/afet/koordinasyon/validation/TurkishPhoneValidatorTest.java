package com.afet.koordinasyon.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class TurkishPhoneValidatorTest {

    private final TurkishPhoneValidator validator = new TurkishPhoneValidator();

    @ParameterizedTest
    @ValueSource(strings = {"+905395471062", "905395471062", "05395471062", "5395471062"})
    @DisplayName("Geçerli TR cep numaraları kabul edilir")
    void acceptsValid(String input) {
        assertThat(validator.isValid(input, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "abc", "02125551234", "0539547106212345"})
    @DisplayName("Geçersiz numaralar reddedilir")
    void rejectsInvalid(String input) {
        assertThat(validator.isValid(input, null)).isFalse();
    }

    @Test
    @DisplayName("Null/boş değer geçerli kabul edilir (@NotBlank ayrı kontrol eder)")
    void nullOrBlankIsValid() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }
}
