package com.cmpdata.dbvendor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.sqlparse.Builder;
import com.sqlparse.SQLStatement;

final class InformixVendor extends DBVendorSPI {

	@Override
	String getResultSetValue(ResultSet rs, int columnidx) throws SQLException {
		return rs.getString(columnidx);
	}

	@Override
	String getDateFormatString() {
		return "%Y-%m-%d";
	}

	@Override
	String getDateTimeFormatString() {
		return "%Y-%m-%d %H:%M:%S";
	}
	
	void setFirst(SQLStatement stmt, int firstcnt)
	{
		Builder.setFirst(stmt, firstcnt);
	}
	
	String buildTempTableSQL(String rightcnttablename, String tablecolumn)
	{
		return "create temp table "+rightcnttablename+"("+tablecolumn+",chk integer, rightcnt integer) with no log;";
	}
	String buildIdxSQL(String rightcnttablename, String tableindex)
	{
		return "create index idx_prm on "+rightcnttablename+"("+tableindex+");";
	}
	void executeTruncateTemptable(Connection conn, String rightcnttablename) throws SQLException
	{
		PreparedStatement pstmt = conn.prepareStatement("truncate table "+rightcnttablename);
		pstmt.execute();
		pstmt.close();
	}
	void executeDropTemptable(Connection conn, String rightcnttablename) throws SQLException
	{
		//informix temptable will drop after disconnecting the session, so do nothing.
	}
	String getTemptableRealname(String tmptablename)
	{
		return tmptablename;
	}
}
