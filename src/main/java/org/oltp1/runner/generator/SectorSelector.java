package org.oltp1.runner.generator;

import java.util.List;

import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.model.Sector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.ResultSetHandler;

public class SectorSelector
{
	private static final Logger log = LoggerFactory.getLogger(SectorSelector.class);

	private final List<Sector> lstSector;

	public SectorSelector(SqlContext sqlCtx)
	{
		try (Connection con = sqlCtx.getSql2o().open())
		{
			Query tx = con.createQuery("SELECT sc_id, sc_name FROM sector;");

			ResultSetHandler<Sector> rsh = (rs) -> new Sector(rs.getString("sc_id"), rs.getString("sc_name"));

			lstSector = tx.executeAndFetch(rsh);

			log.info("Loaded {} sector records the from database", lstSector.size());
		}
	}

	public Sector get()
	{
		CRandom crand = ThreadLocalCRandom.get();
		return lstSector.get(crand.rndIntRange(0, lstSector.size() - 1));
	}
}
