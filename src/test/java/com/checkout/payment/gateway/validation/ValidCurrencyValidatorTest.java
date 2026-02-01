package com.checkout.payment.gateway.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidCurrencyValidatorTest {

  private ValidCurrencyValidator validator;

  @BeforeEach
  void setUp() {
    validator = new ValidCurrencyValidator();
    validator.initialize(null);
  }

  @Test
  void isValid_withNull_shouldReturnTrue() {
    assertTrue(validator.isValid(null, null));
  }

  @Test
  void isValid_withBlank_shouldReturnTrue() {
    assertTrue(validator.isValid("   ", null));
  }

  @Test
  void isValid_withEmpty_shouldReturnTrue() {
    assertTrue(validator.isValid("", null));
  }

  @Test
  void isValid_withUSD_shouldReturnTrue() {
    assertTrue(validator.isValid("USD", null));
  }

  @Test
  void isValid_withEUR_shouldReturnTrue() {
    assertTrue(validator.isValid("EUR", null));
  }

  @Test
  void isValid_withGBP_shouldReturnTrue() {
    assertTrue(validator.isValid("GBP", null));
  }

  @Test
  void isValid_withLowercase_shouldReturnTrue() {
    assertTrue(validator.isValid("usd", null));
    assertTrue(validator.isValid("eur", null));
    assertTrue(validator.isValid("gbp", null));
  }

  @Test
  void isValid_withMixedCase_shouldReturnTrue() {
    assertTrue(validator.isValid("Usd", null));
  }

  @Test
  void isValid_withUnsupportedCurrency_shouldReturnFalse() {
    assertFalse(validator.isValid("JPY", null));
    assertFalse(validator.isValid("CAD", null));
    assertFalse(validator.isValid("AUD", null));
  }
}
