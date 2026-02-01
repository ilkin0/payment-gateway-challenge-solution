package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.configuration.CacheProperties;
import com.checkout.payment.gateway.model.PaymentResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * In-memory cache for payment responses and idempotency keys.
 * <p>
 * In production, this should be replaced by a distributed cache like Redis, Valkey, or Memcached.
 */
@Slf4j
@Service
public class PaymentCacheService {

  private final Map<UUID, CacheEntry> paymentCache = new ConcurrentHashMap<>();
  private final Map<UUID, UUID> idempotencyKeyIndex = new ConcurrentHashMap<>();
  private final Duration cacheTtl;
  private final int maxCacheSize;

  public PaymentCacheService(CacheProperties cacheProperties) {
    this.cacheTtl = Duration.ofSeconds(cacheProperties.ttl());
    this.maxCacheSize = cacheProperties.maxSize();
  }

  public void cachePayment(PaymentResponse response, UUID idempotencyKey) {
    if (response == null || response.id() == null) {
      return;
    }

    if (paymentCache.size() >= maxCacheSize) {
      evictExpiredEntries();
    }

    UUID paymentId = response.id();
    paymentCache.put(paymentId, new CacheEntry(response, Instant.now()));

    if (idempotencyKey != null) {
      idempotencyKeyIndex.put(idempotencyKey, paymentId);
    }

    log.debug("Cached payment {} with idempotency key {}", paymentId, idempotencyKey);
  }

  public Optional<PaymentResponse> getByPaymentId(UUID paymentId) {
    CacheEntry entry = paymentCache.get(paymentId);
    if (entry == null) {
      return Optional.empty();
    }

    if (isExpired(entry)) {
      paymentCache.remove(paymentId);
      return Optional.empty();
    }

    log.debug("Cache hit for payment {}", paymentId);
    return Optional.of(entry.response);
  }

  public Optional<PaymentResponse> getByIdempotencyKey(UUID idempotencyKey) {
    if (idempotencyKey == null) {
      return Optional.empty();
    }

    UUID paymentId = idempotencyKeyIndex.get(idempotencyKey);
    if (paymentId == null) {
      return Optional.empty();
    }

    return getByPaymentId(paymentId);
  }

  public void invalidate(UUID paymentId) {
    CacheEntry entry = paymentCache.remove(paymentId);
    if (entry != null) {
      idempotencyKeyIndex.entrySet().removeIf(e -> e.getValue().equals(paymentId));
    }
  }

  private boolean isExpired(CacheEntry entry) {
    return Duration.between(entry.cachedAt, Instant.now()).compareTo(cacheTtl) > 0;
  }

  private void evictExpiredEntries() {
    int beforeSize = paymentCache.size();
    paymentCache.entrySet().removeIf(entry -> isExpired(entry.getValue()));

    idempotencyKeyIndex.entrySet().removeIf(entry -> !paymentCache.containsKey(entry.getValue()));
    log.info("Cache eviction: {} -> {} entries", beforeSize, paymentCache.size());
  }

  private record CacheEntry(PaymentResponse response, Instant cachedAt) {

  }
}
