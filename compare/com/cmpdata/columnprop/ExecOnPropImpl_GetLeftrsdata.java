package com.cmpdata.columnprop;

import java.io.UnsupportedEncodingException;
import java.util.*;

import com.cmpdata.JSONHandler;
import com.cmpdata.TreeMapUtil;


public class ExecOnPropImpl_GetLeftrsdata {
	//methodargs[0]: left.getResultSetValue(task.rs, columnidx)
	//methodargs[1]: task.rs.getBytes(right.cols.get(i).toString())

			
	private static final ExecOnPropMethod_AND GetLeftrsdata_PROP_DEFAULT = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					return JSONHandler.getRTrimColumnValueStringNull((String)methodargs[0]);
				}
			};
			
	private static final ExecOnPropMethod_AND GetLeftrsdata_PROP_CHINESE = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					return (String)methodargs[1];
				}
			};
					
	private static final ExecOnPropMethod_AND GetLeftrsdata_PROP_UNTRIM = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					return JSONHandler.getColumnValueStringNull((String)methodargs[0]);
				}
			};		
			
	public static final TreeMap<String, ExecOnPropMethod_AND> impls= new TreeMap<String, ExecOnPropMethod_AND>()
	{{
		put(ExecOnPropMethod_AND.PROP_DEFAULT, GetLeftrsdata_PROP_DEFAULT);
		put(ExecOnPropMethod_AND.PROP_CHINESE, GetLeftrsdata_PROP_CHINESE);
		put(ExecOnPropMethod_AND.PROP_UNTRIM, GetLeftrsdata_PROP_UNTRIM);
	}};			
	static void main(String []args)
	{
		Object []objs = new Object[]{"aa","bb"};
		String props = "nullcheck,chinese";
		String value = "";
		//ExecOnPropMethod_OR.exec(ExecOnPropImpl_setPKTreeMap.impls, props, value, objs);
	}
}
