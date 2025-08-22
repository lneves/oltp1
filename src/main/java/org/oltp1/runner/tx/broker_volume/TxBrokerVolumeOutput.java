package org.oltp1.runner.tx.broker_volume;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.oltp1.runner.perf.TxOutput;

public class TxBrokerVolumeOutput extends TxOutput
{
	public List<Map<String, Object>> volume;
	public int list_len;

	public TxBrokerVolumeOutput()
	{
		this(0);
	}

	public TxBrokerVolumeOutput(int status)
	{
		super(status);
		volume = Collections.emptyList();
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("volume", volume)
				.append("list_len", list_len)
				.append("tx_status", getStatus())
				.append("tx_status_message", getStatusMessage())
				.toString();
	}
}