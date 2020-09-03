package com.sqlparse;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
public class SQLExpression implements SQLPart{
	private Expression exprsson;
	
	public SQLExpression()
	{
		exprsson = null;
	}
	protected SQLExpression(Expression _exprsson)
	{
		exprsson = _exprsson;
	}
	protected void setExpress(Expression _exprsson)
	{
		exprsson = _exprsson;
	}
	protected Expression getExpress()
	{
		return exprsson;
	}
	public String toString()
	{
		return exprsson.toString();
	}
	public void copy(SQLExpression src)
	{
		this.exprsson = src.exprsson;
	}
	public boolean isnull()
	{
		if(exprsson == null)
			return true;
		return false;
	}
	public void setnull()
	{
		exprsson = null;
	}
	
	public String getLeftExpression()
	{
		 if (exprsson instanceof ComparisonOperator) {
             return ((ComparisonOperator)exprsson).getLeftExpression().toString();
         }
		 else
			 return "";
	}
	
	public String getStringExpression()
	{
		if (exprsson instanceof ComparisonOperator) {
            return ((ComparisonOperator)exprsson).getStringExpression().toString();
        }
		 else
			 return "";
	}
	
	public String getRightExpression()
	{
		if (exprsson instanceof ComparisonOperator) {
            return ((ComparisonOperator)exprsson).getRightExpression().toString();
        }
		 else
			 return "";
	}
}
