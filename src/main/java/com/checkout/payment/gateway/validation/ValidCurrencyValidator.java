package com.checkout.payment.gateway.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

public class ValidCurrencyValidator implements ConstraintValidator<ValidCurrency, String> {

  @Value("#{'${application.supported-currencies}'.split(',')}")
  private List<String> supportedCurrencies;

  @Override
  public boolean isValid(String currency, ConstraintValidatorContext context) {
    if (currency == null || currency.isBlank()) {
      return true;
    }
    return supportedCurrencies.contains(currency.toUpperCase());
  }
}
