package com.cmpdata.columnprop;

import java.io.UnsupportedEncodingException;
import java.util.*;

import com.cmpdata.JSONHandler;
import com.cmpdata.TreeMapUtil;
import com.cmpdata.dbvendor.DBInterface;

import com.cmpdata.ColumnMeta;


public class ExecOnPropImpl_getPKWhere {
	//methodargs[0]: columnmeta
	//methodargs[1]: ColumnMetaName
	//methodargs[2]: dbinterface
	//methodargs[3]: nullmark 
	/*		
	public static final String exec(List<String> props, ColumnMeta cm, String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		if(props==null)
		{
			return cm.getCheckPKWhere_default(ColumnMetaName);
		}
		else if(props.indexOf(ExecOnPropMethod_AND.PROP_NULLCHECK)!=-1 || props.indexOf(ExecOnPropMethod_AND.PROP_NULLCHECK.toLowerCase())!=-1)
		{
			return cm.getCheckPKWhere_Nullcheck(ColumnMetaName, dbinterface, nullmark);
		}
		else
		{
			return cm.getCheckPKWhere_default(ColumnMetaName);
		}
	}
	*/
	
	private static final ExecOnPropMethod_AND getPKWhere_PROP_DEFAULT = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					return ((ColumnMeta)methodargs[0]).getCheckPKWhere_default((String)methodargs[1]);
				}
			}; 
			
	private static final ExecOnPropMethod_AND getPKWhere_PROP_NULLCHECK = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					return ((ColumnMeta)methodargs[0]).getCheckPKWhere_Nullcheck((String)methodargs[1], (DBInterface)methodargs[2], (String)methodargs[3]);
				}
			};
			
	private static final ExecOnPropMethod_AND getPKWhere_PROP_SERVATTRNULLCHECK = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					return "NVL((case when "+(String)methodargs[1]+"='F ' then 'F' else "+(String)methodargs[1]+" end),'${NULL}') = ?";
				}
			};		
			
	public static final TreeMap<String, ExecOnPropMethod_AND> impls= new TreeMap<String, ExecOnPropMethod_AND>()
	{{
		put(ExecOnPropMethod_AND.PROP_DEFAULT, getPKWhere_PROP_DEFAULT);
		put(ExecOnPropMethod_AND.PROP_NULLCHECK, getPKWhere_PROP_NULLCHECK);
		put("servattrvalnullcheck", getPKWhere_PROP_SERVATTRNULLCHECK);
	}};
	static void main(String []args)
	{
		Object []objs = new Object[]{"aa","bb"};
		String props = "nullcheck,chinese";
		String value = "";
		//ExecOnPropMethod_OR.exec(ExecOnPropImpl_setPKTreeMap.impls, props, value, objs);
	}
}
