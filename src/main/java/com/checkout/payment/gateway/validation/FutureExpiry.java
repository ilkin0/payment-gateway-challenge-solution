package com.checkout.payment.gateway.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = FutureExpiryValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface FutureExpiry {
    String message() default "Card expiry date must be in the future";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
