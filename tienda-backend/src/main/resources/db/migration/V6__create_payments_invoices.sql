CREATE TABLE payments (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id             UUID NOT NULL REFERENCES orders(id),
    method               VARCHAR(30) NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'INITIATED'
                         CHECK (status IN ('INITIATED','APPROVED','DECLINED','FAILED','REFUNDED')),
    amount               NUMERIC(10,2) NOT NULL,
    provider_reference   VARCHAR(150),
    idempotency_key      VARCHAR(150) NOT NULL UNIQUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_order_id ON payments(order_id);

CREATE TABLE invoices (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id             UUID NOT NULL UNIQUE REFERENCES orders(id),
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('PENDING','AUTHORIZED','REJECTED','FAILED','CANCELLED')),
    invoice_number       VARCHAR(30),
    access_key           VARCHAR(60),
    xml_url              VARCHAR(500),
    pdf_url              VARCHAR(500),
    external_reference   VARCHAR(150),
    idempotency_key      VARCHAR(150) NOT NULL UNIQUE,
    tax_amount           NUMERIC(10,2) NOT NULL DEFAULT 0,
    total                NUMERIC(10,2) NOT NULL DEFAULT 0,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE invoice_items (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id   UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description  VARCHAR(200) NOT NULL,
    quantity     INTEGER NOT NULL,
    unit_price   NUMERIC(10,2) NOT NULL,
    tax_rate     NUMERIC(5,2) NOT NULL,
    subtotal     NUMERIC(10,2) NOT NULL
);

CREATE INDEX idx_invoice_items_invoice_id ON invoice_items(invoice_id);
