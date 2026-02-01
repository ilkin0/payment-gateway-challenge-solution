package com.checkout.payment.gateway.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidCurrencyValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCurrency {
    String message() default "Currency must be a valid ISO 4217 code (USD, EUR, GBP)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
