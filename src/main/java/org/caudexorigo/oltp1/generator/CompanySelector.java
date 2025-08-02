package org.caudexorigo.oltp1.generator;

import java.util.concurrent.atomic.AtomicInteger;

import org.caudexorigo.db.SqlContext;
import org.caudexorigo.oltp1.model.Company;
import org.caudexorigo.oltp1.model.CompanyStore;
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

	private final CompanyStore companyStore;
	private final int storeLen;

	public CompanySelector(SqlContext sqlCtx)
	{
		try (Connection con = sqlCtx.getSql2o().open())
		{
			int secCount = con
					.createQuery("SELECT COUNT(*) FROM \"security\";")
					.executeScalar(Integer.class);

			companyStore = new CompanyStore(secCount * 2);

			AtomicInteger ix = new AtomicInteger(0);

			con
					.createQuery("""
							SELECT s_symb, s_issue, co_id, co_name
							FROM \"security\"
							INNER JOIN company ON s_co_id = co_id;
							""")
					.executeAndFetchTable()
					.rows()
					.stream()
					.forEach(r -> {

						Company c = new Company(
								r.getString("s_symb"),
								r.getString("s_issue"),
								r.getLong("co_id"),
								r.getString("co_name"));

						companyStore.add(ix.incrementAndGet(), c.getSymbol(), c);
					});

			storeLen = ix.get();

			log.info("Loaded {} companies from database", ix.get());
			
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
		return companyStore.getCompanyByIndex(ix);
	}

	public Company randomCompany()
	{
		CRandom crand = ThreadLocalCRandom.get();
		return get(crand.rndIntRange(1, storeLen));
	}

	public Company forSymbol(String symbol)
	{
		return companyStore.getCompanyBySymbol(symbol);
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