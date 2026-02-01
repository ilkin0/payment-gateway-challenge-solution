CREATE TABLE payments
(
  id                    UUID PRIMARY KEY         DEFAULT uuidv7(),
  card_number_last_four VARCHAR(4)  NOT NULL,
  expiry_month          INTEGER     NOT NULL,
  expiry_year           INTEGER     NOT NULL,
  currency              VARCHAR(3)  NOT NULL,
  amount                INTEGER     NOT NULL,
  status                VARCHAR(20) NOT NULL,
  authorization_code    VARCHAR(50),
  idempotency_key       UUID        NOT NULL,
  created_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_idempotency_key ON payments (idempotency_key);