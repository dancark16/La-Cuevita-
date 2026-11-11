CREATE TABLE customers (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID UNIQUE REFERENCES users(id),
    id_type        VARCHAR(20) NOT NULL CHECK (id_type IN ('CEDULA','RUC','PASAPORTE')),
    id_number      VARCHAR(20) NOT NULL,
    first_name     VARCHAR(100),
    last_name      VARCHAR(100),
    business_name  VARCHAR(150),
    email          VARCHAR(150) NOT NULL,
    phone          VARCHAR(20),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_customers_id_number UNIQUE (id_type, id_number)
);

CREATE TABLE addresses (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id  UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    line1        VARCHAR(200) NOT NULL,
    line2        VARCHAR(200),
    city         VARCHAR(100) NOT NULL,
    province     VARCHAR(100) NOT NULL,
    reference    VARCHAR(200),
    is_default   BOOLEAN NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_addresses_customer_id ON addresses(customer_id);
