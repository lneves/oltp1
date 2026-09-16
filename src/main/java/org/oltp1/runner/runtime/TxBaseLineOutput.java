package org.oltp1.runner.runtime;

public class TxBaseLineOutput extends TxOutput
{

	public TxBaseLineOutput()
	{
		this(0);
	}

	public TxBaseLineOutput(int status)
	{
		super(status);
	}

	@Override
	public String toString()
	{
		return String.format("TxBaseLineOutput [status=%s, txTime=%s ms]", getStatus(), getTxTime());
	}
}