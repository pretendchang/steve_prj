package com.cmpdata.rst;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import com.cmpdata.CmpTask;
import com.cmpdata.Cmpsource;
import com.cmpdata.controller.GlobalVariable;
import com.cmpdata.JSONHandler;
import com.cmpdata.RetryDBConnection;
import com.steve.db.data.manager.DBManagerRuntimeException;
import com.steve.db.data.manager.DSManager;
import com.steve.db.data.manager.InputPstmt;

public class RstWriting {
	final public static String dburl = "sql://informix_rst";
	final public static String rst_table_name = "reg_cmpdata_rst";//what's wrong with the raw data
	final public static String meta_table_name = "reg_cmpdata_meta";//how to read raw data
	final public static String raw_table_name = "reg_cmpdata_raw";//for raw data
	final public static String partsummary_table_name = "reg_cmpdata_part_summary";//for part cmpjobsummary
	final public static String summary_table_name = "reg_cmpdata_summary";//for cmpjobsummary
	
	public static boolean enable_write_reg_cmpdata = true;
	
	RstFormat format;//meta table id
	public String casetype;
	String batchno;

	//public String side;//1 left 2 right
	public TreeMap<String,Object> leftrawdata;
	public TreeMap<String,Object> rightrawdata;
	
	public RstWriting(RstFormat _format)
	{
		this.format = _format;
		this.batchno = format.batchno;
		leftrawdata = new TreeMap<String,Object>(String.CASE_INSENSITIVE_ORDER);
		rightrawdata = new TreeMap<String,Object>(String.CASE_INSENSITIVE_ORDER);
	}
	
	protected static void writeMeta(final String id, final String side, final String attrname, final String value)
	{
		DSManager.getInstance().execute(dburl, "insert into "+meta_table_name+" (id,side, attrname, value) values(?,?,?,?)",
				new InputPstmt(){
			
			@Override
			public void PstmtMapping(PreparedStatement pstmt) throws SQLException {
				pstmt.setString(1, id);
				pstmt.setString(2, side);
				pstmt.setString(3, attrname);
				pstmt.setString(4, value);
			}

			@Override
			public <T> T DB2ObjectMapping(ResultSet rs) throws SQLException {
				return null;
			}
			
		});
	}
	
	protected static void writeRst(List<List<Object>> RstWritingRst, final String batchno, final String rowno, final String meta, final String casetype)
	{
		if(enable_write_reg_cmpdata)
		{
			if(RstWritingRst.size()>1000)
			{
				batchwriteRst(RstWritingRst);
				RstWritingRst.clear();
			}
			List<Object> l = new ArrayList<Object>();
			l.add(batchno);l.add(rowno);l.add(meta);l.add(casetype);
			RstWritingRst.add(l);
		}
	}
	
