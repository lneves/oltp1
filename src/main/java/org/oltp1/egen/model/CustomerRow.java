package org.oltp1.egen.model;

import java.util.StringJoiner;

import org.oltp1.egen.util.DateTime;

public class CustomerRow
{
	public long c_id;
	public String c_tax_id;
	public String c_st_id;
	public String c_l_name;
	public String c_f_name;
	public String c_m_name;
	public char c_gndr;
	public char c_tier;
	public DateTime c_dob;
	public long c_ad_id;
	public String c_ctry_1;
	public String c_area_1;
	public String c_local_1;
	public String c_ext_1;
	public String c_ctry_2;
	public String c_area_2;
	public String c_local_2;
	public String c_ext_2;
	public String c_ctry_3;
	public String c_area_3;
	public String c_local_3;
	public String c_ext_3;
	public String c_email_1;
	public String c_email_2;

	@Override
	public String toString()
	{
		// Using StringJoiner for clean and efficient concatenation
		return new StringJoiner("|")
				.add(String.valueOf(c_id))
				.add(c_tax_id != null ? c_tax_id : "")
				.add(c_st_id != null ? c_st_id : "")
				.add(c_l_name != null ? c_l_name : "")
				.add(c_f_name != null ? c_f_name : "")
				.add(c_m_name != null ? c_m_name : "")
				.add(String.valueOf(c_gndr))
				.add(String.valueOf(c_tier))
				.add(c_dob != null ? c_dob.toFormattedString(10) : "") // 10 = yyyy-mm-dd format
				.add(String.valueOf(c_ad_id))
				.add(c_ctry_1 != null ? c_ctry_1 : "")
				.add(c_area_1 != null ? c_area_1 : "")
				.add(c_local_1 != null ? c_local_1 : "")
				.add(c_ext_1 != null ? c_ext_1 : "")
				.add(c_ctry_2 != null ? c_ctry_2 : "")
				.add(c_area_2 != null ? c_area_2 : "")
				.add(c_local_2 != null ? c_local_2 : "")
				.add(c_ext_2 != null ? c_ext_2 : "")
				.add(c_ctry_3 != null ? c_ctry_3 : "")
				.add(c_area_3 != null ? c_area_3 : "")
				.add(c_local_3 != null ? c_local_3 : "")
				.add(c_ext_3 != null ? c_ext_3 : "")
				.add(c_email_1 != null ? c_email_1 : "")
				.add(c_email_2 != null ? c_email_2 : "")
				.toString();
	}
}