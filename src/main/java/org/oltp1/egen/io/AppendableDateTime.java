package org.oltp1.egen.io;

import java.io.IOException;

import org.oltp1.egen.util.DateTime;

public final class AppendableDateTime
{
	public static void appendDateTime(Appendable out, DateTime dt) throws IOException
	{
		int[] components = dt.getYmdhms();
		int year = components[0];
		int month = components[1];
		int day = components[2];
		int hour = components[3];
		int minute = components[4];
		int second = components[5];
		int msec = components[6];

		appendDateTime(out, year, month, day, hour, minute, second, msec);
	}

	public static void appendDate(Appendable out, DateTime dt) throws IOException
	{
		int[] components = dt.getYmdhms();
		int year = components[0];
		int month = components[1];
		int day = components[2];

		appendDate(out, year, month, day);
	}

	public static void appendDate(
			Appendable out,
			int year, int month, int day) throws IOException
	{
		four(out, year);
		out.append('-');
		two(out, month);
		out.append('-');
		two(out, day);
	}

	public static void appendDateTime(
			Appendable out,
			int year, int month, int day,
			int hour, int minute, int second, int millis) throws IOException
	{
		four(out, year);
		out.append('-');
		two(out, month);
		out.append('-');
		two(out, day);
		out.append(' ');
		two(out, hour);
		out.append(':');
		two(out, minute);
		out.append(':');
		two(out, second);
		out.append('.');
		three(out, millis);
	}

	private static void two(Appendable out, int v) throws IOException
	{
		out.append((char) ('0' + (v / 10)));
		out.append((char) ('0' + (v % 10)));
	}

	private static void three(Appendable out, int v) throws IOException
	{
		out.append((char) ('0' + (v / 100)));
		out.append((char) ('0' + ((v / 10) % 10)));
		out.append((char) ('0' + (v % 10)));
	}

	private static void four(Appendable out, int v) throws IOException
	{
		out.append((char) ('0' + (v / 1000)));
		out.append((char) ('0' + ((v / 100) % 10)));
		out.append((char) ('0' + ((v / 10) % 10)));
		out.append((char) ('0' + (v % 10)));
	}
}
