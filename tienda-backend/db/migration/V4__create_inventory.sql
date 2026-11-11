CREATE TABLE inventory (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variant_id         UUID NOT NULL UNIQUE REFERENCES product_variants(id) ON DELETE CASCADE,
    quantity_on_hand   INTEGER NOT NULL DEFAULT 0 CHECK (quantity_on_hand >= 0),
    quantity_reserved  INTEGER NOT NULL DEFAULT 0 CHECK (quantity_reserved >= 0),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE inventory_movements (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_id  UUID NOT NULL REFERENCES inventory(id),
    type          VARCHAR(20) NOT NULL CHECK (type IN ('ENTRADA','SALIDA','VENTA','DEVOLUCION','AJUSTE','ANULACION')),
    quantity      INTEGER NOT NULL,
    reason        VARCHAR(200),
    reference     VARCHAR(100),
    order_id      UUID,
    user_id       UUID REFERENCES users(id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_movements_inventory_id_created_at ON inventory_movements(inventory_id, created_at);
CREATE INDEX idx_movements_order_id ON inventory_movements(order_id);
