package com.cmpdata.dbvendor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.sqlparse.SQLStatement;

abstract class DBVendorSPI {
	DBVendorSPI(){}
	abstract String getResultSetValue(ResultSet rs, int columnidx) throws SQLException;
	abstract String getDateFormatString();
	abstract String getDateTimeFormatString();
	abstract void setFirst(SQLStatement stmt, int firstcnt);
	abstract String buildTempTableSQL(String rightcnttablename, String tablecolumn);
	abstract String buildIdxSQL(String rightcnttablename, String tableindex);
	abstract void executeTruncateTemptable(Connection conn, String rightcnttablename) throws SQLException;
	abstract void executeDropTemptable(Connection conn, String rightcnttablename) throws SQLException;
	abstract String getTemptableRealname(String tmptablename);
}
