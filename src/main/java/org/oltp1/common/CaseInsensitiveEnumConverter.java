package org.oltp1.common;

import org.oltp1.runner.db.SqlEngine;

import picocli.CommandLine.ITypeConverter;

public class CaseInsensitiveEnumConverter implements ITypeConverter<SqlEngine>
{
	@Override
	public SqlEngine convert(String value) throws Exception
	{
		return SqlEngine.fromString(value);
	}
}
