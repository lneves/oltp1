package org.oltp1.egen.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A utility class for writing generated data rows to output flat files. It
 * encapsulates a BufferedWriter for efficient writing and implements
 * AutoCloseable for safe resource management. Based on inc/FlatFileLoader.h
 */
public class FastFlatFileWriter implements AutoCloseable
{
	private final BufferedWriter writer;

	/**
	 * Creates a new FlatFileWriter. The default behavior is to overwrite the file
	 * if it already exists, which matches the standard EGenLoader workflow.
	 *
	 * @param filePath
	 *            The full path to the output file.
	 * @throws IOException
	 *             if the file cannot be opened for writing.
	 */
	public FastFlatFileWriter(String filePath) throws IOException
	{
		// The second argument 'false' to FileWriter specifies overwrite mode.
		this.writer = new BufferedWriter(new FileWriter(new File(filePath), false));
	}

	/**
	 * Writes a single record to the file, followed by a newline character. The
	 * record object's toString() method is used for serialization.
	 *
	 * @param record
	 *            The data object to write.
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public void writeRecord(AppendableRow record) throws IOException
	{
		record.writeObject(writer);
		writer.newLine();
	}

	/**
	 * Closes the underlying file writer, ensuring all buffered data is flushed.
	 * This method is automatically called when the object is used in a
	 * try-with-resources statement.
	 *
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	@Override
	public void close() throws IOException
	{
		if (writer != null)
		{
			writer.flush();
			writer.close();
		}
	}
}