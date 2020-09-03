package com.cmpdata.scripting;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import com.cmpdata.JSONHandler;
import com.cmpdata.controller.GlobalVariable;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.steve.db.data.manager.DSManager;
import com.steve.db.data.manager.InputPstmt;

public class CheckScriptRef {
	/*
	String []pk;
	TreeMap<String,String> QueryRefVal;
	protected CheckScriptRef(TreeMap<String,String> _QueryRefVal, String []_pk)
	{
		QueryRefVal=_QueryRefVal;
		pk=_pk;
	}*/
	//keys:pk1_pk2_{ref:db, ref:sql, ref:var}
	private static LoadingCache<String, List<TreeMap<String,String>>> cachdata = CacheBuilder.newBuilder()
			.expireAfterAccess(10, TimeUnit.MINUTES)
			.maximumSize(1000)
	        .build(
	           new CacheLoader<String, List<TreeMap<String,String>>>() {
	        	   //yyymm_c1
	             public List<TreeMap<String,String>> load(String key) {	
	            	String a[] = key.split("\\{d");
	     			String pk[]=a[0].split("_", -1);
	     			String map = a[1].substring(0,a[1].length()-1);
	   				return (List<TreeMap<String,String>>)QueryRefData(parseMap(map), pk);
	             }
	           });
	
	private static TreeMap<String,String> parseMap(String s)
	{
		TreeMap<String,String> ret = new TreeMap<String,String>();
		int dbi = s.indexOf("b=");
		int dbgi = s.indexOf("dbg=");
		int sqli = s.indexOf("sql=");
		int vari = s.indexOf("var=");
		
		try {
			String dbv = s.substring(dbi+2, dbgi-2);
			ret.put("db", dbv);
			
			String dbgv = s.substring(dbgi+4, sqli-2);
			ret.put("dbg", dbgv);
			
			String sqlv = s.substring(sqli+4, vari-2);
			ret.put("sql", sqlv);
			
			String varv = s.substring(vari+4);
			ret.put("var", varv);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return ret;
	}
	private HashMap<String,String> Query(String []pk, String []col)
	{
		HashMap<String,String> ret = new HashMap<String,String>();
		
		System.out.println("pk");
		for(String p:pk)
		{
			System.out.println("pk:"+p);
		}
		
		System.out.println("col");
		for(String c:col)
		{
			System.out.println("col:"+c);
		}
		
		ret.put("test","test111");
		ret.put("aas","steve");
		
		return ret;
	}
	@SuppressWarnings("unchecked")
	protected static TreeMap<String,String> QueryRef(final TreeMap<String,Object> ref, final String []pk)
	{
		try {
			//TreeMap<String,Object> refclone = new TreeMap<String,Object>(ref);
			ref.remove("col");
			ref.remove("varscripts");
			ref.remove("multi");

			boolean isfirst=true;
			StringBuffer sb = new StringBuffer();
			for(String s:pk)
			{
				if(!isfirst)
					sb.append("_");
				sb.append(s);
				isfirst=false;
			}
			
			/*without cache
			String key=sb.toString()+refclone;
			String a[] = key.split("\\{");
 			String pkval[]=a[0].split("_");
 			String map = a[1].substring(0,a[1].length()-1);
				return QueryRefData(parseMap(map), pkval);
			*/
			List<TreeMap<String,String>> l = (List<TreeMap<String,String>>)cachdata.get(sb.toString()+ref+Thread.currentThread().getId());//refン㎝refン块把计暗keyр琩高挡狦guava cache
			if(l.size() ==0) 
				return null;
			else if(l.size()>1)
			{
				//inform
			}
			return l.get(0);
		}
		catch (Exception e) {
			//System.out.println(e);
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	protected static List<TreeMap<String,String>> QueryRefMulti(final TreeMap<String,Object> ref, final String []pk)
	{
		try {
			//TreeMap<String,Object> refclone = new TreeMap<String,Object>(ref);
			ref.remove("col");
			ref.remove("varscripts");
			ref.remove("multi");

			StringBuffer sb = new StringBuffer();
			for(String s:pk)
			{
				if(sb.length()!=0)
					sb.append("_");
				sb.append(s);
			}
			
			/*without cache
			String key=sb.toString()+refclone;
			String a[] = key.split("\\{");
 			String pkval[]=a[0].split("_");
 			String map = a[1].substring(0,a[1].length()-1);
				return QueryRefData(parseMap(map), pkval);
			*/
			return (List<TreeMap<String,String>>) cachdata.get(sb.toString()+ref+Thread.currentThread().getId());//refン㎝refン块把计暗keyр琩高挡狦guava cache
		}
		catch (Exception e) {
			System.out.println(e);
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	private static List<TreeMap<String,String>> QueryRefData(final TreeMap<String,String> ref, final String []pk)
	{
		if(GlobalVariable.dbgmode==1|| (ref.get("dbg")!=null && ref.get("dbg").equals("YES")))
		{
			System.out.println("QueryRefData db:"+ref.get("db")+"/sql:"+ref.get("sql")+"/"+Arrays.asList(pk));
		}
		@SuppressWarnings("unchecked")
		List<TreeMap> l = DSManager.getInstance().getallobject(ref.get("db"), ref.get("sql"), TreeMap.class,
				new InputPstmt(){
			
					@Override
					public void PstmtMapping(PreparedStatement pstmt) throws SQLException {
						if(!pk[0].equals("null"))
						{
							for(int i=0; i<pk.length; i++)
							{
								if(GlobalVariable.dbgmode==1)
									System.out.println("QueryRefData PstmtMapping:"+pk[i]);
								pstmt.setString(i+1, pk[i]);
							}
						}
					}
	
					@Override
					public <T> T DB2ObjectMapping(ResultSet rs) throws SQLException {
						TreeMap<String,String> r = new TreeMap<String,String>(String.CASE_INSENSITIVE_ORDER);
						ResultSetMetaData rsmeta = rs.getMetaData();
			        	for(int j=1; j<=rsmeta.getColumnCount(); j++)
			        	{
			        		r.put(rsmeta.getColumnLabel(j).toLowerCase(), JSONHandler.getTrimColumnValueString(rs.getString(j)));
			        	}
						return (T)r;
					}
					
				});
		if(GlobalVariable.dbgmode==1)
			System.out.println("QueryRefData rst:"+l);
		List<TreeMap<String, String>> ret = new ArrayList<TreeMap<String,String>>();
		for(TreeMap t : l)
		{
			ret.add(t);
		}
		return ret;
	}
}
