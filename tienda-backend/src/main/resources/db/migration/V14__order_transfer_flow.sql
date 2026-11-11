CREATE SEQUENCE order_number_seq START 1001;

ALTER TABLE orders
    ADD COLUMN order_number         BIGINT,
    ADD COLUMN payment_method       VARCHAR(30),
    ADD COLUMN receipt_file_id      UUID REFERENCES stored_files(id),
    ADD COLUMN receipt_uploaded_at  TIMESTAMPTZ,
    ADD COLUMN admin_note           VARCHAR(500),
    ADD COLUMN paid_at              TIMESTAMPTZ,
    ADD COLUMN shipped_at           TIMESTAMPTZ;

UPDATE orders SET order_number = nextval('order_number_seq') WHERE order_number IS NULL;

ALTER TABLE orders ALTER COLUMN order_number SET NOT NULL;

CREATE UNIQUE INDEX uq_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_status_created_at ON orders(status, created_at DESC);
