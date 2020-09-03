package com.cmpdata;


import com.sqlparse.Builder;
import com.sqlparse.Parser;
import com.sqlparse.SQLExpression;
import com.sqlparse.SQLStatement;


public class HashSQLStatement {
	SQLStatement stmt;
	public SQLStatement getStmt()
	{
		return stmt;
	}
	SQLExpression pkwhereptn;
	SQLExpression pkwhere;
	final private static SQLExpression CONSTANT_3_EQUAL_3 = Parser.parseCondExpression("3=3");
	
	private HashSQLStatement()
	{
		
	}
	
	public void buildPKWhere(int repeattime)
	{
		SQLExpression ret = null;
		for(int i=0; i<repeattime; i++)
		{
			ret=Builder.addExpression(ret, Builder.OP_OR, pkwhereptn);
		}
		Builder.replaceConstantExpressWithTargetExpress(stmt, ret, CONSTANT_3_EQUAL_3);
		this.pkwhere = ret;
	}
	
	public void cleanPKWhere()
	{
		if(this.pkwhere == null)
		{
			//throw exception
			System.out.println("HashSQLStatement buildPKWhere firstly");
		}
		Builder.removeWhereWithConstantExpress(stmt, this.pkwhere, CONSTANT_3_EQUAL_3);
	}
	
	public static HashSQLStatement buildHashSQLStatement(String sql, SQLExpression PKWherePtn)
	{
		HashSQLStatement ret = new HashSQLStatement();
		ret.stmt = (SQLStatement)Parser.parseStatement(sql);
		ret.pkwhereptn = PKWherePtn;
		
		return ret;
	}
	
	public static void main(String []args)
	{
	}
	
}

