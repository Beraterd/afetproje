package com.afet.koordinasyon.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Türkiye cep telefonu numarası doğrulaması.
 * Kabul edilen formatlar: +905XXXXXXXXX, 905XXXXXXXXX, 05XXXXXXXXX, 5XXXXXXXXX.
 * <p>
 * Null/boş değerleri geçerli sayar (zorunluluk için ayrıca {@code @NotBlank} kullanın).
 */
@Documented
@Constraint(validatedBy = TurkishPhoneValidator.class)
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface TurkishPhone {
    String message() default "Geçersiz telefon numarası. Örnek: +905XXXXXXXXX veya 05XXXXXXXXX";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
