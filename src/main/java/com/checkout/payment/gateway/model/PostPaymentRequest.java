package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PostPaymentRequest(
    @JsonProperty("card_number_last_four")
    int cardNumberLastFour,
    @JsonProperty("expiry_month")
    int expiryMonth,
    @JsonProperty("expiry_year")
    int expiryYear,
    String currency,
    int amount,
    int cvv
) {

  @JsonProperty("expiry_date")
  public String getExpiryDate() {
    return String.format("%d/%d", expiryMonth, expiryYear);
  }
}
