ALTER TABLE paper_fills
    ALTER COLUMN fill_price TYPE NUMERIC(19, 8),
    ALTER COLUMN transaction_cost TYPE NUMERIC(19, 8);

ALTER TABLE paper_positions
    ALTER COLUMN average_price TYPE NUMERIC(19, 8);

ALTER TABLE paper_portfolio
    ALTER COLUMN available_cash TYPE NUMERIC(19, 8),
    ALTER COLUMN daily_realized_profit_loss TYPE NUMERIC(19, 8);
