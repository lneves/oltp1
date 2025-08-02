package org.caudexorigo.oltp1;

import picocli.CommandLine.ITypeConverter;

public class CaseInsensitiveEnumConverter implements ITypeConverter<DbEngine>
{
	@Override
	public DbEngine convert(String value) throws Exception
	{
		return DbEngine.valueOf(value.toUpperCase());
	}
}
