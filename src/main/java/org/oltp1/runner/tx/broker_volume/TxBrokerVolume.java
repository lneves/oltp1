package org.oltp1.runner.tx.broker_volume;

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
import org.sql2o.Query;
import org.sql2o.Sql2o;

public class TxBrokerVolume extends TxBase
{
	private static final int max_broker_list_len = 40;

	private final Sql2o sql2o;
	private final TxInputGenerator txInputGen;
	private final BrokerVolumeDialect sql;
	private static final TransactionSpec txSpec = TransactionSpec.BROKER_VOLUME;

	public TxBrokerVolume(TxInputGenerator txInputGen, SqlContext sqlCtx, BenchmarkMetrics metrics)
	{
		super(metrics, new TxStatsCollector(txSpec));
		this.txInputGen = txInputGen;
		this.sql2o = sqlCtx.getSql2o();
		this.sql = QueryFactory.getQueries(BrokerVolumeDialect.class, sqlCtx.getSqlEngine());
	}

	@Override
	protected final TxOutput run()
	{
		final TxBrokerVolumeInput txInput = txInputGen.generateBrokerVolumeInput();
		final TxBrokerVolumeOutput txOutput = new TxBrokerVolumeOutput();

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

	private void executeFrame1(final Connection con, final TxBrokerVolumeInput txInput, final TxBrokerVolumeOutput txOutput) throws Exception
	{
		String brokerLst = sql.buildBrokerList(txInput.brokerList());

		Query txQ = con.createQuery(sql.getVolume());

		txQ
				.addParameter("broker_list", brokerLst)
				.addParameter("sector_name", txInput.sectorName());

		final List<Map<String, Object>> lstBrokerVolume = txQ
				.executeAndFetchTable()
				.asList();

		// row_count will frequently be zero near the start of a Test Run when
		// TRADE_REQUEST table is mostly empty
		final int listLen = lstBrokerVolume.size();
		final int status = ((listLen < 0) || (listLen > max_broker_list_len)) ? -111 : 0;

		txOutput.volume = lstBrokerVolume;
		txOutput.list_len = listLen;
		txOutput.setStatus(status);
	}
}