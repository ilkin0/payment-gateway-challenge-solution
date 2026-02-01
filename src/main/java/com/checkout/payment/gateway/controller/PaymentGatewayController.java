package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get payment by ID", description = "Retrieves a previously processed payment by its ID")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Payment found"),
      @ApiResponse(responseCode = "404", description = "Payment not found")
  })
  public ResponseEntity<PaymentResponse> getPostPaymentEventById(@PathVariable UUID id) {
    PaymentResponse paymentResponse = paymentGatewayService.getPaymentById(id);
    return new ResponseEntity<>(paymentResponse, HttpStatus.OK);
  }

  @PostMapping
  @Operation(summary = "Process a payment", description = "Processes a card payment through the acquiring bank")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Payment processed successfully"),
      @ApiResponse(responseCode = "400", description = "Invalid request data"),
      @ApiResponse(responseCode = "502", description = "Bank unavailable")
  })
  public ResponseEntity<PaymentResponse> processPayment(
      @Valid @RequestBody PostPaymentRequest request,
      @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey
  ) {
    PaymentResponse response = paymentGatewayService.processPayment(request, idempotencyKey);
    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}
