package com.checkout.payment.gateway.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("bank.simulator")
public record BankSimulatorProperties(
    String url,
    Timeout timeout
) {

  record Timeout(int connect, int read) {

  }
}
