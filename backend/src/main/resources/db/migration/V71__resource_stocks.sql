-- =====================================================================
-- V71: Kaynak/ekipman stok yönetimi (bölgesel) + stok hareket geçmişi
-- =====================================================================

CREATE TABLE resource_stocks (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                  VARCHAR(200) NOT NULL,
    category              VARCHAR(50)  NOT NULL,
    quantity              INTEGER      NOT NULL DEFAULT 0,
    unit                  VARCHAR(30)  NOT NULL DEFAULT 'adet',
    daily_usage_estimate  DOUBLE PRECISION,
    critical_threshold    INTEGER      NOT NULL DEFAULT 0,
    district_id           UUID         NOT NULL REFERENCES districts(id),
    neighborhood_id       UUID         REFERENCES neighborhoods(id),
    warehouse_name        VARCHAR(200),
    notes                 TEXT,
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_by_user_id    UUID         REFERENCES users(id) ON DELETE SET NULL,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_resource_stock_district     ON resource_stocks(district_id);
CREATE INDEX idx_resource_stock_neighborhood ON resource_stocks(neighborhood_id);
CREATE INDEX idx_resource_stock_category     ON resource_stocks(category);

CREATE TABLE resource_stock_movements (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stock_id           UUID         NOT NULL REFERENCES resource_stocks(id) ON DELETE CASCADE,
    movement_type      VARCHAR(20)  NOT NULL,
    quantity_change    INTEGER      NOT NULL,
    previous_quantity  INTEGER      NOT NULL,
    new_quantity       INTEGER      NOT NULL,
    reason             TEXT,
    created_by_user_id UUID         REFERENCES users(id) ON DELETE SET NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_movement_stock ON resource_stock_movements(stock_id);
