package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.YearMonth;

public class FutureExpiryValidator implements
    ConstraintValidator<FutureExpiry, PostPaymentRequest> {

  @Override
  public boolean isValid(PostPaymentRequest request, ConstraintValidatorContext context) {
    if (request == null) {
      return true;
    }

    int month = request.expiryMonth();
    int year = request.expiryYear();

    if (month < 1 || month > 12) {
      return true;
    }

    YearMonth expiry = YearMonth.of(year, month);
    YearMonth now = YearMonth.now();

    return expiry.isAfter(now);
  }
}
