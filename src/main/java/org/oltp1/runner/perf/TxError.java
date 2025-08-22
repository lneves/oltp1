package org.oltp1.runner.perf;

public class TxError extends TxOutput
{
	private final String errorMessage;

	public TxError(String errorMessage)
	{
		super(-1);
		this.errorMessage = errorMessage;
	}

	@Override
	public String toString()
	{
		return String.format("TxError [%nerrorMessage: %s%n, status: %s%n, tx_time: %s%n]", errorMessage, getStatus(), getTxTime());
	}
}