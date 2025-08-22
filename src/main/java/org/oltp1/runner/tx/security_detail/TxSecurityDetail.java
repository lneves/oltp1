package org.oltp1.runner.tx.security_detail;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.oltp1.common.ErrorCtx;
import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.generator.TxInputGenerator;
import org.oltp1.runner.tx.QueryFactory;
import org.oltp1.runner.perf.TxBase;
import org.oltp1.runner.perf.TxOutput;
import org.oltp1.runner.perf.TxStatsCollector;
import org.sql2o.Connection;
import org.sql2o.Sql2o;

public class TxSecurityDetail extends TxBase
{
	private static final int min_day_len = 5;
	private static final int max_day_len = 20;
	private static final int max_fin_len = 20;
	private static final int max_news_len = 2;

	private final Sql2o sql2o;

	private final TxInputGenerator txInputGen;
	private final SecurityDetailQueries sql;

	public TxSecurityDetail(TxInputGenerator txInputGen, SqlContext sqlCtx)
	{
		super(new TxStatsCollector("Security-Detail"));

		this.txInputGen = txInputGen;
		this.sql2o = sqlCtx.getSql2o();
		this.sql = QueryFactory.getQueries(SecurityDetailQueries.class, sqlCtx.getSqlEngine());
	}

	@Override
	protected final TxOutput run()
	{
		TxSecurityDetailOutput txOutput = new TxSecurityDetailOutput();

		final TxSecurityDetailInput txInput = txInputGen.generateSecurityDetailInput();

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

	private void executeFrame1(final Connection con, final TxSecurityDetailInput txInput, final TxSecurityDetailOutput txOutput)
	{
		Map<String, Object> sdInfo1 = con
				.createQuery(sql.getInfo1())
				.addParameter("symbol", txInput.symbol)
				.executeAndFetchTable()
				.asList()
				.stream()
				.findFirst()
				.orElse(Collections.emptyMap());

		if (sdInfo1.isEmpty())
		{
			txOutput.setStatus(-1);
			return;
		}

		List<Map<String, Object>> lstSdInfo2 = con
				.createQuery(sql.getInfo2())
				.addParameter("co_id", sdInfo1.get("co_id"))
				.executeAndFetchTable()
				.asList();

		List<Map<String, Object>> lstSdInfo3 = con
				.createQuery(sql.getInfo3())
				.addParameter("co_id", sdInfo1.get("co_id"))
				.executeAndFetchTable()
				.asList();

		List<Map<String, Object>> lstSdInfo4 = con
				.createQuery(sql.getInfo4())
				.addParameter("max_rows_to_return", txInput.max_rows_to_return)
				.addParameter("symbol", txInput.symbol)
				.addParameter("start_day", txInput.start_day)
				.executeAndFetchTable()
				.asList();

		Map<String, Object> sdInfo5 = con
				.createQuery(sql.getInfo5())
				.addParameter("symbol", txInput.symbol)
				.executeAndFetchTable()
				.asList()
				.get(0);

		List<Map<String, Object>> lstSdInfo6;
		List<Map<String, Object>> lstSdInfo7;

		if (txInput.access_lob_flag)
		{
			lstSdInfo6 = con
					.createQuery(sql.getInfo6())
					.addParameter("co_id", sdInfo1.get("co_id"))
					.executeAndFetchTable()
					.asList();

			lstSdInfo7 = Collections.emptyList();
		}
		else
		{
			lstSdInfo6 = Collections.emptyList();
			lstSdInfo7 = con
					.createQuery(sql.getInfo7())
					.addParameter("co_id", sdInfo1.get("co_id"))
					.executeAndFetchTable()
					.asList();
		}

		txOutput.sd_info_1 = sdInfo1;
		txOutput.lst_sd_info_2 = lstSdInfo2;
		txOutput.lst_sd_info_3 = lstSdInfo3;
		txOutput.lst_sd_info_4 = lstSdInfo4;
		txOutput.sd_info_5 = sdInfo5;
		txOutput.lst_sd_info_6 = lstSdInfo6;
		txOutput.lst_sd_info_7 = lstSdInfo7;

		int day_len = lstSdInfo4.size();
		int fin_len = lstSdInfo3.size();
		int news_len = lstSdInfo6.size() + lstSdInfo7.size();

		if ((day_len < min_day_len) || (day_len > max_day_len))
		{
			txOutput.setStatus(-511);
			txOutput.setStatusMessage("(day_len < min_day_len) || (day_len > max_day_len)");
		}
		else if (fin_len != max_fin_len)
		{
			txOutput.setStatus(-512);
			txOutput.setStatusMessage("fin_len != max_fin_len");
		}
		else if (news_len != max_news_len)
		{
			txOutput.setStatus(-512);
			txOutput.setStatusMessage("news_len != max_news_len");
		}
	}
}