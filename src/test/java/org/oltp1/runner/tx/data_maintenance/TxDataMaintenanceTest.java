package org.oltp1.runner.tx.data_maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.oltp1.runner.generator.TxInputGenerator;
import org.sql2o.Connection;
import org.sql2o.Query;
import org.sql2o.Sql2o;
import org.sql2o.data.Row;
import org.sql2o.data.Table;

class TxDataMaintenanceTest
{
	@Test
	void accountPermissionReadsColumnsByNameAndFlipsTheAcl()
	{
		TxDataMaintenanceInput input = new TxDataMaintenanceInput();
		input.table_name = "ACCOUNT_PERMISSION";
		input.acct_id = 77L;

		TxInputGenerator inputGen = mock(TxInputGenerator.class);
		when(inputGen.generateDataMaintenanceInput()).thenReturn(input);

		DataMaintenanceDialect sql = mock(DataMaintenanceDialect.class);
		when(sql.getApAcl()).thenReturn("get-ap-acl");
		when(sql.updateApAcl()).thenReturn("update-ap-acl");

		Sql2o sql2o = mock(Sql2o.class);
		Connection con = mock(Connection.class);
		when(sql2o.beginTransaction()).thenReturn(con);

		Table table = mock(Table.class);
		Row row = mock(Row.class);
		when(row.getString("ap_acl")).thenReturn("1111");
		when(row.getString("ap_tax_id")).thenReturn("TAX-1");
		when(table.rows()).thenReturn(List.of(row));

		Query readQuery = mock(Query.class);
		when(readQuery.addParameter(anyString(), anyLong())).thenReturn(readQuery);
		when(readQuery.executeAndFetchTable()).thenReturn(table);

		Query updateQuery = mock(Query.class);
		when(updateQuery.addParameter(anyString(), anyLong())).thenReturn(updateQuery);
		when(updateQuery.addParameter(anyString(), anyString())).thenReturn(updateQuery);
		when(updateQuery.executeUpdate()).thenReturn(con);

		when(con.createQuery("get-ap-acl")).thenReturn(readQuery);
		when(con.createQuery("update-ap-acl")).thenReturn(updateQuery);
		when(con.getResult()).thenReturn(1);

		TxDataMaintenance tx = new TxDataMaintenance(inputGen, sql2o, sql, null);

		TxDataMaintenanceOutput output = (TxDataMaintenanceOutput) tx.execute();

		assertEquals(0, output.getStatus(), output.getStatusMessage());
		assertEquals("ACCOUNT_PERMISSION", output.table_name);
		assertEquals(1, output.rows_affected);

		verify(updateQuery).addParameter("acct_id", 77L);
		verify(updateQuery).addParameter("tax_id", "TAX-1");
		verify(updateQuery).addParameter("ap_acl", "0011");
		verify(con).commit();
	}

	@Test
	void accountPermissionWithoutMatchingRowSkipsTheUpdate()
	{
		TxDataMaintenanceInput input = new TxDataMaintenanceInput();
		input.table_name = "ACCOUNT_PERMISSION";
		input.acct_id = 77L;

		TxInputGenerator inputGen = mock(TxInputGenerator.class);
		when(inputGen.generateDataMaintenanceInput()).thenReturn(input);

		DataMaintenanceDialect sql = mock(DataMaintenanceDialect.class);
		when(sql.getApAcl()).thenReturn("get-ap-acl");

		Sql2o sql2o = mock(Sql2o.class);
		Connection con = mock(Connection.class);
		when(sql2o.beginTransaction()).thenReturn(con);

		Table table = mock(Table.class);
		when(table.rows()).thenReturn(List.of());

		Query readQuery = mock(Query.class);
		when(readQuery.addParameter(anyString(), anyLong())).thenReturn(readQuery);
		when(readQuery.executeAndFetchTable()).thenReturn(table);

		when(con.createQuery("get-ap-acl")).thenReturn(readQuery);

		TxDataMaintenance tx = new TxDataMaintenance(inputGen, sql2o, sql, null);

		TxDataMaintenanceOutput output = (TxDataMaintenanceOutput) tx.execute();

		assertEquals(0, output.getStatus(), output.getStatusMessage());
		assertEquals(0, output.rows_affected);

		verify(con, never()).createQuery("update-ap-acl");
		verify(con).commit();
	}
}
