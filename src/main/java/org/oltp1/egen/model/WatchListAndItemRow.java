package org.oltp1.egen.model;

public class WatchListAndItemRow
{
	public WatchListRow watchList;
	public WatchItemRow[] watchItems;

	public WatchListAndItemRow(int itemCount)
	{
		this.watchList = new WatchListRow();
		this.watchItems = new WatchItemRow[itemCount];
		for (int i = 0; i < watchItems.length; i++)
		{
			watchItems[i] = new WatchItemRow();
		}
	}
}
