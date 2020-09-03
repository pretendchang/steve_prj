package com.cmpdata.dbvendor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.sqlparse.SQLStatement;

final class DBExecutor implements DBInterface {
	private DBVendorSPI spi1;
	
	DBExecutor(DBVendorSPI _spi)
	{
		spi1 = _spi;
	}
	
	@Override
	public String getResultSetValue(ResultSet rs, int columnidx) throws SQLException {
		return spi1.getResultSetValue(rs, columnidx);
	}

	@Override
	public String getDateFormatString() {
		// TODO Auto-generated method stub
		return spi1.getDateFormatString();
	}

	@Override
	public String getDateTimeFormatString() {
		// TODO Auto-generated method stub
		return spi1.getDateTimeFormatString();
	}
	public void setFirst(SQLStatement stmt, int firstcnt)
	{
		spi1.setFirst(stmt, firstcnt);
	}
	public String buildTempTableSQL(String rightcnttablename, String tablecolumn)
	{
		return spi1.buildTempTableSQL(rightcnttablename, tablecolumn);
	}
	public String buildIdxSQL(String rightcnttablename, String tableindex)
	{
		return spi1.buildIdxSQL(rightcnttablename, tableindex);
	}
	public void executeTruncateTemptable(Connection conn, String rightcnttablename) throws SQLException
	{
		spi1.executeTruncateTemptable(conn, rightcnttablename);
	}
	public void executeDropTemptable(Connection conn, String rightcnttablename) throws SQLException
	{
		spi1.executeDropTemptable(conn, rightcnttablename);
	}
	public String getTemptableRealname(String tmptablename)
	{
		return spi1.getTemptableRealname(tmptablename);
	}

}
