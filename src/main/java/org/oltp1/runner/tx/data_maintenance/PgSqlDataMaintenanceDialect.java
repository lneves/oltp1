package org.oltp1.runner.tx.data_maintenance;

/**
 * Provides the PostgreSQL-specific SQL queries for the Data-Maintenance
 * transaction.
 */
public class PgSqlDataMaintenanceDialect extends DefaultDataMaintenanceQueries
{
	@Override
	public String countFinancialOnFirstOfMonth()
	{
		return "SELECT COUNT(*) FROM financial WHERE fi_co_id = :co_id AND date_part('day', fi_qtr_start_date) = 1";
	}

	@Override
	public String updateDailyMarketVol()
	{
		return "UPDATE daily_market SET dm_vol = dm_vol + :vol_incr WHERE dm_s_symb = :symbol AND date_part('day', dm_date) = :day_of_month";
	}

	@Override
	public String updateFinancialQtrStartDateAdd()
	{
		return "UPDATE financial SET fi_qtr_start_date = fi_qtr_start_date  + interval '1 day' WHERE fi_co_id = :co_id";
	}

	@Override
	public String updateFinancialQtrStartDateSubtract()
	{
		return "UPDATE financial SET fi_qtr_start_date = fi_qtr_start_date - interval '1 day' WHERE fi_co_id = :co_id";
	}

	@Override
	public String updateNewsItemDts()
	{
		return "UPDATE news_item SET ni_dts = ni_dts + INTERVAL '1 day' WHERE ni_id IN (SELECT nx_ni_id FROM news_xref WHERE nx_co_id = :co_id)";
	}

	@Override
	public String updateSecurityExchDate()
	{
		return "UPDATE security SET s_exch_date = s_exch_date + INTERVAL '1 day' WHERE s_symb = :symbol";
	}
}
