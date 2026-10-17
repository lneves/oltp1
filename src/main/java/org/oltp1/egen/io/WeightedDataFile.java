package org.oltp1.egen.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Represents a weighted data file, like LastName.txt or AreaCode.txt. This
 * class correctly replicates the C++ EGen architecture by maintaining two
 * lists: 1. A list of unique records. 2. A larger list of indices into the
 * unique records, expanded by weight. Random selections are made against the
 * weighted index list.
 *
 * @param <T>
 *            The type of the data file record.
 */
public class WeightedDataFile<T>
{

	private final List<T> uniqueRecords;
	private final List<Integer> weightedIndexes;

	public WeightedDataFile(List<String> lines, Function<String, T> parser, Function<T, Integer> weightExtractor)
	{
		List<T> tempUniqueRecords = new ArrayList<>();
		List<Integer> tempWeightedIndexes = new ArrayList<>();

		for (String line : lines)
		{
			T record = parser.apply(line);
			int weight = weightExtractor.apply(record);

			// The index for this new unique record is its position in the list.
			int recordIndex = tempUniqueRecords.size();
			tempUniqueRecords.add(record);

			// Add the index to the weighted list 'weight' times.
			for (int i = 0; i < weight; i++)
			{
				tempWeightedIndexes.add(recordIndex);
			}
		}

		this.uniqueRecords = Collections.unmodifiableList(tempUniqueRecords);
		this.weightedIndexes = Collections.unmodifiableList(tempWeightedIndexes);
	}

	/**
	 * Gets a record by its index in the expanded, weighted list. This is the
	 * primary method used for random selection.
	 * 
	 * @param weightedIndex
	 *            An index from 0 to size() - 1.
	 * @return The data record at that weighted position.
	 */
	public T getRecord(int weightedIndex)
	{
		int uniqueIndex = weightedIndexes.get(weightedIndex);
		return uniqueRecords.get(uniqueIndex);
	}

	/**
	 * Gets a record by its index in the unique records list.
	 * 
	 * @param uniqueIndex
	 *            An index from 0 to uniqueSize() - 1.
	 * @return The unique data record.
	 */
	public T getUniqueRecord(int uniqueIndex)
	{
		return uniqueRecords.get(uniqueIndex);
	}

	/**
	 * Returns the total size of the expanded, weighted list.
	 */
	public int size()
	{
		return weightedIndexes.size();
	}

	/**
	 * Returns the number of unique records in the file.
	 */
	public int uniqueSize()
	{
		return uniqueRecords.size();
	}
}