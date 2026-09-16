package org.oltp1.runner.tx;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.oltp1.runner.db.SqlEngine;
import org.oltp1.runner.tx.broker_volume.BrokerVolumeDialect;
import org.oltp1.runner.tx.broker_volume.MariaDbBrokerVolumeDialect;
import org.oltp1.runner.tx.broker_volume.MsSqlBrokerVolumeDialect;
import org.oltp1.runner.tx.broker_volume.PgSqlBrokerVolumeDialect;
import org.oltp1.runner.tx.customer_position.CustomerPositionDialect;
import org.oltp1.runner.tx.customer_position.DefaultCustomerPositionDialect;
import org.oltp1.runner.tx.data_maintenance.DataMaintenanceDialect;
import org.oltp1.runner.tx.data_maintenance.MariaDbDataMaintenanceDialect;
import org.oltp1.runner.tx.data_maintenance.MsSqlDataMaintenanceDialect;
import org.oltp1.runner.tx.data_maintenance.PgSqlDataMaintenanceDialect;
import org.oltp1.runner.tx.market_feed.MariaDbMarketFeedDialect;
import org.oltp1.runner.tx.market_feed.MarketFeedDialect;
import org.oltp1.runner.tx.market_feed.MsSqlMarketFeedDialect;
import org.oltp1.runner.tx.market_feed.PgSqlMarketFeedDialect;
import org.oltp1.runner.tx.market_watch.DefaultMarketWatchDialect;
import org.oltp1.runner.tx.market_watch.MarketWatchDialect;
import org.oltp1.runner.tx.security_detail.DefaultSecurityDetailDialect;
import org.oltp1.runner.tx.security_detail.SecurityDetailDialect;
import org.oltp1.runner.tx.trade_cleanup.MariaDbTradeCleanupDialect;
import org.oltp1.runner.tx.trade_cleanup.MsSqlTradeCleanupDialect;
import org.oltp1.runner.tx.trade_cleanup.PgSqlTradeCleanupDialect;
import org.oltp1.runner.tx.trade_cleanup.TradeCleanupDialect;
import org.oltp1.runner.tx.trade_lookup.MariaDbTradeLookupDialect;
import org.oltp1.runner.tx.trade_lookup.MsSqlTradeLookupDialect;
import org.oltp1.runner.tx.trade_lookup.PgSqlTradeLookupDialect;
import org.oltp1.runner.tx.trade_lookup.TradeLookupDialect;
import org.oltp1.runner.tx.trade_order.DefaultTradeOrderDialect;
import org.oltp1.runner.tx.trade_order.TradeOrderDialect;
import org.oltp1.runner.tx.trade_result.MariaDbTradeResultDialect;
import org.oltp1.runner.tx.trade_result.MsSqlTradeResultDialect;
import org.oltp1.runner.tx.trade_result.PgSqlTradeResultDialect;
import org.oltp1.runner.tx.trade_result.TradeResultDialect;
import org.oltp1.runner.tx.trade_status.DefaultTradeStatusDialect;
import org.oltp1.runner.tx.trade_status.TradeStatusDialect;
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
		register(BrokerVolumeDialect.class)
				.withEngine(SqlEngine.POSTGRESQL, PgSqlBrokerVolumeDialect::new)
				.withEngine(SqlEngine.MSSQL, MsSqlBrokerVolumeDialect::new)
				.withEngine(SqlEngine.MARIADB, MariaDbBrokerVolumeDialect::new)
				.withEngine(SqlEngine.ORIOLEDB, PgSqlBrokerVolumeDialect::new); // Same as PostgreSQL

		// Customer Position
		register(CustomerPositionDialect.class)
				.withEngine(SqlEngine.POSTGRESQL, DefaultCustomerPositionDialect::new)
				.withEngine(SqlEngine.MSSQL, DefaultCustomerPositionDialect::new)
				.withEngine(SqlEngine.MARIADB, DefaultCustomerPositionDialect::new) // Same as PostgreSQL
				.withEngine(SqlEngine.ORIOLEDB, DefaultCustomerPositionDialect::new);

		// Data Maintenance
		register(DataMaintenanceDialect.class)
				.withEngine(SqlEngine.POSTGRESQL, PgSqlDataMaintenanceDialect::new)
				.withEngine(SqlEngine.MSSQL, MsSqlDataMaintenanceDialect::new)
				.withEngine(SqlEngine.MARIADB, MariaDbDataMaintenanceDialect::new)
				.withEngine(SqlEngine.ORIOLEDB, PgSqlDataMaintenanceDialect::new);

		// Market Feed
		register(MarketFeedDialect.class)
				.withEngine(SqlEngine.POSTGRESQL, PgSqlMarketFeedDialect::new)
				.withEngine(SqlEngine.MSSQL, MsSqlMarketFeedDialect::new)
				.withEngine(SqlEngine.MARIADB, MariaDbMarketFeedDialect::new)
				.withEngine(SqlEngine.ORIOLEDB, PgSqlMarketFeedDialect::new);

		// Market Watch (uses default for all engines)
		register(MarketWatchDialect.class)
				.withEngine(SqlEngine.POSTGRESQL, DefaultMarketWatchDialect::new)
				.withEngine(SqlEngine.MSSQL, DefaultMarketWatchDialect::new)
				.withEngine(SqlEngine.MARIADB, DefaultMarketWatchDialect::new)
				.withEngine(SqlEngine.ORIOLEDB, DefaultMarketWatchDialect::new);

		// Security Detail (uses default for all engines)
		register(SecurityDetailDialect.class)
				.withEngine(SqlEngine.POSTGRESQL, DefaultSecurityDetailDialect::new)
				.withEngine(SqlEngine.MSSQL, DefaultSecurityDetailDialect::new)
				.withEngine(SqlEngine.MARIADB, DefaultSecurityDetailDialect::new)
				.withEngine(SqlEngine.ORIOLEDB, DefaultSecurityDetailDialect::new);

		// Trade Cleanup
		register(TradeCleanupDialect.class)
				.withEngine(SqlEngine.POSTGRESQL, PgSqlTradeCleanupDialect::new)
				.withEngine(SqlEngine.MSSQL, MsSqlTradeCleanupDialect::new)
				.withEngine(SqlEngine.MARIADB, MariaDbTradeCleanupDialect::new)
				.withEngine(SqlEngine.ORIOLEDB, PgSqlTradeCleanupDialect::new);

		// Trade Lookup
		register(TradeLookupDialect.class)
				.withEngine(SqlEngine.POSTGRESQL, PgSqlTradeLookupDialect::new)
				.withEngine(SqlEngine.MSSQL, MsSqlTradeLookupDialect::new)
				.withEngine(SqlEngine.MARIADB, MariaDbTradeLookupDialect::new)
				.withEngine(SqlEngine.ORIOLEDB, PgSqlTradeLookupDialect::new);

		// Trade Order (uses default for all engines)
		register(TradeOrderDialect.class)
				.withEngine(SqlEngine.POSTGRESQL, DefaultTradeOrderDialect::new)
				.withEngine(SqlEngine.MSSQL, DefaultTradeOrderDialect::new)
				.withEngine(SqlEngine.MARIADB, DefaultTradeOrderDialect::new)
				.withEngine(SqlEngine.ORIOLEDB, DefaultTradeOrderDialect::new);

		// Trade Result (uses default for all engines)
		register(TradeResultDialect.class)
				.withEngine(SqlEngine.POSTGRESQL, PgSqlTradeResultDialect::new)
				.withEngine(SqlEngine.MSSQL, MsSqlTradeResultDialect::new)
				.withEngine(SqlEngine.MARIADB, MariaDbTradeResultDialect::new)
				.withEngine(SqlEngine.ORIOLEDB, PgSqlTradeResultDialect::new);

		// Trade Status (uses default for all engines)
		register(TradeStatusDialect.class)
				.withEngine(SqlEngine.POSTGRESQL, DefaultTradeStatusDialect::new)
				.withEngine(SqlEngine.MSSQL, DefaultTradeStatusDialect::new)
				.withEngine(SqlEngine.MARIADB, DefaultTradeStatusDialect::new)
				.withEngine(SqlEngine.ORIOLEDB, DefaultTradeStatusDialect::new);

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
		// private final Class<T> queryInterface;
		private final Map<SqlEngine, Supplier<?>> engineMap;

		public RegistrationBuilder(Class<T> queryInterface)
		{
			// this.queryInterface = queryInterface;
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