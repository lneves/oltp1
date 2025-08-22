package org.oltp1.runner.perf;

public class TxBaseLineOutput extends TxOutput
{
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