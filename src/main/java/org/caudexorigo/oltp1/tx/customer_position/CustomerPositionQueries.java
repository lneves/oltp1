package org.caudexorigo.oltp1.tx.customer_position;

/**
 * Defines the contract for supplying SQL queries for the Customer-Position transaction.
 */
public interface CustomerPositionQueries
{
	String getCustomerByCid();

	String getCustomerByTaxid();

	String getCustomerAccounts();

	String getTradeHistory();
}
