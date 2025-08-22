package org.oltp1.runner.generator;

import org.oltp1.runner.model.AccountPermission;
import org.oltp1.runner.model.Company;
import org.oltp1.runner.model.RandomCustomer;
import org.oltp1.runner.model.TradeStatus;
import org.oltp1.runner.model.TradeType;
import org.oltp1.runner.tx.trade_order.TxTradeOrderInput;

public class TradeOrderInputGenerator
{
	// Default values from DriverParamSettings.h for a compliant run
	private static final int TO_PERCENT_MARKET_TRADE = 60;
	private static final int TO_PERCENT_SECURITY_BY_SYMBOL = 60;
	private static final int TO_PERCENT_BUY_ORDERS = 50;
	private static final int TO_PERCENT_STOP_LOSS = 50;
	private static final int TO_PERCENT_LIFO = 35;
	private static final int TO_PERCENT_MARGIN_TRADE = 8;
	private static final int TO_PERCENT_EXECUTOR_IS_OWNER = 90;
	private static final int TO_PERCENT_ROLLBACK = 1;

	private final CustomerSelector customerSelector;
	private final CompanySelector companySelector;
	private AccountPermissionSelector aclSelector;

	public TradeOrderInputGenerator(
			CustomerSelector customerSelector, CompanySelector companySelector, AccountPermissionSelector aclSelector)
	{
		this.customerSelector = customerSelector;
		this.companySelector = companySelector;
		this.aclSelector = aclSelector;
	}

	public TxTradeOrderInput generateTradeOrderInput()
	{
		// Transactions requested by a third party:
		// 10% 9.5% to 10.5%
		//
		// Security chosen by company name and issue:
		// 40% 38% to 42%
		//
		// type_is_margin:
		// 1 8% - 7.5% to 8.5%
		//
		// roll_it_back:
		// 1 ~1% - 0.94% to 1.04%
		//
		// is_lifo:
		// 1 35% - 33% to 37%
		//
		// trade_qty:
		// 100 25% - 24% to 26%
		// 200 25% - 24% to 26%
		// 400 25% - 24% to 26%
		// 800 25% - 24% to 26%
		//
		// trade_type:
		// TMB 30% - 29.7% to 30.3%
		// TMS 30% - 29.7% to 30.3%
		// TLB 20% - 19.8% to 20.2%
		// TLS 10% - 9.9% to 10.1%
		// TSL 10% - 9.9% to 10.1%
		TxTradeOrderInput input = new TxTradeOrderInput();
		CRandom random = ThreadLocalCRandom.get();

		// Select a non-uniform random customer.
		RandomCustomer rndCustomer = customerSelector.randomCustomer();

		// Select one of that customer's accounts and a security within it.

		input.acct_id = customerSelector.randomAccId(rndCustomer);

		// Determine who is executing the trade.
		// For compliant runs, this is the account owner 90% of the time.
		if (random.rndPercent(TO_PERCENT_EXECUTOR_IS_OWNER))
		{
			AccountPermission acl = aclSelector.getAcl(input.acct_id, true);

			input.exec_f_name = acl.fName;
			input.exec_l_name = acl.lName;
			input.exec_tax_id = acl.taxId;
		}
		else
		{
			AccountPermission acl = aclSelector.getAcl(input.acct_id, false);

			input.exec_f_name = acl.fName;
			input.exec_l_name = acl.lName;
			input.exec_tax_id = acl.taxId;
		}

		Company rndCompany = companySelector.randomCompany();

		// Decide whether to identify the security by symbol or company name.
		if (random.rndPercent(TO_PERCENT_SECURITY_BY_SYMBOL))
		{
			input.symbol = rndCompany.getSymbol();
		}
		else
		{
			input.co_name = rndCompany.getCoName();
			input.issue = rndCompany.getIssue();
		}

		// Generate trade details.
		input.trade_qty = new int[] { 100, 200, 400, 800 }[random.rndIntRange(0, 3)];
		input.requested_price = random.rndDoubleIncrRange(20.00, 30.00, 0.01);
		input.is_lifo = random.rndPercent(TO_PERCENT_LIFO);
		input.roll_it_back = random.rndPercent(TO_PERCENT_ROLLBACK);

		// Determine the exact trade type (Market/Limit, Buy/Sell/Stop-Loss).
		boolean isMarket = random.rndPercent(TO_PERCENT_MARKET_TRADE);
		boolean isBuy = random.rndPercent(TO_PERCENT_BUY_ORDERS);

		if (isBuy)
		{
			if (isMarket)
			{
				input.trade_type = TradeType.MARKET_BUY;
			}
			else
			{
				input.trade_type = TradeType.LIMIT_BUY;
			}

			// Margin trades are only possible on buys.
			input.type_is_margin = random.rndPercent(TO_PERCENT_MARGIN_TRADE);
		}
		else
		{ // Is a Sell
			input.type_is_margin = false;

			if (isMarket)
			{
				input.trade_type = TradeType.MARKET_SELL;
			}
			else
			{ // Is a Limit Sell
				input.trade_type = random.rndPercent(TO_PERCENT_STOP_LOSS) ? TradeType.STOP_LOSS : TradeType.LIMIT_SELL;
			}
		}

		// Set pending and submitted status IDs from loaded data.
		input.st_pending_id = TradeStatus.PENDING.id;
		input.st_submitted_id = TradeStatus.SUBMITTED.id;

		return input;
	}
}