package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.entity.Payment;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

  @Mock
  private PaymentRepository paymentRepository;

  private PaymentGatewayService paymentGatewayService;


  @BeforeEach
  void setUp() {
    paymentGatewayService = new PaymentGatewayService(paymentRepository);
  }

  @Test
  void getPaymentById_shouldFetchFromDatabase() {
    UUID id = UUID.randomUUID();
    Payment entity = Payment.builder()
        .id(id)
        .cardNumberLastFour("1234")
        .expiryMonth(12)
        .expiryYear(2027)
        .currency("USD")
        .amount(BigDecimal.valueOf(1000))
        .status(PaymentStatus.AUTHORIZED)
        .build();

    when(paymentRepository.findById(id)).thenReturn(Optional.of(entity));

    PostPaymentResponse response = paymentGatewayService.getPaymentById(id);

    assertNotNull(response);
    assertEquals(id, response.id());
    assertEquals(PaymentStatus.AUTHORIZED, response.status());
    assertEquals(1234, response.cardNumberLastFour());
    assertEquals(12, response.expiryMonth());
    assertEquals(2027, response.expiryYear());
    assertEquals("USD", response.currency());
    assertEquals(1000, response.amount());
  }

  @Test
  void getPaymentById_whenNotExists_shouldThrowException() {
    UUID id = UUID.randomUUID();

    when(paymentRepository.findById(id)).thenReturn(Optional.empty());

    PaymentNotFoundException exception = assertThrows(
        PaymentNotFoundException.class,
        () -> paymentGatewayService.getPaymentById(id)
    );

    assertEquals("Payment not found with id: " + id, exception.getMessage());
  }
}
