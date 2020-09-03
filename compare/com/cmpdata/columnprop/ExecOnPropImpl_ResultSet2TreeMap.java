package com.cmpdata.columnprop;

import java.io.UnsupportedEncodingException;
import java.util.*;

import com.cmpdata.JSONHandler;


public class ExecOnPropImpl_ResultSet2TreeMap {
	//value:right.getResultSetValue(rs, columnidx)
	//methodargs[0]: rs.getBytes(realcolumn);
			
	private static final ExecOnPropMethod_AND ResultSet2TreeMap_PROP_DEFAULT = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					String value = null;
					try {
						value = JSONHandler.getRTrimColumnValueStringNull((String) methodargs[0]);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return value;
				}

			};
			
	private static final ExecOnPropMethod_AND ResultSet2TreeMap_PROP_CHINESE = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					return methodargs[1];
				}
			};
					
	private static final ExecOnPropMethod_AND ResultSet2TreeMap_PROP_UNTRIM = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					return JSONHandler.getColumnValueStringNull((String) methodargs[0]);
				}
			};		
			
	public static TreeMap<String, ExecOnPropMethod_AND> impls= new TreeMap<String, ExecOnPropMethod_AND>()
	{{
		put(ExecOnPropMethod_AND.PROP_DEFAULT, ResultSet2TreeMap_PROP_DEFAULT);
		put(ExecOnPropMethod_AND.PROP_CHINESE, ResultSet2TreeMap_PROP_CHINESE);
		put(ExecOnPropMethod_AND.PROP_UNTRIM, ResultSet2TreeMap_PROP_UNTRIM);
	}};			
	static void main(String []args)
	{
		Object []objs = new Object[]{"aa","bb"};
		String props = "nullcheck,chinese";
		String value = "";
		//ExecOnPropMethod_OR.exec(ExecOnPropImpl_setPKTreeMap.impls, props, value, objs);
	}
}
