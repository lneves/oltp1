USE tpce;

SET ANSI_NULLS ON;
SET ANSI_NULL_DFLT_OFF ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;
GO


ALTER TABLE account_permission WITH NOCHECK ADD CONSTRAINT fk_account_permission_customer_account FOREIGN KEY (ap_ca_id) REFERENCES customer_account(ca_id) ;
GO

ALTER TABLE address WITH NOCHECK ADD CONSTRAINT fk_address_zip_code FOREIGN KEY (ad_zc_code) REFERENCES zip_code(zc_code) ;
GO

ALTER TABLE broker WITH NOCHECK ADD CONSTRAINT fk_broker_status FOREIGN KEY (b_st_id) REFERENCES status_type(st_id) ;
GO

ALTER TABLE cash_transaction WITH NOCHECK ADD CONSTRAINT fk_cash_transaction_trade FOREIGN KEY (ct_t_id) REFERENCES trade(t_id) ;
GO

ALTER TABLE charge WITH NOCHECK ADD CONSTRAINT fk_charge_tradetype FOREIGN KEY (ch_tt_id) REFERENCES trade_type(tt_id) ;
GO

ALTER TABLE commission_rate WITH NOCHECK ADD CONSTRAINT fk_commission_rate_tradetype FOREIGN KEY (cr_tt_id) REFERENCES trade_type(tt_id) ;
GO

ALTER TABLE commission_rate WITH NOCHECK ADD CONSTRAINT fk_commission_rate_exchange FOREIGN KEY (cr_ex_id) REFERENCES exchange(ex_id) ;
GO

ALTER TABLE company WITH NOCHECK ADD CONSTRAINT fk_company_status FOREIGN KEY (co_st_id) REFERENCES status_type(st_id) ;
GO

ALTER TABLE company WITH NOCHECK ADD CONSTRAINT fk_company_address FOREIGN KEY (co_ad_id) REFERENCES address(ad_id) ;
GO

ALTER TABLE company WITH NOCHECK ADD CONSTRAINT fk_company_industry FOREIGN KEY (co_in_id) REFERENCES industry(in_id) ;
GO

ALTER TABLE company_competitor WITH NOCHECK ADD CONSTRAINT fk_company_comp_company1 FOREIGN KEY (cp_co_id) REFERENCES company(co_id) ;
GO

ALTER TABLE company_competitor WITH NOCHECK ADD CONSTRAINT fk_company_comp_company2 FOREIGN KEY (cp_comp_co_id) REFERENCES company(co_id) ;
GO

ALTER TABLE company_competitor WITH NOCHECK ADD CONSTRAINT fk_company_comp_industry FOREIGN KEY (cp_in_id) REFERENCES industry(in_id) ;
GO

ALTER TABLE customer WITH NOCHECK ADD CONSTRAINT fk_customer_status_type FOREIGN KEY (c_st_id) REFERENCES status_type(st_id) ;
GO

ALTER TABLE customer WITH NOCHECK ADD CONSTRAINT fk_customer_address FOREIGN KEY (c_ad_id) REFERENCES address(ad_id) ;
GO

ALTER TABLE customer_account WITH NOCHECK ADD CONSTRAINT fk_customer_account_broker FOREIGN KEY (ca_b_id) REFERENCES broker(b_id) ;
GO

ALTER TABLE customer_account WITH NOCHECK ADD CONSTRAINT fk_customer_account_customer FOREIGN KEY (ca_c_id) REFERENCES customer(c_id) ;
GO

ALTER TABLE customer_taxrate WITH NOCHECK ADD CONSTRAINT fk_customer_taxrate_tax FOREIGN KEY (cx_tx_id) REFERENCES taxrate(tx_id) ;
GO

ALTER TABLE customer_taxrate WITH NOCHECK ADD CONSTRAINT fk_customer_taxrate_customer FOREIGN KEY (cx_c_id) REFERENCES customer(c_id) ;
GO

ALTER TABLE daily_market WITH NOCHECK ADD CONSTRAINT fk_daily_market_security FOREIGN KEY (dm_s_symb) REFERENCES security(s_symb) ;
GO

ALTER TABLE exchange WITH NOCHECK ADD CONSTRAINT fk_exchange_address FOREIGN KEY (ex_ad_id) REFERENCES address(ad_id) ;
GO

ALTER TABLE financial WITH NOCHECK ADD CONSTRAINT fk_financial_company FOREIGN KEY (fi_co_id) REFERENCES company(co_id) ;
GO

ALTER TABLE industry WITH NOCHECK ADD CONSTRAINT fk_industry_sector FOREIGN KEY (in_sc_id) REFERENCES sector(sc_id) ;
GO

ALTER TABLE last_trade WITH NOCHECK ADD CONSTRAINT fk_last_trade_security FOREIGN KEY (lt_s_symb) REFERENCES security(s_symb) ;
GO

