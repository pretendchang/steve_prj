package com.cmpdata.columnprop;

import java.io.UnsupportedEncodingException;
import java.util.*;

import com.cmpdata.JSONHandler;
import com.cmpdata.TreeMapUtil;
import com.cmpdata.controller.GlobalVariable;


public class ExecOnPropImpl_checkPK {
	//methodargs[0]: tranformrs.get(lrealcolumn)
	//methodargs[1]: right.getResultSetValue(rsinfo, columnidx)
	//methodargs[2]: rsinfo.getBytes(rrealcolumn)

			
	private static final ExecOnPropMethod_AND subcompare_checkPK_PROP_DEFAULT = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					if(!JSONHandler.cmpColumnValueString(TreeMapUtil.getString(methodargs[0]),JSONHandler.getRTrimColumnValueStringNull((String)methodargs[1])))
						return false;
					return true;
				}
			};
			
	private static final ExecOnPropMethod_AND subcompare_checkPK_PROP_CHINESE = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					byte []bleft = TreeMapUtil.getBytes(methodargs[0], GlobalVariable.getLeftCharset());
					  byte []bright = (byte[]) methodargs[2];
					  if(bright==null)
						  bright=new String("${NULL}").getBytes();
					  //System.out.print("bleft:");printbyte(bleft);
					  //System.out.print("bright:");printbyte(bright);
					  if(!Arrays.equals(bleft, bright))
							return false;
					  return true;
				}
			};
					
	private static final ExecOnPropMethod_AND subcompare_checkPK_PROP_UNTRIM = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					if(!JSONHandler.cmpColumnValueString(TreeMapUtil.getString(methodargs[0]),JSONHandler.getColumnValueStringNull((String) methodargs[1])))
						return false;
					return true;
				}
			};		
	public static final TreeMap<String, ExecOnPropMethod_AND> impls= new TreeMap<String, ExecOnPropMethod_AND>()
	{{
		put(ExecOnPropMethod_AND.PROP_DEFAULT, subcompare_checkPK_PROP_DEFAULT);
		put(ExecOnPropMethod_AND.PROP_CHINESE, subcompare_checkPK_PROP_CHINESE);
		put(ExecOnPropMethod_AND.PROP_UNTRIM, subcompare_checkPK_PROP_UNTRIM);
	}};		
	static void main(String []args)
	{
		Object []objs = new Object[]{"aa","bb"};
		String props = "nullcheck,chinese";
		String value = "";
		//ExecOnPropMethod_OR.exec(ExecOnPropImpl_setPKTreeMap.impls, props, value, objs);
	}
}
