package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.client.BankPaymentRequest;
import com.checkout.payment.gateway.client.BankPaymentResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

  @Mock
  private PaymentRepository paymentRepository;

  @Mock
  private BankClient bankClient;

  @Mock
  private PaymentCacheService cacheService;

  private PaymentGatewayService paymentGatewayService;


  @BeforeEach
  void setUp() {
    paymentGatewayService = new PaymentGatewayService(paymentRepository, cacheService, bankClient);
  }

  @Test
  void getPaymentById_shouldFetchFromDatabase() {
    UUID id = UUID.randomUUID();
    Payment entity = Payment.builder()
        .id(id)
        .cardNumberLastFour("3456")
        .expiryMonth(12)
        .expiryYear(2027)
        .currency("USD")
        .amount(BigDecimal.valueOf(1000))
        .status(PaymentStatus.AUTHORIZED)
        .build();

    when(cacheService.getByPaymentId(id)).thenReturn(Optional.empty());
    when(paymentRepository.findById(id)).thenReturn(Optional.of(entity));

    PaymentResponse response = paymentGatewayService.getPaymentById(id);

    assertNotNull(response);
    assertEquals(id, response.id());
    assertEquals(PaymentStatus.AUTHORIZED, response.status());
    assertEquals(3456, response.cardNumberLastFour());
    assertEquals(12, response.expiryMonth());
    assertEquals(2027, response.expiryYear());
    assertEquals("USD", response.currency());
    assertEquals(1000, response.amount());
  }

  @Test
  void getPaymentById_whenNotExists_shouldThrowException() {
    UUID id = UUID.randomUUID();

    when(cacheService.getByPaymentId(id)).thenReturn(Optional.empty());
    when(paymentRepository.findById(id)).thenReturn(Optional.empty());

    PaymentNotFoundException exception = assertThrows(
        PaymentNotFoundException.class,
        () -> paymentGatewayService.getPaymentById(id)
    );

    assertEquals("Payment not found with id: " + id, exception.getMessage());
  }

  @Test
  void processPayment_whenBankAuthorized_shouldSaveAuthorizedPayment() {
    PostPaymentRequest request = createPostPaymentRequest();
    UUID idempotencyKey = UUID.randomUUID();
    BankPaymentResponse bankResponse = new BankPaymentResponse(true, "AUTH123");

    when(cacheService.getByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
    when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
    when(bankClient.processPayment(any(BankPaymentRequest.class))).thenReturn(bankResponse);
    when(paymentRepository.save(any(Payment.class))).thenAnswer(
        invocation -> invocation.getArgument(0));

    PaymentResponse response = paymentGatewayService.processPayment(request, idempotencyKey);

    assertEquals(PaymentStatus.AUTHORIZED, response.status());

    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentRepository).save(paymentCaptor.capture());
    Payment savedPayment = paymentCaptor.getValue();
    assertEquals(PaymentStatus.AUTHORIZED, savedPayment.getStatus());
    assertEquals("AUTH123", savedPayment.getAuthorizationCode());
    assertEquals("3456", savedPayment.getCardNumberLastFour());
  }

  @Test
  void processPayment_whenBankDeclined_shouldSaveDeclinedPayment() {
    PostPaymentRequest request = createPostPaymentRequest();
    UUID idempotencyKey = UUID.randomUUID();
    BankPaymentResponse bankResponse = new BankPaymentResponse(false, null);

    when(cacheService.getByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
    when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
    when(bankClient.processPayment(any(BankPaymentRequest.class))).thenReturn(bankResponse);
    when(paymentRepository.save(any(Payment.class))).thenAnswer(
        invocation -> invocation.getArgument(0));

    PaymentResponse response = paymentGatewayService.processPayment(request, idempotencyKey);

    assertEquals(PaymentStatus.DECLINED, response.status());

    ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
    verify(paymentRepository).save(paymentCaptor.capture());
    Payment savedPayment = paymentCaptor.getValue();
    assertEquals(PaymentStatus.DECLINED, savedPayment.getStatus());
  }

  @Test
  void processPayment_whenBankFails_shouldThrowProcessingException() {
    PostPaymentRequest request = createPostPaymentRequest();
    UUID idempotencyKey = UUID.randomUUID();

    when(cacheService.getByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
    when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
    when(bankClient.processPayment(any(BankPaymentRequest.class))).thenThrow(
        new RuntimeException("Connection error"));

    PaymentProcessingException exception = assertThrows(
        PaymentProcessingException.class,
        () -> paymentGatewayService.processPayment(request, idempotencyKey)
    );

    assertEquals("Bank unavailable: Connection error", exception.getMessage());
  }

  private PostPaymentRequest createPostPaymentRequest() {
    return new PostPaymentRequest(
        "1234567890123456",
        12,
        2027,
        "USD",
        1000,
        "123"
    );
  }
}
