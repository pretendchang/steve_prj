package com.cmpdata.dbvendor;

import java.sql.SQLException;

public final class DBExecutorFactory {
	
	public static DBInterface create(String dbvendorname)
	{
		if(dbvendorname.equals("oracle"))
		{
			return new DBExecutor(new OracleVendor());
		}
		else if(dbvendorname.equals("informix"))
		{
			return new DBExecutor(new InformixVendor());
		}
		else
		{
			//throw exception
		}
		return null;
	}
	
	
	public static void main(String []args) throws SQLException
	{
		DBInterface exec = DBExecutorFactory.create("oracle");
		exec.getResultSetValue(null ,1);
	}
}
