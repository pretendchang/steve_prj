package com.cmpdata;

import java.util.List;

import com.cmpdata.logic.CmpPair;

public final class SQLScriptBuilder {
	/*
	 * initRightcnttable
	 */
	//"create temp table "+rightcnttablename+"("+_tablecolumn+",chk integer, rightcnt integer) with no log;"
	public static String buildRightCntTable(String rightcnttablename, List<CmpPair> lpk)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("create temp table ").append(rightcnttablename)
			.append("(").append(buildRightCntTableKeyColumnDefinition(lpk)).append(",chk integer, rightcnt integer) with no log;");
		
		return sb.toString();
	}
	private static String buildRightCntTableKeyColumnDefinition(List<CmpPair> lpk)
	{
		StringBuilder sb = new StringBuilder();

		return sb.toString();
	}
	/*
	 * insertRightcnttable
	 */
	
	/*
	 * updateRightcnttable
	 */
	
	/*
	 * checkRightcnttable
	 */
	
	/*
	 * checkduplicate
	 */
	
	/*
	 * ref sql
	 */
	
	/*
	 * getresultset
	 */
	
	/*
	 * subcompare
	 */
}
