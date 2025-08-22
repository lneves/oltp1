package org.oltp1.runner.tx.data_maintenance;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.oltp1.common.ErrorCtx;
import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.generator.TxInputGenerator;
import org.oltp1.runner.tx.QueryFactory;
import org.oltp1.runner.perf.TxBase;
import org.oltp1.runner.perf.TxOutput;
import org.oltp1.runner.perf.TxStatsCollector;
import org.sql2o.Connection;
import org.sql2o.Sql2o;
import org.sql2o.data.Row;

public class TxDataMaintenance extends TxBase
{
	private final Sql2o sql2o;
	private final TxInputGenerator txInputGen;
	private final DataMaintenanceQueries sql;

	public TxDataMaintenance(TxInputGenerator txInputGen, SqlContext sqlCtx)
	{
		super(new TxStatsCollector("Data-Maintenance"));

		this.txInputGen = txInputGen;
		this.sql2o = sqlCtx.getSql2o();
		this.sql = QueryFactory.getQueries(DataMaintenanceQueries.class, sqlCtx.getSqlEngine());
	}

	@Override
	protected TxOutput run()
	{
		final TxDataMaintenanceInput txInput = txInputGen.generateDataMaintenanceInput();
		final TxDataMaintenanceOutput txOutput = new TxDataMaintenanceOutput();

		try (Connection con = sql2o.beginTransaction())
		{
			int rowsAffected = 0;

			switch (txInput.table_name)
			{
			case "ACCOUNT_PERMISSION":
				String currentAcl = con
						.createQuery(sql.getApAcl())
						.addParameter("acct_id", txInput.acct_id)
						.executeScalar(String.class);
				if (currentAcl != null)
				{
					String newAcl = "1111".equals(currentAcl) ? "0011" : "1111";
					rowsAffected = con
							.createQuery(sql.updateApAcl())
							.addParameter("acct_id", txInput.acct_id)
							.addParameter("ap_acl", newAcl)
							.executeUpdate()
							.getResult();
				}
				break;
			case "ADDRESS":
				Row addressInfo = (txInput.c_id != 0)
						? con
								.createQuery(sql.getCustomerAddress())
								.addParameter("c_id", txInput.c_id)
								.executeAndFetchTable()
								.rows()
								.getFirst()
						: con
								.createQuery(sql.getCompanyAddress())
								.addParameter("co_id", txInput.co_id)
								.executeAndFetchTable()
								.rows()
								.getFirst();

				if (addressInfo != null)
				{
					String currentLine2 = addressInfo.getString("ad_line2");
					String newLine2 = "Apt. 10C".equals(currentLine2) ? "Apt. 22" : "Apt. 10C";
					rowsAffected = con
							.createQuery(sql.updateAddressLine2())
							.addParameter("ad_id", addressInfo.getLong("ad_id"))
							.addParameter("ad_line2", newLine2)
							.executeUpdate()
							.getResult();
				}
				break;
			case "COMPANY":
				String currentSpRate = con
						.createQuery(sql.getCompanySpRate())
						.addParameter("co_id", txInput.co_id)
						.executeScalar(String.class);

				if (currentSpRate != null)
				{
					String newSpRate = "ABA".equals(currentSpRate) ? "AAA" : "ABA";
					rowsAffected = con
							.createQuery(sql.updateCompanySpRate())
							.addParameter("co_id", txInput.co_id)
							.addParameter("sp_rate", newSpRate)
							.executeUpdate()
							.getResult();
				}
				break;
			case "CUSTOMER":
				String currentEmail2 = con
						.createQuery(sql.getCustomerEmail2())
						.addParameter("c_id", txInput.c_id)
						.executeScalar(String.class);
				if (currentEmail2 != null)
				{
					String[] emailParts = currentEmail2.split("@");
					String newEmail2 = emailParts[0] + (emailParts.length > 1 && "mindspring.com".equals(emailParts[1]) ? "@earthlink.com" : "@mindspring.com");
					rowsAffected = con
							.createQuery(sql.updateCustomerEmail2())
							.addParameter("c_id", txInput.c_id)
							.addParameter("c_email_2", newEmail2)
							.executeUpdate()
							.getResult();
				}
				break;
			case "CUSTOMER_TAXRATE":
				List<String> currentTaxRates = con
						.createQuery(sql.getCustomerTaxrateIds())
						.addParameter("c_id", txInput.c_id)
						.executeScalarList(String.class);
				for (String tx_id : currentTaxRates)
				{
					List<String> usRates = Arrays.asList("US1", "US2", "US3", "US4", "US5");
					List<String> cnRates = Arrays.asList("CN1", "CN2", "CN3", "CN4");
					String new_tx_id = null;
					if (tx_id.startsWith("US"))
						new_tx_id = usRates.get((usRates.indexOf(tx_id) + 1) % usRates.size());
					else if (tx_id.startsWith("CN"))
						new_tx_id = cnRates.get((cnRates.indexOf(tx_id) + 1) % cnRates.size());
					if (new_tx_id != null)
						rowsAffected += con
								.createQuery(sql.updateCustomerTaxrate())
								.addParameter("c_id", txInput.c_id)
								.addParameter("old_tx_id", tx_id)
								.addParameter("new_tx_id", new_tx_id)
								.executeUpdate()
								.getResult();
				}
				break;
			case "DAILY_MARKET":
				rowsAffected = con
						.createQuery(sql.updateDailyMarketVol())
						.addParameter("symbol", txInput.symbol)
						.addParameter("day_of_month", txInput.day_of_month)
						.addParameter("vol_incr", txInput.vol_incr)
						.executeUpdate()
						.getResult();
				break;
			case "EXCHANGE":
				List<Row> exchanges = con.createQuery(sql.getAllExchanges()).executeAndFetchTable().rows();
				for (Row exchange : exchanges)
				{
					String ex_id = exchange.getString("ex_id");
					String currentDesc = exchange.getString("ex_desc");
					String newDesc;
					Pattern p = Pattern.compile("LAST UPDATED .*");
					Matcher m = p.matcher(currentDesc);
					String updateString = "LAST UPDATED " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
					if (m.find())
						newDesc = m.replaceAll(updateString);
					else
						newDesc = currentDesc + " " + updateString;
					rowsAffected += con
							.createQuery(sql.updateExchangeDesc())
							.addParameter("ex_id", ex_id)
							.addParameter("ex_desc", newDesc)
							.executeUpdate()
							.getResult();
				}
				break;
			case "FINANCIAL":
				long count = con
						.createQuery(sql.countFinancialOnFirstOfMonth())
						.addParameter("co_id", txInput.co_id)
						.executeScalar(Long.class);

				if (count > 0)
					rowsAffected = con
							.createQuery(sql.updateFinancialQtrStartDateAdd())
							.addParameter("co_id", txInput.co_id)
							.executeUpdate()
							.getResult();
				else
					rowsAffected = con
							.createQuery(sql.updateFinancialQtrStartDateSubtract())
							.addParameter("co_id", txInput.co_id)
							.executeUpdate()
							.getResult();
				break;
			case "NEWS_ITEM":
				rowsAffected = con
						.createQuery(sql.updateNewsItemDts())
						.addParameter("co_id", txInput.co_id)
						.executeUpdate()
						.getResult();
				break;
			case "SECURITY":
				rowsAffected = con
						.createQuery(sql.updateSecurityExchDate())
						.addParameter("symbol", txInput.symbol)
						.executeUpdate()
						.getResult();
				break;
			case "TAXRATE":
				String currentTxName = con
						.createQuery(sql.getTaxrateName())
						.addParameter("tx_id", txInput.tx_id)
						.executeScalar(String.class);

				if (currentTxName != null)
				{
					String newTxName = currentTxName.contains("Tax") ? currentTxName.replace("Tax", "tax") : currentTxName.replace("tax", "Tax");
					rowsAffected = con
							.createQuery(sql.updateTaxrateName())
							.addParameter("tx_id", txInput.tx_id)
							.addParameter("tx_name", newTxName)
							.executeUpdate()
							.getResult();
				}
				break;
			case "WATCH_ITEM":
				List<Row> watchList = con
						.createQuery(sql.getWatchListItems())
						.addParameter("c_id", txInput.c_id)
						.executeAndFetchTable()
						.rows();
				if (watchList.size() > 0)
				{
					int middleIndex = watchList.size() / 2;
					String symbolToReplace = watchList.get(middleIndex).getString("wi_s_symb");
					long wl_id = watchList.get(middleIndex).getLong("wi_wl_id");
					String newSymbol = con
							.createQuery(sql.getNextSecurityForWatchList())
							.addParameter("wl_id", wl_id)
							.executeScalar(String.class);
					if (newSymbol != null)
						rowsAffected = con
								.createQuery(sql.updateWatchItem())
								.addParameter("wl_id", wl_id)
								.addParameter("old_symbol", symbolToReplace)
								.addParameter("new_symbol", newSymbol)
								.executeUpdate()
								.getResult();
				}
				break;
			}

			txOutput.table_name = txInput.table_name;
			txOutput.rows_affected = rowsAffected;
			con.commit();
		}
		catch (Throwable t)
		{
			ErrorCtx ectx = new ErrorCtx(t);
			String fullMsg = String.format("Data-Maintenance transaction failed for table '%s' -> ", txInput.table_name, ectx.toString());
			txOutput.setStatus(-1);
			txOutput.setStatusMessage(fullMsg);
		}
		return txOutput;
	}
}