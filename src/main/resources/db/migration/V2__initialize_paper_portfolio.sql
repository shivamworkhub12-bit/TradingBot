INSERT INTO paper_portfolio (id, available_cash, daily_realized_profit_loss)
VALUES (TRUE, 100000.0000, 0.0000)
ON CONFLICT (id) DO NOTHING;
