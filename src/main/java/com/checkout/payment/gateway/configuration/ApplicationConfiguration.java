package com.checkout.payment.gateway.configuration;

import com.checkout.payment.gateway.client.BankClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
@EnableConfigurationProperties({BankSimulatorProperties.class, CacheProperties.class})
public class ApplicationConfiguration {

  @Bean
  public RestClient bankSimulatorRestClient(BankSimulatorProperties bankSimulatorProperties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofMillis(bankSimulatorProperties.timeout().connect()));
    requestFactory.setReadTimeout(Duration.ofMillis(bankSimulatorProperties.timeout().read()));

    return RestClient.builder()
        .baseUrl(bankSimulatorProperties.url())
        .requestFactory(requestFactory)
        .build();
  }

  @Bean
  public BankClient bankClient(RestClient restClient) {
    HttpServiceProxyFactory factory = HttpServiceProxyFactory
        .builderFor(RestClientAdapter.create(restClient))
        .build();
    return factory.createClient(BankClient.class);
  }
}
