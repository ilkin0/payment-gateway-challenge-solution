package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentGatewayService {

  private final PaymentRepository paymentRepository;

  public PostPaymentResponse getPaymentById(UUID id) {
    log.debug("Requesting access to to payment with ID {}", id);
    return paymentRepository.findById(id)
        .map(PostPaymentResponse::fromEntity)
        .orElseThrow(() -> new PaymentNotFoundException(id));
  }

  public UUID processPayment(PostPaymentRequest paymentRequest) {
    return UUID.randomUUID();
  }
}
