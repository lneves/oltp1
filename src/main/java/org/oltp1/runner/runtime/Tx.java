package org.oltp1.runner.runtime;

public interface Tx
{
	String name();

	TxOutput execute();
}