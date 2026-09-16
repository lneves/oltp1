package org.oltp1.runner.runtime;

import org.oltp1.common.ErrorCtx;
import org.oltp1.runner.db.SqlContext;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

public class TxBaseLine extends TxBase
{
	private final Sql2o sql2o;
	private final String baselineQuery;

	public TxBaseLine(SqlContext sqlCtx, BenchmarkMetrics metrics)
	{
		super(metrics, new TxStatsCollector(TransactionSpec.BASELINE));
		sql2o = sqlCtx.getSql2o();
		baselineQuery = sqlCtx.getSqlEngine().getBaselineQuery();
	}

	@Override
	public TxOutput run()
	{
		TxBaseLineOutput txOutput = new TxBaseLineOutput();

		try (Connection con = sql2o.open())
		{
			int ret = con.createQuery(baselineQuery).executeScalar(Integer.class);
			txOutput.setStatus(ret);
		}
		catch (Throwable t)
		{
			ErrorCtx ectx = new ErrorCtx(t);
			txOutput.setStatus(-1);
			txOutput.setStatusMessage(ectx.toString());
		}

		return txOutput;
	}
}