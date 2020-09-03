package com.cmpdata;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.cmpdata.dbvendor.DBExecutorFactory;
import com.cmpdata.dbvendor.DBInterface;
import com.cmpdata.range.Range;
import com.cmpdata.rst.Cmpsummary;
import com.cmpdata.scripting.CheckScript;
import com.sqlparse.Parser;
import com.sqlparse.SQLStatement;

public class Cmpsource {
	private DBInterface dbinterface;
	public String dbconn;
	private String table;
	
	private String dbtype;
	public String sqlcountcolumn;//checkduplicate㎝オ╰参璸计穦ノ
	public String checkduplicatesql;//checkduplicatesql 璸计逆璶ノcnt
	
	public boolean rightcountref_usegroupby=false;
	public boolean usefirstunionselect=false;
	//ノcmptask.checkRightcnttable∕﹚╰参璸计琌璶groupby箇砞璶groupbyservice m砞称ぃ
	
	private CheckScript countscript;
	public void setCountscript(String script, PrintWriter writer, int threadcnt)
	{
		countscript = CheckScript.CheckScriptFactory("SourceCountScript", script, null, null, writer, threadcnt);
	}
	public void invokeCountscript(Cmpsummary summary, Cmpsummary allsummary, int cnt)
	{
		countscript.InvokeCheckScript(new Object[]{summary, allsummary, cnt});
	}
	public boolean checkCountscript()
	{
		return (countscript==null)?false:true;
	}
	//ノcmptask.checkRightcnttable∕﹚╰参璸计璹呸胯ヘ玡service m砞称穦ノ
	
	
	public List<String> pk;
	private StringBuffer mainsqlsb;
	private SQLStatement stmt;
	//public List<ColumnMeta> cols;
	public List<String> duplicatepk;
	
	public Range range;
	
	public Cmpsource(String _dbtype)
	{
		dbtype = _dbtype;
		dbinterface = DBExecutorFactory.create(dbtype);
		pk = new ArrayList<String>();
		mainsqlsb = new StringBuffer();
		//cols = new ArrayList<ColumnMeta>();
	}
	
	public String getTable()
	{
		return table;
	}
	
	
	public void setTable(String t)
	{
		table = t;
	}
	
	public String getPartSQL()
	{
		return mainsqlsb.toString();
	}
	
	public void setMainsqlsb(StringBuffer sb)
	{
		mainsqlsb = sb;
		stmt = Parser.parseStatement(sb.toString());
	}
	
	public StringBuffer getMainsqlsb()
	{
		return mainsqlsb;
	}
	
	public SQLStatement getStatement()
	{
		stmt = Parser.parseStatement(mainsqlsb.toString());
		return stmt;
	}
	
	//ざ郎ゑ癸ノ
	int []keyfrom;
	int []keyto;
	public void initKeyRangeArr(int size)
	{
		keyfrom = new int[size];
		keyto   = new int[size];
	}
	
	public int getKeyRangeLength()
	{
		int ret=0;
		for(int i=0;i<keyfrom.length;i++)
		{
			ret+=(keyto[i]-keyfrom[i]+1);
		}
		return ret;
	}
	
	public String getResultSetValue(ResultSet rs, int columnidx) throws SQLException
	{
		return dbinterface.getResultSetValue(rs, columnidx);
		
	}
	
	public byte[] getResultSetByte_ignoreException(ResultSet rs, int columnidx) throws SQLException
	{
		byte []o2 = null;
		try {
			o2 = rs.getBytes(columnidx);
		} catch (SQLException e) {
		}
		return o2;
		
	}
	
	public DBInterface getDBInterface()
	{
		return dbinterface;
	}
	
	public String getDateFormatString()
	{
		return dbinterface.getDateFormatString();
	}
	
	public String getDateTimeFormatString()
	{
		return dbinterface.getDateTimeFormatString();
	}
	
	public String getDBType()
	{
		return new String(dbtype);
	}
	
	public void setFirst(SQLStatement stmt, int firstcnt)
	{
		dbinterface.setFirst(stmt, firstcnt);
	}
	
	public String buildTempTableSQL(String rightcnttablename, String tablecolumn)
	{
		return dbinterface.buildTempTableSQL(rightcnttablename, tablecolumn);
	}
	
	public String buildIdxSQL(String rightcnttablename, String tableindex)
	{
		return dbinterface.buildIdxSQL(rightcnttablename, tableindex);
	}
	
	public void executeTruncateTemptable(Connection conn, String rightcnttablename) throws SQLException
	{
		dbinterface.executeTruncateTemptable(conn, rightcnttablename);
	}
	
	public void executeDropTemptable(Connection conn, String rightcnttablename) throws SQLException
	{
		dbinterface.executeDropTemptable(conn, rightcnttablename);
	}
	
	public String getTemptableRealname(String tmptablename)
	{
		return dbinterface.getTemptableRealname(tmptablename);
	}
}
