package org.oltp1.runner.perf;

public interface Tx
{
	public String name();

	public TxOutput execute();
}