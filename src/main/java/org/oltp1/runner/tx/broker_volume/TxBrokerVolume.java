package org.oltp1.runner.tx.broker_volume;

import java.sql.Array;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.oltp1.common.ErrorCtx;
import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.db.SqlEngine;
import org.oltp1.runner.generator.TxInputGenerator;
import org.oltp1.runner.tx.QueryFactory;
import org.oltp1.runner.perf.TxBase;
import org.oltp1.runner.perf.TxOutput;
import org.oltp1.runner.perf.TxStatsCollector;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TxBrokerVolume extends TxBase
{
	private static final int max_broker_list_len = 40;

	private final ObjectMapper json = new ObjectMapper();
	private SqlContext sqlCtx;
	private final Sql2o sql2o;
	private final TxInputGenerator txInputGen;
	private final BrokerVolumeQueries sql;

	public TxBrokerVolume(TxInputGenerator txInputGen, SqlContext sqlCtx)
	{
		super(new TxStatsCollector("Broker-Volume"));
		this.txInputGen = txInputGen;
		this.sqlCtx = sqlCtx;
		this.sql2o = sqlCtx.getSql2o();
		this.sql = QueryFactory.getQueries(BrokerVolumeQueries.class, sqlCtx.getSqlEngine());
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

	private void executeFrame1(Connection con, final TxBrokerVolumeInput txInput, TxBrokerVolumeOutput txOutput) throws Exception
	{
		Query txQ = con
				.createQuery(sql.getVolume());

		if (sqlCtx.getSqlEngine() == SqlEngine.POSTGRESQL || sqlCtx.getSqlEngine() == SqlEngine.ORIOLEDB)
		{
			Array brokerArr = con.getJdbcConnection().createArrayOf("varchar", txInput.broker_list);

			txQ
					.addParameter("broker_list", brokerArr)
					.addParameter("sector_name", txInput.sector_name);
		}
		else if (sqlCtx.getSqlEngine() == SqlEngine.MSSQL)
		{
			String brokerLstCsv = StringUtils.join(txInput.broker_list, ",");
			txQ
					.addParameter("broker_list", brokerLstCsv)
					.addParameter("sector_name", txInput.sector_name);
		}
		else if (sqlCtx.getSqlEngine() == SqlEngine.MARIADB)
		{
			String brokerLstJsonArr = json.writeValueAsString(txInput.broker_list);

			txQ
					.addParameter("broker_list", brokerLstJsonArr)
					.addParameter("sector_name", txInput.sector_name);
		}
		else
		{
			throw new UnsupportedOperationException("Unsupported Database for this operation");
		}

		final List<Map<String, Object>> lstBrokerVolume = txQ
				.executeAndFetchTable()
				.asList();

		// row_count will frequently be zero near the start of a Test Run when
		// TRADE_REQUEST table is mostly empty
		final int status = (lstBrokerVolume.size() > max_broker_list_len) ? -111 : 0;

		txOutput.volume = lstBrokerVolume;
		txOutput.list_len = lstBrokerVolume.size();
		txOutput.setStatus(status);
	}
}