	public static void batchwriteRst(List<List<Object>> RstWritingRst)
	{
		if(enable_write_reg_cmpdata)
		{
			Connection conn = (Connection)DSManager.getInstance().getConn(dburl);

			PreparedStatement pstmt=null;
			try {
				pstmt = conn.prepareStatement("insert into "+rst_table_name+" (batchno, rowno, meta, casetype) values(?,?,?,?)");
				
				for(List<Object> l : RstWritingRst)
				{
					pstmt.setString(1,l.get(0).toString());
					pstmt.setString(2,l.get(1).toString());
					pstmt.setString(3,l.get(2).toString());
					pstmt.setString(4,l.get(3).toString());
					pstmt.addBatch();
				}
				pstmt.executeBatch();
	
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally
			{
				try {
					if(pstmt!=null)
						pstmt.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	protected static void writeSummary(final String batchno)
	{
		DSManager.getInstance().execute(dburl, "insert into "+summary_table_name+" (batchno) values(?)",
				new InputPstmt(){
			
			@Override
			public void PstmtMapping(PreparedStatement pstmt) throws SQLException {
				pstmt.setString(1, batchno);
			}

			@Override
			public <T> T DB2ObjectMapping(ResultSet rs) throws SQLException {
				return null;
			}
			
		});
	}
	
	protected static void writeRaw(List<List<Object>> RstWritingRaw,final String rawno, final String rowno, final String side, final String attrname, final String value)
	{
		if(enable_write_reg_cmpdata)
		{
			if(RstWritingRaw.size()>1000)
			{
				batchwriteRaw(RstWritingRaw);
				RstWritingRaw.clear();
			}
			List<Object> l = new ArrayList<Object>();
			l.add(rawno);l.add(rowno);l.add(side);l.add(attrname);l.add(value);
			RstWritingRaw.add(l);
		}
	}
	
	public static void batchwriteRaw(List<List<Object>> RstWritingRaw)
	{
		if(enable_write_reg_cmpdata)
		{
			Connection conn = (Connection)DSManager.getInstance().getConn(dburl);
			PreparedStatement pstmt=null;
			try {
				pstmt = conn.prepareStatement("insert into "+raw_table_name+" (rawno,rowno, side, attrname, value) values(?,?,?,?,?)");
				
				for(List<Object> l : RstWritingRaw)
				{
					pstmt.setString(1, l.get(0).toString());
					pstmt.setString(2, l.get(1).toString());
					pstmt.setString(3, l.get(2).toString());
					pstmt.setString(4, l.get(3).toString());
					pstmt.setString(5, l.get(4).toString());
					pstmt.addBatch();
				}
				pstmt.executeBatch();
				pstmt.close();
	
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally
			{
				try {
					if(pstmt != null)
						pstmt.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	protected static int queryNextBatchno()
	{
		/*
		 * sql.append("select first ").append(allocateCount).append(" ");
		sql.append(seqName).append(".nextval seq from ").append(SEQUENCE_GENERATION_TABLE);
		private static String SEQUENCE_GENERATION_TABLE = "sysmaster:systables";
		SELECT first 1 transcolrec_seq.NEXTVAL FROM systables
		 */
		
		Integer nxt = DSManager.getInstance().getobject(dburl, "select first 1 reg_cmpdata_summary_seq.NEXTVAL as nxt from blmqcdb:systables", Integer.class,
					new InputPstmt(){
					
					@Override
					public void PstmtMapping(PreparedStatement pstmt) throws SQLException {
					}
		
					@Override
					public <T> T DB2ObjectMapping(ResultSet rs) throws SQLException {
						return (T)Integer.valueOf(rs.getInt("nxt"));
					}
					
				});
		if(nxt == null)
			return 1;
		return nxt.intValue();
	}
	
	protected static int queryNextMeta()
	{
		Integer nxt = DSManager.getInstance().getobject(dburl, "select first 1 reg_cmpdata_meta_seq.NEXTVAL as nxt from blmqcdb:systables", Integer.class,
					new InputPstmt(){
					
					@Override
					public void PstmtMapping(PreparedStatement pstmt) throws SQLException {
					}
		
					@Override
					public <T> T DB2ObjectMapping(ResultSet rs) throws SQLException {
						return (T)Integer.valueOf(rs.getInt("nxt"));
					}
					
				});
		
		if(nxt == null)
			return 1;
		return nxt.intValue();
	}
	
	protected static int queryNextRowno()
	{
		Integer nxt = null;
		boolean getvalue=false;
		while(!getvalue)
		{
			try {
				nxt = DSManager.getInstance().getobject(dburl, "select first 1 reg_cmpdata_rst_seq.NEXTVAL as nxt from blmqcdb:systables", Integer.class,
						new InputPstmt(){
						
						@Override
						public void PstmtMapping(PreparedStatement pstmt) throws SQLException {
						}

						@Override
						public <T> T DB2ObjectMapping(ResultSet rs) throws SQLException {
							return (T)Integer.valueOf(rs.getInt("nxt"));
						}
						
					});
				getvalue=true;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println(e);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
		if(nxt == null)
			return 1;
		return nxt.intValue();
	}
	
	protected static int queryNextRawno()
	{
		Integer nxt = null;
		boolean getvalue=false;
		while(!getvalue)
		{
			try {
				nxt = DSManager.getInstance().getobject(dburl, "select first 1 reg_cmpdata_raw_seq.NEXTVAL as nxt from blmqcdb:systables", Integer.class,
						new InputPstmt(){
						
						@Override
						public void PstmtMapping(PreparedStatement pstmt) throws SQLException {
						}

						@Override
						public <T> T DB2ObjectMapping(ResultSet rs) throws SQLException {
							return (T)Integer.valueOf(rs.getInt("nxt"));
						}
						
					});
				getvalue=true;	
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.out.println(e);
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			
		}
		if(nxt == null)
			return 1;
		return nxt.intValue();
	}
	
	protected static int queryNextPartSummanryId()
	{
		Integer nxt = null;
		boolean getvalue=false;
		int retry =0;
		while(!getvalue && retry <2)
		{
			try {
				nxt = DSManager.getInstance().getobject(dburl, "select first 1 reg_cmpdata_part_summary_seq.NEXTVAL as nxt from blmqcdb:systables", Integer.class,
							new InputPstmt(){
							
							@Override
							public void PstmtMapping(PreparedStatement pstmt) throws SQLException {
							}
				
							@Override
							public <T> T DB2ObjectMapping(ResultSet rs) throws SQLException {
								return (T)Integer.valueOf(rs.getInt("nxt"));
							}
							
						});
				getvalue=true;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				int sleeptime=100;
				if(e instanceof DBManagerRuntimeException)
				{
					DBManagerRuntimeException runtimeexception = (DBManagerRuntimeException)e;
					if(runtimeexception.desc.contains("cmpdata002"))
					{//Â_½u
						sleeptime=10000;
						retry++;
					}
				}
				try {
					Thread.sleep(sleeptime);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					
				}
			}
		}
		if(nxt == null)
			return 1;
		return nxt.intValue();
	}
	
	public static int writeWrongData(List<List<Object>> RstWritingRst,List<List<Object>> RstWritingRaw, RstWriting rst, int wrongrowno)
	{
		if(enable_write_reg_cmpdata)
		{
			int rowno;
			if(wrongrowno == -1)
			{
				rowno = queryNextRowno();
				writeRst(RstWritingRst, rst.batchno, new Integer(rowno).toString(), rst.format.metaid, rst.casetype);
			}
			else
				rowno=wrongrowno;
			rst.format.writeRaw(RstWritingRaw, rowno, rst);
			return rowno;
		}
		return -1;
	}
	
	public static void writePartSummary(final RstFormat fmt, final String leftsql, final String rightsql, final Cmpsummary summary)
	{
		if(enable_write_reg_cmpdata)
		{
			CmpTask task = GlobalVariable.threadtaskmap.get(Long.toString(Thread.currentThread().getId(), 10));
			List<List<Object>> RstWritingPartSummary = task.RstWritingPartSummary;
			if(RstWritingPartSummary.size()>10)
			{
				batchwritePartSummary();
				RstWritingPartSummary.clear();
			}
			int id = queryNextPartSummanryId();
			List<Object> l = new ArrayList<Object>();
			int leftsqllast255=leftsql.length()-255;
			if(leftsqllast255<0)
				leftsqllast255=0;
			int rightsqllast255=rightsql.length()-255;
			if(rightsqllast255<0)
				rightsqllast255=0;
			l.add(id);l.add(fmt.batchno);l.add(fmt.metaid);l.add(leftsql.substring(leftsqllast255));l.add(rightsql.substring(rightsqllast255));
			l.add(new Long(summary.leftcount));l.add(new Long(summary.rightcount));l.add(new Long(summary.case1bcount));l.add(new Long(summary.case2count));l.add(new Long(summary.case3count));l.add(new Long(summary.case4count));
			l.add(summary.begintime);l.add(new java.util.Date());
			RstWritingPartSummary.add(l);
		}
	}
	
	public static void batchwritePartSummary()
	{	
		if(enable_write_reg_cmpdata)
		{
			Connection conn = (Connection)DSManager.getInstance().getConn(dburl);
			CmpTask task = GlobalVariable.threadtaskmap.get(Long.toString(Thread.currentThread().getId(), 10));
			PreparedStatement pstmt=null;
			while(true)
			{
				String execsql = null;
				try {
					execsql = "insert into "+partsummary_table_name+" (id,batchno, meta, leftsql, rightsql, leftsqlcount, rightsqlcount, case1bcount, case2count, case3count, case4count,begintime,endtime) values(?,?,?,?,?,?,?,?,?,?,?,?,?)";
					pstmt = conn.prepareStatement(execsql);
					
					for(List<Object> l : task.RstWritingPartSummary)
					{
						Integer tid = (Integer)l.get(0);
						pstmt.setInt(1, tid.intValue());
						pstmt.setString(2, l.get(1).toString());
						pstmt.setString(3, l.get(2).toString());
						pstmt.setString(4, l.get(3).toString());
						pstmt.setString(5, l.get(4).toString());
						pstmt.setLong(6, ((Long)l.get(5)).longValue());
						pstmt.setLong(7, ((Long)l.get(6)).longValue());
						pstmt.setLong(8, ((Long)l.get(7)).longValue());
						pstmt.setLong(9, ((Long)l.get(8)).longValue());
						pstmt.setLong(10,((Long)l.get(9)).longValue());
						pstmt.setLong(11, ((Long)l.get(10)).longValue());
						pstmt.setDate(12, (new java.sql.Date(((java.util.Date)l.get(11)).getTime())));
						pstmt.setDate(13, (new java.sql.Date(((java.util.Date)l.get(12)).getTime())));
						pstmt.addBatch();
					}
					pstmt.executeBatch();
					pstmt.close();
					break;
		
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					RetryDBConnection retrydbconn = new RetryDBConnection(conn, null, null);
					retrydbconn = retrydbconn.Retry(dburl, null);
					conn = retrydbconn.conn;
					try {
						if(conn.isClosed())
							break;
					} catch (SQLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}
	}
	
	public static void writeSummary(final RstFormat fmt, final Cmpsource left, final Cmpsource right, final Cmpsummary summary)
	{
		if(enable_write_reg_cmpdata)
		{
			DSManager.getInstance().execute(dburl, "insert into "+summary_table_name+" (batchno, meta, leftdbconn, rightdbconn, lefttablename, righttablename, leftsqlcount, rightsqlcount, case1bcount, case2count, case3count, case4count, begintime, endtime,note) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
					new InputPstmt(){
				
				@Override
				public void PstmtMapping(PreparedStatement pstmt) throws SQLException {
					pstmt.setString(1, fmt.batchno);
					pstmt.setString(2, fmt.metaid);
					pstmt.setString(3, left.dbconn);
					pstmt.setString(4, right.dbconn);
					pstmt.setString(5, left.getTable());
					pstmt.setString(6, right.getTable());
					pstmt.setLong(7, summary.leftcount);
					pstmt.setLong(8, summary.rightcount);
					pstmt.setLong(9, summary.case1bcount);
					pstmt.setLong(10, summary.case2count);
					pstmt.setLong(11, summary.case3count);
					pstmt.setLong(12, summary.case4count);
					pstmt.setDate(13, new java.sql.Date(summary.begintime.getTime()));
					pstmt.setDate(14, new java.sql.Date(summary.endtime.getTime()));
					if(summary.case2rechckscriptcount>0)
					{
						pstmt.setString(15, "case2 recheck script ok:"+summary.case2rechckscriptcount);
					}
					else
						pstmt.setString(15, "");
				}
	
				@Override
				public <T> T DB2ObjectMapping(ResultSet rs) throws SQLException {
					return null;
				}
				
			});
		}
	}
	
	public static String writeSummary_sql(final RstFormat fmt, final Cmpsource left, final Cmpsource right, final Cmpsummary summary)
	{
		String ret = null;
		if(enable_write_reg_cmpdata)
		{
			SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			ret = "insert into blmqcdb:"+summary_table_name+" (batchno, meta, leftdbconn, rightdbconn, lefttablename, righttablename, leftsqlcount, rightsqlcount, case1bcount, case2count, case3count, case4count, begintime, endtime,note) values('%s','%s',?,?,'%s','%s','%s','%s','%s','%s','%s','%s',?,?,'')";
			ret = ret.format(ret, fmt.batchno, fmt.metaid, left.getTable(), right.getTable(), 
					summary.leftcount, summary.rightcount, summary.case1bcount, summary.case2count, summary.case3count, summary.case4count);
		}
		return ret;
	}
}
