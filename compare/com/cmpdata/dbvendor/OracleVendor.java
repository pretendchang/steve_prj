package com.cmpdata.dbvendor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.sqlparse.Builder;
import com.sqlparse.SQLStatement;

final class OracleVendor extends DBVendorSPI {

	@Override
	String getResultSetValue(ResultSet rs, int columnidx) throws SQLException {
		return rs.getString(columnidx);
	}

	@Override
	String getDateFormatString() {
		return "YYYY-MM-DD";
	}

	@Override
	String getDateTimeFormatString() {
		return "YYYY-MM-DD HH24:MI:SS";
	}
	
	void setFirst(SQLStatement stmt, int firstcnt)
	{
		Builder.addWhere(stmt, Builder.OP_AND, "rownum<="+firstcnt);
	}
	
	String buildTempTableSQL(String rightcnttablename, String tablecolumn)
	{
		return "CREATE GLOBAL TEMPORARY TABLE ORA$PTT_"+rightcnttablename+"("+tablecolumn+",chk number, rightcnt number) ON COMMIT PRESERVE ROWS";
	}
	String buildIdxSQL(String rightcnttablename, String tableindex)
	{
		return "create index id_"+rightcnttablename+" on ORA$PTT_"+rightcnttablename+"("+tableindex+")";
	}
	void executeTruncateTemptable(Connection conn, String rightcnttablename) throws SQLException
	{
		PreparedStatement pstmt = conn.prepareStatement("truncate table ORA$PTT_"+rightcnttablename);
		pstmt.execute();
		pstmt.close();
	}
	void executeDropTemptable(Connection conn, String rightcnttablename) throws SQLException
	{
		PreparedStatement pstmt = conn.prepareStatement("truncate table ORA$PTT_"+rightcnttablename);
		pstmt.execute();
		pstmt.close();
		pstmt = conn.prepareStatement("drop table ORA$PTT_"+rightcnttablename);
		pstmt.execute();
		pstmt.close();
	}
	String getTemptableRealname(String tmptablename)
	{
		return "ORA$PTT_"+tmptablename;
	}

}
