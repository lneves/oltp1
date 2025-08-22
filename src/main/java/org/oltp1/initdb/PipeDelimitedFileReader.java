package org.oltp1.initdb;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import com.microsoft.sqlserver.jdbc.ISQLServerBulkData;

public class PipeDelimitedFileReader implements ISQLServerBulkData
{
	private static final long serialVersionUID = 3703724643177850339L;
	private final BufferedReader reader;
	private String[] currentRow;
	private String[] columnNames;
	private int columnCount;
	private boolean hasNext = true;

	public PipeDelimitedFileReader(Path filePath) throws IOException
	{
		this.reader = new BufferedReader(
				new InputStreamReader(
						new FileInputStream(filePath.toFile()),
						StandardCharsets.UTF_8));

		// For now, we'll determine column count from the first row
		// In a real implementation, you might want to get this from table metadata
		String firstLine = reader.readLine();
		if (firstLine != null)
		{
			currentRow = firstLine.split("\\|", -1); // -1 to include empty trailing fields
			columnCount = currentRow.length;
		}
		else
		{
			hasNext = false;
			columnCount = 0;
		}
	}

	@Override
	public Object[] getRowData()
	{
		if (currentRow == null)
		{
			return null;
		}

		Object[] rowData = new Object[columnCount];
		for (int i = 0; i < columnCount && i < currentRow.length; i++)
		{
			String value = currentRow[i];
			// Convert empty strings to null for proper NULL handling
			rowData[i] = value.isEmpty() ? null : value;
		}

		// Read next row for next iteration
		String nextLine;
		try
		{
			nextLine = reader.readLine();

			if (nextLine != null)
			{
				currentRow = nextLine.split("\\|", -1);
			}
			else
			{
				currentRow = null;
				hasNext = false;
			}
		}
		catch (IOException ioe)
		{
			throw new RuntimeException(ioe);
		}

		return rowData;
	}

	@Override
	public boolean next()
	{
		return hasNext;
	}

	public void close() throws Exception
	{
		if (reader != null)
		{
			reader.close();
		}
	}

	public int getColumnCount()
	{
		return columnCount;
	}

	public String getColumnName(int column)
	{
		// Return generic column names - the bulk copy operation
		// will map these to actual table columns by position
		return "Column" + (column + 1);
	}

	@Override
	public Set<Integer> getColumnOrdinals()
	{
		// Return ordinals for all columns (1-based indexing)
		Set<Integer> ordinals = new HashSet<>();
		for (int i = 1; i <= columnCount; i++)
		{
			ordinals.add(i);
		}
		return ordinals;
	}

	@Override
	public int getColumnType(int column)
	{
		// Return VARCHAR for all columns - SQL Server will handle type conversion
		return java.sql.Types.VARCHAR;
	}

	@Override
	public int getPrecision(int column)
	{
		// For VARCHAR, return a reasonable maximum length
		return 8000; // SQL Server VARCHAR max length without specifying MAX
	}

	@Override
	public int getScale(int column)
	{
		// Scale is not applicable for VARCHAR
		return 0;
	}


}