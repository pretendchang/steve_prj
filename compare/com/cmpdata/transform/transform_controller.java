package com.cmpdata.transform;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import com.cmpdata.genericCmpDataUtil;
import com.cmpdata.controller.GlobalVariable;
import com.cmpdata.logic.CmpPair;
import com.cmpdata.logic.ScriptAPI;
import com.cmpdata.rst.PKDisplay;
import com.cmpdata.rst.RstFormat;
import com.cmpdata.scripting.CheckScript;
import com.sqlparse.SQLStatement;

import com.cmpdata.*;


public class transform_controller {
	private Cmpsource left;
	private Cmpsource right;
	private String logformatsplitter=",";
	private RstFormat format;
	private int dbgmode=0;
	private int resultsetsize=0;
	
	private SQLStatement leftfirstNowStmt;
	private SQLStatement rightfirstNowStmt;
	private HashMap<String,CmpTask> threadtaskmap;
	private HashMap<String,Integer> threadrunidmap;
	private PrintWriter errpk;
	private String logformat;
	public final PKDisplay pkdisplay; 
	private static transform_controller controller = null;
	synchronized public static transform_controller transform_controllerfactory
	(
		Cmpsource left,
		Cmpsource right,
		SQLStatement leftfirstNowStmt,
		SQLStatement rightfirstNowStmt,
		HashMap<String,CmpTask> threadtaskmap,
		HashMap<String,Integer> threadrunidmap,
		String logformat,
		String logformatsplitter,
		PrintWriter errpk,
		RstFormat format,
		int dbgmode
	)
	{
		if(controller == null)
		{
			controller = 
					   new transform_controller
					   (
							left,
							right,
							leftfirstNowStmt,
							rightfirstNowStmt,
							threadtaskmap,
							threadrunidmap,
							logformat,
							logformatsplitter,
							errpk,
							format,
							dbgmode						   
					   );
		}
		return controller;
	}
	
	private transform_controller
	(
		Cmpsource _left,
		Cmpsource _right,
		SQLStatement _leftfirstNowStmt,
		SQLStatement _rightfirstNowStmt,
		HashMap<String,CmpTask> _threadtaskmap,
		HashMap<String,Integer> _threadrunidmap,
		String _logformat,
		String _logformatsplitter,
		PrintWriter _errpk,
		RstFormat _format,
		int _dbgmode			
	)
	{
		left = _left;
		right = _right;
		leftfirstNowStmt = _leftfirstNowStmt;
		rightfirstNowStmt = _rightfirstNowStmt;
		threadtaskmap = _threadtaskmap;
		threadrunidmap = _threadrunidmap;
		logformat = _logformat;
		logformatsplitter = _logformatsplitter;
		errpk = _errpk;
		format = _format;
		dbgmode = _dbgmode;		
		
		pkdisplay = new PKDisplay()
		 {
			public StringBuffer Display(List<String> pks, TreeMap<String,Object> rs, String splitter)
			{
				return null;
			}
		 };
	}
	
