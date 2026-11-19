-- Datos del emisor que salen en la factura (estilo SRI). Se editan en Configuración.
ALTER TABLE store_settings
    ADD COLUMN business_name       VARCHAR(200),
    ADD COLUMN trade_name          VARCHAR(200),
    ADD COLUMN ruc                 VARCHAR(13),
    ADD COLUMN head_office_address VARCHAR(300),
    ADD COLUMN branch_address      VARCHAR(300),
    ADD COLUMN special_taxpayer    VARCHAR(30),
    ADD COLUMN accounting_required BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE store_settings SET trade_name = 'La Cuevita' WHERE id = 1;
