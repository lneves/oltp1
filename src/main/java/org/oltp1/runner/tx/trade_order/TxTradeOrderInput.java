package org.oltp1.runner.tx.trade_order;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.oltp1.runner.model.TradeType;

public class TxTradeOrderInput
{
	public double requested_price;
	public long acct_id;
	public boolean is_lifo;
	public boolean roll_it_back;
	public long trade_qty;
	public boolean type_is_margin;
	public long trade_id;
	public String co_name;
	public String exec_f_name;
	public String exec_l_name;
	public String exec_tax_id;
	public String issue;
	public String st_pending_id;
	public String st_submitted_id;
	public String symbol;
	public TradeType trade_type;

	public TxTradeOrderInput()
	{
		super();
	}

	@Override
	public String toString()
	{
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
				.append("requested_price", requested_price)
				.append("acct_id", acct_id)
				.append("is_lifo", is_lifo)
				.append("roll_it_back", roll_it_back)
				.append("trade_qty", trade_qty)
				.append("type_is_margin", type_is_margin)
				.append("trade_id", trade_id)
				.append("co_name", co_name)
				.append("exec_f_name", exec_f_name)
				.append("exec_l_name", exec_l_name)
				.append("exec_tax_id", exec_tax_id)
				.append("issue", issue)
				.append("st_pending_id", st_pending_id)
				.append("st_submitted_id", st_submitted_id)
				.append("symbol", symbol)
				.append("trade_type_id", trade_type)
				.toString();
	}
}