-- Transportistas que el administrador maneja desde Configuración.
-- tracking_url lleva {guia} donde va el número de guía, ej: https://sitio.com/rastreo?guia={guia}
CREATE TABLE carriers (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(80) NOT NULL UNIQUE,
    tracking_url  VARCHAR(300),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order    INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO carriers (name, sort_order) VALUES
    ('Servientrega', 1),
    ('Tramaco Express', 2),
    ('Laar Courier', 3),
    ('Cooperativa de transporte', 4),
    ('Entrega propia (moto)', 5),
    ('Retiro en tienda', 6);

ALTER TABLE orders
    ADD COLUMN carrier_name     VARCHAR(80),
    ADD COLUMN tracking_number  VARCHAR(80),
    ADD COLUMN tracking_url     VARCHAR(500),
    ADD COLUMN delivered_at     TIMESTAMPTZ;

-- Novedades del envío que la clienta ve en su pedido
CREATE TABLE shipping_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    message     VARCHAR(300) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_shipping_events_order ON shipping_events(order_id, created_at);
