CREATE TABLE strategy_signals (
    id UUID PRIMARY KEY,
    symbol VARCHAR(64) NOT NULL,
    action VARCHAR(16) NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE paper_fills (
    id UUID PRIMARY KEY,
    symbol VARCHAR(64) NOT NULL,
    side VARCHAR(8) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    fill_price NUMERIC(19, 4) NOT NULL CHECK (fill_price > 0),
    filled_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE paper_portfolio (
    id BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (id),
    available_cash NUMERIC(19, 4) NOT NULL CHECK (available_cash >= 0),
    daily_realized_profit_loss NUMERIC(19, 4) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE paper_positions (
    symbol VARCHAR(64) PRIMARY KEY,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    average_price NUMERIC(19, 4) NOT NULL CHECK (average_price > 0),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
