package org.oltp1.egen.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

/**
 * A generic class for loading and accessing bucketed data files. Records are
 * grouped into buckets based on an integer key in the first column.
 * 
 * @param <T>
 *            The type of the data file record.
 */
public class BucketedDataFile<T>
{

	private final List<List<T>> buckets = new ArrayList<>();

	public BucketedDataFile(List<String> lines, Function<Deque<String>, T> parser)
	{
		for (String line : lines)
		{
			if (line.trim().isEmpty())
				continue;

			String[] fields = line.split("\\t");
			int bucketId = Integer.parseInt(fields[0]);

			// Convert remaining fields to a Deque for the parser
			Deque<String> recordFields = new LinkedList<>();
			for (int i = 1; i < fields.length; i++)
			{
				recordFields.add(fields[i]);
			}

			T record = parser.apply(recordFields);

			// Ensure bucket list is large enough
			while (buckets.size() < bucketId)
			{
				buckets.add(new ArrayList<>());
			}

			// Bucket IDs are 1-based, list indices are 0-based
			buckets.get(bucketId - 1).add(record);
		}
	}

	/**
	 * Gets all records belonging to a specific bucket.
	 * 
	 * @param bucketId
	 *            The 1-based ID of the bucket.
	 * @return An unmodifiable list of records in that bucket.
	 */
	public List<T> getBucket(int bucketId)
	{
		if (bucketId <= 0 || bucketId > buckets.size())
		{
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(buckets.get(bucketId - 1));
	}

	public int getBucketCount()
	{
		return buckets.size();
	}
}