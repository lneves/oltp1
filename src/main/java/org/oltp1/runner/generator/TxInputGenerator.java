package org.oltp1.runner.generator;

import org.oltp1.runner.db.SqlContext;
import org.oltp1.runner.tx.broker_volume.TxBrokerVolumeInput;
import org.oltp1.runner.tx.customer_position.TxCustomerPositionInput;
import org.oltp1.runner.tx.data_maintenance.TxDataMaintenanceInput;
import org.oltp1.runner.tx.market_watch.TxMarketWatchInput;
import org.oltp1.runner.tx.security_detail.TxSecurityDetailInput;
import org.oltp1.runner.tx.trade_cleanup.TxTradeCleanupInput;
import org.oltp1.runner.tx.trade_lookup.TxTradeLookupInput;
import org.oltp1.runner.tx.trade_order.TxTradeOrderInput;
import org.oltp1.runner.tx.trade_status.TxTradeStatusInput;
import org.oltp1.runner.tx.trade_update.TxTradeUpdateInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// see: 6.4.1 Input Value Mix Requirements
public class TxInputGenerator
{
	private static final Logger log = LoggerFactory.getLogger(TxInputGenerator.class);

	private final EnvironementSelector environementSelector;
	private final BrokerSelector brokerSelector;
	private final CompanySelector companySelector;
	private final CustomerSelector customerSelector;
	private final IndustrySelector industrySelector;
	private final SectorSelector sectorSelector;
	private final AccountPermissionSelector accountPermissionSelector;

	private final SecurityDetailInputGenerator securityDetailInputGenerator;
	private final BrokerVolumeInputGenerator brokerVolumeInputGenerator;
	private final CustomerPositionInputGenerator customerPositionInputGenerator;
	private final MarketWatchInputGenerator marketWatchInputGenerator;
	private final TradeLookupInputGenerator tradeLookupInputGenerator;
	private final TradeStatusInputGenerator tradeStatusInputGenerator;
	private final TradeOrderInputGenerator tradeOrderInputGenerator;
	private final TradeUpdateInputGenerator tradeUpdateInputGenerator;
	private final TradeCleanupInputGenerator tradeCleanupInputGenerator;
	private final DataMaintenanceInputGenerator dataMaintenanceInputGenerator;

	public TxInputGenerator(SqlContext sqlCtx)
	{
		super();
		log.info("Populate data generators");

		environementSelector = new EnvironementSelector(sqlCtx);
		brokerSelector = new BrokerSelector(sqlCtx);
		companySelector = new CompanySelector(sqlCtx);
		customerSelector = new CustomerSelector(sqlCtx);
		industrySelector = new IndustrySelector(sqlCtx);
		sectorSelector = new SectorSelector(sqlCtx);
		accountPermissionSelector = new AccountPermissionSelector(sqlCtx);

		securityDetailInputGenerator = new SecurityDetailInputGenerator(companySelector);
		brokerVolumeInputGenerator = new BrokerVolumeInputGenerator(brokerSelector, sectorSelector);
		customerPositionInputGenerator = new CustomerPositionInputGenerator(customerSelector);
		marketWatchInputGenerator = new MarketWatchInputGenerator(customerSelector, industrySelector);
		tradeLookupInputGenerator = new TradeLookupInputGenerator(customerSelector, companySelector, environementSelector);
		tradeStatusInputGenerator = new TradeStatusInputGenerator(customerSelector);
		tradeOrderInputGenerator = new TradeOrderInputGenerator(customerSelector, companySelector, accountPermissionSelector);
		tradeUpdateInputGenerator = new TradeUpdateInputGenerator(customerSelector, companySelector, environementSelector);
		tradeCleanupInputGenerator = new TradeCleanupInputGenerator(environementSelector);

		dataMaintenanceInputGenerator = new DataMaintenanceInputGenerator(customerSelector, companySelector);
	}

	public TxBrokerVolumeInput generateBrokerVolumeInput()
	{
		return brokerVolumeInputGenerator.generateBrokerVolumeInput();
	}

	public TxCustomerPositionInput generateCustomerPositionInput()
	{
		return customerPositionInputGenerator.generateCustomerPositionInput();
	}

	public TxMarketWatchInput generateMarketWatchInput()
	{
		return marketWatchInputGenerator.generateMarketWatchInput();
	}

	public TxSecurityDetailInput generateSecurityDetailInput()
	{
		return securityDetailInputGenerator.generateSecurityDetailInput();
	}

	public TxTradeLookupInput generateTradeLookupInput()
	{

		return tradeLookupInputGenerator.generateTradeLookupInput();
	}

	public TxTradeStatusInput generateTradeStatusInput()
	{
		return tradeStatusInputGenerator.generateTradeStatusInput();
	}

	public TxTradeOrderInput generateTradeOrderInput()
	{
		return tradeOrderInputGenerator.generateTradeOrderInput();
	}

	public TxTradeUpdateInput generateTradeUpdateInput()
	{
		return tradeUpdateInputGenerator.generateTradeUpdateInput();
	}

	public TxDataMaintenanceInput generateDataMaintenanceInput()
	{
		return dataMaintenanceInputGenerator.generateDataMaintenanceInput();
	}

	public TxTradeCleanupInput generateTradeCleanupInput()
	{
		return tradeCleanupInputGenerator.generateTradeCleanupInput();
	}

}