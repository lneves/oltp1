package org.oltp1.runner.perf;

public class TxVoid extends TxBase
{
	private static TxOutput nullOutput = new TxOutput(0)
	{
	};

	public TxVoid(TxStatsCollector stats)
	{
		super(stats);
	}

	@Override
	public TxOutput run()
	{
		return nullOutput;
	}
}