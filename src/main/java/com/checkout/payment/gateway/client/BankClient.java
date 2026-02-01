package com.checkout.payment.gateway.client;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/payments")
public interface BankClient {
    @PostExchange
    BankPaymentResponse processPayment(@RequestBody BankPaymentRequest request);
}
