package com.cmpdata.columnprop;

import java.io.UnsupportedEncodingException;
import java.util.*;

import com.cmpdata.JSONHandler;
import com.cmpdata.TreeMapUtil;
import com.cmpdata.dbvendor.DBInterface;
import com.sqlparse.SQLExpression;

import com.cmpdata.ColumnMeta;


public final class ExecOnPropImpl_buildPKWherePtn {
	//methodargs[0]: columnmeta
	//methodargs[1]: ColumnMetaName
	//methodargs[2]: dbinterface
	//methodargs[3]: nullmark
	/*
	public static final SQLExpression exec(List<String> props, ColumnMeta cm, String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		if(props==null)
		{
			return cm.buildPKWherePtn_default(ColumnMetaName);
		}
		else if(props.indexOf(ExecOnPropMethod_AND.PROP_NULLCHECK)!=-1 || props.indexOf(ExecOnPropMethod_AND.PROP_NULLCHECK.toLowerCase())!=-1)
		{
			return cm.buildPKWherePtn_Nullcheck(ColumnMetaName, dbinterface, nullmark);
		}
		else
		{
			return cm.buildPKWherePtn_default(ColumnMetaName);
		}
	}*/
			
	private static final ExecOnPropMethod_AND buildPKWherePtn_PROP_DEFAULT = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					return ((ColumnMeta)methodargs[0]).buildPKWherePtn_default((String)methodargs[1]);
				}
			};
			
	private static final ExecOnPropMethod_AND buildPKWherePtn_PROP_NULLCHECK = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					return ((ColumnMeta)methodargs[0]).buildPKWherePtn_Nullcheck((String)methodargs[1], (DBInterface)methodargs[2], (String)methodargs[3]);
				}
			};
	public static final TreeMap<String, ExecOnPropMethod_AND> impls= new TreeMap<String, ExecOnPropMethod_AND>()
	{{
		put(ExecOnPropMethod_AND.PROP_DEFAULT, buildPKWherePtn_PROP_DEFAULT);
		put(ExecOnPropMethod_AND.PROP_NULLCHECK, buildPKWherePtn_PROP_NULLCHECK);
	}};
	public static void main(String []args)
	{
		System.out.println(impls);
		//ExecOnPropMethod_OR.exec(ExecOnPropImpl_setPKTreeMap.impls, props, value, objs);
	}
}
