package org.caudexorigo.perf;

public interface Tx
{
	public String name();

	public TxOutput execute();
}