package org.oltp1.runner.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.oltp1.runner.model.Company;
import org.oltp1.runner.tx.trade_lookup.TxTradeLookupInput;
import org.oltp1.runner.tx.trade_update.TxTradeUpdateInput;

class TradeTimeWindowInputGeneratorTest
{
	private static final long SEED = 20250914L;
	private static final int DAYS = 300;
	private static final long MAX_INITIAL_TRADE_ID = 2_592_000L;
	private static final LocalDateTime END_OF_INITIAL_TRADES = GenUtils.endOfInitialTrades(DAYS);

	private TradeTracker tradeTracker;
	private CustomerSelector customerSelector;
	private CompanySelector companySelector;
	private EnvironementSelector environementSelector;

	@BeforeEach
	void setUp()
	{
		tradeTracker = new TradeTracker(MAX_INITIAL_TRADE_ID, LocalDateTime.of(2006, 2, 27, 9, 0));
		ThreadLocalCRandom.set(new CRandom(SEED));

		customerSelector = mock(CustomerSelector.class);
		when(customerSelector.randomAccId()).thenReturn(4_300_000_000L);
		when(customerSelector.getMaxAccId()).thenReturn(4_300_050_000L);

		Company company = new Company("AAAA", "1", 1L, "Company A");
		companySelector = mock(CompanySelector.class);
		when(companySelector.getActiveSecuritiesCount()).thenReturn(10);
		when(companySelector.get(anyInt())).thenReturn(company);
		when(companySelector.randomCompany()).thenReturn(company);

		environementSelector = mock(EnvironementSelector.class);
		when(environementSelector.getDaysOfInitialTrades()).thenReturn(DAYS);
	}

	@Test
	void tradeLookupDateRangesEndAtTheEndOfInitialTrades()
	{
		TradeLookupInputGenerator generator = new TradeLookupInputGenerator(
				tradeTracker,
				customerSelector,
				companySelector,
				environementSelector);

		int timedFrames = 0;

		for (int i = 0; i < 500; i++)
		{
			TxTradeLookupInput input = generator.generateTradeLookupInput();

			if (input.frame_to_execute == 2 || input.frame_to_execute == 3)
			{
				timedFrames++;
				assertNotNull(input.start_trade_dts);
				assertEquals(END_OF_INITIAL_TRADES, input.end_trade_dts);
				assertFalse(input.start_trade_dts.isAfter(input.end_trade_dts));
			}
			else
			{
				assertNull(input.end_trade_dts);
			}
		}

		assertTrue(timedFrames > 0, "expected at least one timed frame");
	}

	@Test
	void tradeUpdateDateRangesEndAtTheEndOfInitialTrades()
	{
		TradeUpdateInputGenerator generator = new TradeUpdateInputGenerator(
				tradeTracker,
				customerSelector,
				companySelector,
				environementSelector);

		int timedFrames = 0;

		for (int i = 0; i < 500; i++)
		{
			TxTradeUpdateInput input = generator.generateTradeUpdateInput();

			if (input.frame_to_execute == 2 || input.frame_to_execute == 3)
			{
				timedFrames++;
				assertNotNull(input.start_trade_dts);
				assertEquals(END_OF_INITIAL_TRADES, input.end_trade_dts);
				assertFalse(input.start_trade_dts.isAfter(input.end_trade_dts));
			}
			else
			{
				assertNull(input.end_trade_dts);
			}
		}

		assertTrue(timedFrames > 0, "expected at least one timed frame");
	}
}
