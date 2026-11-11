CREATE TABLE store_settings (
    id               SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    whatsapp_number  VARCHAR(20),
    instagram_user   VARCHAR(60),
    payphone_enabled BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO store_settings (id, whatsapp_number, instagram_user, payphone_enabled)
VALUES (1, NULL, 'lacuevitamach_', FALSE);

CREATE TABLE bank_accounts (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bank_name      VARCHAR(80)  NOT NULL,
    account_type   VARCHAR(20)  NOT NULL DEFAULT 'Ahorros',
    account_number VARCHAR(40)  NOT NULL,
    holder         VARCHAR(120) NOT NULL,
    holder_id      VARCHAR(20),
    sort_order     INTEGER      NOT NULL DEFAULT 0,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
