package com.checkout.payment.gateway.repository;

import com.checkout.payment.gateway.entity.Payment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends CrudRepository<Payment, UUID> {

  Optional<Payment> findByIdempotencyKey(UUID key);
}
