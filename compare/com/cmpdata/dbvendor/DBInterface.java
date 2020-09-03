package com.cmpdata.dbvendor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.sqlparse.SQLStatement;

public interface DBInterface {
	public String getResultSetValue(ResultSet rs, int columnidx) throws SQLException;
	public String getDateFormatString();
	public String getDateTimeFormatString();
	public void setFirst(SQLStatement stmt, int firstcnt);
	public String buildTempTableSQL(String rightcnttablename, String tablecolumn);
	public String buildIdxSQL(String rightcnttablename, String tableindex);
	public void executeTruncateTemptable(Connection conn, String rightcnttablename) throws SQLException;
	public void executeDropTemptable(Connection conn, String rightcnttablename) throws SQLException;
	public String getTemptableRealname(String tmptablename);
}
