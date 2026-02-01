package com.checkout.payment.gateway.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

public class ValidCurrencyValidator implements ConstraintValidator<ValidCurrency, String> {

  @Value("#{'${application.supported-currencies:USD,EUR,GBP}'.split(',')}")
  private List<String> supportedCurrencies;

  @Override
  public void initialize(ValidCurrency constraintAnnotation) {
    if (supportedCurrencies == null) {
      supportedCurrencies = Arrays.asList("USD", "EUR", "GBP");
    }
  }

  @Override
  public boolean isValid(String currency, ConstraintValidatorContext context) {
    if (currency == null || currency.isBlank()) {
      return true;
    }
    return supportedCurrencies.contains(currency.toUpperCase());
  }
}
