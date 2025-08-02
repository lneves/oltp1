package org.caudexorigo.oltp1.tx.trade_status;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

public class TxTradeStatus extends TxBase
{
	private static final Logger log = LoggerFactory.getLogger(TxTradeStatus.class);

	private final Sql2o sql2o;

	private final TxInputGenerator txInputGen;
	private final TradeStatusQueries sql;

	public TxTradeStatus(TxInputGenerator txInputGen, SqlContext sqlCtx)
	{
		super(new TxStatsCollector("Trade-Status"));

		this.txInputGen = txInputGen;
		this.sql2o = sqlCtx.getSql2o();
		this.sql = new DefaultTradeStatusQueries();
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
			Throwable r = ErrorAnalyser.findRootCause(t);
			log.error(r.getMessage(), r);
			txOutput.setStatus(-1);
			txOutput.setStatusMessage(r.getMessage());
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
	}
}