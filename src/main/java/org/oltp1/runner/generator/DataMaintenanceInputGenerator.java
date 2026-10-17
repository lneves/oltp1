package org.oltp1.runner.generator;

import org.oltp1.runner.tx.data_maintenance.TxDataMaintenanceInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataMaintenanceInputGenerator
{
	private static final Logger log = LoggerFactory.getLogger(DataMaintenanceInputGenerator.class);

	private static final String[] tables = {
			"ACCOUNT_PERMISSION", "ADDRESS", "COMPANY", "CUSTOMER", "CUSTOMER_TAXRATE",
			"DAILY_MARKET", "EXCHANGE", "FINANCIAL", "NEWS_ITEM", "SECURITY",
			"TAXRATE", "WATCH_ITEM"
	};

	private int dataMaintenanceCounter = 0;

	private CustomerSelector customerSelector;

	private CompanySelector companySelector;

	private TaxRateSelector taxRateSelector;; // Internal counter to cycle through tables.

	public DataMaintenanceInputGenerator(CustomerSelector customerSelector, CompanySelector companySelector, TaxRateSelector taxRateSelector)
	{
		this.customerSelector = customerSelector;
		this.companySelector = companySelector;
		this.taxRateSelector = taxRateSelector;
	}

	public TxDataMaintenanceInput generateDataMaintenanceInput()
	{
		TxDataMaintenanceInput input = new TxDataMaintenanceInput();
		CRandom crand = ThreadLocalCRandom.get();

		// Get the next table name in the cycle.
		input.table_name = tables[dataMaintenanceCounter];
		dataMaintenanceCounter = (dataMaintenanceCounter + 1) % tables.length; // ensures wrap-around

		try
		{
			// Generate inputs based on the selected table
			switch (input.table_name)
			{
			case "ACCOUNT_PERMISSION":
				input.acct_id = customerSelector.randomAccId();
				break;
			case "ADDRESS":
				if (crand.rndPercent(67))
				{
					input.c_id = customerSelector.randomCustomer().cId;
				}
				else
				{
					input.co_id = companySelector.randomCompany().getCoId();
				}
				break;
			case "COMPANY":
				input.co_id = companySelector.randomCompany().getCoId();
				break;
			case "CUSTOMER":
				input.c_id = customerSelector.randomCustomer().cId;
				break;
			case "CUSTOMER_TAXRATE":
				input.c_id = customerSelector.randomCustomer().cId;
				break;
			case "DAILY_MARKET":
				input.symbol = companySelector.randomCompany().getSymbol();
				input.day_of_month = crand.rndIntRange(1, 31);
				input.vol_incr = randomDailyMarketVolumeIncrement(crand);
				break;
			case "EXCHANGE":
				// No input needed
				break;
			case "FINANCIAL":
				input.co_id = companySelector.randomCompany().getCoId();
				break;
			case "NEWS_ITEM":
				input.co_id = companySelector.randomCompany().getCoId();
				break;
			case "SECURITY":
				input.symbol = companySelector.randomCompany().getSymbol();
				break;
			case "TAXRATE":
				input.tx_id = taxRateSelector.randomTaxRateId();
				break;
			case "WATCH_ITEM":
				input.c_id = customerSelector.randomCustomer().cId;
				break;
			}
		}
		catch (Exception e)
		{
			log.error("Error during Data-Maintenance input generation for table '{}'.", input.table_name, e);
			// Fallback to a safe default if any selector fails
			input.table_name = "TAXRATE";
			input.tx_id = "US" + crand.rndIntRange(1, 6);
		}

		return input;
	}

	private static int randomDailyMarketVolumeIncrement(CRandom rnd)
	{
		int volIncr = rnd.rndIntRange(-2, 3);
		return (volIncr == 0) ? -3 : volIncr;
	}
}