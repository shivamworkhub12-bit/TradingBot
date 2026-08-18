CREATE TABLE fno_strategy_decisions (
    id BIGSERIAL PRIMARY KEY,
    strategy_name VARCHAR(100) NOT NULL,
    symbol VARCHAR(100) NOT NULL,
    candle_closed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    direction VARCHAR(20) NOT NULL,
    fast_ema NUMERIC(20, 8),
    slow_ema NUMERIC(20, 8),
    rsi NUMERIC(10, 4),
    underlying_price NUMERIC(20, 8) NOT NULL,
    option_type VARCHAR(2),
    expiry DATE,
    strike NUMERIC(20, 4),
    trading_symbol VARCHAR(100),
    execution_status VARCHAR(40) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (strategy_name, symbol, candle_closed_at)
);

CREATE INDEX idx_fno_strategy_decisions_candle_time
    ON fno_strategy_decisions (candle_closed_at DESC);
