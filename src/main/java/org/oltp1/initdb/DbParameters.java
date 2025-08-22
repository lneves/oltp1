package org.oltp1.initdb;

public class DbParameters
{
	public String host;
	public int port;
	public String dbName;
	public String user;
	public String password;

	public DbParameters(String host, int port, String dbName, String user, String password)
	{
		super();
		this.host = host;
		this.port = port;
		this.dbName = dbName;
		this.user = user;
		this.password = password;
	}
}
