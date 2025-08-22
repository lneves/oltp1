package org.oltp1.runner.generator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.model.Broker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.ResultSetHandler;

public class BrokerSelector
{
	private static final Logger log = LoggerFactory.getLogger(BrokerSelector.class);

	private final List<Broker> lstBroker;

	public BrokerSelector(SqlContext sqlCtx)
	{
		try (Connection con = sqlCtx.getSql2o().open())
		{
			Query tx = con.createQuery("SELECT b_id, b_st_id, b_name FROM broker;");

			ResultSetHandler<Broker> rsh = (rs) -> new Broker(rs.getLong("b_id"), rs.getString("b_st_id"), rs.getString("b_name"));

			lstBroker = tx.executeAndFetch(rsh);

			log.info("Loaded {} brokers from database", lstBroker.size());
		}
	}

	public List<Broker> getList(int size)
	{
		List<Broker> list = new ArrayList<Broker>(lstBroker);
		Collections.shuffle(list);
		return list.subList(0, Math.min(size, list.size()));
	}

	public int getBrokerCount()
	{
		return lstBroker.size();
	}
}