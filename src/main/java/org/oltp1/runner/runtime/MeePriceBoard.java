package org.oltp1.runner.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.oltp1.runner.db.JdbcQuery;
import org.oltp1.runner.db.SqlContext;

/**
 * Minimal MEE price board: the current last-trade price for every security,
 * loaded once when the MEE starts and kept up to date as real tickers are
 * processed. Artificial ticker entries take their price from this board so
 * padding never writes stale or synthetic prices into {@code last_trade}.
 */
final class MeePriceBoard implements MeeTickerTape.PriceSource
{
	private final String[] symbols;
	private final double[] prices;
	private final Map<String, Integer> indexBySymbol;

	MeePriceBoard(SqlContext sqlCtx)
	{
		List<String> symbolList = new ArrayList<>();
		List<Double> priceList = new ArrayList<>();

		new JdbcQuery(sqlCtx).executeQuery(
				"SELECT lt_s_symb, lt_price FROM last_trade",
				1000,
				rs -> {
					symbolList.add(rs.getString("lt_s_symb"));
					priceList.add(rs.getDouble("lt_price"));
				});

		this.symbols = symbolList.toArray(new String[0]);
		this.prices = new double[priceList.size()];
		this.indexBySymbol = new HashMap<>(Math.max(16, symbols.length * 2));

		for (int i = 0; i < symbols.length; i++)
		{
			prices[i] = priceList.get(i);
			indexBySymbol.putIfAbsent(symbols[i], i);
		}
	}

	@Override
	public int size()
	{
		return symbols.length;
	}

	@Override
	public String symbol(int index)
	{
		return symbols[index];
	}

	@Override
	public double price(int index)
	{
		return prices[index];
	}

	@Override
	public void update(String symbol, double price)
	{
		Integer index = indexBySymbol.get(symbol);

		if (index != null)
		{
			prices[index] = price;
		}
	}
}
