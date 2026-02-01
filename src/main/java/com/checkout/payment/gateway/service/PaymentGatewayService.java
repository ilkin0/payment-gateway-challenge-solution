package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.client.BankPaymentRequest;
import com.checkout.payment.gateway.client.BankPaymentResponse;
import com.checkout.payment.gateway.client.BankPaymentResult;
import com.checkout.payment.gateway.entity.Payment;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.exception.PaymentProcessingException;
import com.checkout.payment.gateway.metrics.PaymentMetrics;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.repository.PaymentRepository;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentGatewayService {

  private final PaymentRepository paymentRepository;
  private final PaymentCacheService cacheService;
  private final PaymentMetrics metrics;
  private final BankClient bankClient;

  public PaymentResponse getPaymentById(UUID id) {
    MDC.put("paymentId", id.toString());

    try {
      log.debug("Requesting access to to payment with ID {}", id);

      Optional<PaymentResponse> cachedResponse = cacheService.getByPaymentId(id);
      if (cachedResponse.isPresent()) {
        log.debug("Cache hit");
        metrics.recordCacheHit();
        return cachedResponse.get();
      }
      metrics.recordCacheMiss();
      log.debug("Cache miss, querying database");

      return paymentRepository.findById(id)
          .map(payment -> {
            PaymentResponse paymentResponse = PaymentResponse.fromEntity(payment);
            cacheService.cachePayment(paymentResponse, payment.getIdempotencyKey());
            return paymentResponse;
          })
          .orElseThrow(() -> new PaymentNotFoundException(id));
    } finally {
      MDC.remove("paymentId");
    }
  }

  @Transactional
  public PaymentResponse processPayment(PostPaymentRequest request, UUID idempotencyKey) {
    Timer.Sample timer = metrics.startTimer();
    MDC.put("cardLastFour", request.getLastFourDigits());
    MDC.put("currency", request.currency());
    MDC.put("amount", String.valueOf(request.amount()));

    try {
      if (idempotencyKey != null) {
        MDC.put("idempotencyKey", idempotencyKey.toString());
        Optional<PaymentResponse> existingPayment = findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
          log.info("Idempotent request - returning existing payment");
          metrics.recordIdempotentRequest();
          return existingPayment.get();
        }
      }

      BankPaymentRequest bankRequest = BankPaymentRequest.from(request);
      BankPaymentResult result;
      Timer.Sample bankTimer = metrics.startTimer();
      try {
        BankPaymentResponse response = bankClient.processPayment(bankRequest);
        result = new BankPaymentResult.Success(response.authorized(), response.authorizationCode());
      } catch (Exception e) {
        log.error("Bank communication failed: {}", e.getMessage());
        result = new BankPaymentResult.BankUnavailable(e.getMessage());
      }
      metrics.stopBankCallTimer(bankTimer);

      return switch (result) {
        case BankPaymentResult.Success success ->
            handleBankSuccess(request, success, idempotencyKey);
        case BankPaymentResult.BankUnavailable unavailable -> handleBankFailure(unavailable);
      };
    } finally {
      metrics.stopPaymentTimer(timer);
      MDC.clear();
    }
  }

  private PaymentResponse handleBankSuccess(PostPaymentRequest request,
      BankPaymentResult.Success success, UUID idempotencyKey) {
    PaymentStatus status =
        success.authorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED;

    Payment payment = Payment.builder()
        .id(UUID.randomUUID())
        .cardNumberLastFour(request.getLastFourDigits())
        .expiryMonth(request.expiryMonth())
        .expiryYear(request.expiryYear())
        .currency(request.currency())
        .amount(BigDecimal.valueOf(request.amount()))
        .status(status)
        .authorizationCode(success.authorizationCode())
        .idempotencyKey(idempotencyKey)
        .build();

    Payment savedPayment = paymentRepository.save(payment);
    MDC.put("paymentId", savedPayment.getId().toString());
    MDC.put("status", status.getName());
    log.info("Payment processed successfully");
    metrics.recordPaymentOutcome(status);

    PaymentResponse paymentResponse = PaymentResponse.fromEntity(savedPayment);
    cacheService.cachePayment(paymentResponse, idempotencyKey);
    return paymentResponse;
  }

  private PaymentResponse handleBankFailure(BankPaymentResult.BankUnavailable unavailable) {
    log.error("Bank unavailable: {}", unavailable.reason());
    metrics.recordPaymentOutcome(PaymentStatus.REJECTED);
    throw new PaymentProcessingException("Unable to process payment: " + unavailable.reason());
  }

  private Optional<PaymentResponse> findByIdempotencyKey(UUID idempotencyKey) {
    return cacheService.getByIdempotencyKey(idempotencyKey)
        .or(() -> paymentRepository.findByIdempotencyKey(idempotencyKey)
            .map(payment -> {
              PaymentResponse paymentResponse = PaymentResponse.fromEntity(payment);
              cacheService.cachePayment(paymentResponse, payment.getIdempotencyKey());
              return paymentResponse;
            })
        );
  }
}
