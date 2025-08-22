package org.oltp1.runner.tx.data_maintenance;

/**
 * Provides the database agnostic SQL queries for the Data-Maintenance
 * transaction.
 */
public abstract class DefaultDataMaintenanceQueries implements DataMaintenanceQueries
{

	@Override
	public String getAllExchanges()
	{
		return "SELECT ex_id, ex_desc FROM exchange";
	}

	@Override
	public String getCompanyAddress()
	{
		return "SELECT ad_id, ad_line2 FROM address JOIN company ON ad_id = co_ad_id WHERE co_id = :co_id";
	}

	@Override
	public String getCompanySpRate()
	{
		return "SELECT co_sp_rate FROM company WHERE co_id = :co_id";
	}

	@Override
	public String getCustomerAddress()
	{
		return "SELECT ad_id, ad_line2 FROM address JOIN customer ON ad_id = c_ad_id WHERE c_id = :c_id";
	}

	@Override
	public String getCustomerEmail2()
	{
		return "SELECT c_email_2 FROM customer WHERE c_id = :c_id";
	}

	@Override
	public String getCustomerTaxrateIds()
	{
		return "SELECT cx_tx_id FROM customer_taxrate WHERE cx_c_id = :c_id";
	}

	@Override
	public String getTaxrateName()
	{
		return "SELECT tx_name FROM taxrate WHERE tx_id = :tx_id";
	}

	@Override
	public String getWatchListItems()
	{
		return "SELECT wi_wl_id, wi_s_symb FROM watch_item JOIN watch_list ON wi_wl_id = wl_id WHERE wl_c_id = :c_id ORDER BY wi_s_symb";
	}

	@Override
	public String updateAddressLine2()
	{
		return "UPDATE address SET ad_line2 = :ad_line2 WHERE ad_id = :ad_id";
	}

	@Override
	public String updateApAcl()
	{
		return "UPDATE account_permission SET ap_acl = :ap_acl WHERE ap_ca_id = :acct_id";
	}

	@Override
	public String updateCompanySpRate()
	{
		return "UPDATE company SET co_sp_rate = :sp_rate WHERE co_id = :co_id";
	}

	@Override
	public String updateCustomerEmail2()
	{
		return "UPDATE customer SET c_email_2 = :c_email_2 WHERE c_id = :c_id";
	}

	@Override
	public String updateCustomerTaxrate()
	{
		return "UPDATE customer_taxrate SET cx_tx_id = :new_tx_id WHERE cx_c_id = :c_id AND cx_tx_id = :old_tx_id";
	}

	@Override
	public String updateExchangeDesc()
	{
		return "UPDATE exchange SET ex_desc = :ex_desc WHERE ex_id = :ex_id";
	}

	@Override
	public String updateTaxrateName()
	{
		return "UPDATE taxrate SET tx_name = :tx_name WHERE tx_id = :tx_id";
	}

	@Override
	public String updateWatchItem()
	{
		return "UPDATE watch_item SET wi_s_symb = :new_symbol WHERE wi_wl_id = :wl_id AND wi_s_symb = :old_symbol";
	}
}