	public void transform(CmpTask task, Connection conn, Set<TreeMap<String,Object>> lrs)
	{
		StringBuilder sb = new StringBuilder();
		StringBuilder sbvalue = new StringBuilder();
		List<CmpPair> lcmppair = CmpPair.getlcmppair();
		int cnt=0;
		sb.append("insert into "+GlobalVariable.getRightCmpsource().getTable()+" (");
		sbvalue.append("values(");
		for(CmpPair cp : lcmppair)
		{
			sb.append(cp.getRightcm().getName());
			sbvalue.append("?");
			if(cnt < (lcmppair.size()-1))
			{
				sb.append(",");
				sbvalue.append(",");
			}
			cnt++;
		}
		sb.append(")");
		sbvalue.append(")");
		sb.append(" ").append(sbvalue);
		int []rst;
		PreparedStatement pstmt = null;
		int erridx=-1;
		do
		{
			if(erridx != -1)
			{
				for(int i=0;i<erridx+1;i++)
				{
					for(TreeMap<String,Object> rs : lrs)
					{
						lrs.remove(rs);
						break;
					}
				}
				erridx=-1;
			}
		try {
				pstmt = conn.prepareStatement(sb.toString());

				for(TreeMap<String,Object> rs : lrs)
				{
					int pstmtidx=1;
					for(CmpPair cp : lcmppair)
					{
						Object rsvalue=null;
						if(cp.getLeftcm() != null)
							rsvalue=rs.get(cp.getLeftcm().getName());
						
						Object v = null;
						if(pstmt.getParameterMetaData().getParameterType(pstmtidx)==java.sql.Types.BIGINT || pstmt.getParameterMetaData().getParameterType(pstmtidx)==java.sql.Types.INTEGER)
							v = getValue(cp, rsvalue, rs, Long.class);
						else
							v = getValue(cp, rsvalue, rs, String.class);
						
						setPstmt(pstmt, pstmtidx++, cp, v);
					}
					pstmt.addBatch();

				}
				rst = pstmt.executeBatch();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				if(e instanceof BatchUpdateException)
				{
					BatchUpdateException ex = (BatchUpdateException)e;
					rst = ex.getUpdateCounts();
					erridx = rst.length;
					ScriptAPI.errpk_println(errpk,"obj can't write("+e+"): "+lrs.toArray()[erridx]);
				}
				else
					e.printStackTrace();
			}
			finally
			{
				try {
					//conn.commit();
					pstmt.close();
					pstmt=null;
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}while(erridx!=-1);
	}
	
	
	
	public void setPstmt(PreparedStatement pstmt, int pstmtidx, CmpPair cp, Object v) throws SQLException
	{
		if(pstmt.getParameterMetaData().getParameterType(pstmtidx)==java.sql.Types.BIGINT || pstmt.getParameterMetaData().getParameterType(pstmtidx)==java.sql.Types.INTEGER)
		{
			pstmt.setLong(pstmtidx, (Long)v);
		}
		else if(cp.getRightcm()!=null && cp.getRightcm().getProps()!=null && (cp.getRightcm().getProps().indexOf("chinese")!=-1 || cp.getRightcm().getProps().indexOf("multibytes")!=-1))
		{
			if(v!=null)
			{
				byte []bs = genericCmpDataUtil.TranscodeCharset((byte[])v, GlobalVariable.getLeftCharset(), GlobalVariable.getRightCharset());
				pstmt.setBytes(pstmtidx, bs);
			}
			else
				pstmt.setBytes(pstmtidx, null);
		}
		else
			pstmt.setString(pstmtidx, (String)v);
	}
	
	public <Val>Val getValue(CmpPair cp, Object obj, TreeMap<String,Object> ref, Class<Val> clazz)
	{
		try {
			List<Object> refs = new ArrayList<Object>();
			List<TreeMap<String,Object>> thisrow = new ArrayList<TreeMap<String,Object>>();
			thisrow.add(ref);
			refs.add(thisrow);//加入thisrow left
				
			if(cp.getTransValueScriptrefs() != null)
			{
				if(!cp.lazyinitrefs)
					refs.addAll(CheckScript.generateRef(GlobalVariable.getSessionVariable(), thisrow, cp.getTransValueScriptrefs()));//加入設定檔定義的ref
				else
					refs.add(cp.getTransValueScriptrefs());
			}
			Integer runid = threadrunidmap.get(Long.toString(Thread.currentThread().getId(), 10));
			Val ret =  cp.getTransValueScript().transvalue(obj, refs.toArray(), runid, cp.getTransLogicvars(), clazz, null);
			if(ret !=null && ret instanceof String && ((String)ret).equals(genericCmpDataConstant.CONSTANT_STRING_NULL))
				ret = null;
			return ret;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
