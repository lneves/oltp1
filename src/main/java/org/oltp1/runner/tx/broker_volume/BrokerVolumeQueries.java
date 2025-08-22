package org.oltp1.runner.tx.broker_volume;

/**
 * Defines the contract for supplying SQL queries for the Broker-Volume
 * transaction.
 */
public interface BrokerVolumeQueries
{
	/**
	 * Returns the SQL query to calculate the total volume for a list of brokers in
	 * a specific sector.
	 * 
	 * @return A SQL query string.
	 */
	String getVolume();
}
