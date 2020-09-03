package com.sqlparse;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.OrderByElement;

public class SQLOrderBy {
	private OrderByElement o;
	
	public SQLOrderBy(boolean isASC, String column)
	{
		o = new OrderByElement();
		o.setAscDescPresent(true);
		o.setAsc(isASC);
		o.setExpression(new Column(column));
		
	}
	
	public OrderByElement getOrderByElement()
	{
		return o;
	}
}
