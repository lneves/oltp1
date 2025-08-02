package org.caudexorigo.oltp1.tx.broker_volume;

import java.sql.Array;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.caudexorigo.db.SqlContext;
import org.caudexorigo.db.SqlEngine;
import org.caudexorigo.oltp1.generator.TxInputGenerator;
import org.caudexorigo.perf.ErrorAnalyser;
import org.caudexorigo.perf.TxBase;
import org.caudexorigo.perf.TxOutput;
import org.caudexorigo.perf.TxStatsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;

public class TxBrokerVolume extends TxBase
{
	private static Logger log = LoggerFactory.getLogger(TxBrokerVolume.class);
	
	private static final int max_broker_list_len = 40;

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
		this.sql = BrokerVolumeQueriesFactory.getQueries(sqlCtx.getSqlEngine());
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
			Throwable r = ErrorAnalyser.findRootCause(t);
			log.error(r.getMessage(), r);
			txOutput.setStatus(-1);
			txOutput.setStatusMessage(r.getMessage());
		}

		return txOutput;
	}

	private void executeFrame1(Connection con, final TxBrokerVolumeInput txInput, TxBrokerVolumeOutput txOutput) throws SQLException
	{
		Query txQ = con
				.createQuery(sql.getVolume());
		
		if (sqlCtx.getSqlEngine() == SqlEngine.POSTGRESQL)
		{
			try
			{
				Array tradeIds = con.getJdbcConnection().createArrayOf("varchar", txInput.broker_list);

				txQ
						.addParameter("broker_list", tradeIds)
						.addParameter("sector_name", txInput.sector_name);
			}
			catch (SQLException e)
			{
				throw new RuntimeException(e);
			}
		}
		else if (sqlCtx.getSqlEngine() == SqlEngine.MSSQL)
		{
			String broker_list_csv = StringUtils.join(txInput.broker_list, ",");

			txQ
					.addParameter("broker_list", broker_list_csv)
					.addParameter("sector_name", txInput.sector_name);
		}
		else
		{
			throw new NotImplementedException("Unsupported Database for this operation");
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