package org.oltp1.runner.perf;

import org.apache.commons.lang3.StringUtils;

public class TxOutput
{
	private int status;
	private String statusMessage;
	private double txTime;

	public TxOutput(int status)
	{
		super();
		this.status = status;
	}

	public int getStatus()
	{
		return status;
	}

	public String getStatusMessage()
	{
		if (StringUtils.isBlank(statusMessage))
		{
			return "n/a";
		}
		else
		{
			return statusMessage;
		}
	}

	public double getTxTime()
	{
		return (txTime);
	}

	public void setStatus(int status)
	{
		this.status = status;
	}

	public void setStatusMessage(String statusMessage)
	{
		this.statusMessage = statusMessage;
	}

	public void setTxTime(double txTime)
	{
		this.txTime = txTime;
	}
}