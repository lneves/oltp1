package org.oltp1.runner.model;

/**
 * Represents all defined status codes for TPC-E transactions as per the v1.14.0
 * specification.
 * <p>
 * Status codes are categorized by the transaction they apply to.
 * <ul>
 * <li><b>SUCCESS (0):</b> Indicates successful completion.</li>
 * <li><b>Positive Codes (> 0):</b> Indicate warnings (e.g., no data found where
 * it was optional).</li>
 * <li><b>Negative Codes (< 0):</b> Indicate errors that invalidate a run.</li>
 * </ul>
 */
public enum TransactionStatus
{
	// General Success Code
	SUCCESS(0, "Transaction successful"),

	// Broker-Volume (Clause 3.3.1)
	BROKER_VOLUME_INVALID_LIST_LENGTH(-111, "list_len is out of range [0, max_broker_list_len]"),

	// Customer-Position (Clause 3.3.2)
	CUSTOMER_POSITION_INVALID_ACCT_LEN(-211, "acct_len is out of range [1, max_acct_len]"), CUSTOMER_POSITION_INVALID_HIST_LEN(-221, "hist_len is out of range [10, max_hist_len]"),

	// Market-Feed (Clause 3.3.3)
	MARKET_FEED_INVALID_UPDATE_COUNT(-311, "num_updated is less than unique_symbols"),

	// Market-Watch (Clause 3.3.4)
	MARKET_WATCH_BAD_INPUT_DATA(-411, "Bad input data: acct_id, cust_id, and industry_name are all invalid"),

	// Security-Detail (Clause 3.3.5)
	SECURITY_DETAIL_INVALID_DAY_LEN(-511, "day_len is out of range [min_day_len, max_day_len]"), SECURITY_DETAIL_INVALID_FIN_LEN(-512, "fin_len does not equal max_fin_len"), SECURITY_DETAIL_INVALID_NEWS_LEN(-513, "news_len does not equal max_news_len"),

	// Trade-Lookup (Clause 3.3.6)
	TRADE_LOOKUP_F1_INVALID_NUM_FOUND(-611, "num_found does not equal max_trades"), TRADE_LOOKUP_F2_INVALID_NUM_FOUND(-621, "num_found is out of range [0, max_trades]"), TRADE_LOOKUP_F2_WARN_NO_TRADES_FOUND(621, "Warning: num_found is 0"), TRADE_LOOKUP_F3_INVALID_NUM_FOUND(-631, "num_found is out of range [0, max_trades]"), TRADE_LOOKUP_F3_WARN_NO_TRADES_FOUND(631, "Warning: num_found is 0"), TRADE_LOOKUP_F4_INVALID_TRADE_COUNT(-641, "num_trades_found is not 1"), TRADE_LOOKUP_F4_WARN_NO_TRADES_FOUND(641, "Warning: num_trades_found is 0"), TRADE_LOOKUP_F4_INVALID_HIST_COUNT(-642, "num_found is out of range [1, 20]"),

	// Trade-Order (Clause 3.3.7)
	TRADE_ORDER_F1_NO_ACCOUNT_FOUND(-711, "num_found is not 1"), TRADE_ORDER_F2_UNAUTHORIZED(-721, "Executor does not have permission (ap_acl is empty)"), TRADE_ORDER_F3_TAX_AMOUNT_ERROR(-731, "tax_amount is <= 0.00 when it should be positive"), TRADE_ORDER_F3_COMMISSION_RATE_ERROR(-732, "comm_rate is <= 0.0000"), TRADE_ORDER_F3_CHARGE_AMOUNT_ERROR(-733, "charge_amount is 0.00"),

	// Trade-Result (Clause 3.3.8)
	TRADE_RESULT_F1_INVALID_TRADE_ID(-811, "num_found is not 1 for the given trade_id"), TRADE_RESULT_F3_TAX_AMOUNT_ERROR(-831, "tax_amount is <= 0.00 when it should be positive"), TRADE_RESULT_F4_COMMISSION_RATE_ERROR(-841, "comm_rate is <= 0.00"),

	// Trade-Status (Clause 3.3.9)
	TRADE_STATUS_F1_INVALID_TRADE_COUNT(-911, "num_found does not equal max_trade_status_len"),

	// Trade-Update (Clause 3.3.10)
	TRADE_UPDATE_F1_INVALID_NUM_FOUND(-1011, "num_found does not equal max_trades"), TRADE_UPDATE_F1_INVALID_UPDATE_COUNT(-1012, "num_updated does not equal max_updates"), TRADE_UPDATE_F2_INVALID_NUM_FOUND(-1021, "num_found is out of range [0, max_trades]"), TRADE_UPDATE_F2_WARN_NO_UPDATES(1021, "Warning: num_updated is 0"), TRADE_UPDATE_F2_UPDATE_MISMATCH(-1022, "num_updated does not equal num_found"), TRADE_UPDATE_F3_INVALID_NUM_FOUND(-1031, "num_found is out of range [0, max_trades]"), TRADE_UPDATE_F3_WARN_NO_UPDATES(1032, "Warning: num_updated is 0"), TRADE_UPDATE_F3_UPDATE_MISMATCH(-1032, "num_updated is greater than num_found");

	private final int code;
	private final String message;

	TransactionStatus(int code, String message)
	{
		this.code = code;
		this.message = message;
	}

	public int getCode()
	{
		return code;
	}

	public String getMessage()
	{
		return message;
	}

	public boolean isError()
	{
		return code < 0;
	}

	public boolean isWarning()
	{
		return code > 0;
	}
}
