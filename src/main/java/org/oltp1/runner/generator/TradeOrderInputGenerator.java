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
	
	// Loader distribution: 60% of accounts have only the owner permission row
	// (egen CustomerAccountsAndPermissionsTable.PERCENT_ACCOUNT_ADDITIONAL_PERMISSIONS_0).
	private static final int PERCENT_ACCOUNTS_WITHOUT_ADDITIONAL_PERMS = 60;
	// Derived from DriverParamSettings.h: type_is_margin (8) is a percentage of buys (50) => 16%.
	private static final int TO_MARGIN_PERCENT_OF_BUYS = TO_PERCENT_MARGIN_TRADE * 100 / TO_PERCENT_BUY_ORDERS;
	// Reference converts TO.rollback (1) to a 1/101 chance via TradeOrderMixLevel (101).
	private static final int TO_ROLLBACK_LIMIT = 101;

	private final CustomerSelector customerSelector;
	private final CompanySelector companySelector;
	private AccountPermissionSelector aclSelector;
	private final HeldSymbolProvider heldSymbolProvider;

	public TradeOrderInputGenerator(
			CustomerSelector customerSelector,
			CompanySelector companySelector,
			AccountPermissionSelector aclSelector,
			HeldSymbolProvider heldSymbolProvider)
	{
		this.customerSelector = customerSelector;
		this.companySelector = companySelector;
		this.aclSelector = aclSelector;
		this.heldSymbolProvider = heldSymbolProvider;
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

		// Determine who is executing the trade. "exec_is_owner" is an overall target (90%);
		// accounts with no additional permissions must always use the owner, so the owner
		// probability for the remaining accounts is adjusted to (90-60)*100/(100-60) = 75%.
		int additionalPerms = aclSelector.getAdditionalPermCount(input.acct_id);

		boolean executorIsOwner;
		if (additionalPerms == 0)
		{
		    executorIsOwner = true;
		}
		else
		{
		    int adjustedOwnerPercent = (TO_PERCENT_EXECUTOR_IS_OWNER - PERCENT_ACCOUNTS_WITHOUT_ADDITIONAL_PERMS)
		            * 100 / (100 - PERCENT_ACCOUNTS_WITHOUT_ADDITIONAL_PERMS);
		    executorIsOwner = random.rndPercent(adjustedOwnerPercent);
		}

		AccountPermission acl = executorIsOwner
		        ? aclSelector.getOwnerAcl(input.acct_id)
		        : aclSelector.getNonOwnerAcl(input.acct_id);

		input.exec_f_name = acl.fName;
		input.exec_l_name = acl.lName;
		input.exec_tax_id = acl.taxId;

		// Reference: the security is one the chosen account actually holds
		// (GenerateRandomAccountSecurity), not an unrelated random security.
		Company rndCompany = null;
		String heldSymbol = heldSymbolProvider.randomHeldSymbol(input.acct_id, random);

		if (heldSymbol != null)
		{
			rndCompany = companySelector.forSymbol(heldSymbol);
		}

		if (rndCompany == null)
		{
			// The account holds nothing yet (or the held symbol is unknown); fall
			// back to a random security so input generation never fails.
			rndCompany = companySelector.randomCompany();
		}

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
		input.roll_it_back = (TO_PERCENT_ROLLBACK >= random.rndIntRange(1, TO_ROLLBACK_LIMIT));

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
			input.type_is_margin = random.rndPercent(TO_MARGIN_PERCENT_OF_BUYS);
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