package com.checkout.payment.gateway.metrics;

import com.checkout.payment.gateway.enums.PaymentStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class PaymentMetrics {

  private static final String PAYMENT_PREFIX = "payment_gateway";

  private final Counter authorizedPayments;
  private final Counter declinedPayments;
  private final Counter rejectedPayments;
  private final Counter idempotentRequests;
  private final Counter cacheHits;
  private final Counter cacheMisses;
  private final Timer paymentProcessingTimer;
  private final Timer bankCallTimer;

  public PaymentMetrics(MeterRegistry registry) {
    // Payment outcome counters
    this.authorizedPayments = Counter.builder(PAYMENT_PREFIX + "_authorized_total")
        .description("Total number of authorized payments")
        .register(registry);

    this.declinedPayments = Counter.builder(PAYMENT_PREFIX + "_declined_total")
        .description("Total number of declined payments")
        .register(registry);

    this.rejectedPayments = Counter.builder(PAYMENT_PREFIX + "_rejected_total")
        .description("Total number of rejected payments (bank unavailable)")
        .register(registry);

    this.idempotentRequests = Counter.builder(PAYMENT_PREFIX + "_idempotent_requests_total")
        .description("Total number of idempotent request hits")
        .register(registry);

    // Cache metrics
    this.cacheHits = Counter.builder(PAYMENT_PREFIX + "_cache_hits_total")
        .description("Total number of cache hits")
        .register(registry);

    this.cacheMisses = Counter.builder(PAYMENT_PREFIX + "_cache_misses_total")
        .description("Total number of cache misses")
        .register(registry);

    // Timing metrics
    this.paymentProcessingTimer = Timer.builder(PAYMENT_PREFIX + "_processing_duration_seconds")
        .description("Time taken to process a payment")
        .register(registry);

    this.bankCallTimer = Timer.builder(PAYMENT_PREFIX + "_bank_call_duration_seconds")
        .description("Time taken for bank API calls")
        .register(registry);
  }

  public void recordPaymentOutcome(PaymentStatus status) {
    switch (status) {
      case AUTHORIZED -> authorizedPayments.increment();
      case DECLINED -> declinedPayments.increment();
      case REJECTED -> rejectedPayments.increment();
    }
  }

  public void recordIdempotentRequest() {
    idempotentRequests.increment();
  }

  public void recordCacheHit() {
    cacheHits.increment();
  }

  public void recordCacheMiss() {
    cacheMisses.increment();
  }

  public void recordPaymentProcessingTime(long durationMs) {
    paymentProcessingTimer.record(durationMs, TimeUnit.MILLISECONDS);
  }

  public void recordBankCallTime(long durationMs) {
    bankCallTimer.record(durationMs, TimeUnit.MILLISECONDS);
  }

  public Timer.Sample startTimer() {
    return Timer.start();
  }

  public void stopPaymentTimer(Timer.Sample sample) {
    sample.stop(paymentProcessingTimer);
  }

  public void stopBankCallTimer(Timer.Sample sample) {
    sample.stop(bankCallTimer);
  }
}
