package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.client.BankPaymentRequest;
import com.checkout.payment.gateway.client.BankPaymentResponse;
import com.checkout.payment.gateway.client.BankPaymentResult;
import com.checkout.payment.gateway.entity.Payment;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.exception.PaymentProcessingException;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentGatewayService {

  private final PaymentRepository paymentRepository;
  private final PaymentCacheService cacheService;
  private final BankClient bankClient;

  public PaymentResponse getPaymentById(UUID id) {
    log.debug("Requesting access to to payment with ID {}", id);

    return cacheService.getByPaymentId(id)
        .orElseGet(() -> paymentRepository.findById(id)
            .map(payment -> {
              PaymentResponse paymentResponse = PaymentResponse.fromEntity(payment);
              cacheService.cachePayment(paymentResponse, payment.getIdempotencyKey());
              return paymentResponse;
            })
            .orElseThrow(() -> new PaymentNotFoundException(id)));
  }

  @Transactional
  public PaymentResponse processPayment(PostPaymentRequest request, UUID idempotencyKey) {
    log.info("Processing payment request");

    if (idempotencyKey != null) {
      Optional<PaymentResponse> existingPayment = findByIdempotencyKey(idempotencyKey);
      if (existingPayment.isPresent()) {
        log.info("Idempotent request - returning existing payment");
        return existingPayment.get();
      }
    }

    BankPaymentRequest bankRequest = BankPaymentRequest.from(request);

    BankPaymentResult result;
    try {
      BankPaymentResponse response = bankClient.processPayment(bankRequest);
      result = new BankPaymentResult.Success(response.authorized(), response.authorizationCode());
    } catch (Exception e) {
      log.error("Bank communication failed: {}", e.getMessage());
      result = new BankPaymentResult.BankUnavailable(e.getMessage());
    }

    return switch (result) {
      case BankPaymentResult.Success success -> handleBankSuccess(request, success, idempotencyKey);
      case BankPaymentResult.BankUnavailable unavailable -> handleBankFailure(unavailable);
    };
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
    PaymentResponse paymentResponse = PaymentResponse.fromEntity(savedPayment);
    cacheService.cachePayment(paymentResponse, idempotencyKey);
    return paymentResponse;
  }

  private PaymentResponse handleBankFailure(BankPaymentResult.BankUnavailable unavailable) {
    throw new PaymentProcessingException("Bank unavailable: " + unavailable.reason());
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
