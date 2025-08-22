package org.oltp1.runner.tx;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.oltp1.runner.db.SqlEngine;
import org.oltp1.runner.tx.broker_volume.BrokerVolumeQueries;
import org.oltp1.runner.tx.broker_volume.MariaDbBrokerVolumeQueries;
import org.oltp1.runner.tx.broker_volume.MsSqlBrokerVolumeQueries;
import org.oltp1.runner.tx.broker_volume.PgSqlBrokerVolumeQueries;
import org.oltp1.runner.tx.customer_position.CustomerPositionQueries;
import org.oltp1.runner.tx.customer_position.MsSqlCustomerPositionQueries;
import org.oltp1.runner.tx.customer_position.PgSqlCustomerPositionQueries;
import org.oltp1.runner.tx.data_maintenance.DataMaintenanceQueries;
import org.oltp1.runner.tx.data_maintenance.MariaDbDataMaintenanceQueries;
import org.oltp1.runner.tx.data_maintenance.MsSqlDataMaintenanceQueries;
import org.oltp1.runner.tx.data_maintenance.PgSqlDataMaintenanceQueries;
import org.oltp1.runner.tx.market_feed.MariaDbMarketFeedQueries;
import org.oltp1.runner.tx.market_feed.MarketFeedQueries;
import org.oltp1.runner.tx.market_feed.MsSqlMarketFeedQueries;
import org.oltp1.runner.tx.market_feed.PgSqlMarketFeedQueries;
import org.oltp1.runner.tx.market_watch.DefaultMarketWatchQueries;
import org.oltp1.runner.tx.market_watch.MarketWatchQueries;
import org.oltp1.runner.tx.security_detail.DefaultSecurityDetailQueries;
import org.oltp1.runner.tx.security_detail.SecurityDetailQueries;
import org.oltp1.runner.tx.trade_cleanup.MariaDbTradeCleanupQueries;
import org.oltp1.runner.tx.trade_cleanup.MsSqlTradeCleanupQueries;
import org.oltp1.runner.tx.trade_cleanup.PgSqlTradeCleanupQueries;
import org.oltp1.runner.tx.trade_cleanup.TradeCleanupQueries;
import org.oltp1.runner.tx.trade_lookup.MariaDbTradeLookupQueries;
import org.oltp1.runner.tx.trade_lookup.MsSqlTradeLookupQueries;
import org.oltp1.runner.tx.trade_lookup.PgSqlTradeLookupQueries;
import org.oltp1.runner.tx.trade_lookup.TradeLookupQueries;
import org.oltp1.runner.tx.trade_order.DefaultTradeOrderQueries;
import org.oltp1.runner.tx.trade_order.TradeOrderQueries;
import org.oltp1.runner.tx.trade_result.DefaultTradeResultQueries;
import org.oltp1.runner.tx.trade_result.TradeResultQueries;
import org.oltp1.runner.tx.trade_status.DefaultTradeStatusQueries;
import org.oltp1.runner.tx.trade_status.TradeStatusQueries;
import org.oltp1.runner.tx.trade_update.MariaDbTradeUpdateQueries;
import org.oltp1.runner.tx.trade_update.MsSqlTradeUpdateQueries;
import org.oltp1.runner.tx.trade_update.PgSqlTradeUpdateQueries;
import org.oltp1.runner.tx.trade_update.TradeUpdateQueries;

/**
 * Centralized factory for creating database-specific query implementations.
 */
public class QueryFactory
{

	private static final Map<Class<?>, Map<SqlEngine, Supplier<?>>> queryRegistry = new HashMap<>();

	static
	{
		registerQueries();
	}

