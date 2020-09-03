package com.sqlparse;

public class SqlObject {
	String objecttype;
	Object value;
	String alias;
	
	public SqlObject(String _objecttype, Object _value)
	{
		objecttype = _objecttype;
		value = _value;
		alias = "";
	}
	
	public SqlObject(String _objecttype, Object _value, String _alias)
	{
		objecttype = _objecttype;
		value = _value;
		alias = _alias;
	}
}
