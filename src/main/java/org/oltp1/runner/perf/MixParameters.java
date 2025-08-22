package org.oltp1.runner.perf;

public class MixParameters
{
	public String sutInfo;
	public int clients;
	public boolean isPacingEnabled;
	public int tps;

	public MixParameters(String sutInfo, int clients, boolean isPacingEnabled, int tps)
	{
		super();
		this.sutInfo = sutInfo;
		this.clients = clients;
		this.isPacingEnabled = isPacingEnabled;
		this.tps = tps;
	}
}