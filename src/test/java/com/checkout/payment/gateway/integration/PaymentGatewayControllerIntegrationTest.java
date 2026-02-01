package com.checkout.payment.gateway.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.TestcontainersConfiguration;
import com.checkout.payment.gateway.client.BankClient;
import com.checkout.payment.gateway.client.BankPaymentRequest;
import com.checkout.payment.gateway.client.BankPaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.YearMonth;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PaymentGatewayControllerIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private PaymentRepository paymentRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean
  private BankClient bankClient;

  @BeforeEach
  void setUp() {
    paymentRepository.deleteAll();
  }

  private PostPaymentRequest createValidRequest() {
    YearMonth future = YearMonth.now().plusMonths(6);
    return new PostPaymentRequest(
        "4111111111111111",
        future.getMonthValue(),
        future.getYear(),
        "USD",
        1000,
        "123"
    );
  }

  @Test
  void fullPaymentFlow_createAndRetrieve() throws Exception {
    when(bankClient.processPayment(any(BankPaymentRequest.class)))
        .thenReturn(new BankPaymentResponse(true, "AUTH123"));

    PostPaymentRequest request = createValidRequest();

    MvcResult postResult = mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.id").exists())
        .andReturn();

    String responseBody = postResult.getResponse().getContentAsString();
    Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
    String paymentId = (String) response.get("id");

    assertNotNull(paymentId);
    assertEquals(1, paymentRepository.count());

    mvc.perform(get("/api/v1/payments/" + paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(paymentId))
        .andExpect(jsonPath("$.status").value("Authorized"));
  }

  @Test
  void multiplePayments_shouldAllPersist() throws Exception {
    when(bankClient.processPayment(any(BankPaymentRequest.class)))
        .thenReturn(new BankPaymentResponse(true, "AUTH123"));

    PostPaymentRequest request1 = createValidRequest();
    PostPaymentRequest request2 = createValidRequest();
    PostPaymentRequest request3 = createValidRequest();

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request1)))
        .andExpect(status().isOk());

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request2)))
        .andExpect(status().isOk());

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request3)))
        .andExpect(status().isOk());

    assertEquals(3, paymentRepository.count());
  }

  @Test
  void declinedPayment_shouldStillPersist() throws Exception {
    when(bankClient.processPayment(any(BankPaymentRequest.class)))
        .thenReturn(new BankPaymentResponse(false, null));

    PostPaymentRequest request = createValidRequest();

    MvcResult postResult = mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Declined"))
        .andReturn();

    String responseBody = postResult.getResponse().getContentAsString();
    Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);
    String paymentId = (String) response.get("id");

    assertEquals(1, paymentRepository.count());

    mvc.perform(get("/api/v1/payments/" + paymentId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("Declined"));
  }

  @Test
  void bankUnavailable_shouldNotPersist() throws Exception {
    when(bankClient.processPayment(any(BankPaymentRequest.class)))
        .thenThrow(new RuntimeException("Bank down"));

    PostPaymentRequest request = createValidRequest();

    mvc.perform(post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadGateway());

    assertEquals(0, paymentRepository.count());
  }
}
