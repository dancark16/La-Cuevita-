CREATE TABLE categories (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id   UUID REFERENCES categories(id),
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE sizes (
    id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    label  VARCHAR(10) NOT NULL UNIQUE
);

CREATE TABLE colors (
    id    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name  VARCHAR(50) NOT NULL UNIQUE,
    hex   VARCHAR(7) NOT NULL
);

CREATE TABLE products (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id  UUID NOT NULL REFERENCES categories(id),
    name         VARCHAR(150) NOT NULL,
    description  TEXT,
    base_status  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (base_status IN ('ACTIVE','INACTIVE','DRAFT')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_products_category_id ON products(category_id);

CREATE TABLE product_variants (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id   UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    size_id      UUID NOT NULL REFERENCES sizes(id),
    color_id     UUID NOT NULL REFERENCES colors(id),
    sku          VARCHAR(60) NOT NULL UNIQUE,
    barcode      VARCHAR(60),
    price        NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    promo_price  NUMERIC(10,2) CHECK (promo_price >= 0),
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','DISCONTINUED')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_variant_combo UNIQUE (product_id, size_id, color_id)
);

CREATE INDEX idx_variants_product_id ON product_variants(product_id);

CREATE TABLE product_images (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    variant_id  UUID NOT NULL REFERENCES product_variants(id) ON DELETE CASCADE,
    url         VARCHAR(500) NOT NULL,
    sort_order  INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_images_variant_id ON product_images(variant_id);
