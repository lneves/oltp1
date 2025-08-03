package org.caudexorigo.oltp1.tx.trade_cleanup;

import org.caudexorigo.db.SqlContext;
import org.caudexorigo.oltp1.generator.TxInputGenerator;
import org.caudexorigo.perf.ErrorAnalyser;
import org.caudexorigo.perf.TxBase;
import org.caudexorigo.perf.TxOutput;
import org.caudexorigo.perf.TxStatsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

public class TxTradeCleanup extends TxBase
{
	private static final Logger log = LoggerFactory.getLogger(TxTradeCleanup.class);

	private final Sql2o sql2o;
	private final TxInputGenerator txInputGen;
	private final TradeCleanupQueries sql;

	public TxTradeCleanup(TxInputGenerator txInputGen, SqlContext sqlCtx)
	{
		super(new TxStatsCollector("Trade-Cleanup"));
		this.txInputGen = txInputGen;
		this.sql2o = sqlCtx.getSql2o();
		this.sql = TradeCleanupQueriesFactory.getQueries(sqlCtx.getSqlEngine());
	}

	@Override
	protected TxOutput run()
	{
		TxTradeCleanupInput txInput = txInputGen.generateTradeCleanupInput();
		TxTradeCleanupOutput txOutput = new TxTradeCleanupOutput();

		try (Connection con = sql2o.beginTransaction())
		{

			con
					.createQuery(sql.insertTradeHistory1())
					.addParameter("st_id", txInput.st_submitted_id)
					.executeUpdate();

			con
					.createQuery(sql.updateTrade1())
					.addParameter("st_canceled_id", txInput.st_canceled_id)
					.executeUpdate();

			con
					.createQuery(sql.insertTradeHistory1())
					.addParameter("st_id", txInput.st_canceled_id)
					.executeUpdate();

			con
					.createQuery(sql.deleteTradeRequest())
					.executeUpdate();

			int cleanedCount = con.getResult();

			con
					.createQuery(sql.updateTrade2())
					.addParameter("start_trade_id", txInput.start_trade_id)
					.addParameter("st_submitted_id", txInput.st_submitted_id)
					.addParameter("st_canceled_id", txInput.st_canceled_id)
					.executeUpdate();

			con
					.createQuery(sql.insertTradeHistory2())
					.addParameter("start_trade_id", txInput.start_trade_id)
					.addParameter("st_submitted_id", txInput.st_submitted_id)
					.addParameter("st_canceled_id", txInput.st_canceled_id)
					.executeUpdate();

			cleanedCount += con.getResult();

			txOutput.trades_cleaned_up = cleanedCount;

			con.commit();

		}
		catch (Throwable t)
		{
			Throwable r = ErrorAnalyser.findRootCause(t);
			txOutput.setStatus(-1);
			txOutput.setStatusMessage(r.getMessage());
			writeLog(log, r);
		}
		return txOutput;
	}
}