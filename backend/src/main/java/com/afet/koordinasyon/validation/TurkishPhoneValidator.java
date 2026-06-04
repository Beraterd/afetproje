package com.afet.koordinasyon.validation;

import com.afet.koordinasyon.util.PhoneNumberUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link TurkishPhone} doğrulayıcısı. Tüm telefon doğrulama mantığı merkezi
 * {@link PhoneNumberUtil#isValidTurkishMobile(String)} üzerinden geçer.
 */
public class TurkishPhoneValidator implements ConstraintValidator<TurkishPhone, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Boş değer geçerli kabul edilir; zorunluluk @NotBlank ile kontrol edilir.
        if (value == null || value.isBlank()) {
            return true;
        }
        return PhoneNumberUtil.isValidTurkishMobile(value);
    }
}
