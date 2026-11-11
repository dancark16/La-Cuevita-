CREATE TABLE stored_files (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kind          VARCHAR(30)  NOT NULL CHECK (kind IN ('PRODUCT_IMAGE','PAYMENT_RECEIPT')),
    original_name VARCHAR(255),
    content_type  VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    sha256        VARCHAR(64)  NOT NULL,
    storage_path  VARCHAR(500) NOT NULL,
    uploaded_by   UUID REFERENCES users(id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_stored_files_kind_sha256 ON stored_files(kind, sha256);
