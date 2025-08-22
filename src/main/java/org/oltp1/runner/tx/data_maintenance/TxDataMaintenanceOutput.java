package org.oltp1.runner.tx.data_maintenance;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.oltp1.runner.perf.TxOutput;

public class TxDataMaintenanceOutput extends TxOutput
{
	public String table_name;
	public int rows_affected;

	public TxDataMaintenanceOutput()
	{
		super(0);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("table_name", table_name)
				.append("rows_affected", rows_affected)
				.append("tx_status", getStatus())
				.append("tx_status_message", getStatusMessage())
				.toString();
	}

}