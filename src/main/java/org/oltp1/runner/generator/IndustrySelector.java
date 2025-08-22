package org.oltp1.runner.generator;

import java.util.List;

import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.model.Industry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.ResultSetHandler;

public class IndustrySelector
{
	private static final Logger log = LoggerFactory.getLogger(IndustrySelector.class);

	private final List<Industry> lstIndustry;

	public IndustrySelector(SqlContext sqlCtx)
	{
		super();

		try (Connection con = sqlCtx.getSql2o().open())
		{
			Query tx = con.createQuery("SELECT in_id, in_name, in_sc_id FROM industry;");

			ResultSetHandler<Industry> rsh = (rs) -> new Industry(rs.getString("in_id"), rs.getString("in_name"), rs.getString("in_sc_id"));
			lstIndustry = tx.executeAndFetch(rsh);

			log.info("Loaded {} industry records the from database", lstIndustry.size());

		}
		catch (Throwable t)
		{
			throw new RuntimeException(t);
		}
	}

	public int getLen()
	{
		return lstIndustry.size();
	}

	public Industry get(int ix)
	{
		return lstIndustry.get(ix);
	}
}