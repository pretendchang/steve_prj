package com.cmpdata.columnprop;

import java.io.UnsupportedEncodingException;
import java.util.*;

import com.cmpdata.JSONHandler;
import com.cmpdata.TreeMapUtil;
import com.cmpdata.dbvendor.DBInterface;
import com.sqlparse.SQLExpression;
import com.sqlparse.SQLOrderBy;

import com.cmpdata.ColumnMeta;


public class ExecOnPropImpl_getPKOrder_jsql {
	//methodargs[0]: columnmeta
	//methodargs[1]: ColumnMetaName
	//methodargs[2]: dbinterface
	//methodargs[3]: nullmark
	/*
	public static final List<SQLOrderBy> exec(List<String> props, ColumnMeta cm, String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		if(props==null)
		{
			return cm.getPKOrder_jsql_default(ColumnMetaName);
		}
		else if(props.indexOf(ExecOnPropMethod_AND.PROP_NULLCHECK)!=-1 || props.indexOf(ExecOnPropMethod_AND.PROP_NULLCHECK.toLowerCase())!=-1)
		{
			return cm.getPKOrder_jsql_Nullcheck(ColumnMetaName, dbinterface, nullmark);
		}
		else
		{
			return cm.getPKOrder_jsql_default(ColumnMetaName);
		}
	}*/		
	private static final ExecOnPropMethod_AND getPKOrder_jsql_PROP_DEFAULT = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					return ((ColumnMeta)methodargs[0]).getPKOrder_jsql_default((String)methodargs[1]);
				}
			}; 
			
	private static final ExecOnPropMethod_AND getPKOrder_jsql_PROP_NULLCHECK = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					return ((ColumnMeta)methodargs[0]).getPKOrder_jsql_Nullcheck((String)methodargs[1], (DBInterface)methodargs[2], (String)methodargs[3]);
				}
			};
			
	public static final TreeMap<String, ExecOnPropMethod_AND> impls= new TreeMap<String, ExecOnPropMethod_AND>()
	{{
		put(ExecOnPropMethod_AND.PROP_DEFAULT, getPKOrder_jsql_PROP_DEFAULT);
		put(ExecOnPropMethod_AND.PROP_NULLCHECK, getPKOrder_jsql_PROP_NULLCHECK);
	}};
	static void main(String []args)
	{
		Object []objs = new Object[]{"aa","bb"};
		String props = "nullcheck,chinese";
		String value = "";
		//ExecOnPropMethod_OR.exec(ExecOnPropImpl_setPKTreeMap.impls, props, value, objs);
	}
}
