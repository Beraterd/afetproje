package com.afet.koordinasyon.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Negatif stok engellemesi: ResourceStockService.updateQuantity servis katmanında değil,
 * bu DTO'nun @PositiveOrZero kısıtıyla controller sınırında (bean validation) engelleniyor.
 */
class UpdateStockQuantityRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("Negatif yeni miktar reddedilir (@PositiveOrZero)")
    void negativeQuantity_isRejected() {
        UpdateStockQuantityRequest req = new UpdateStockQuantityRequest();
        req.setNewQuantity(-5);

        Set<ConstraintViolation<UpdateStockQuantityRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("newQuantity"));
    }

    @Test
    @DisplayName("Sıfır ve pozitif miktarlar kabul edilir")
    void zeroOrPositiveQuantity_isAccepted() {
        UpdateStockQuantityRequest zero = new UpdateStockQuantityRequest();
        zero.setNewQuantity(0);
        UpdateStockQuantityRequest positive = new UpdateStockQuantityRequest();
        positive.setNewQuantity(42);

        assertThat(validator.validate(zero)).isEmpty();
        assertThat(validator.validate(positive)).isEmpty();
    }

    @Test
    @DisplayName("newQuantity zorunludur")
    void missingQuantity_isRejected() {
        UpdateStockQuantityRequest req = new UpdateStockQuantityRequest();

        Set<ConstraintViolation<UpdateStockQuantityRequest>> violations = validator.validate(req);

        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("newQuantity"));
    }
}
