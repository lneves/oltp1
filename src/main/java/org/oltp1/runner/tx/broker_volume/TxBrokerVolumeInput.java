package org.oltp1.runner.tx.broker_volume;

import java.util.Arrays;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public record TxBrokerVolumeInput(String[] brokerList, String sectorName)
{

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("broker_list", Arrays.toString(brokerList))
				.append("sector_name", sectorName)
				.toString();
	}
}
