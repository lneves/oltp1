package org.caudexorigo.oltp1.tx.market_watch;

/**
 * Defines the contract for supplying SQL queries for the Market-Watch transaction.
 */
public interface MarketWatchQueries
{
	String getPctChangeByAccount();

	String getPctChangeByCustomer();

	String getPctChangeByIndustry();
}
