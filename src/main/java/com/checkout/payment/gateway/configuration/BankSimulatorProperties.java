package com.checkout.payment.gateway.configuration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bank.simulator")
public record BankSimulatorProperties(
    @NotBlank String url,
    @NotNull Timeout timeout
) {

  record Timeout(@Positive int connect, @Positive int read) {

  }
}
