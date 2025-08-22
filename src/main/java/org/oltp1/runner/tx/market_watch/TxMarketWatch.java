package org.oltp1.runner.tx.market_watch;

import org.apache.commons.lang3.StringUtils;
import org.oltp1.common.ErrorCtx;
import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.generator.TxInputGenerator;
import org.oltp1.runner.tx.QueryFactory;
import org.oltp1.runner.perf.TxBase;
import org.oltp1.runner.perf.TxOutput;
import org.oltp1.runner.perf.TxStatsCollector;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

public class TxMarketWatch extends TxBase
{
	private final MarketWatchQueries sql;
	private final Sql2o sql2o;

	private final TxInputGenerator txInputGen;

	public TxMarketWatch(TxInputGenerator txInputGen, SqlContext sqlCtx)
	{
		super(new TxStatsCollector("Market-Watch"));
		this.txInputGen = txInputGen;
		this.sql = QueryFactory.getQueries(MarketWatchQueries.class, sqlCtx.getSqlEngine());
		this.sql2o = sqlCtx.getSql2o();
	}

	@Override
	protected final TxOutput run()
	{
		TxMarketWatchOutput txOutput = new TxMarketWatchOutput();

		final TxMarketWatchInput txInput = txInputGen.generateMarketWatchInput();

		if ((txInput.acct_id == 0) && (txInput.c_id == 0) && StringUtils.isBlank(txInput.industry_name))
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
		double pct_change;

		if (txInput.c_id != 0)
		{
			pct_change = con
					.createQuery(sql.getPctChangeByCustomer())
					.addParameter("cust_id", txInput.c_id)
					.addParameter("start_date", txInput.start_day)
					.executeScalar(Double.class);
		}
		else if (StringUtils.isNotBlank(txInput.industry_name))
		{
			pct_change = con
					.createQuery(sql.getPctChangeByIndustry())
					.addParameter("industry_name", txInput.industry_name)
					.addParameter("start_date", txInput.start_day)
					.addParameter("starting_co_id", txInput.starting_co_id)
					.addParameter("ending_co_id", txInput.ending_co_id)
					.executeScalar(Double.class);
		}
		else if (txInput.acct_id != 0)
		{
			pct_change = con
					.createQuery(sql.getPctChangeByAccount())
					.addParameter("acct_id", txInput.acct_id)
					.addParameter("start_date", txInput.start_day)
					.executeScalar(Double.class);
		}
		else
		{
			throw new IllegalArgumentException("Bad input data in the Market-Watch transaction");
		}

		txOutput.pct_change = pct_change;
	}
}