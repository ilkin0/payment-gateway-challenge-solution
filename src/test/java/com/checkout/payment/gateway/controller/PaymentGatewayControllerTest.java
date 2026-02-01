package com.checkout.payment.gateway.controller;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.entity.Payment;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  PaymentRepository paymentRepository;

  @BeforeEach
  void setUp() {
    paymentRepository.deleteAll();
  }

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    Payment payment = Payment.builder()
        .cardNumberLastFour("4321")
        .expiryMonth(12)
        .expiryYear(2024)
        .currency("USD")
        .amount(BigDecimal.valueOf(10))
        .authorizationCode("AUTH123")
        .idempotencyKey(UUID.randomUUID())
        .status(PaymentStatus.AUTHORIZED)
        .build();
    Payment savedPayment = paymentRepository.save(payment);

    mvc.perform(get("/api/v1/payments/" + savedPayment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    UUID randomUUID = UUID.randomUUID();
    mvc.perform(get("/api/v1/payments/" + randomUUID))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.message").value("Payment not found with id: %s".formatted(randomUUID)));
  }

  @Test
  void getPayment_whenExists_shouldReturn200() throws Exception {
    Payment payment = Payment.builder()
        .cardNumberLastFour("4321")
        .expiryMonth(12)
        .expiryYear(2024)
        .currency("USD")
        .amount(BigDecimal.valueOf(10))
        .authorizationCode("AUTH123")
        .idempotencyKey(UUID.randomUUID())
        .status(PaymentStatus.AUTHORIZED)
        .build();
    paymentRepository.save(payment);

    mvc.perform(get("/api/v1/payments/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.cardNumberLastFour").value(4321))
        .andExpect(jsonPath("$.expiryMonth").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiryYear").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void getPayment_whenNotExists_shouldReturn404() throws Exception {
    mvc.perform(get("/api/v1/payments/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").exists());
  }
}
