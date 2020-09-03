package com.cmpdata.columnprop;

import java.io.UnsupportedEncodingException;
import java.util.*;

import com.cmpdata.JSONHandler;


public class ExecOnPropImpl_setPKDisplay {
	//value:right.getResultSetValue(rs, columnidx)
	//methodargs[0]: rs.getBytes(realcolumn);
			
	private static final ExecOnPropMethod_OR subcompare_setPKDisplay_PROP_DEFAULT = 
			new ExecOnPropMethod_OR()
			{
				public Object yesfun(Object value, Object ...methodargs)
				{
					try {
						value = JSONHandler.getTrimColumnValueString((String)value);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return value;
				}
				public Object nofun(Object value, Object ...methodargs)
				{
					return JSONHandler.getTrimColumnValueString((String)value);
				}
			};
			
	private static final ExecOnPropMethod_OR subcompare_setPKDisplay_PROP_CHINESE = 
			new ExecOnPropMethod_OR()
			{
				public Object yesfun(Object value, Object ...methodargs)
				{
					try {
						  if(methodargs[0]!=null)
							  value = new String((byte [])methodargs[0],"ms950");
						  else
							  value = new String("${NULL}");
						//pk.append(new String(rs.getBytes(realcolumn),"ms950"));
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return value;
				}
				public Object nofun(Object value, Object ...methodargs)
				{
					return value;
				}
			};
					
	private static final ExecOnPropMethod_OR subcompare_setPKDisplay_PROP_UNTRIM = 
			new ExecOnPropMethod_OR()
			{
				public Object yesfun(Object value, Object ...methodargs)
				{
					return JSONHandler.getColumnValueString((String)value);
				}
				public Object nofun(Object value, Object ...methodargs)
				{
					return JSONHandler.getTrimColumnValueString((String)value);
				}
			};		
			
	public static TreeMap<String, ExecOnPropMethod_OR> impls= new TreeMap<String, ExecOnPropMethod_OR>()
	{{
		put(ExecOnPropMethod_OR.PROP_DEFAULT, subcompare_setPKDisplay_PROP_DEFAULT);
		put(ExecOnPropMethod_OR.PROP_CHINESE, subcompare_setPKDisplay_PROP_CHINESE);
		put(ExecOnPropMethod_OR.PROP_UNTRIM, subcompare_setPKDisplay_PROP_UNTRIM);
	}};			
	static void main(String []args)
	{
		Object []objs = new Object[]{"aa","bb"};
		String props = "nullcheck,chinese";
		String value = "";
		//ExecOnPropMethod_OR.exec(ExecOnPropImpl_setPKTreeMap.impls, props, value, objs);
	}
}
