package com.sqlparse;

import java.util.List;

import net.sf.jsqlparser.util.TablesNamesFinder;

public class InformixBuilder {
	public static String fixInformixTableForCompatible(String s)
	{
		return s.replace(":", ".");
	}
	
	public static String fixTableForInformix(SQLStatement stmt)
	{
		StringBuffer sb = new StringBuffer(stmt.toString());
		
		TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
		List<String> tableList = tablesNamesFinder.getTableList(stmt.getStatement());

		for(String s : tableList)
		{
			int i;
			int from=0;
			String sn=s.replace('.', ':');
			while((i = sb.indexOf(s, from))!=-1)
			{
				sb.replace(i, i+s.length(), sn);
				from = i+sn.length();
			}
		}
		return sb.toString();
	}
	
	public static String fixInformixSelectFirst(String s)
	{//replace select top with select first
		return s.toLowerCase().replace("select top", "select first");
	}
	
	public static String setInformixSelectSkipFirst(int skip, String s)
	{//replace select top with select first
		return s.toLowerCase().replace("select top", "select skip "+skip+" first");
	}
}