	/**
	 * Gets the appropriate query implementation for the specified interface and
	 * database engine.
	 * 
	 * @param <T>
	 *            The query interface type
	 * @param queryInterface
	 *            The query interface class
	 * @param engine
	 *            The database engine
	 * @return An instance of the query implementation
	 * @throws IllegalArgumentException
	 *             if no implementation is registered for the given combination
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getQueries(Class<T> queryInterface, SqlEngine engine)
	{
		Map<SqlEngine, Supplier<?>> engineMap = queryRegistry.get(queryInterface);
		if (engineMap == null)
		{
			throw new IllegalArgumentException(
					String
							.format(
									"No query implementations registered for interface: %s",
									queryInterface.getSimpleName()));
		}

		Supplier<?> supplier = engineMap.get(engine);
		if (supplier == null)
		{
			throw new IllegalArgumentException(
					String
							.format(
									"Unsupported SQL engine %s for %s",
									engine,
									queryInterface.getSimpleName()));
		}

		return (T) supplier.get();
	}

	/*
	 * Registers all query implementations with their corresponding interfaces and
	 * engines.
	 */
	private static void registerQueries()
	{
		// Broker Volume
		register(BrokerVolumeQueries.class)
				.withEngine(SqlEngine.POSTGRESQL, PgSqlBrokerVolumeQueries::new)
				.withEngine(SqlEngine.MSSQL, MsSqlBrokerVolumeQueries::new)
				.withEngine(SqlEngine.MARIADB, MariaDbBrokerVolumeQueries::new)
				.withEngine(SqlEngine.ORIOLEDB, PgSqlBrokerVolumeQueries::new); // Same as PostgreSQL

		// Customer Position
		register(CustomerPositionQueries.class)
				.withEngine(SqlEngine.POSTGRESQL, PgSqlCustomerPositionQueries::new)
				.withEngine(SqlEngine.MSSQL, MsSqlCustomerPositionQueries::new)
				.withEngine(SqlEngine.MARIADB, PgSqlCustomerPositionQueries::new) // Same as PostgreSQL
				.withEngine(SqlEngine.ORIOLEDB, PgSqlCustomerPositionQueries::new);

		// Data Maintenance
		register(DataMaintenanceQueries.class)
				.withEngine(SqlEngine.POSTGRESQL, PgSqlDataMaintenanceQueries::new)
				.withEngine(SqlEngine.MSSQL, MsSqlDataMaintenanceQueries::new)
				.withEngine(SqlEngine.MARIADB, MariaDbDataMaintenanceQueries::new)
				.withEngine(SqlEngine.ORIOLEDB, PgSqlDataMaintenanceQueries::new);

		// Market Feed
		register(MarketFeedQueries.class)
				.withEngine(SqlEngine.POSTGRESQL, PgSqlMarketFeedQueries::new)
				.withEngine(SqlEngine.MSSQL, MsSqlMarketFeedQueries::new)
				.withEngine(SqlEngine.MARIADB, MariaDbMarketFeedQueries::new)
				.withEngine(SqlEngine.ORIOLEDB, PgSqlMarketFeedQueries::new);

		// Market Watch (uses default for all engines)
		register(MarketWatchQueries.class)
				.withEngine(SqlEngine.POSTGRESQL, DefaultMarketWatchQueries::new)
				.withEngine(SqlEngine.MSSQL, DefaultMarketWatchQueries::new)
				.withEngine(SqlEngine.MARIADB, DefaultMarketWatchQueries::new)
				.withEngine(SqlEngine.ORIOLEDB, DefaultMarketWatchQueries::new);

		// Security Detail (uses default for all engines)
		register(SecurityDetailQueries.class)
				.withEngine(SqlEngine.POSTGRESQL, DefaultSecurityDetailQueries::new)
				.withEngine(SqlEngine.MSSQL, DefaultSecurityDetailQueries::new)
				.withEngine(SqlEngine.MARIADB, DefaultSecurityDetailQueries::new)
				.withEngine(SqlEngine.ORIOLEDB, DefaultSecurityDetailQueries::new);

		// Trade Cleanup
		register(TradeCleanupQueries.class)
				.withEngine(SqlEngine.POSTGRESQL, PgSqlTradeCleanupQueries::new)
				.withEngine(SqlEngine.MSSQL, MsSqlTradeCleanupQueries::new)
				.withEngine(SqlEngine.MARIADB, MariaDbTradeCleanupQueries::new)
				.withEngine(SqlEngine.ORIOLEDB, PgSqlTradeCleanupQueries::new);

		// Trade Lookup
		register(TradeLookupQueries.class)
				.withEngine(SqlEngine.POSTGRESQL, PgSqlTradeLookupQueries::new)
				.withEngine(SqlEngine.MSSQL, MsSqlTradeLookupQueries::new)
				.withEngine(SqlEngine.MARIADB, MariaDbTradeLookupQueries::new)
				.withEngine(SqlEngine.ORIOLEDB, PgSqlTradeLookupQueries::new);

		// Trade Order (uses default for all engines)
		register(TradeOrderQueries.class)
				.withEngine(SqlEngine.POSTGRESQL, DefaultTradeOrderQueries::new)
				.withEngine(SqlEngine.MSSQL, DefaultTradeOrderQueries::new)
				.withEngine(SqlEngine.MARIADB, DefaultTradeOrderQueries::new)
				.withEngine(SqlEngine.ORIOLEDB, DefaultTradeOrderQueries::new);

		// Trade Result (uses default for all engines)
		register(TradeResultQueries.class)
				.withEngine(SqlEngine.POSTGRESQL, DefaultTradeResultQueries::new)
				.withEngine(SqlEngine.MSSQL, DefaultTradeResultQueries::new)
				.withEngine(SqlEngine.MARIADB, DefaultTradeResultQueries::new)
				.withEngine(SqlEngine.ORIOLEDB, DefaultTradeResultQueries::new);

		// Trade Status (uses default for all engines)
		register(TradeStatusQueries.class)
				.withEngine(SqlEngine.POSTGRESQL, DefaultTradeStatusQueries::new)
				.withEngine(SqlEngine.MSSQL, DefaultTradeStatusQueries::new)
				.withEngine(SqlEngine.MARIADB, DefaultTradeStatusQueries::new)
				.withEngine(SqlEngine.ORIOLEDB, DefaultTradeStatusQueries::new);

		// Trade Update
		register(TradeUpdateQueries.class)
				.withEngine(SqlEngine.POSTGRESQL, PgSqlTradeUpdateQueries::new)
				.withEngine(SqlEngine.MSSQL, MsSqlTradeUpdateQueries::new)
				.withEngine(SqlEngine.MARIADB, MariaDbTradeUpdateQueries::new)
				.withEngine(SqlEngine.ORIOLEDB, PgSqlTradeUpdateQueries::new);
	}

	private static <T> RegistrationBuilder<T> register(Class<T> queryInterface)
	{
		return new RegistrationBuilder<>(queryInterface);
	}

	private static class RegistrationBuilder<T>
	{
		private final Class<T> queryInterface;
		private final Map<SqlEngine, Supplier<?>> engineMap;

		public RegistrationBuilder(Class<T> queryInterface)
		{
			this.queryInterface = queryInterface;
			this.engineMap = new HashMap<>();
			queryRegistry.put(queryInterface, engineMap);
		}

		public RegistrationBuilder<T> withEngine(SqlEngine engine, Supplier<? extends T> supplier)
		{
			engineMap.put(engine, supplier);
			return this;
		}
	}
}