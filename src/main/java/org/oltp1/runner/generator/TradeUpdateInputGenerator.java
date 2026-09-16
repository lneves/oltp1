package org.oltp1.runner.generator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.oltp1.runner.tx.trade_update.TxTradeUpdateInput;

public class TradeUpdateInputGenerator
{
	// Default values from DriverParamSettings.h for a compliant run
	private static final int TU_PERCENT_FRAME_1 = 33;
	private static final int TU_PERCENT_FRAME_2 = 33;
	// The remaining 34% is for Frame 3

	private static final int TU_MAX_ROWS_FRAME_1 = 20;
	private static final int TU_MAX_UPDATES_FRAME_1 = 20;
	private static final int TU_MAX_ROWS_FRAME_2 = 20;
	private static final int TU_MAX_UPDATES_FRAME_2 = 20;
	private static final int TU_MAX_ROWS_FRAME_3 = 20;
	private static final int TU_MAX_UPDATES_FRAME_3 = 20;

	// Constants from MiscConsts.h for non-uniform random generation
	private static final int TU_A_VALUE_FRAME_1 = 65535;
	private static final int TU_S_VALUE_FRAME_1 = 7;

	private static final int TU_A_VALUE_FOR_TIME_GEN_FRAME2 = 4095;
	private static final int TU_S_VALUE_FOR_TIME_GEN_FRAME2 = 16;

	// BackOffFromEndTime defaults from DriverParamSettings.h (CTradeUpdateSettings)
	private static final int TU_BACKOFF_SECONDS_FRAME2 = 4 * 8 * 3600; // four 8-hour days
	private static final int TU_BACKOFF_SECONDS_FRAME3 = 200 * 60;

	private final TradeTracker tradeTracker;
	private final CustomerSelector customerSelector;
	private final CompanySelector companySelector;
	private final EnvironementSelector environementSelector;

	public TradeUpdateInputGenerator(TradeTracker tradeTracker, CustomerSelector customerSelector, CompanySelector companySelector, EnvironementSelector environementSelector)
	{
		this.tradeTracker = tradeTracker;
		this.customerSelector = customerSelector;
		this.companySelector = companySelector;
		this.environementSelector = environementSelector;
	}

	public TxTradeUpdateInput generateTradeUpdateInput()
	{
		// frame_to_execute:
		// 1 33% 31% to 35%
		// 2 33% 31% to 35%
		// 3 34% 32% to 36%

		TxTradeUpdateInput input = new TxTradeUpdateInput();
		CRandom random = ThreadLocalCRandom.get();

		int threshold = random.rndIntRange(1, 100);
		int daysOfinitialTrades = environementSelector.getDaysOfInitialTrades();

		if (threshold <= TU_PERCENT_FRAME_1)
		{
			// ### FRAME 1: Update by a list of Trade IDs ###
			input.frame_to_execute = 1;
			input.max_trades = TU_MAX_ROWS_FRAME_1;
			input.max_updates = TU_MAX_UPDATES_FRAME_1;

			Set<Long> tradeIdSet = new HashSet<Long>();

			while (tradeIdSet.size() < TU_MAX_ROWS_FRAME_1)
			{
				long maxActiveTradeId = tradeTracker.getInitialMaxTradeId();
				long t = GenUtils.nonUniformTradeId(random, maxActiveTradeId, TU_A_VALUE_FRAME_1, TU_S_VALUE_FRAME_1);
				tradeIdSet.add(t);
			}

			input.trade_id = tradeIdSet
					.stream()
					.mapToLong(Long::longValue)
					.toArray();

		}
		else if (threshold <= TU_PERCENT_FRAME_1 + TU_PERCENT_FRAME_2)
		{
			// ### FRAME 2: Update by Account ID and Date Range ###
			input.frame_to_execute = 2;
			input.max_trades = TU_MAX_ROWS_FRAME_2;
			input.max_updates = TU_MAX_UPDATES_FRAME_2;
			input.acct_id = customerSelector.randomAccId();

			LocalDateTime s_dts = GenUtils
					.nonUniformTradeDts(
							random,
							daysOfinitialTrades,
							TU_BACKOFF_SECONDS_FRAME2,
							TU_A_VALUE_FOR_TIME_GEN_FRAME2,
							TU_S_VALUE_FOR_TIME_GEN_FRAME2);

			input.start_trade_dts = s_dts;
			// Reference: the interval ends at the end of the initial trade period.
			input.end_trade_dts = GenUtils.endOfInitialTrades(daysOfinitialTrades);
		}
		else
		{
			// ### FRAME 3: Update by Security Symbol and Date Range ###
			input.frame_to_execute = 3;
			input.max_trades = TU_MAX_ROWS_FRAME_3;
			input.max_updates = TU_MAX_UPDATES_FRAME_3;

			input.symbol = companySelector.randomCompany().getSymbol();

			LocalDateTime s_dts = GenUtils
					.nonUniformTradeDts(
							random,
							daysOfinitialTrades,
							TU_BACKOFF_SECONDS_FRAME3,
							TU_A_VALUE_FOR_TIME_GEN_FRAME2,
							TU_S_VALUE_FOR_TIME_GEN_FRAME2);

			input.start_trade_dts = s_dts;
			// Reference: the interval ends at the end of the initial trade period.
			input.end_trade_dts = GenUtils.endOfInitialTrades(daysOfinitialTrades);

			input.max_acct_id = customerSelector.getMaxAccId();
		}
		return input;
	}
}