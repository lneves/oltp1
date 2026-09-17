SET FOREIGN_KEY_CHECKS=0;

CREATE INDEX ix_company_comp_nc1 ON company_competitor (cp_co_id);

CREATE UNIQUE INDEX ix_broker_nc1 ON broker (b_name, b_id);
CREATE UNIQUE INDEX ix_broker_nc2 ON broker (b_id, b_name);

CREATE UNIQUE INDEX ix_company_nc1 ON company (co_name, co_id);
CREATE UNIQUE INDEX ix_company_nc2 ON company (co_in_id, co_id);

CREATE UNIQUE INDEX ix_customer_account_nc1 ON customer_account (ca_c_id, ca_id);

CREATE UNIQUE INDEX ix_customer_nc1 ON customer (c_tax_id, c_id);
CREATE UNIQUE INDEX ix_customer_nc2 ON customer (c_id, c_tier);

CREATE UNIQUE INDEX ix_holding_history_nc1 ON holding_history (hh_t_id, hh_h_t_id);
CREATE UNIQUE INDEX ix_holding_nc1 ON holding (h_ca_id, h_s_symb, h_dts, h_t_id);

CREATE UNIQUE INDEX ix_industry_nc1 ON industry (in_name, in_id);
CREATE UNIQUE INDEX ix_sector_nc1 ON sector (sc_name, sc_id);

CREATE UNIQUE INDEX ix_security_nc1 ON security (s_co_id, s_issue, s_ex_id, s_symb);

CREATE UNIQUE INDEX ix_trade_nc1 ON trade (t_ca_id, t_dts, t_id);
CREATE UNIQUE INDEX ix_trade_nc2 ON trade (t_s_symb, t_dts, t_id);

CREATE UNIQUE INDEX ix_trade_request_nc1 ON trade_request (tr_b_id, tr_s_symb, tr_t_id, tr_bid_price, tr_qty);
CREATE UNIQUE INDEX ix_trade_request_nc2 ON trade_request (tr_s_symb, tr_t_id, tr_tt_id, tr_bid_price, tr_qty);

CREATE UNIQUE INDEX ix_tt_nc ON trade_type (tt_id, tt_is_mrkt, tt_is_sell, tt_name);

CREATE UNIQUE INDEX ix_watch_list_nc1 ON watch_list (wl_c_id, wl_id);


-- Foreign keys (columns only; the indexes above are reused where possible).

ALTER TABLE broker
	ADD CONSTRAINT fk_broker_st FOREIGN KEY (b_st_id) REFERENCES status_type (st_id); 

ALTER TABLE customer
	ADD CONSTRAINT fk_customer_ad FOREIGN KEY (c_ad_id) REFERENCES address (ad_id),
	ADD CONSTRAINT fk_customer_st FOREIGN KEY (c_st_id) REFERENCES status_type (st_id);

ALTER TABLE company
	ADD CONSTRAINT fk_company_ad FOREIGN KEY (co_ad_id) REFERENCES address (ad_id),
	ADD CONSTRAINT fk_company_in FOREIGN KEY (co_in_id) REFERENCES industry (in_id),
	ADD CONSTRAINT fk_company_st FOREIGN KEY (co_st_id) REFERENCES status_type (st_id);

ALTER TABLE holding
	ADD CONSTRAINT fk_holding_hs FOREIGN KEY (h_ca_id, h_s_symb) REFERENCES holding_summary (hs_ca_id, hs_s_symb),
	ADD CONSTRAINT fk_holding_t FOREIGN KEY (h_t_id) REFERENCES trade (t_id);

ALTER TABLE trade
	ADD CONSTRAINT fk_trade_ca FOREIGN KEY (t_ca_id) REFERENCES customer_account (ca_id),
	ADD CONSTRAINT fk_trade_s FOREIGN KEY (t_s_symb) REFERENCES security (s_symb),
	ADD CONSTRAINT fk_trade_st FOREIGN KEY (t_st_id) REFERENCES status_type (st_id),
	ADD CONSTRAINT fk_trade_tt FOREIGN KEY (t_tt_id) REFERENCES trade_type (tt_id); 

ALTER TABLE trade_history
	ADD CONSTRAINT fk_trade_history_st FOREIGN KEY (th_st_id) REFERENCES status_type (st_id),
	ADD CONSTRAINT fk_trade_history_t FOREIGN KEY (th_t_id) REFERENCES trade (t_id);

ALTER TABLE commission_rate
	ADD CONSTRAINT fk_commission_rate_ex FOREIGN KEY (cr_ex_id) REFERENCES exchange (ex_id),
	ADD CONSTRAINT fk_commission_rate_tt FOREIGN KEY (cr_tt_id) REFERENCES trade_type (tt_id);

