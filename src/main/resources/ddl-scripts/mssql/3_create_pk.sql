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


CREATE UNIQUE CLUSTERED INDEX pk_account_permission ON account_permission (ap_ca_id, ap_tax_id);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_address ON address (ad_id);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_broker ON broker (b_id) WITH (FILLFACTOR = 75);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_cash_transaction ON cash_transaction (ct_t_id) WITH (FILLFACTOR = 95);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_charge ON charge (ch_tt_id, ch_c_tier);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_commission_rate ON commission_rate (cr_c_tier, cr_tt_id, cr_ex_id, cr_from_qty);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_company ON company (co_id);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_company_competitor ON company_competitor (cp_co_id, cp_comp_co_id, cp_in_id);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_customer ON customer (c_id);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_customer_account ON customer_account (ca_id) WITH (FILLFACTOR = 82);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_customer_taxrate ON customer_taxrate (cx_c_id, cx_tx_id);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_daily_market ON daily_market (dm_s_symb, dm_date);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_exchange ON exchange (ex_id);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_financial ON financial (fi_co_id, fi_year, fi_qtr);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_holding ON holding (h_t_id) WITH (FILLFACTOR = 80);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_holding_history ON holding_history (hh_h_t_id, hh_t_id) WITH (FILLFACTOR = 95);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_holding_summary ON holding_summary (hs_ca_id, hs_s_symb) WITH (FILLFACTOR = 75);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_industry ON industry (in_id);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_last_trade ON last_trade (lt_s_symb) WITH (FILLFACTOR = 75);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_news_item ON news_item (ni_id);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_news_xref ON news_xref (nx_co_id, nx_ni_id);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_sector ON sector (sc_id);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_security ON security (s_symb);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_settlement ON settlement (se_t_id) WITH (FILLFACTOR = 92);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_status_type ON status_type (st_id);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_taxrate ON taxrate (tx_id);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_trade ON trade (t_id) WITH (FILLFACTOR = 92);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_trade_history ON trade_history (th_t_id, th_st_id) WITH (FILLFACTOR = 95);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_trade_request ON trade_request (tr_t_id);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_trade_type ON trade_type (tt_id);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_watch_item ON watch_item (wi_wl_id, wi_s_symb);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_watch_list ON watch_list (wl_id);
GO
 
CREATE UNIQUE CLUSTERED INDEX pk_zip_code ON zip_code (zc_code);
GO
