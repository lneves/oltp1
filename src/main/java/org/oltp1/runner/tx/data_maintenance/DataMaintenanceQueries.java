package org.oltp1.runner.tx.data_maintenance;

/**
 * Defines the contract for supplying SQL queries for the Data-Maintenance
 * transaction.
 */
public interface DataMaintenanceQueries
{
	String getApAcl();

	String updateApAcl();

	String getCustomerAddress();

	String getCompanyAddress();

	String updateAddressLine2();

	String getCompanySpRate();

	String updateCompanySpRate();

	String getCustomerEmail2();

	String updateCustomerEmail2();

	String getCustomerTaxrateIds();

	String updateCustomerTaxrate();

	String updateDailyMarketVol();

	String getAllExchanges();

	String updateExchangeDesc();

	String countFinancialOnFirstOfMonth();

	String updateFinancialQtrStartDateAdd();

	String updateFinancialQtrStartDateSubtract();

	String updateNewsItemDts();

	String updateSecurityExchDate();

	String getTaxrateName();

	String updateTaxrateName();

	String getWatchListItems();

	String getNextSecurityForWatchList();

	String updateWatchItem();
}
