package org.oltp1.runner.tx.customer_position;

/**
 * Provides default, database-agnostic implementations for common
 * Customer-Position queries. Database-specific queries are left abstract for
 * subclasses to implement.
 */
public abstract class DefaultCustomerPositionQueries implements CustomerPositionQueries
{

	@Override
	public String getCustomerByCid()
	{
		return "SELECT *, c_id AS cust_id FROM customer WHERE c_id = :cust_id;";
	}

	@Override
	public String getCustomerByTaxid()
	{
		return "SELECT c_id AS cust_id FROM customer WHERE c_tax_id = :tax_id;";
	}

	// The following methods are database-specific and must be implemented by
	// subclasses.
	@Override
	public abstract String getCustomerAccounts();

	@Override
	public abstract String getTradeHistory();
}