package com.checkout.payment.gateway.configuration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "application.cache")
public record CacheProperties(
    @NotNull @Positive Long ttl,
    @NotNull @Positive Integer maxSize
) {

}
