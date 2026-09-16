package org.oltp1.runner.tx.market_watch;

import org.apache.commons.lang3.StringUtils;
import org.oltp1.common.ErrorCtx;
import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.generator.TxInputGenerator;
import org.oltp1.runner.runtime.BenchmarkMetrics;
import org.oltp1.runner.runtime.TransactionSpec;
import org.oltp1.runner.runtime.TxBase;
import org.oltp1.runner.runtime.TxOutput;
import org.oltp1.runner.runtime.TxStatsCollector;
import org.oltp1.runner.tx.QueryFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

public class TxMarketWatch extends TxBase
{
	private final MarketWatchDialect sql;
	private final Sql2o sql2o;

	private final TxInputGenerator txInputGen;

	public TxMarketWatch(TxInputGenerator txInputGen, SqlContext sqlCtx, BenchmarkMetrics metrics)
	{
		super(metrics, new TxStatsCollector(TransactionSpec.MARKET_WATCH));
		this.txInputGen = txInputGen;
		this.sql = QueryFactory.getQueries(MarketWatchDialect.class, sqlCtx.getSqlEngine());
		this.sql2o = sqlCtx.getSql2o();
	}

	@Override
	protected final TxOutput run()
	{
		TxMarketWatchOutput txOutput = new TxMarketWatchOutput();

		final TxMarketWatchInput txInput = txInputGen.generateMarketWatchInput();

		if ((txInput.acctId() == 0) && (txInput.cId() == 0) && StringUtils.isBlank(txInput.industryName()))
		{
			txOutput.setStatus(-411);
			return txOutput;
		}

		try (Connection con = sql2o.beginTransaction())
		{
			executeFrame1(con, txInput, txOutput);

			con.commit();
		}
		catch (Throwable t)
		{
			ErrorCtx ectx = new ErrorCtx(t);
			txOutput.setStatus(-1);
			txOutput.setStatusMessage(ectx.toString());
		}

		return txOutput;
	}

	private void executeFrame1(final Connection con, final TxMarketWatchInput txInput, final TxMarketWatchOutput txOutput)
	{
		Double pct_change;

		if (txInput.cId() != 0)
		{
			pct_change = con
					.createQuery(sql.getPctChangeByCustomer())
					.addParameter("cust_id", txInput.cId())
					.addParameter("start_date", txInput.startDay())
					.executeScalar(Double.class);
		}
		else if (StringUtils.isNotBlank(txInput.industryName()))
		{
			pct_change = con
					.createQuery(sql.getPctChangeByIndustry())
					.addParameter("industry_name", txInput.industryName())
					.addParameter("start_date", txInput.startDay())
					.addParameter("starting_co_id", txInput.startingCoId())
					.addParameter("ending_co_id", txInput.endingCoId())
					.executeScalar(Double.class);
		}
		else if (txInput.acctId() != 0)
		{
			pct_change = con
					.createQuery(sql.getPctChangeByAccount())
					.addParameter("acct_id", txInput.acctId())
					.addParameter("start_date", txInput.startDay())
					.executeScalar(Double.class);
		}
		else
		{
			throw new IllegalArgumentException("Bad input data in the Market-Watch transaction");
		}

		txOutput.pct_change = (pct_change != null) ? pct_change.doubleValue() : 0.0;
		;
	}
}