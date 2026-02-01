package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

public record BankPaymentRequest(
    @JsonProperty("card_number") String cardNumber,
    @JsonProperty("expiry_date") String expiryDate,
    String currency,
    int amount,
    String cvv
) {

  public static BankPaymentRequest from(PostPaymentRequest request) {
    return new BankPaymentRequest(
        request.cardNumber(),
        request.getFormattedExpiryDate(),
        request.currency(),
        request.amount(),
        request.cvv()
    );
  }
}
