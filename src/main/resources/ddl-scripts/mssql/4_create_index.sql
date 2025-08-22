USE tpce;
GO

SET ANSI_NULLS ON;
SET ANSI_NULL_DFLT_OFF ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
GO


CREATE INDEX ix_company_comp_nc1 ON company_competitor (cp_co_id);
GO

CREATE UNIQUE INDEX ix_broker_nc1 ON broker (b_name, b_id);
GO

CREATE UNIQUE INDEX ix_broker_nc2 ON broker (b_id, b_name);
GO

CREATE UNIQUE INDEX ix_company_nc1 ON company (co_name, co_id);
GO

CREATE UNIQUE INDEX ix_company_nc2 ON company (co_in_id, co_id);
GO

CREATE UNIQUE INDEX ix_customer_account_nc1 ON customer_account (ca_c_id, ca_id);
GO

CREATE UNIQUE INDEX ix_customer_nc1 ON customer (c_tax_id, c_id);
GO

CREATE UNIQUE INDEX ix_customer_nc2 ON customer (c_id, c_tier);
GO

CREATE UNIQUE INDEX ix_holding_history_nc1 ON holding_history (hh_t_id, hh_h_t_id) WITH (PAD_INDEX = ON, FILLFACTOR = 90);
GO

CREATE UNIQUE INDEX ix_holding_nc1 ON holding (h_ca_id, h_s_symb, h_dts, h_t_id) WITH (PAD_INDEX = ON, FILLFACTOR = 90);
GO

CREATE UNIQUE INDEX ix_industry_nc1 ON industry (in_name, in_id);
GO

CREATE UNIQUE INDEX ix_sector_nc1 ON sector (sc_name, sc_id);
GO

CREATE UNIQUE INDEX ix_security_nc1 ON security (s_co_id, s_issue, s_ex_id, s_symb);
GO

CREATE UNIQUE INDEX ix_trade_nc1 ON trade (t_ca_id, t_dts, t_id) WITH (pad_index = ON, FILLFACTOR = 90);
GO

CREATE UNIQUE INDEX ix_trade_nc2 ON trade (t_s_symb, t_dts, t_id) WITH (pad_index = ON);
GO

CREATE UNIQUE INDEX ix_trade_request_nc1 ON trade_request (tr_b_id ASC, tr_s_symb ASC, tr_t_id ASC) INCLUDE (tr_bid_price, tr_qty) WITH (ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON);
GO

CREATE UNIQUE INDEX ix_trade_request_nc2 ON trade_request ( tr_s_symb, tr_t_id, tr_tt_id, tr_bid_price, tr_qty );
GO

CREATE UNIQUE INDEX ix_tt_nc ON trade_type (tt_id, tt_is_mrkt, tt_is_sell, tt_name);
GO

CREATE UNIQUE INDEX ix_watch_list_nc1 ON watch_list (wl_c_id, wl_id);
GO