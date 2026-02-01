package com.checkout.payment.gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PaymentResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentCacheServiceTest {

  private PaymentCacheService cacheService;

  @BeforeEach
  void setUp() {
    cacheService = new PaymentCacheService();
  }

  private PaymentResponse createResponse(UUID id) {
    return PaymentResponse.builder()
        .id(id)
        .status(PaymentStatus.AUTHORIZED)
        .cardNumberLastFour(1234)
        .expiryMonth(12)
        .expiryYear(2027)
        .currency("USD")
        .amount(1000)
        .build();
  }

  @Test
  void cachePayment_withValidResponse_shouldCacheSuccessfully() {
    UUID id = UUID.randomUUID();
    PaymentResponse response = createResponse(id);
    UUID idempotencyKey = UUID.randomUUID();

    cacheService.cachePayment(response, idempotencyKey);

    Optional<PaymentResponse> cached = cacheService.getByPaymentId(id);
    assertTrue(cached.isPresent());
    assertEquals(id, cached.get().id());
  }

  @Test
  void cachePayment_withNullResponse_shouldNotCache() {
    cacheService.cachePayment(null, UUID.randomUUID());
  }

  @Test
  void cachePayment_withNullId_shouldNotCache() {
    PaymentResponse response = PaymentResponse.builder().id(null).build();

    cacheService.cachePayment(response, UUID.randomUUID());
  }

  @Test
  void cachePayment_withNullIdempotencyKey_shouldStillCacheByPaymentId() {
    UUID id = UUID.randomUUID();
    PaymentResponse response = createResponse(id);

    cacheService.cachePayment(response, null);

    Optional<PaymentResponse> cached = cacheService.getByPaymentId(id);
    assertTrue(cached.isPresent());
  }

  @Test
  void getByPaymentId_whenNotCached_shouldReturnEmpty() {
    Optional<PaymentResponse> result = cacheService.getByPaymentId(UUID.randomUUID());
    assertFalse(result.isPresent());
  }

  @Test
  void getByIdempotencyKey_whenCached_shouldReturnPayment() {
    UUID id = UUID.randomUUID();
    PaymentResponse response = createResponse(id);
    UUID idempotencyKey = UUID.randomUUID();

    cacheService.cachePayment(response, idempotencyKey);

    Optional<PaymentResponse> cached = cacheService.getByIdempotencyKey(idempotencyKey);
    assertTrue(cached.isPresent());
    assertEquals(id, cached.get().id());
  }

  @Test
  void getByIdempotencyKey_whenNotCached_shouldReturnEmpty() {
    Optional<PaymentResponse> result = cacheService.getByIdempotencyKey(UUID.randomUUID());
    assertFalse(result.isPresent());
  }

  @Test
  void getByIdempotencyKey_withNullKey_shouldReturnEmpty() {
    Optional<PaymentResponse> result = cacheService.getByIdempotencyKey(null);
    assertFalse(result.isPresent());
  }

  @Test
  void invalidate_shouldRemoveFromCache() {
    UUID id = UUID.randomUUID();
    PaymentResponse response = createResponse(id);
    UUID idempotencyKey = UUID.randomUUID();

    cacheService.cachePayment(response, idempotencyKey);
    assertTrue(cacheService.getByPaymentId(id).isPresent());

    cacheService.invalidate(id);

    assertFalse(cacheService.getByPaymentId(id).isPresent());
    assertFalse(cacheService.getByIdempotencyKey(idempotencyKey).isPresent());
  }

  @Test
  void invalidate_whenNotCached_shouldNotThrow() {
    cacheService.invalidate(UUID.randomUUID());
  }

  @Test
  void cachePayment_multipleTimes_shouldOverwrite() {
    UUID id = UUID.randomUUID();
    PaymentResponse response1 = createResponse(id);
    PaymentResponse response2 = PaymentResponse.builder()
        .id(id)
        .status(PaymentStatus.AUTHORIZED)
        .cardNumberLastFour(1234)
        .expiryMonth(12)
        .expiryYear(2027)
        .currency("USD")
        .amount(2000)
        .build();

    UUID key = UUID.randomUUID();
    cacheService.cachePayment(response1, key);
    cacheService.cachePayment(response2, key);

    Optional<PaymentResponse> cached = cacheService.getByPaymentId(id);
    assertTrue(cached.isPresent());
    assertEquals(2000, cached.get().amount());
  }

  @Test
  void getByIdempotencyKey_afterInvalidate_shouldReturnEmpty() {
    UUID id = UUID.randomUUID();
    PaymentResponse response = createResponse(id);
    UUID key = UUID.randomUUID();

    cacheService.cachePayment(response, key);
    cacheService.invalidate(id);

    assertFalse(cacheService.getByIdempotencyKey(key).isPresent());
  }

  @Test
  void cachePayment_withDifferentIdempotencyKeys_shouldTrackBoth() {
    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    PaymentResponse response1 = createResponse(id1);
    PaymentResponse response2 = createResponse(id2);

    UUID key1 = UUID.randomUUID();
    UUID key2 = UUID.randomUUID();
    cacheService.cachePayment(response1, key1);
    cacheService.cachePayment(response2, key2);

    assertTrue(cacheService.getByIdempotencyKey(key1).isPresent());
    assertTrue(cacheService.getByIdempotencyKey(key2).isPresent());
    assertEquals(id1, cacheService.getByIdempotencyKey(key1).get().id());
    assertEquals(id2, cacheService.getByIdempotencyKey(key2).get().id());
  }

  @Test
  void cachePayment_whenCacheFull_shouldEvictExpiredEntries() {
    PaymentCacheService smallCache = new PaymentCacheService(Duration.ofMillis(1), 2);

    UUID id1 = UUID.randomUUID();
    UUID id2 = UUID.randomUUID();
    UUID id3 = UUID.randomUUID();

    smallCache.cachePayment(createResponse(id1), UUID.randomUUID());
    smallCache.cachePayment(createResponse(id2), UUID.randomUUID());

    try {
      Thread.sleep(10);
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
    }

    smallCache.cachePayment(createResponse(id3), UUID.randomUUID());

    assertTrue(smallCache.getByPaymentId(id3).isPresent());
  }

  @Test
  void getByPaymentId_whenExpired_shouldReturnEmpty() {
    PaymentCacheService shortTtlCache = new PaymentCacheService(Duration.ofMillis(1), 100);

    UUID id = UUID.randomUUID();
    shortTtlCache.cachePayment(createResponse(id), UUID.randomUUID());

    try {
      Thread.sleep(10);
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
    }

    Optional<PaymentResponse> result = shortTtlCache.getByPaymentId(id);
    assertFalse(result.isPresent());
  }

  @Test
  void getByIdempotencyKey_whenPaymentExpired_shouldReturnEmpty() {
    PaymentCacheService shortTtlCache = new PaymentCacheService(Duration.ofMillis(1), 100);

    UUID id = UUID.randomUUID();
    UUID key = UUID.randomUUID();
    shortTtlCache.cachePayment(createResponse(id), key);

    try {
      Thread.sleep(10);
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
    }

    Optional<PaymentResponse> result = shortTtlCache.getByIdempotencyKey(key);
    assertFalse(result.isPresent());
  }
}
