package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.entity.Payment;
import com.checkout.payment.gateway.enums.PaymentStatus;
import java.util.UUID;
import lombok.Builder;

@Builder
public record PaymentResponse(
    UUID id,
    PaymentStatus status,
    int cardNumberLastFour,
    int expiryMonth,
    int expiryYear,
    String currency,
    int amount
) {

  public static PaymentResponse fromEntity(Payment payment) {
    return PaymentResponse.builder()
        .id(payment.getId())
        .status(payment.getStatus())
        .cardNumberLastFour(Integer.parseInt(payment.getCardNumberLastFour()))
        .expiryMonth(payment.getExpiryMonth())
        .expiryYear(payment.getExpiryYear())
        .currency(payment.getCurrency())
        .amount(payment.getAmount().intValue())
        .build();
  }
}
