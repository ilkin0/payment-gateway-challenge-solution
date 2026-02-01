package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.validation.FutureExpiry;
import com.checkout.payment.gateway.validation.ValidCurrency;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@FutureExpiry
public record PostPaymentRequest(

    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^\\d{14,19}$", message = "Card number must be between 14 and 19 digits")
    @JsonProperty("card_number")
    String cardNumber,

    @NotNull(message = "Expiry month is required")
    @Min(value = 1, message = "Expiry month must be between 1 and 12")
    @Max(value = 12, message = "Expiry month must be between 1 and 12")
    @JsonProperty("expiry_month")
    Integer expiryMonth,

    @NotNull(message = "Expiry year is required")
    @Min(value = 2000, message = "Expiry year must be valid")
    @JsonProperty("expiry_year")
    Integer expiryYear,

    @NotBlank(message = "Currency is required")
    @ValidCurrency
    String currency,

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be greater than 0")
    int amount,

    @NotBlank(message = "CVV is required")
    @Pattern(regexp = "^\\d{3,4}$", message = "CVV must be 3 or 4 digits")
    String cvv
) {

  public String getLastFourDigits() {
    if (cardNumber == null || cardNumber.length() < 4) {
      return null;
    }
    return cardNumber.substring(cardNumber.length() - 4);
  }

  public String getFormattedExpiryDate() {
    return String.format("%02d/%d", expiryMonth, expiryYear);
  }
}
