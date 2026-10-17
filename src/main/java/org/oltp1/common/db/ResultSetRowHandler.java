package org.oltp1.common.db;

import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetRowHandler
{
	void handle(java.sql.ResultSet r) throws SQLException;
}