package org.oltp1.runner.generator;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.OffHeapStore;
import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.model.Company;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.data.Row;

public class CompanySelector
{
	private static final Logger log = LoggerFactory.getLogger(CompanySelector.class);

	private final int activeCompanyCount;
	private final long minCoId;
	private final long maxCoId;

	private final MVMap<Integer, Company> companyList;
	private final MVMap<String, Company> companyMap;

	private final int storeLen;

	public CompanySelector(SqlContext sqlCtx)
	{
		try (Connection con = sqlCtx.getSql2o().open();)
		{
			OffHeapStore offHeap = new OffHeapStore();
			MVStore store = new MVStore.Builder().fileStore(offHeap).open();

			AtomicInteger ix = new AtomicInteger(0);

			companyList = store.openMap("companyList");
			companyMap = store.openMap("companyList");

			// fallback to raw JDBC, sql2o does not expose the "fetchSize" property
			try (Connection sql2oConn = sqlCtx.getSql2o().open(); java.sql.Connection jdbcConn = sql2oConn.getJdbcConnection(); java.sql.Statement stmt = jdbcConn.createStatement();)
			{
				stmt.setFetchSize(1000);

				String query = ("""
							SELECT s_symb, s_issue, co_id, co_name
							FROM security
							INNER JOIN company ON s_co_id = co_id;
						""");
				try (java.sql.ResultSet r = stmt.executeQuery(query))
				{
					while (r.next())
					{
						Company c = new Company(
								r.getString("s_symb"),
								r.getString("s_issue"),
								r.getLong("co_id"),
								r.getString("co_name"));

						companyList.put(ix.getAndIncrement(), c);
						companyMap.put(r.getString("s_symb"), c);
					}
				}
			}
			catch (SQLException e)
			{
				throw new RuntimeException(e);
			}

			store.commit();
			store.compactFile(60000); // max compact time: 1 minute

			storeLen = ix.get();

			log.info("Loaded {} securities from database", storeLen);
		}

		try (Connection con = sqlCtx.getSql2o().open())
		{
			String sql = """
					SELECT
						COUNT(*) AS co_count
						, MIN(co_id) AS min_co_id
						, MAX(co_id) AS max_co_id
					FROM
						company;
					""";

			Row tuple = con
					.createQuery(sql)
					.executeAndFetchTable()
					.rows()
					.get(0);

			activeCompanyCount = tuple.getInteger("co_count");
			minCoId = tuple.getLong("min_co_id");
			maxCoId = tuple.getLong("max_co_id");

			log.info("Active company count: {}", activeCompanyCount);
			log.info("Min. Company Id: {}", minCoId);
			log.info("Max. Company Id: {}", maxCoId);
		}
	}

	public Company get(int ix)
	{
		return companyList.get(ix);
	}

	public Company randomCompany()
	{
		CRandom crand = ThreadLocalCRandom.get();
		return get(crand.rndIntRange(0, storeLen - 1));
	}

	public Company forSymbol(String symbol)
	{
		return companyMap.get(symbol);
	}

	public int getActiveCompanyCount()
	{
		return activeCompanyCount;
	}

	public int getActiveSecuritiesCount()
	{
		return storeLen;
	}

	public long getMinCoId()
	{
		return minCoId;
	}

	public long getMaxCoId()
	{
		return maxCoId;
	}
}