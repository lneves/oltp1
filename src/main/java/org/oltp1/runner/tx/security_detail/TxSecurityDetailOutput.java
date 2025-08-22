package org.oltp1.runner.tx.security_detail;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.oltp1.runner.perf.TxOutput;

public class TxSecurityDetailOutput extends TxOutput
{
	public Map<String, Object> sd_info_1;
	public List<Map<String, Object>> lst_sd_info_2;
	public List<Map<String, Object>> lst_sd_info_3;
	public List<Map<String, Object>> lst_sd_info_4;
	public Map<String, Object> sd_info_5;
	public List<Map<String, Object>> lst_sd_info_6;
	public List<Map<String, Object>> lst_sd_info_7;

	protected TxSecurityDetailOutput()
	{
		this(0);
	}

	protected TxSecurityDetailOutput(int status)
	{
		super(status);
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("sd_info_1", sd_info_1)
				.append("lst_sd_info_2", lst_sd_info_2)
				.append("lst_sd_info_3", lst_sd_info_3)
				.append("lst_sd_info_4", lst_sd_info_4)
				.append("sd_info_5", sd_info_5)
				.append("lst_sd_info_6", lst_sd_info_6)
				.append("lst_sd_info_7", lst_sd_info_7)
				.append("tx_status", getStatus())
				.append("tx_status_message", getStatusMessage())
				.toString();
	}
}