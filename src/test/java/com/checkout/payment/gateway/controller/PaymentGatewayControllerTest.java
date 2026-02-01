package com.checkout.payment.gateway.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.TestcontainersConfiguration;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers
class PaymentGatewayControllerTest {

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("bank.simulator.url", () ->
        "http://" + TestcontainersConfiguration.getBankSimulatorContainer().getHost() + ":" +
            TestcontainersConfiguration.getBankSimulatorContainer().getMappedPort(8080));
  }

  @Autowired
  private MockMvc mvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private PostPaymentRequest createPostPaymentRequest(String cardNumber, Integer expiryMonth,
      Integer expiryYear, String currency, int amount, String cvv) {
    return new PostPaymentRequest(cardNumber, expiryMonth, expiryYear, currency, amount, cvv);
  }

  private PostPaymentRequest createValidPostPaymentRequest() {
    YearMonth future = YearMonth.now().plusMonths(6);
    return createPostPaymentRequest("4111111111111111", future.getMonthValue(), future.getYear(),
        "USD", 1000, "123");
  }

  @Test
  void postPayment_withValidRequest_shouldReturn200() throws Exception {
    PostPaymentRequest request = createValidPostPaymentRequest();

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.cardNumberLastFour").value(1111));
  }

  @Test
  void postPayment_withValidRequest_whenBankUnavailable_shouldReturn502() throws Exception {
    PostPaymentRequest request = createPostPaymentRequest("4111111111111110", 12, 2030, "USD", 1000,
        "123");

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadGateway());
  }

  @Test
  void postPayment_withInvalidCardNumber_shouldReturn400() throws Exception {
    PostPaymentRequest request = createPostPaymentRequest("123", 12, 2030, "USD", 1000, "123");

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void postPayment_withInvalidExpiryMonth_shouldReturn400() throws Exception {
    PostPaymentRequest request = createPostPaymentRequest("4111111111111111", 13, 2030, "USD", 1000,
        "123");

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void postPayment_withPastExpiryDate_shouldReturn400() throws Exception {
    PostPaymentRequest request = createPostPaymentRequest("4111111111111111", 1, 2020, "USD", 1000,
        "123");

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void postPayment_withInvalidCvv_shouldReturn400() throws Exception {
    PostPaymentRequest request = createPostPaymentRequest("4111111111111111", 12, 2030, "USD", 1000,
        "1");

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void postPayment_withInvalidCurrency_shouldReturn400() throws Exception {
    PostPaymentRequest request = createPostPaymentRequest("4111111111111111", 12, 2030, "XXX", 1000,
        "123");

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void postPayment_withNegativeAmount_shouldReturn400() throws Exception {
    PostPaymentRequest request = createPostPaymentRequest("4111111111111111", 12, 2030, "USD", -1,
        "123");

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void postPayment_withZeroAmount_shouldReturn400() throws Exception {
    PostPaymentRequest request = createPostPaymentRequest("4111111111111111", 12, 2030, "USD", 0,
        "123");

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getPayment_whenExists_shouldReturn200() throws Exception {
    PostPaymentRequest request = createValidPostPaymentRequest();
    MvcResult postResult = mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andReturn();

    String responseBody = postResult.getResponse().getContentAsString();
    Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
    String paymentId = (String) response.get("id");

    mvc.perform(get("/api/v1/payments/" + paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(paymentId))
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.amount").value(1000));
  }

  @Test
  void getPayment_whenNotExists_shouldReturn404() throws Exception {
    mvc.perform(get("/api/v1/payments/" + UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }

  @Test
  void postPayment_withIdempotencyKey_shouldReturnSameResponse() throws Exception {
    PostPaymentRequest request = createValidPostPaymentRequest();
    UUID idempotencyKey = UUID.randomUUID();

    MvcResult firstResult = mvc.perform(post("/api/v1/payments")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andReturn();

    String firstResponseBody = firstResult.getResponse().getContentAsString();

    MvcResult secondResult = mvc.perform(post("/api/v1/payments")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andReturn();

    String secondResponseBody = secondResult.getResponse().getContentAsString();

    assertEquals(firstResponseBody, secondResponseBody);
  }

  private void assertEquals(Object expected, Object actual) {
    if (expected == null && actual == null) {
      return;
    }
    if (expected != null && expected.equals(actual)) {
      return;
    }
    throw new AssertionError("Expected: " + expected + ", but was: " + actual);
  }
}
