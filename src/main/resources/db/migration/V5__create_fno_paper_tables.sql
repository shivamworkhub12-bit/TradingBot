CREATE TABLE fno_paper_portfolio (
    id BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (id),
    available_cash NUMERIC(19, 8) NOT NULL CHECK (available_cash >= 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO fno_paper_portfolio (id, available_cash) VALUES (TRUE, 100000) ON CONFLICT (id) DO NOTHING;

CREATE TABLE fno_paper_positions (
    trading_symbol VARCHAR(64) PRIMARY KEY,
    underlying VARCHAR(16) NOT NULL,
    expiry DATE NOT NULL,
    strike NUMERIC(19, 4) NOT NULL,
    option_type VARCHAR(2) NOT NULL,
    lot_size INTEGER NOT NULL,
    lots INTEGER NOT NULL,
    average_premium NUMERIC(19, 8) NOT NULL
);

CREATE TABLE fno_paper_fills (
    id UUID PRIMARY KEY,
    trading_symbol VARCHAR(64) NOT NULL,
    underlying VARCHAR(16) NOT NULL,
    expiry DATE NOT NULL,
    strike NUMERIC(19, 4) NOT NULL,
    option_type VARCHAR(2) NOT NULL,
    lot_size INTEGER NOT NULL,
    side VARCHAR(8) NOT NULL,
    lots INTEGER NOT NULL,
    premium NUMERIC(19, 8) NOT NULL,
    filled_at TIMESTAMPTZ NOT NULL
);
