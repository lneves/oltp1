package org.oltp1.runner.generator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.oltp1.runner.tx.trade_lookup.TxTradeLookupInput;

public class TradeLookupInputGenerator
{
	// Default values from DriverParamSettings.h for a compliant run
	private static final int TL_PERCENT_FRAME_1 = 30;
	private static final int TL_PERCENT_FRAME_2 = 30;
	private static final int TL_PERCENT_FRAME_3 = 30;
	// The remaining 10% is for Frame 4

	private static final int TL_MAX_ROWS_FRAME_1 = 20;
	private static final int TL_MAX_ROWS_FRAME_2 = 20;
	private static final int TL_MAX_ROWS_FRAME_3 = 20;

	// Constants from MiscConsts.h for non-uniform random generation

	private static final int TL_A_VALUE_FOR_TRADE_ID_GEN_FRAME1 = 65535;
	private static final int TL_S_VALUE_FOR_TRADE_ID_GEN_FRAME1 = 7;

	private static final int TL_A_VALUE_FOR_TIME_GEN_FRAME2 = 4095;
	private static final int TL_S_VALUE_FOR_TIME_GEN_FRAME2 = 16;

	private static final int TL_A_VALUE_FOR_SYMBOL_FRAME3 = 0;
	private static final int TL_S_VALUE_FOR_SYMBOL_FRAME3 = 0;

	private static final int TL_A_VALUE_FOR_TIME_GEN_FRAME3 = 4095;
	private static final int TL_S_VALUE_FOR_TIME_GEN_FRAME3 = 16;

	private static final int TL_A_VALUE_FOR_TIME_GEN_FRAME4 = 4095;
	private static final int TL_S_VALUE_FOR_TIME_GEN_FRAME4 = 16;

	private static final long TL_TRADE_SHIFT = 200_000_000_000_000L;

	private final CustomerSelector customerSelector;
	private final CompanySelector companySelector;
	private final EnvironementSelector environementSelector;

	public TradeLookupInputGenerator(CustomerSelector customerSelector, CompanySelector companySelector, EnvironementSelector environementSelector)
	{
		this.customerSelector = customerSelector;
		this.companySelector = companySelector;
		this.environementSelector = environementSelector;
	}

	public TxTradeLookupInput generateTradeLookupInput()
	{
		// frame_to_execute:
		// 1 30% 28.5% to 31.5%
		// 2 30% 28.5% to 31.5%
		// 3 30% 28.5% to 31.5%
		// 4 10% 9.5% to 10.5%

		TxTradeLookupInput input = new TxTradeLookupInput();
		CRandom random = ThreadLocalCRandom.get();

		int threshold = random.rndIntRange(1, 100);
		long maxOfInitialTradeId = environementSelector.getMaxInitialTradeId() - TL_TRADE_SHIFT;
		int daysOfinitialTrades = environementSelector.getDaysOfInitialTrades();

		if (threshold <= TL_PERCENT_FRAME_1)
		{
			// ### FRAME 1: Lookup by a list of Trade IDs ###
			input.frame_to_execute = 1;
			input.frame_to_execute = 1;
			input.max_trades = TL_MAX_ROWS_FRAME_1;

			input.acct_id = 0;
			input.max_acct_id = 0;
			input.start_trade_dts = null;
			input.end_trade_dts = null;
			input.symbol = "";

			Set<Long> tradeIdSet = new HashSet<Long>();

			while (tradeIdSet.size() < TL_MAX_ROWS_FRAME_1)
			{
				long t = GenUtils.nonUniformTradeId(random, maxOfInitialTradeId, TL_A_VALUE_FOR_TRADE_ID_GEN_FRAME1, TL_S_VALUE_FOR_TRADE_ID_GEN_FRAME1);
				tradeIdSet.add(t);
			}

			input.trade_id = tradeIdSet
					.stream()
					.mapToLong(Long::longValue)
					.toArray();
		}
		else if (threshold <= TL_PERCENT_FRAME_1 + TL_PERCENT_FRAME_2)
		{
			// ### FRAME 2: Lookup by Account ID and Date Range ###
			input.frame_to_execute = 2;
			input.max_trades = TL_MAX_ROWS_FRAME_2;
			input.acct_id = customerSelector.randomAccId();

			LocalDateTime s_dts = GenUtils.nonUniformTradeDts(random, daysOfinitialTrades, TL_A_VALUE_FOR_TIME_GEN_FRAME2, TL_S_VALUE_FOR_TIME_GEN_FRAME2);
			// LocalDateTime e_dts = GenUtils.endOfInitialTrades(daysOfinitialTrades);

			input.start_trade_dts = s_dts;
			input.end_trade_dts = environementSelector.getEndOfInitialTrades();
		}
		else if (threshold <= TL_PERCENT_FRAME_1 + TL_PERCENT_FRAME_2 + TL_PERCENT_FRAME_3)
		{
			// ### FRAME 3: Lookup by Security Symbol and Date Range ###
			input.frame_to_execute = 3;
			input.max_trades = TL_MAX_ROWS_FRAME_3;
			int securityIndex = (int) random.nonUniformRandom(0, companySelector.getActiveSecuritiesCount() - 1, TL_A_VALUE_FOR_SYMBOL_FRAME3, TL_S_VALUE_FOR_SYMBOL_FRAME3);
			input.symbol = companySelector.get(securityIndex).getSymbol();

			LocalDateTime s_dts = GenUtils.nonUniformTradeDts(random, daysOfinitialTrades, TL_A_VALUE_FOR_TIME_GEN_FRAME3, TL_S_VALUE_FOR_TIME_GEN_FRAME3);
			// LocalDateTime e_dts = GenUtils.endOfInitialTrades(daysOfinitialTrades);

			input.start_trade_dts = s_dts;
			input.end_trade_dts = environementSelector.getEndOfInitialTrades();

			input.max_acct_id = customerSelector.getMaxAccId();
		}
		else
		{
			// ### FRAME 4: Lookup by Account ID and a specific Trade timestamp ###
			input.frame_to_execute = 4;
			input.acct_id = customerSelector.randomAccId();

			LocalDateTime s_dts = GenUtils.nonUniformTradeDts(random, daysOfinitialTrades, TL_A_VALUE_FOR_TIME_GEN_FRAME4, TL_S_VALUE_FOR_TIME_GEN_FRAME4);
			input.start_trade_dts = s_dts;
		}
		return input;
	}
}
