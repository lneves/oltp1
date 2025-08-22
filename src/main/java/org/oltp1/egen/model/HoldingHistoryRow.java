package org.oltp1.egen.model;

import java.io.IOException;
import java.util.StringJoiner;

import org.oltp1.egen.io.AppendableRow;

public class HoldingHistoryRow implements AppendableRow
{
	public long HH_H_T_ID;
	public long HH_T_ID;
	public int HH_BEFORE_QTY;
	public int HH_AFTER_QTY;

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(HH_H_T_ID))
				.add(String.valueOf(HH_T_ID))
				.add(String.valueOf(HH_BEFORE_QTY))
				.add(String.valueOf(HH_AFTER_QTY))
				.toString();
	}

	@Override
	public void writeObject(Appendable out) throws IOException
	{
		write(out, HH_H_T_ID);
		write(out, '|');
		write(out, HH_T_ID);
		write(out, '|');
		write(out, HH_BEFORE_QTY);
		write(out, '|');
		write(out, HH_AFTER_QTY);
	}
}