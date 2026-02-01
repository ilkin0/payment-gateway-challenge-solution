package com.checkout.payment.gateway.client;

public sealed interface BankPaymentResult {

  record Success(boolean authorized, String authorizationCode) implements
      BankPaymentResult {

  }

  record BankUnavailable(String reason) implements BankPaymentResult {

  }
}
