package com.sqlparse;

import java.util.List;

import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;

public class SQLStatement implements SQLPart {
	public static final String CONSTANT_DBVENDOR_INFORMIX= "informix"; 
	private net.sf.jsqlparser.statement.Statement stmt;
	private String dbtype = null;
	public SQLStatement()
	{
		stmt= null;
	}
	protected SQLStatement(net.sf.jsqlparser.statement.Statement _stmt)
	{
		stmt = _stmt;
	}
	
	protected SQLStatement(net.sf.jsqlparser.statement.Statement _stmt, String _dbtype)
	{
		stmt = _stmt;
		dbtype = _dbtype;
	}
	
	protected net.sf.jsqlparser.statement.Statement getStatement()
	{
		return stmt;
	}
	
	protected void setStatement(net.sf.jsqlparser.statement.Statement _stmt)
	{
		stmt = _stmt;
	}
	
	public String toString()
	{
		return stmt.toString();
	}
	
	public String getSQL()
	{
		if(dbtype != null && dbtype.equalsIgnoreCase(CONSTANT_DBVENDOR_INFORMIX))
			return InformixBuilder.fixTableForInformix(this);
		else
			return stmt.toString();
	}
	
	public boolean isnull()
	{
		if(stmt == null)
			return true;
		return false;
	}
	public void setnull()
	{
		stmt = null;
	}
	public void copy(SQLStatement s)
	{
		stmt=s.stmt;
	}
	public StringBuffer getStringBuffer()
	{
		if(dbtype != null && dbtype.equalsIgnoreCase(CONSTANT_DBVENDOR_INFORMIX))
			return new StringBuffer(InformixBuilder.fixTableForInformix(this));
		else
			return new StringBuffer(stmt.toString());
	}
}
