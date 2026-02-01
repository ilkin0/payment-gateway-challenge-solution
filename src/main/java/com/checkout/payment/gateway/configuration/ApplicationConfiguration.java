package com.checkout.payment.gateway.configuration;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(BankSimulatorProperties.class)
public class ApplicationConfiguration {

  @Bean
  public WebClient bankSimulatorClient(BankSimulatorProperties bankSimulatorProperties) {

    HttpClient httpClient = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, bankSimulatorProperties.timeout().connect())
        .responseTimeout(Duration.ofMillis(bankSimulatorProperties.timeout().read()));

    return WebClient.builder()
        .baseUrl(bankSimulatorProperties.url())
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
  }
}
