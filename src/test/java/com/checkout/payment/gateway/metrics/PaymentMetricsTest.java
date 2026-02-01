package com.checkout.payment.gateway.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.checkout.payment.gateway.enums.PaymentStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentMetricsTest {

  private MeterRegistry registry;
  private PaymentMetrics metrics;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
    metrics = new PaymentMetrics(registry);
  }

  @Test
  void recordPaymentOutcome_authorized_shouldIncrementCounter() {
    metrics.recordPaymentOutcome(PaymentStatus.AUTHORIZED);

    double count = registry.counter("payment_gateway_authorized_total").count();
    assertEquals(1.0, count);
  }

  @Test
  void recordPaymentOutcome_declined_shouldIncrementCounter() {
    metrics.recordPaymentOutcome(PaymentStatus.DECLINED);

    double count = registry.counter("payment_gateway_declined_total").count();
    assertEquals(1.0, count);
  }

  @Test
  void recordPaymentOutcome_rejected_shouldIncrementCounter() {
    metrics.recordPaymentOutcome(PaymentStatus.REJECTED);

    double count = registry.counter("payment_gateway_rejected_total").count();
    assertEquals(1.0, count);
  }

  @Test
  void recordIdempotentRequest_shouldIncrementCounter() {
    metrics.recordIdempotentRequest();
    metrics.recordIdempotentRequest();

    double count = registry.counter("payment_gateway_idempotent_requests_total").count();
    assertEquals(2.0, count);
  }

  @Test
  void recordCacheHit_shouldIncrementCounter() {
    metrics.recordCacheHit();

    double count = registry.counter("payment_gateway_cache_hits_total").count();
    assertEquals(1.0, count);
  }

  @Test
  void recordCacheMiss_shouldIncrementCounter() {
    metrics.recordCacheMiss();

    double count = registry.counter("payment_gateway_cache_misses_total").count();
    assertEquals(1.0, count);
  }

  @Test
  void recordPaymentProcessingTime_shouldRecordTimer() {
    metrics.recordPaymentProcessingTime(100);

    Timer timer = registry.timer("payment_gateway_processing_duration_seconds");
    assertEquals(1, timer.count());
  }

  @Test
  void recordBankCallTime_shouldRecordTimer() {
    metrics.recordBankCallTime(50);

    Timer timer = registry.timer("payment_gateway_bank_call_duration_seconds");
    assertEquals(1, timer.count());
  }

  @Test
  void startTimer_shouldReturnSample() {
    Timer.Sample sample = metrics.startTimer();
    assertNotNull(sample);
  }

  @Test
  void stopPaymentTimer_shouldRecordDuration() {
    Timer.Sample sample = metrics.startTimer();
    metrics.stopPaymentTimer(sample);

    Timer timer = registry.timer("payment_gateway_processing_duration_seconds");
    assertEquals(1, timer.count());
  }

  @Test
  void stopBankCallTimer_shouldRecordDuration() {
    Timer.Sample sample = metrics.startTimer();
    metrics.stopBankCallTimer(sample);

    Timer timer = registry.timer("payment_gateway_bank_call_duration_seconds");
    assertEquals(1, timer.count());
  }
}
