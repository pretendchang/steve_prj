package com.cmpdata.logic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.steve.db.data.manager.DSManager;

import com.cmpdata.genericCmpDataConstant;

public class TransLogicDefaultImpl {
	public static TransLogic GetDefaultImpl(String fun)
	{
		if(fun.equalsIgnoreCase("SubStringEuqalVar"))
			return SubStringEuqalVar;
		else if(fun.equalsIgnoreCase("SetConstant"))
			return SetConstant;
		else if(fun.equalsIgnoreCase("GetSeq"))
			return GetSeq;
		return StringEqual;
	}
	
	public static TransLogic StringEqual = new TransLogic(){
		public CmpLogicRst result = new CmpLogicRst();
		public <Val>Val transvalue(Object s1, Object []args, int runid, Object []vars, Class<Val> clazz, TransLogic logic)
		{
			if(clazz.getName().contains("Long"))
				return (Val) new Long((String)s1);
			return (Val)s1;
		}
	};
	
	public static TransLogic SubStringEuqalVar = new TransLogic(){
		public CmpLogicRst result = new CmpLogicRst();
		@SuppressWarnings("unchecked")
		public <Val>Val transvalue(Object s1, Object []args, int runid, Object []vars, Class<Val> clazz, TransLogic logic)
		{
			String s = (String)s1;
			if(s.equals("${NULL}"))
				return (Val)s;
			else
			{
				return (Val)s.substring(Integer.parseInt((String)vars[0]), Integer.parseInt((String)vars[1]));
			}
		}
	};
	
	public static TransLogic GetSeq = new TransLogic(){
		public CmpLogicRst result = new CmpLogicRst();
		private List<List<Long>> lnxt = new ArrayList<List<Long>>();
		private int[] nownxt;
		int threadcnt=24;
		boolean init=false;
		private final int maxnownxt=200;
		@SuppressWarnings("unchecked")
		public <Val>Val transvalue(Object s1, Object []args, int runid, Object []vars, Class<Val> clazz, TransLogic logic)
		{
			if(!init)
			{
				nownxt = new int[threadcnt];
				for(int i=0;i<threadcnt; i++)
				{
					List<Long> nxt = new ArrayList<Long>();
					lnxt.add(nxt);
					nownxt[i]=0;
				}
				init=true;
			}
			
			Long ret=null;
			if(nownxt[runid]==0 || nownxt[runid]>=maxnownxt)
			{
				nownxt[runid]=0;
				List<Long> nxt = lnxt.get(runid);
				nxt.clear();
				
				Connection conn = (Connection)DSManager.getInstance().getConn((String) vars[0]);
				try {
					PreparedStatement pstmt = conn.prepareStatement("select first "+maxnownxt+" "+((String)vars[1])+".NEXTVAL as nxt from "+((String)vars[2])+":systables");
					ResultSet rs = pstmt.executeQuery();
					ret = null;
					while(rs.next())
					{
						nxt.add(rs.getLong(1));
					}
					rs.close();
					pstmt.close();
					rs=null;
					pstmt=null;
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ret = nxt.get(0);
			}
			else
			{
				ret = lnxt.get(runid).get(nownxt[runid]);
			}
			nownxt[runid]++;
			return (Val)ret;
		}
	};
	
	public static TransLogic SetConstant = new TransLogic(){
		public CmpLogicRst result = new CmpLogicRst();
		@SuppressWarnings("unchecked")
		public <Val>Val transvalue(Object s1, Object []args, int runid, Object []vars, Class<Val> clazz, TransLogic logic)
		{
			if(((String)vars[0]).equals(genericCmpDataConstant.CONSTANT_STRING_NULL))
				return null;
			
			if(clazz.getName().contains("Long"))
				return (Val) new Long((String)vars[0]);
			
			return (Val)vars[0];
		}
	};
}
