package org.oltp1.runner.perf;

import java.util.ArrayList;
import java.util.List;

public class TxRunSummary
{
	private final int numClients;
	private final List<TxSummary> txStats;
	private final String sutInfo;

	public TxRunSummary(String sutInfo, int numClients)
	{
		this.sutInfo = sutInfo;
		this.numClients = numClients;
		this.txStats = new ArrayList<>();
	}

	public int getNumClients()
	{
		return numClients;
	}

	public void addTxSummary(TxSummary tsummary)
	{
		txStats.add(tsummary);
	}

	public List<TxSummary> getTxStats()
	{
		return txStats;
	}

	public String getSutInfo()
	{
		return sutInfo;
	}

	public void clearSummary()
	{

		txStats.clear();
	}
}