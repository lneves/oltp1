package org.oltp1.runner.runtime;

public interface Tx
{
	public String name();

	public TxOutput execute();
}