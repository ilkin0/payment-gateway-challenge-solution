package com.checkout.payment.gateway.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@RestClientTest
class BankClientTest {

  private BankClient bankClient;

  @Autowired
  private RestClient.Builder builder;

  private MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    server = MockRestServiceServer.bindTo(builder).build();
    RestClient restClient = builder.baseUrl("http://localhost:8080").build();
    HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(RestClientAdapter.create(restClient))
        .build();
    bankClient = factory.createClient(BankClient.class);
  }

  @Test
  void processPayment_whenBankAuthorizes_shouldReturnAuthorizedResponse() {
    this.server.expect(requestTo("http://localhost:8080/payments"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(jsonPath("$.card_number").value("4111111111111111"))
        .andRespond(withSuccess("""
            {
              "authorized": true,
              "authorization_code": "AUTH123"
            }
            """, MediaType.APPLICATION_JSON));

    BankPaymentRequest request = new BankPaymentRequest(
        "4111111111111111",
        "12/2027",
        "USD",
        1000,
        "123"
    );

    BankPaymentResponse response = bankClient.processPayment(request);

    assertTrue(response.authorized());
    assertEquals("AUTH123", response.authorizationCode());
  }

  @Test
  void processPayment_whenBankDeclines_shouldReturnDeclinedResponse() {
    this.server.expect(requestTo("http://localhost:8080/payments"))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("""
            {
              "authorized": false,
              "authorization_code": null
            }
            """, MediaType.APPLICATION_JSON));

    BankPaymentRequest request = new BankPaymentRequest(
        "4111111111111111",
        "12/2027",
        "USD",
        1000,
        "123"
    );

    BankPaymentResponse response = bankClient.processPayment(request);

    assertFalse(response.authorized());
    assertNull(response.authorizationCode());
  }
}
