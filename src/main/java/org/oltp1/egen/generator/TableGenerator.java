package org.oltp1.egen.generator;

// Interface for generic table generation
public interface TableGenerator<T>
{
	boolean hasMoreRecords();

	T generateNextRecord();
}