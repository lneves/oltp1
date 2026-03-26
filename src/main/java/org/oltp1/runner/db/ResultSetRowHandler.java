package org.oltp1.runner.db;

import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetRowHandler
{
	void handle(java.sql.ResultSet r) throws SQLException;
}