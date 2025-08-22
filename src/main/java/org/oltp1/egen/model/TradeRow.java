package org.oltp1.egen.model;

import java.io.IOException;
import java.util.StringJoiner;

import org.oltp1.egen.io.AppendableRow;
import org.oltp1.egen.util.DateTime;

public class TradeRow implements AppendableRow
{
	public long T_ID;
	public DateTime T_DTS;
	public String T_ST_ID;
	public String T_TT_ID;
	public boolean T_IS_CASH;
	public String T_S_SYMB;
	public int T_QTY;
	public double T_BID_PRICE;
	public long T_CA_ID;
	public String T_EXEC_NAME;
	public double T_TRADE_PRICE;
	public double T_CHRG;
	public double T_COMM;
	public double T_TAX;
	public boolean T_LIFO;

	public TradeRow()
	{
		super();
	}

	@Override
	public String toString()
	{
		return new StringJoiner("|")
				.add(String.valueOf(T_ID))
				.add(T_DTS.toFormattedString(12)) // YYYY-MM-DD HH:mm:ss.SSS
				.add(T_ST_ID)
				.add(T_TT_ID)
				.add(T_IS_CASH ? "1" : "0")
				.add(T_S_SYMB)
				.add(String.valueOf(T_QTY))
				.add(String.format("%.2f", T_BID_PRICE))
				.add(String.valueOf(T_CA_ID))
				.add(T_EXEC_NAME)
				.add(String.format("%.2f", T_TRADE_PRICE))
				.add(String.format("%.2f", T_CHRG))
				.add(String.format("%.2f", T_COMM))
				.add(String.format("%.2f", T_TAX))
				.add(T_LIFO ? "1" : "0")
				.toString();
	}

	@Override
	public void writeObject(Appendable out) throws IOException
	{
		write(out, T_ID);
		write(out, '|');
		writeDateTime(out, T_DTS); // YYYY-MM-DD HH:mm:ss.SSS
		write(out, '|');
		write(out, T_ST_ID);
		write(out, '|');
		write(out, T_TT_ID);
		write(out, '|');
		write(out, T_IS_CASH ? "1" : "0");
		write(out, '|');
		write(out, T_S_SYMB);
		write(out, '|');
		write(out, T_QTY);
		write(out, '|');
		write(out, T_BID_PRICE);
		write(out, '|');
		write(out, T_CA_ID);
		write(out, '|');
		write(out, T_EXEC_NAME);
		write(out, '|');
		write(out, T_TRADE_PRICE);
		write(out, '|');
		write(out, T_CHRG);
		write(out, '|');
		write(out, T_COMM);
		write(out, '|');
		write(out, T_TAX);
		write(out, '|');
		write(out, T_LIFO ? "1" : "0");
	}
}