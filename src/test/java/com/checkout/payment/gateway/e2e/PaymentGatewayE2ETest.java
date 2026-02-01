package com.checkout.payment.gateway.e2e;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.TestcontainersConfiguration;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.YearMonth;
import java.util.Map;
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
class PaymentGatewayE2ETest {

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("bank.simulator.url", () ->
        "http://" + TestcontainersConfiguration.getBankSimulatorContainer().getHost() + ":" +
            TestcontainersConfiguration.getBankSimulatorContainer().getMappedPort(8080));
  }

  @Autowired
  private MockMvc mvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private PostPaymentRequest createRequest(String cardNumber) {
    YearMonth future = YearMonth.now().plusMonths(6);
    return new PostPaymentRequest(
        cardNumber,
        future.getMonthValue(),
        future.getYear(),
        "USD",
        1000,
        "123"
    );
  }

  @Test
  void cardEndingIn1_shouldBeAuthorized() throws Exception {
    PostPaymentRequest request = createRequest("4111111111111111");

    MvcResult result = mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.cardNumberLastFour").value(1111))
        .andReturn();

    String responseBody = result.getResponse().getContentAsString();
    Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
    String paymentId = (String) response.get("id");

    mvc.perform(get("/api/v1/payments/" + paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Authorized"));
  }

  @Test
  void cardEndingIn3_shouldBeAuthorized() throws Exception {
    PostPaymentRequest request = createRequest("4111111111111113");

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Authorized"));
  }

  @Test
  void cardEndingIn5_shouldBeAuthorized() throws Exception {
    PostPaymentRequest request = createRequest("4111111111111115");

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Authorized"));
  }

  @Test
  void cardEndingIn2_shouldBeDeclined() throws Exception {
    PostPaymentRequest request = createRequest("4111111111111112");

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Declined"));
  }

  @Test
  void cardEndingIn4_shouldBeDeclined() throws Exception {
    PostPaymentRequest request = createRequest("4111111111111114");

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Declined"));
  }

  @Test
  void cardEndingIn8_shouldBeDeclined() throws Exception {
    PostPaymentRequest request = createRequest("4111111111111118");

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Declined"));
  }

  @Test
  void cardEndingIn0_shouldReturnBankUnavailable() throws Exception {
    PostPaymentRequest request = createRequest("4111111111111110");

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadGateway());
  }

  @Test
  void postPayment_withInvalidCardNumber_shouldReturn400() throws Exception {
    PostPaymentRequest request = createRequest("123");

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getPayment_whenExists_shouldReturn200() throws Exception {
    PostPaymentRequest request = createRequest("4111111111111111");
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
        .andExpect(jsonPath("$.id").value(paymentId));
  }
}
