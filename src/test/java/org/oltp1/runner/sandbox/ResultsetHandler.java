package org.oltp1.runner.sandbox;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class ResultsetHandler
{
	public static String rowToString(ResultSet rs) throws SQLException
	{
		ResultSetMetaData meta = rs.getMetaData();
		int columnCount = meta.getColumnCount();
		StringBuilder rowString = new StringBuilder();

		for (int i = 1; i <= columnCount; i++)
		{
			String columnName = meta.getColumnLabel(i); // or getColumnName(i)
			Object value = rs.getObject(i);
			rowString.append(columnName).append("=").append(value);
			if (i < columnCount)
			{
				rowString.append(", ");
			}
		}

		return rowString.toString();
	}

}