ALTER TABLE holding WITH NOCHECK ADD CONSTRAINT fk_holding_trade FOREIGN KEY (h_t_id) REFERENCES trade(t_id) ;
GO

ALTER TABLE holding WITH NOCHECK ADD CONSTRAINT fk_holding_holding_summary FOREIGN KEY (h_ca_id, h_s_symb) REFERENCES holding_summary (hs_ca_id, hs_s_symb) ;
GO

ALTER TABLE holding_history WITH NOCHECK ADD CONSTRAINT fk_holding_history_trade1 FOREIGN KEY (hh_h_t_id) REFERENCES trade(t_id) ;
GO

ALTER TABLE holding_history WITH NOCHECK ADD CONSTRAINT fk_holding_history_trade2 FOREIGN KEY (hh_t_id) REFERENCES trade(t_id) ;
GO

ALTER TABLE holding_summary WITH NOCHECK ADD CONSTRAINT fk_holding_summary_account FOREIGN KEY (hs_ca_id) REFERENCES customer_account(ca_id) ;
GO

ALTER TABLE holding_summary WITH NOCHECK ADD CONSTRAINT fk_holding_summary_security FOREIGN KEY (hs_s_symb) REFERENCES security(s_symb) ;
GO

ALTER TABLE news_xref WITH NOCHECK ADD CONSTRAINT fk_news_xref_news_item FOREIGN KEY (nx_ni_id) REFERENCES news_item(ni_id) ;
GO

ALTER TABLE news_xref WITH NOCHECK ADD CONSTRAINT fk_news_xref_company FOREIGN KEY (nx_co_id) REFERENCES company(co_id) ;
GO

ALTER TABLE security WITH NOCHECK ADD CONSTRAINT fk_security_status FOREIGN KEY (s_st_id) REFERENCES status_type(st_id) ;
GO

ALTER TABLE security WITH NOCHECK ADD CONSTRAINT fk_security_exchange FOREIGN KEY (s_ex_id) REFERENCES exchange(ex_id) ;
GO

ALTER TABLE security WITH NOCHECK ADD CONSTRAINT fk_security_company FOREIGN KEY (s_co_id) REFERENCES company(co_id) ;
GO

ALTER TABLE settlement WITH NOCHECK ADD CONSTRAINT fk_settlement_trade FOREIGN KEY (se_t_id) REFERENCES trade(t_id) ;
GO

ALTER TABLE trade WITH NOCHECK ADD CONSTRAINT fk_trade_status FOREIGN KEY (t_st_id) REFERENCES status_type(st_id) ;
GO

ALTER TABLE trade WITH NOCHECK ADD CONSTRAINT fk_trade_tradetype FOREIGN KEY (t_tt_id) REFERENCES trade_type(tt_id) ;
GO

ALTER TABLE trade WITH NOCHECK ADD CONSTRAINT fk_trade_security FOREIGN KEY (t_s_symb) REFERENCES security(s_symb) ;
GO

ALTER TABLE trade WITH NOCHECK ADD CONSTRAINT fk_trade_customer_account FOREIGN KEY (t_ca_id) REFERENCES customer_account(ca_id) ;
GO

ALTER TABLE trade_history WITH NOCHECK ADD CONSTRAINT fk_trade_history_trade FOREIGN KEY (th_t_id) REFERENCES trade(t_id) ;
GO

ALTER TABLE trade_history WITH NOCHECK ADD CONSTRAINT fk_trade_history_status FOREIGN KEY (th_st_id) REFERENCES status_type(st_id) ;
GO

ALTER TABLE trade_request WITH NOCHECK ADD CONSTRAINT fk_trade_request_trade FOREIGN KEY (tr_t_id) REFERENCES trade(t_id) ;
GO

ALTER TABLE trade_request WITH NOCHECK ADD CONSTRAINT fk_trade_request_tradetype FOREIGN KEY (tr_tt_id) REFERENCES trade_type(tt_id) ;
GO

ALTER TABLE trade_request WITH NOCHECK ADD CONSTRAINT fk_trade_request_security FOREIGN KEY (tr_s_symb) REFERENCES security(s_symb) ;
GO

ALTER TABLE trade_request WITH NOCHECK ADD CONSTRAINT fk_trade_request_broker FOREIGN KEY (tr_b_id) REFERENCES broker(b_id) ;
GO

ALTER TABLE watch_item WITH NOCHECK ADD CONSTRAINT fk_watch_item_watchlist FOREIGN KEY (wi_wl_id) REFERENCES watch_list(wl_id) ;
GO

ALTER TABLE watch_item WITH NOCHECK ADD CONSTRAINT fk_watch_item_security FOREIGN KEY (wi_s_symb) REFERENCES security(s_symb) ;
GO

ALTER TABLE watch_list WITH NOCHECK ADD CONSTRAINT fk_watch_list_customer FOREIGN KEY (wl_c_id) REFERENCES customer(c_id);
GO
