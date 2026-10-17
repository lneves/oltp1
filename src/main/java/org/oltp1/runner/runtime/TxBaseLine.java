package org.oltp1.runner.runtime;

import org.oltp1.common.ErrorCtx;
import org.oltp1.common.MissingReferenceDataException;
import org.oltp1.common.db.Scalars;
import org.oltp1.common.db.SqlContext;
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
			// The baseline query is a constant SELECT, so a missing row would mean the dialect
			// does not return the expected scalar. Name it rather than unboxing a null Integer.
			Integer result = Scalars.find(con, baselineQuery, Integer.class);

			if (result == null)
			{
				throw new MissingReferenceDataException(
						"baseline query " + baselineQuery,
						"it returned no rows; the dialect's getBaselineQuery() must be a single-row SELECT");
			}

			// The scalar is the transaction status: 0 means "the round trip worked", anything
			// else is propagated so a misconfigured baseline query is visible in the report.
			txOutput.setStatus(result);
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