ALTER TABLE company_competitor
	ADD CONSTRAINT fk_company_competitor_co1 FOREIGN KEY (cp_co_id) REFERENCES company (co_id),
	ADD CONSTRAINT fk_company_competitor_co2 FOREIGN KEY (cp_comp_co_id) REFERENCES company (co_id),
	ADD CONSTRAINT fk_company_competitor_in FOREIGN KEY (cp_in_id) REFERENCES industry (in_id);

ALTER TABLE customer_account
	ADD CONSTRAINT fk_customer_account_b FOREIGN KEY (ca_b_id) REFERENCES broker (b_id),
	ADD CONSTRAINT fk_customer_account_c FOREIGN KEY (ca_c_id) REFERENCES customer (c_id); 

ALTER TABLE customer_taxrate
	ADD CONSTRAINT fk_customer_taxrate_c FOREIGN KEY (cx_c_id) REFERENCES customer (c_id),
	ADD CONSTRAINT fk_customer_taxrate_tx FOREIGN KEY (cx_tx_id) REFERENCES taxrate (tx_id);
 
ALTER TABLE holding_history
	ADD CONSTRAINT fk_holding_history_t1 FOREIGN KEY (hh_h_t_id) REFERENCES trade (t_id),
	ADD CONSTRAINT fk_holding_history_t2 FOREIGN KEY (hh_t_id) REFERENCES trade (t_id);

ALTER TABLE holding_summary
	ADD CONSTRAINT fk_holding_summary_ca FOREIGN KEY (hs_ca_id) REFERENCES customer_account (ca_id),
	ADD CONSTRAINT fk_holding_summary_s FOREIGN KEY (hs_s_symb) REFERENCES security (s_symb);

ALTER TABLE industry
	ADD CONSTRAINT fk_industry_sc FOREIGN KEY (in_sc_id) REFERENCES sector (sc_id);

ALTER TABLE news_xref
	ADD CONSTRAINT fk_news_xref_co FOREIGN KEY (nx_co_id) REFERENCES company (co_id),
	ADD CONSTRAINT fk_news_xref_ni FOREIGN KEY (nx_ni_id) REFERENCES news_item (ni_id);

ALTER TABLE security
	ADD CONSTRAINT fk_security_co FOREIGN KEY (s_co_id) REFERENCES company (co_id),
	ADD CONSTRAINT fk_security_ex FOREIGN KEY (s_ex_id) REFERENCES exchange (ex_id),
	ADD CONSTRAINT fk_security_st FOREIGN KEY (s_st_id) REFERENCES status_type (st_id); 

ALTER TABLE trade_request
	ADD CONSTRAINT fk_trade_request_b FOREIGN KEY (tr_b_id) REFERENCES broker (b_id),
	ADD CONSTRAINT fk_trade_request_s FOREIGN KEY (tr_s_symb) REFERENCES security (s_symb),
	ADD CONSTRAINT fk_trade_request_t FOREIGN KEY (tr_t_id) REFERENCES trade (t_id),
	ADD CONSTRAINT fk_trade_request_tt FOREIGN KEY (tr_tt_id) REFERENCES trade_type (tt_id); 

ALTER TABLE watch_list
	ADD CONSTRAINT fk_watch_list_c FOREIGN KEY (wl_c_id) REFERENCES customer (c_id);

ALTER TABLE watch_item
	ADD CONSTRAINT fk_watch_item_s FOREIGN KEY (wi_s_symb) REFERENCES security (s_symb),
	ADD CONSTRAINT fk_watch_item_wl FOREIGN KEY (wi_wl_id) REFERENCES watch_list (wl_id);


ALTER TABLE last_trade ADD CONSTRAINT fk_last_trade_s FOREIGN KEY (lt_s_symb) REFERENCES security (s_symb);
ALTER TABLE account_permission ADD CONSTRAINT fk_account_permission_ca FOREIGN KEY (ap_ca_id) REFERENCES customer_account (ca_id);
ALTER TABLE address ADD CONSTRAINT fk_address_zc FOREIGN KEY (ad_zc_code) REFERENCES zip_code (zc_code);
ALTER TABLE cash_transaction ADD CONSTRAINT fk_cash_transaction_t FOREIGN KEY (ct_t_id) REFERENCES trade (t_id);
ALTER TABLE charge ADD CONSTRAINT fk_charge_tt FOREIGN KEY (ch_tt_id) REFERENCES trade_type (tt_id);
ALTER TABLE daily_market ADD CONSTRAINT fk_daily_market_s FOREIGN KEY (dm_s_symb) REFERENCES security (s_symb);
ALTER TABLE exchange ADD CONSTRAINT fk_exchange_ad FOREIGN KEY (ex_ad_id) REFERENCES address (ad_id);
ALTER TABLE financial ADD CONSTRAINT fk_financial_co FOREIGN KEY (fi_co_id) REFERENCES company (co_id);
ALTER TABLE settlement ADD CONSTRAINT fk_settlement_t FOREIGN KEY (se_t_id) REFERENCES trade (t_id);


SET FOREIGN_KEY_CHECKS=1;

