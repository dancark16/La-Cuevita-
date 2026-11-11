CREATE TABLE shipping (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id         UUID NOT NULL UNIQUE REFERENCES orders(id),
    carrier          VARCHAR(80),
    tracking_number  VARCHAR(80),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING','SHIPPED','DELIVERED','RETURNED')),
    shipped_at       TIMESTAMPTZ
);

CREATE TABLE audit_logs (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID REFERENCES users(id),
    entity         VARCHAR(60) NOT NULL,
    entity_id      UUID NOT NULL,
    action         VARCHAR(30) NOT NULL,
    payload_diff   JSONB,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity, entity_id);
