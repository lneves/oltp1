package org.oltp1.runner.tx.data_maintenance;

/**
 * Provides the Microsoft SQL Server-specific SQL queries for the
 * Data-Maintenance transaction.
 */
public class MsSqlDataMaintenanceQueries extends DefaultDataMaintenanceQueries
{
	@Override
	public String countFinancialOnFirstOfMonth()
	{
		return "SELECT count(*) FROM financial WHERE fi_co_id = :co_id AND DAY(fi_qtr_start_date) = 1";
	}

	@Override
	public String getApAcl()
	{
		return "SELECT TOP 1 ap_acl FROM account_permission WHERE ap_ca_id = :acct_id ORDER BY ap_tax_id";
	}

	@Override
	public String getNextSecurityForWatchList()
	{
		return "SELECT TOP 1 s_symb FROM security WHERE s_symb NOT IN (SELECT wi_s_symb FROM watch_item WHERE wi_wl_id = :wl_id) ORDER BY s_symb";
	}

	@Override
	public String updateDailyMarketVol()
	{
		return "UPDATE daily_market SET dm_vol = dm_vol + :vol_incr WHERE dm_s_symb = :symbol AND DAY(dm_date) = :day_of_month";
	}

	@Override
	public String updateFinancialQtrStartDateAdd()
	{
		return "UPDATE financial SET fi_qtr_start_date = DATEADD(day, 1, fi_qtr_start_date) WHERE fi_co_id = :co_id";
	}

	@Override
	public String updateFinancialQtrStartDateSubtract()
	{
		return "UPDATE financial SET fi_qtr_start_date = DATEADD(day, -1, fi_qtr_start_date) WHERE fi_co_id = :co_id";
	}

	@Override
	public String updateNewsItemDts()
	{
		return "UPDATE news_item SET ni_dts = DATEADD(day, 1, ni_dts) WHERE ni_id IN (SELECT nx_ni_id FROM news_xref WHERE nx_co_id = :co_id)";
	}

	@Override
	public String updateSecurityExchDate()
	{
		return "UPDATE security SET s_exch_date = DATEADD(day, 1, s_exch_date) WHERE s_symb = :symbol";
	}
}
