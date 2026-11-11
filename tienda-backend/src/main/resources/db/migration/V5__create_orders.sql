CREATE TABLE coupons (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(40) NOT NULL UNIQUE,
    discount_type   VARCHAR(20) NOT NULL CHECK (discount_type IN ('PERCENTAGE','FIXED')),
    discount_value  NUMERIC(10,2) NOT NULL CHECK (discount_value >= 0),
    valid_from      TIMESTAMPTZ NOT NULL,
    valid_until     TIMESTAMPTZ NOT NULL,
    usage_limit     INTEGER,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE orders (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id  UUID NOT NULL REFERENCES customers(id),
    address_id   UUID NOT NULL REFERENCES addresses(id),
    coupon_id    UUID REFERENCES coupons(id),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                 CHECK (status IN ('PENDING','PAID','PROCESSING','SHIPPED','DELIVERED','CANCELLED','CONFIRMED')),
    subtotal     NUMERIC(10,2) NOT NULL DEFAULT 0,
    discount     NUMERIC(10,2) NOT NULL DEFAULT 0,
    total        NUMERIC(10,2) NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_customer_id_status ON orders(customer_id, status);

ALTER TABLE inventory_movements
    ADD CONSTRAINT fk_movements_order FOREIGN KEY (order_id) REFERENCES orders(id);

CREATE TABLE order_items (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    variant_id   UUID NOT NULL REFERENCES product_variants(id),
    quantity     INTEGER NOT NULL CHECK (quantity > 0),
    unit_price   NUMERIC(10,2) NOT NULL,
    subtotal     NUMERIC(10,2) NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
