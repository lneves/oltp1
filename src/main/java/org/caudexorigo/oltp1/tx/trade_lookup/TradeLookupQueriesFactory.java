package org.caudexorigo.oltp1.tx.trade_lookup;

import org.caudexorigo.db.SqlEngine;

/**
 * Factory for creating the appropriate Trade-Lookup query strategy.
 */
public class TradeLookupQueriesFactory {

    public static TradeLookupQueries getQueries(SqlEngine engine) {
        switch (engine) {
            case POSTGRESQL:
                return new PgSqlTradeLookupQueries();
            case MSSQL:
                return new MsSqlTradeLookupQueries();
            default:
                throw new IllegalArgumentException(
                    String.format("Unsupported SQL engine for Trade-Lookup: %s", engine)
                );
        }
    }
}
