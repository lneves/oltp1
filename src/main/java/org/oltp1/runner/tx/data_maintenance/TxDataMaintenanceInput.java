
package org.oltp1.runner.tx.data_maintenance;

import java.time.LocalDate;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class TxDataMaintenanceInput
{
	// General purpose field to identify the target table for the operation
	public String table_name;

	// --- Identifiers for various tables ---

	// For CUSTOMER or WATCH_LIST operations
	public long c_id;

	// For ACCOUNT_PERMISSION operations
	public long acct_id;

	// For COMPANY or FINANCIAL operations
	public long co_id;

	// For ADDRESS operations
	public long ad_id;

	// For SECURITY, DAILY_MARKET, etc. operations
	public String symbol;
	public int vol_incr;

	// For TRADE operations
	public long trade_id;

	// For TAXRATE operations
	public String tx_id;

	// For WATCH_LIST/WATCH_ITEM operations
	public long wl_id;

	// --- Fields to hold new values for update operations ---

	public String c_email_2;
	public String st_id;
	public String ap_acl;
	public String ad_line1;
	public java.math.BigDecimal dm_close;
	public LocalDate s_exch_date;
	public double tx_rate;

	public LocalDate start_day;

	public int day_of_month;

	@Override
	public String toString()
	{
		return ReflectionToStringBuilder.toString(this, ToStringStyle.DEFAULT_STYLE, true, false, true, null);
	}
}