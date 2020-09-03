package com.cmpdata.columnprop;

import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import com.cmpdata.JSONHandler;
import com.cmpdata.TreeMapUtil;
import com.cmpdata.controller.GlobalVariable;


public class ExecOnPropImpl_setPstmt {
	//methodargs[0]: tranformrs.get(lrealcolumn)
	//methodargs[1]: right.getResultSetValue(rsinfo, columnidx)
	//methodargs[2]: rsinfo.getBytes(rrealcolumn)
			
	private static final ExecOnPropMethod_AND subcompare_setPstmt_PROP_DEFAULT = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					String  pstmtinfoval = TreeMapUtil.getString(methodargs[0]);
					int dbgmode = (int)methodargs[6];
					PreparedStatement pstmtinfo = (PreparedStatement)methodargs[1];
					List<String> pk = (List<String>)methodargs[2];
					int cntlrs = (int)methodargs[3];
					int i = (int)methodargs[4];
					  if(dbgmode==1)
							System.out.println("setPstmt_internal:"+pstmtinfoval);
					  try {
						pstmtinfo.setString(pk.size()*(cntlrs)+i,pstmtinfoval);
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					  
					  return null;
				}
			};
			
	private static final ExecOnPropMethod_AND subcompare_setPstmt_PROP_CHINESE = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					PreparedStatement pstmtinfo = (PreparedStatement)methodargs[1];
					List<String> pk = (List<String>)methodargs[2];
					int cntlrs = (int)methodargs[3];
					int i = (int)methodargs[4];
					Object val = methodargs[0];
					String charset = (String)methodargs[5];
					  try {
						if(val!=null)
							  pstmtinfo.setBytes(pk.size()*(cntlrs)+i,TreeMapUtil.getBytes(val, charset));
						  else
							  pstmtinfo.setBytes(pk.size()*(cntlrs)+i,TreeMapUtil.getBytes("${NULL}", charset));
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					  return null;
				}
			};
					
	private static final ExecOnPropMethod_AND subcompare_setPstmt_PROP_UNTRIM = 
			new ExecOnPropMethod_AND()
			{
				public Object yesfun(Object ...methodargs)
				{
					int dbgmode = (int)methodargs[6];
					PreparedStatement pstmtinfo = (PreparedStatement)methodargs[1];
					List<String> pk = (List<String>)methodargs[2];
					int cntlrs = (int)methodargs[3];
					int i = (int)methodargs[4];
					String  pstmtinfoval = JSONHandler.rollbackColumnValueString(TreeMapUtil.getString(methodargs[0]));
					  if(dbgmode==1)
							System.out.println("setPstmt_internal:"+pstmtinfoval);
					  try {
						pstmtinfo.setString(pk.size()*(cntlrs)+i,pstmtinfoval);
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					  return null;
				}
			};		
	public static final TreeMap<String, ExecOnPropMethod_AND> impls= new TreeMap<String, ExecOnPropMethod_AND>()
	{{
		put(ExecOnPropMethod_AND.PROP_DEFAULT, subcompare_setPstmt_PROP_DEFAULT);
		put(ExecOnPropMethod_AND.PROP_CHINESE, subcompare_setPstmt_PROP_CHINESE);
		put(ExecOnPropMethod_AND.PROP_UNTRIM, subcompare_setPstmt_PROP_UNTRIM);
	}};			
	static void main(String []args)
	{
		Object []objs = new Object[]{"aa","bb"};
		String props = "nullcheck,chinese";
		String value = "";
		//ExecOnPropMethod_OR.exec(ExecOnPropImpl_setPKTreeMap.impls, props, value, objs);
	}
}
