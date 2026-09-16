package org.oltp1.runner.tx.trade_status;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

public class TxTradeStatus extends TxBase
{
	private static final int MAX_TRADE_STATUS_LEN = 50;

	private final Sql2o sql2o;
	private final TxInputGenerator txInputGen;
	private final TradeStatusDialect sql;

	public TxTradeStatus(TxInputGenerator txInputGen, SqlContext sqlCtx, BenchmarkMetrics metrics)
	{
		super(metrics, new TxStatsCollector(TransactionSpec.TRADE_STATUS));
		this.txInputGen = txInputGen;
		this.sql2o = sqlCtx.getSql2o();
		this.sql = QueryFactory.getQueries(TradeStatusDialect.class, sqlCtx.getSqlEngine());
	}

	@Override
	protected final TxOutput run()
	{
		TxTradeStatusOutput txOutput = new TxTradeStatusOutput();

		final TxTradeStatusInput txInput = txInputGen.generateTradeStatusInput();

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

	private void executeFrame1(final Connection con, final TxTradeStatusInput txInput, final TxTradeStatusOutput txOutput)
	{
		List<Map<String, Object>> tradeStatus = con
				.createQuery(sql.getTradeStatus())
				.addParameter("acct_id", txInput.acct_id)
				.executeAndFetchTable()
				.asList();

		Map<String, Object> tradeName = con
				.createQuery(sql.getTradeName())
				.addParameter("acct_id", txInput.acct_id)
				.executeAndFetchTable()
				.asList()
				.stream()
				.findFirst()
				.orElse(Collections.emptyMap());

		txOutput.trade_name = tradeName;
		txOutput.trade_status = tradeStatus;
		txOutput.num_found = tradeStatus.size();

		if (txOutput.num_found != MAX_TRADE_STATUS_LEN)
		{
			txOutput.setStatus(-911);
			txOutput.setStatusMessage("num-found != max_trade_status_len");
		}
	}
}