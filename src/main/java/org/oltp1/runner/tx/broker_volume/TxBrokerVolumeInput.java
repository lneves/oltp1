package org.oltp1.runner.tx.broker_volume;

import java.util.Arrays;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class TxBrokerVolumeInput
{
	public String[] broker_list;
	public String sector_name;

	public TxBrokerVolumeInput()
	{
		super();
	}

	public TxBrokerVolumeInput(String[] brokerList, String sectorName)
	{
		super();
		this.broker_list = brokerList;
		this.sector_name = sectorName;
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("broker_list", Arrays.toString(broker_list))
				.append("sector_name", sector_name)
				.toString();
	}
}