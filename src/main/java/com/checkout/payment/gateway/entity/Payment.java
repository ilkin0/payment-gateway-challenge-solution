package com.checkout.payment.gateway.entity;

import com.checkout.payment.gateway.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@ToString
@Getter
@Setter
@Table(name = "payments")
public class Payment {

  @Id
  @Column("id")
  private UUID id;

  @Column("card_number_last_four")
  private String cardNumberLastFour;

  @Column("expiry_month")
  private int expiryMonth;

  @Column("expiry_year")
  private int expiryYear;

  @Column("currency")
  private String currency;

  @Column("amount")
  private BigDecimal amount;

  @Column("status")
  private PaymentStatus status;

  @Column("authorization_code")
  private String authorizationCode;

  @Column("idempotency_key")
  private UUID idempotencyKey;

  @Column("created_at")
  private Instant createdAt;
}
