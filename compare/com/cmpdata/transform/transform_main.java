package com.cmpdata.transform;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Element;

import com.cmpdata.controller.GlobalVariable;
import com.cmpdata.datatype.DatatypeDefinition;
import com.cmpdata.range.Range_input;
import com.cmpdata.rst.Cmpsummary;
import com.cmpdata.rst.RstFormat;
import com.cmpdata.rst.RstWriting;
import com.cmpdata.scripting.CheckInterfaceScript;
import com.cmpdata.scripting.CheckScript;
import com.security.PathManipulation;
import com.sqlparse.Builder;
import com.sqlparse.InformixBuilder;
import com.sqlparse.Parser;
import com.sqlparse.SQLExpression;
import com.sqlparse.SQLStatement;
import com.sqlparse.SqlObject;
import com.steve.db.data.manager.DBManagerRuntimeException;
import com.steve.db.data.manager.DSManager;

import com.cmpdata.*;
import com.cmpdata.logic.*;


public class transform_main implements Runnable{
	public transform_main(){runid=-1;}
	public transform_main(int id)
	{ 
		runid = id;
	}
	int runid;
	public void run() {
		int parcnt=GlobalVariable.getCmpunitcnt();//basic duplicate parcnt 10  thread 16 ~ 24    
		Connection db_conninfo = null;
//left連線錯誤時，把當下出問題的part重做
//right連線出錯時，當下出問題的part捨棄(or 呼叫left重作)，且須重建temp table
		try {
				if(cmptype==1)
				{
					cmptasks[runid] = new CmpTask(GlobalVariable.getLeftCmpsource().pk, GlobalVariable.getLeftCmpsource().dbconn);
				}
				
				threadtaskmap.put(Long.toString(Thread.currentThread().getId(), 10), cmptasks[runid]);
				threadrunidmap.put(Long.toString(Thread.currentThread().getId(), 10), runid);
				boolean redo=false;
				Set<TreeMap<String,Object>> ret=null;
				while(cmptasks[runid].getState()==0)
				{
					if(cmptype==1)
					{
						if(db_conninfo==null || db_conninfo.isClosed())
						{
							try {
								db_conninfo= (Connection) DSManager.getInstance().getConn(GlobalVariable.getRightCmpsource().dbconn);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								ScriptAPI.errpk_println(e.toString());
								e.printStackTrace();
								
								RetryDBConnection retrydbconn = new RetryDBConnection(db_conninfo, null, null);
								retrydbconn = retrydbconn.Retry(GlobalVariable.getRightCmpsource().dbconn, null);
								db_conninfo = retrydbconn.conn;
								
							}
						}
					}
					if(!redo)
					{
						ret = GetResultSet_(cmptasks[runid], parcnt);//todo 不分段
					}
					redo = false;//System.out.println("time2:"+new Date().getTime());System.out.flush();
					if(ret==null)
					{
						break;
					}
					if(ret.size()==0)
					{
						continue;
					}
					if(cmptype == 1)
					{
						try {
							GlobalVariable.getTController().transform(cmptasks[runid], db_conninfo,ret);
							ret=null;
						} catch (Exception e) {
							e.printStackTrace();
							redo=true;
						}
					} 
				}
			//db_conninfo.close();
		}
		catch(genericCmpDataException e)
		{
			e.printStackTrace();
			transform_main.ForceExit();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			transform_main.ForceExit();
		}
		finally
		{
			RstWriting.batchwritePartSummary();
			CmpTask task = transform_main.threadtaskmap.get(Long.toString(Thread.currentThread().getId(), 10));
			RstWriting.batchwriteRst(task.RstWritingRst);
			RstWriting.batchwriteRaw(task.RstWritingRaw);
			System.out.println("task"+runid+" exit:"+GlobalVariable.getrrsc()+" "+new Date());
			System.out.flush();
			transform_main.errpk_summary.flush();
			transform_main.errpk.flush();
		}
    }
	
	public static void ForceExit()
	{
		for(int i=0;i<threadcnt;i++)
		{
			cmptasks[i].setState(-1);
		}
	}
	
	public static void SummarizedResult()
	{
		for(int i=0;i<threadcnt;i++)
		{
			AllSummary.leftcount += cmptasks[i].getallsummary().leftcount;
			AllSummary.rightcount += cmptasks[i].getallsummary().rightcount;
			AllSummary.case1bcount += cmptasks[i].getallsummary().case1bcount;
			AllSummary.case2count += cmptasks[i].getallsummary().case2count;
			AllSummary.case3count += cmptasks[i].getallsummary().case3count;
			AllSummary.case4count += cmptasks[i].getallsummary().case4count;
			AllSummary.case2rechckscriptcount +=cmptasks[i].getallsummary().case2rechckscriptcount;
		}
	}
	static void plus_rrsc()
	{
		GlobalVariable.plusrrsc(1);
	}
	static void plus_rrsc(int plus)
	{
		GlobalVariable.plusrrsc(plus);
	}
	static int getrrsc()
	{
		return GlobalVariable.getrrsc();
	}
	public static boolean LeftGetter_checkBalance(CmpTask task)
	{
		if(task.countrightmode || transform_main.strategy==3)
			return true;
		if((task.getsummary().leftcount - task.getsummary().rightcount+task.getsummary().case3count-task.getsummary().case2count-task.getsummary().case4count)!=0)
		{
			return false;
		}
		return true;
	}
	private static Comparator<TreeMap<String,Object>> LeftGetter_GetLeftSetComparator()
	{
		return new Comparator<TreeMap<String,Object>>(){
			  @Override
			  public int compare(TreeMap<String,Object> o1, TreeMap<String,Object> o2) {
				  for(int i=0;i<GlobalVariable.getLeftCmpsource().pk.size(); i++)
				  {
					  String pkname = genericCmpDataUtil.getRealColumn(GlobalVariable.getLeftCmpsource().pk.get(i));
					  
					  ColumnMeta cm = genericCmpDataUtil.findCmpPair(genericCmpDataConstant.CmpLeftElm, pkname);
					  if(cm == null)
						  throw new genericCmpDataException("cmpdata002","findCmpPair pkname null:"+pkname);

					  String realcolumn = genericCmpDataUtil.getRealColumn(cm.getName());
					  int cmprst = cm.compare(TreeMapUtil.getString(o1.get(realcolumn)), TreeMapUtil.getString(o2.get(realcolumn)));
					  
					  if(cmprst!=0)
						  return cmprst;
				  }
				  
				  if(o1!=o2)
					  ScriptAPI.errpk_println("pk get duplicate data:"+o1.toString());
				  return 0;
			  }   
		  };
	}
	private static void LeftGetter_SummaryPartResult(CmpTask task)
	{
		if(task.nowsql[1]!=null)
		{
			if(task.countrightmode)
				ScriptAPI.summary_println(task.nowsql[0]+"|"+task.getsummary().leftcount+"|"+task.getsummary().case1bcount+"|"+task.getsummary().case2count+"|"+task.getsummary().case3count+"|"+task.getsummary().case4count+"|"+task.getsummary().rightcount+"|"+task.nowsql[1]+"|recheck");
			else
				ScriptAPI.summary_println(task.nowsql[0]+"|"+task.getsummary().leftcount+"|"+task.getsummary().case1bcount+"|"+task.getsummary().case2count+"|"+task.getsummary().case3count+"|"+task.getsummary().case4count+"|"+task.getsummary().rightcount+"|"+task.nowsql[1]);
			RstWriting.writePartSummary(format, task.nowsql[0], task.nowsql[1], task.getsummary());
		}
		else
		{
			if(task.countrightmode)
				ScriptAPI.summary_println(task.nowsql[0]+"|"+task.getsummary().leftcount+"|"+task.getsummary().case1bcount+"|"+task.getsummary().case2count+"|"+task.getsummary().case3count+"|"+task.getsummary().case4count+"|"+task.getsummary().rightcount+"|recheck");
			else
				ScriptAPI.summary_println(task.nowsql[0]+"|"+task.getsummary().leftcount+"|"+task.getsummary().case1bcount+"|"+task.getsummary().case2count+"|"+task.getsummary().case3count+"|"+task.getsummary().case4count+"|"+task.getsummary().rightcount);
			RstWriting.writePartSummary(format, task.nowsql[0], "", task.getsummary());
		}
		if(task.countrightmode)
			task.countrightmode=false;
	}
	
	private static void LeftGetter_GetNowSql(CmpTask task)
	{
		task.nowsql = getNextSqlScript();
		task.nowsql[0]=applySessionVariable(task.nowsql[0]);
		
		if(task.nowsql[1]!=null)
		{
			task.nowsql[1]=applySessionVariable(task.nowsql[1]);
			if(task.nowsql[1] == null)
				task.nowsql[1]=null;
		}
	}
	private static void LeftGetter_OpenLeftrs(CmpTask task) throws SQLException
	{
		String filters = PathManipulation.filterSQL(task.nowsql[0]);
		task.pstmt= ((Connection)task.conn).prepareStatement(filters);
		task.pstmt.setQueryTimeout(1200);
		task.rs = task.pstmt.executeQuery();
		task.rs.setFetchSize(GlobalVariable.getFetchsize());
		
		if(dbgmode==1)
		{
			System.out.println("GetResultSet executeQuery:"+filters);
		}
	}
	
	private static void LeftGetter_CloseLeftrs(CmpTask task) throws SQLException
	{
		task.rs.close();
		task.pstmt.close();
	}
	
	private static void LeftGetter_RetryLeftrs(CmpTask task)
	{
		RetryDBConnection retrydbconn = new RetryDBConnection((Connection)task.conn, task.pstmt, task.rs);
		retrydbconn = retrydbconn.Retry(GlobalVariable.getLeftCmpsource().dbconn, task.nowsql[0]);
		task.conn = retrydbconn.conn;task.pstmt = retrydbconn.pstmt;task.rs=retrydbconn.rs;
	}
	
	private static int LeftGetter_GetLeftrsdata(CmpTask task, Set<TreeMap<String, Object>> ret, int cnt) throws SQLException
	{
		TreeMap<String,Object> r = new TreeMap<String,Object>(String.CASE_INSENSITIVE_ORDER);
		for(int i=0; i<ColumnMeta.getLeftColumnSize(); i++)
		{
			if(ColumnMeta.getLeftColumn(i).getName().startsWith("#Constant"))
				continue;
			
			ColumnMeta cmp = genericCmpDataUtil.findCmpPair(genericCmpDataConstant.CmpLeftElm, ColumnMeta.getLeftColumn(i).getName());
			  if(cmp==null)
			  {
				  continue;
				  //System.out.println("getleft null:"+ColumnMeta.getLeftColumn(i).getName());
				  //System.out.println("leftcol:"+CmpPair.getlcmppair());
			  }
			  try {
				  int columnidx = task.rs.findColumn(ColumnMeta.getLeftColumn(i).getName());
				  List<String> props =cmp.getProps();
				  
				  String o1 = GlobalVariable.getLeftCmpsource().getResultSetValue(task.rs, columnidx);
				  byte []o2 = GlobalVariable.getLeftCmpsource().getResultSetByte_ignoreException(task.rs, columnidx);
				  //Object value = ExecOnPropMethod_AND.exec(ExecOnPropImpl_GetLeftrsdata.impls, props, new Object[]{o1, o2});
				  //r.put(ColumnMeta.getLeftColumn(i).getName(), value);

				  if(props==null)
				  {
					  //r.put(right.cols.get(i).toString(),JSONHandler.getRTrimColumnValueStringNull(task.rs.getString(right.cols.get(i).toString())));
					  r.put(ColumnMeta.getLeftColumn(i).getName(),JSONHandler.getRTrimColumnValueStringNull(GlobalVariable.getLeftCmpsource().getResultSetValue(task.rs, columnidx)));
				  }
				  else if(props.contains("multibytes"))
				  {
					  r.put(ColumnMeta.getLeftColumn(i).getName(), task.rs.getBytes(ColumnMeta.getLeftColumn(i).getName()));
				  }
				  else if(props.contains("untrim"))
				  {
					  //r.put(right.cols.get(i).toString(),JSONHandler.getColumnValueStringNull(task.rs.getString(right.cols.get(i).toString())));
					  r.put(ColumnMeta.getLeftColumn(i).getName(),JSONHandler.getColumnValueStringNull(GlobalVariable.getLeftCmpsource().getResultSetValue(task.rs, columnidx)));
				  }
				  else if(props.contains("chinese"))
				  {
					  r.put(ColumnMeta.getLeftColumn(i).getName(), task.rs.getBytes(ColumnMeta.getLeftColumn(i).getName()));
				  }
				  else
				  {
					  //r.put(right.cols.get(i).toString(),JSONHandler.getRTrimColumnValueStringNull(task.rs.getString(right.cols.get(i).toString())));
					  r.put(ColumnMeta.getLeftColumn(i).getName(),JSONHandler.getRTrimColumnValueStringNull(GlobalVariable.getLeftCmpsource().getResultSetValue(task.rs, columnidx)));
				  }
				  
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				throw new genericCmpDataException("cmpdata002", "rrs.getString column:"+ColumnMeta.getLeftColumn(i).getName()+" is not defined in the db");
			}
		}
		if(dbgmode==1)
		{
			System.out.println("GetResultSet before add r:"+ret.size());
		}
		ret.add(r);
		if(dbgmode==1)
		{
			System.out.println("GetResultSet after add r:"+ret.size());
			System.out.println("GetResultSet:"+task.nowsql[0]);
			System.out.println("GetResultSet rs:"+r);
		}
		
		cnt++;
		if(GlobalVariable.getLeftCmpsource().sqlcountcolumn!=null)
		{
			int rscnt = task.rs.getInt(GlobalVariable.getLeftCmpsource().sqlcountcolumn);
			task.nowsqlcnt_plus(rscnt);
		}
		else
			task.nowsqlcnt_plus();
		
		return cnt;
	}
	
	private static boolean LeftGetter_SingleSQLSummaryResultAndGetNowSQL(CmpTask task)
	{
		if(task.nowsql!=null && task.nowsql[0]!=null && task.nowsql[0].equals(GlobalVariable.getLeftCmpsource().getMainsqlsb().toString()))
		{//已經比完了
			//task.checkRightcnttable(GlobalVariable.getLeftCmpsource().pk,GlobalVariable.getRightCmpsource().pk);
			if(!GlobalVariable.getLeftCmpsource().getMainsqlsb().toString().startsWith("file://"))
			{
				ScriptAPI.summary_println(task.nowsql[0]+"|"+task.getsummary().leftcount+"|"+task.getsummary().case1bcount+"|"+task.getsummary().case2count+"|"+task.getsummary().case3count+"|"+task.getsummary().case4count+"|"+task.getsummary().rightcount);
				RstWriting.writePartSummary(format, task.nowsql[0], "", task.getsummary());
			}
			return false;
		}
		task.nowsql=new String[2];
		task.nowsql[0] = GlobalVariable.getLeftCmpsource().getMainsqlsb().toString();
		return true;
	}
	
	public static Set<TreeMap<String,Object>> GetResultSet_(CmpTask task, int lmt)
	{
		int cnt=0;
		Set<TreeMap<String, Object>> ret = new TreeSet<TreeMap<String, Object>>(LeftGetter_GetLeftSetComparator());
		try {
			if(task.rs==null || task.rs.isClosed())
			{
				if(GlobalVariable.getLeftCmpsource().getMainsqlsb().toString().startsWith("file://"))
				{
					/*
					if(task.nowsql!=null && task.nowsql[0] != null)
					{
						LeftGetter_SummaryPartResult(task);
					}
					*/
					LeftGetter_GetNowSql(task);
				}
				else
				{
					if(!LeftGetter_SingleSQLSummaryResultAndGetNowSQL(task))
						return null;
				}
				if(task.nowsql[0] == null)
					return null;
				
				//task.insertRightcnttable();
				task.setZeroNowSql();

				LeftGetter_OpenLeftrs(task);
			}

			while(cnt<lmt && task.rs.next())
			{
				cnt = LeftGetter_GetLeftrsdata(task, ret, cnt);
			}
			if(cnt<lmt)
			{
				//task.nowsql比完
				LeftGetter_CloseLeftrs(task);
				
			}
		} catch (SQLException e) {
			System.out.println("retry connection:"+GlobalVariable.getLeftCmpsource().dbconn+" sql:"+task.nowsql[0]);
			ScriptAPI.errpk_println(e.toString());
			e.printStackTrace();
			//int repeat=0;
			cnt=0;//drop this redo
			task.rollbackAllSummary();
			task.setZeroNowSql();
			//task.setZeroRightcnttable();
			
			LeftGetter_RetryLeftrs(task);
			ret.clear();
		}
		plus_rrsc(cnt);
		return ret;
	}

	//static int fetchsize=500;
	//static int cmpunitcnt=20;
	//public static ResultSet rrs;
	//public static PreparedStatement pstmtrrs;
	//public static int rrsc=0;

	
	//public static Cmpsource left = null;
	//public static Cmpsource right = null;

	
	//public static List<CmpPair> lcmppair = new ArrayList<CmpPair>();
	
	public static StringBuffer rightcountref = new StringBuffer();
	public static StringBuffer rightfirstsql = new StringBuffer();
	public static String rightcountrefidx;
	public static boolean customrightcountref=false;

	public static int strategy=1;//1 for basic(10萬 15.99s)   2 for duplicate(10萬16.45s)   3 for countright(10萬 35.73s)

	public static PrintWriter errpk;
	public static PrintWriter errpk_summary;
	public static Cmpsummary AllSummary = new Cmpsummary();

	public static int threadcnt=8;
	public static boolean isExactEqual=true;//true for StringEuqal   false for StringEuqalNotExact
	public static boolean isForceExit=true;//true for 錯太多中斷   false for 跑到比完

	static CmpTask [] cmptasks;
	public static HashMap<String,CmpTask> threadtaskmap = new HashMap<String,CmpTask>();
	public static HashMap<String,Integer> threadrunidmap = new HashMap<String,Integer>();
	public static int init=0;
	
	public static BufferedReader sqlscriptfile;
	public static BufferedReader sqlscriptfilerightcnt;
	
	public static int cmptype=1;
	//1 is default db vs. db   
	//2. db value verification  db(with script) 設定檔沒有 rightdb, rightpk, rightcol
	//3. db vs. file leftdb以 "file://"起頭 
	
	public static RstFormat format;
	
	
	public static SQLStatement []firstNowStmt = new SQLStatement[2];
	//case2 case3 refs 不用加left right，ref一律加在idx=0的地方
	//public static Invocable case2checkscript;
	//case2錯誤的加驗條件, 當發現case2問題，再加驗此script確認是否正確，
	//若為正確，計數仍然會將此筆計入(避開checkbalance的問題)，但會再文字log註記無問題，並且不將此錯誤detail寫入log
	private static CheckScript case2checkScript;
	//public static List<TreeMap<String, Object>> case2checkscriptrefs;
	private static boolean case2lazyInit=false;

	//public static Invocable case3checkscript;
	//case3錯誤的加驗條件, 當發現case3問題，再加驗此script確認是否正確，
	protected static CheckScript case3checkScript;
	//public static List<TreeMap<String, Object>> case3checkscriptrefs;//只有reference right pk
	protected static boolean case3lazyInit=false;
	
	public static int dbgmode=0;//0 no dbg 1 dbg
	
	//public static String customloginfo;//log錯誤訊息的前置資訊  left.equipoffcode, left.equipnumber, right.equipoffcode, right.equipnumber,
	public static String logformat;//${left.offcode}|${left.equipnum}|${right.equipoffcode}|${right.equipnumber}|#caseconstant('m3', 'm1', 'm2', 'm4')|${tablename}|#columnname(1)|${leftvalue}| ${rightvalue}| #pk(',')
	public static String logformatsplitter=",";
	public static int resultsetsize=0;
	/*
	public static class cmpdataexception extends RuntimeException
	{
		//cmpdata001 db type not match
		//cmpdata002 db column not defined
		//cmpdata003 leftcol no definition
		//cmpdata004 xml definition wrong
		//cmpdata005 data format wrong
		//cmpdata006 column definition error
		//cmpdata007 TreeMapUtil error
		//cmpdata008 right sql error
		//cmpdata009 設定檔定義的pk在resultsetmeta找不到
		//cmpdata010 SQLException 連線相關
		//cmpdata011 ScriptAPI class未初始化
		//cmpdata012 cmptype error only supports t1t3/nameaddr
		String desc;
		public cmpdataexception(String _msg, String _desc)
		{
			StringBuffer sb = new StringBuffer();
			sb.append("id:").append(_msg).append("/").append(_desc);
		    System.out.println(sb);
		    desc = sb.toString();
		}
	}
	*/
	
	
	public static int getColumnType(ResultSetMetaData meta, String name)
	{
		try {
			String realcolumn = genericCmpDataUtil.getRealColumn(name);

			for(int i=1;i<=meta.getColumnCount(); i++)
			{
				if(meta.getColumnName(i).equalsIgnoreCase(realcolumn))
				{
					return meta.getColumnType(i);
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		throw new genericCmpDataException("cmpdata001","column name:"+name+" can't be found in ResultSetMetaData");
	}
	/*
	public static void init_lcmpk(ResultSetMetaData meta)
	{
		for(int i=0;i<lpk.size(); i++)
		{
			CmpPair.ColumnMeta cm = new CmpPair.ColumnMeta(lpk.get(i));
			cm.setType(getColumnType(meta, lpk.get(i)));
			lcmpk.add(cm);
		}
	}*/
	
	public static String getelmvalue(Element rootelm, String elmname)
	{
		Element elm = rootelm.getChild(elmname);
		if(elm == null)
			return null;
        String s=elm.getValue().trim();
        return s;
	}
	
	public static Element getelm(Element rootelm, String elmname)
	{
		Element elm = rootelm.getChild(elmname);
		if(elm == null)
			return null;

        return elm;
	}
	
	public static TreeMap<String, CheckInterfaceScript<VarScript>> getVarScripts(Element rootelm)
	{
		TreeMap<String, CheckInterfaceScript<VarScript>> varscripts = new TreeMap<String, CheckInterfaceScript<VarScript>>(String.CASE_INSENSITIVE_ORDER);
		List<Element> lvs = rootelm.getChildren("varscript");
		if(lvs.size() == 0)
			return null;
		
		for(Element vs : lvs)
		{
			CheckInterfaceScript<VarScript> vsimpl = CheckScript.CheckInterfaceScriptFactory("varscript", getelmvalue(vs,"script"), null, VarScript.class, null, transform_main.errpk, transform_main.threadcnt);
			//VarScript vsimpl = ScriptAPI.EvalVarScript(getelmvalue(vs,"script"));
			varscripts.put(getelmvalue(vs,"name"), vsimpl);
		}
		return varscripts;
		
	}
	
	public static void setCmpPair(ResultSetMetaData leftrsmeta, ResultSetMetaData rightrsmeta) throws SQLException
	{
		if(leftrsmeta != null)
		{
			for(int i=1;i<=leftrsmeta.getColumnCount(); i++)
			{
				ColumnMeta cmp = genericCmpDataUtil.findCmpPair(genericCmpDataConstant.CmpLeftElm, leftrsmeta.getColumnName(i));
				if(cmp == null)
				{//該欄位不需比對
					continue;
				}
				try {
					DatatypeDefinition td = DatatypeDefinition.factory(leftrsmeta.getColumnType(i), leftrsmeta.getColumnTypeName(i), leftrsmeta.getPrecision(i), leftrsmeta.getScale(i));
					cmp.setType(td);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		if(rightrsmeta != null)
		{
			for(int i=1;i<=rightrsmeta.getColumnCount(); i++)
			{
				ColumnMeta cmp = genericCmpDataUtil.findCmpPair(genericCmpDataConstant.CmpRightElm, rightrsmeta.getColumnName(i));
				if(cmp == null)
				{//該欄位不需比對
					continue;
				}
				DatatypeDefinition td = DatatypeDefinition.factory(rightrsmeta.getColumnType(i), rightrsmeta.getColumnTypeName(i), rightrsmeta.getPrecision(i), rightrsmeta.getScale(i));
				cmp.setType(td);
			}
		}
	}
	
	public static void initCmpPair(String leftcol, String rightcol)
	{
		String []ssl = leftcol.split(",");
		for(int i=0;i<ssl.length;i++)
        {
        	ColumnMeta oldcol = new ColumnMeta(ssl[i]);ColumnMeta.addLeftCol(oldcol);
        }
		ssl = rightcol.split(",");
		for(int i=0;i<ssl.length;i++)
        {
        	ColumnMeta oldcol = new ColumnMeta(ssl[i]);ColumnMeta.addRightCol(oldcol);
        }
	}
	
	public static Object [] getDefaultLogicVars(String s)
	{
		if(s == null)
			return null;
		
		return s.split(",");
	}
	public static void getCase2check(Element rootelm, String elmname) throws Exception
	{
		Element elmcolext = rootelm.getChild("case2check");
		if(elmcolext == null)
		{
			return;
		}
		String script = getelmvalue(elmcolext, "script");
		case2checkScript = CheckScript.CheckScriptFactory("case2", script, null, getRefs(elmcolext, "ref"), errpk, threadcnt);
		
		String lazyInit = getelmvalue(elmcolext, "lazyInit");
		if(lazyInit!=null && lazyInit.equals("YES"))
			case2lazyInit=true;
	}
	public static void getCase3check(Element rootelm, String elmname) throws Exception
	{
		Element elmcolext = rootelm.getChild("case3check");
		if(elmcolext == null)
		{
			return;
		}
		String script = getelmvalue(elmcolext, "script");
		case3checkScript = CheckScript.CheckScriptFactory("case3", script, null, getRefs(elmcolext, "ref"), errpk, threadcnt);
		
		String lazyInit = getelmvalue(elmcolext, "lazyInit");
		if(lazyInit!=null && lazyInit.equals("YES"))
			case3lazyInit=true;
	}
	public static void getColKeyTransform(Element rootelm, String elmname, CmpPair cp) throws Exception
	{
		Element elmcolext = rootelm.getChild(elmname);
		if(elmcolext==null)
			return;
		String script = getelmvalue(elmcolext, "script");
		if(script!=null)
		{
			script = applySessionVariable(script);
			if(dbgmode==1)
				System.out.println(script);
			try {
				cp.setkeytransformScript(script, null, getRefs(elmcolext, "ref"), transform_main.errpk, transform_main.threadcnt);
			} catch (NullPointerException e1) {
				throw new genericCmpDataException("cmpdata003", "col:"+cp.name+" leftcol no definition");
			} catch (Exception e2) {
				e2.printStackTrace();
				throw new genericCmpDataException("cmpdata003", "col:"+cp.name+" KeyTransform script syntax error");
			}
		}
	}
	
	public static void getCols(Element rootelm, String elmname) throws Exception
	{
		Element elmcolext = rootelm.getChild("colext");
		if(elmcolext == null)
		{
			throw new genericCmpDataException("cmpdata004", "colext undefined");
		}
		
		List<Element> elms= elmcolext.getChildren(elmname);
		for(Element e:elms)
		{
			String name = getelmvalue(e, "name");
			if(name == null)
				throw new genericCmpDataException("cmpdata004", "colext/col/name undefined");
			
			String leftname = getelmvalue(e, "left");
			String rightname = getelmvalue(e, "right");

			CmpPair cp = CmpPair.factory(name, leftname, rightname, transform_main.threadcnt);
			
			String rselm = getelmvalue(e, "rightprop");
			String lselm = getelmvalue(e, "leftprop");
			cp.setProps(lselm, rselm, transform_main.threadcnt);//set StringEuqal algorithm
			
			String script = getelmvalue(e, "script");
			if(script!=null)
				try {
					script=applySessionVariable(script);
					cp.setTransValueScript(script, null, getRefs(e, "ref"), transform_main.errpk, transform_main.threadcnt);
				} catch (NullPointerException e1) {
					throw new genericCmpDataException("cmpdata003", "col:"+name+" leftcol no definition");
				} catch (Exception e2) {
					e2.printStackTrace();
					throw new genericCmpDataException("cmpdata003", "col:"+name+" script syntax error");
				}
			
			String defaultlogic = getelmvalue(e, "defaultlogic");
			if(defaultlogic!=null)
			{
				cp.setTransValueScript(TransLogicDefaultImpl.GetDefaultImpl(defaultlogic), 
											 getDefaultLogicVars(getelmvalue(e, "defaultlogicvars")),
											 getRefs(e, "ref"),
											 transform_main.threadcnt);
			}
			
			String enableCheck = getelmvalue(e, "enableCheck");
			if(enableCheck!=null)
			{
				if(enableCheck.equals("NO"))
					cp.enableCmp=false;
			}
			String lazyInit = getelmvalue(e, "lazyInit");
			if(lazyInit!=null)
			{
				if(lazyInit.equals("YES"))
					cp.lazyinitrefs=true;
			}
			
			String customtype = getelmvalue(e, "customtype");
			if(customtype!=null)
			{
				cp.setRightCustomType(customtype);
			}
			
			cp.init=true;
			getColKeyTransform(e, "KeyTransform", cp);
			CmpPair.addCmpPair(cp);
		}
	}
	
	public static List<TreeMap<String, Object>> getRefs(Element rootelm, String elmname)
	{
		Element elmcolext = rootelm.getChild("refs");
		if(elmcolext==null)
			return null;
		
		List<Element> elms= elmcolext.getChildren(elmname);
		List<TreeMap<String, Object>> ret = new ArrayList<TreeMap<String, Object>>();
		for(Element e:elms)
		{
			TreeMap<String, Object> ref= new TreeMap<String, Object>();
			String multi = (e.getAttribute("multi")==null)?null:e.getAttribute("multi").getValue();
			String db = applySessionVariable(getelmvalue(e, "db"));
			if(db!=null)
			{
				String sql = applySessionVariable(getelmvalue(e, "sql"));
				String var = getelmvalue(e, "var");
				String col = getelmvalue(e, "col");
				
				
				ref.put("db", db);
				ref.put("sql",sql);
				ref.put("var",var);
				ref.put("col",col);
				if(multi!=null)
					ref.put("multi",multi);
				TreeMap<String, CheckInterfaceScript<VarScript>> varscripts = getVarScripts(e);
				if(varscripts != null)
					ref.put("varscripts", getVarScripts(e));
			}
			else if((db = getelmvalue(e, "val"))!=null)
			{
				ref.put("val", db);
			}
			ret.add(ref);
		}
		return ret;
	}
	
	public static TreeMap<String, String> getVariable(Element rootelm, String name)
	{
		TreeMap<String, String> ret = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
		Element elmvariables = rootelm.getChild("variables");
		if(elmvariables != null)
		{
			List<Element> elmvariable = elmvariables.getChildren("variable");
			for(Element elmv : elmvariable)
			{
				ret.put(getelmvalue(elmv, "name"), getelmvalue(elmv, "value"));
			}
		}
		return ret;
	}
	
	public static void getLogformat(Element rootelm, String name)
	{
		Element elmlog = rootelm.getChild(name);
		if(elmlog == null)
			return;
		String elmpattern = getelmvalue(elmlog,"pattern");
		if(elmpattern != null)
		{
			logformat= elmpattern;
		}
		
		String elmsplitter = getelmvalue(elmlog,"splitter");
		if(elmsplitter != null)
		{
			logformatsplitter= elmsplitter;
		}
	}
	
	public static int getStartegy(String s)
	{
		if(s.equals("basic"))
			return 1;
		else if(s.equals("duplicate"))
			return 2;
		else if(s.equals("countright"))
			return 3;
		return 1;
	}
	
	public static void initRightCountRef(String rightrangeidx)//rightrangeidx: s.userid
	{
		if(GlobalVariable.getLeftCmpsource().getMainsqlsb().indexOf("file://")!=-1)
		{
			String leftmainsqlfile = GlobalVariable.getLeftCmpsource().getMainsqlsb().toString().substring("file://".length());
			try {
				BufferedReader br = new BufferedReader(new FileReader(leftmainsqlfile));
				BufferedWriter brright = new BufferedWriter(new FileWriter(leftmainsqlfile+"right"));
				
				String s;
				while((s=br.readLine())!=null)
				{
					s=applySessionVariable(s);
					SQLStatement sleftstmt = null;
					if(GlobalVariable.getLeftCmpsource().getDBType().equals(genericCmpDataConstant.CONSTANT_DBVENDOR_ORACLE))
						sleftstmt = (SQLStatement)Parser.parse(s);
					else
						sleftstmt = (SQLStatement)Parser.parseInformixTableSupport(s);
					List<SQLExpression> sleftexpr = Builder.getBinaryWhereExpression(sleftstmt, 2, 2);
					
					//抓最後兩個，並比較最後兩個的left是否一致
					boolean checktwo=false;
					SQLExpression expr1 = null;//sleftexpr.get(1);
					SQLExpression expr2 = null;
					if(sleftexpr.size()>0)
						expr2 = sleftexpr.get(0);
					if(sleftexpr.size()==2)
					{
						expr1 = sleftexpr.get(1);
					
						if(expr1 !=null && expr2 !=null)
						{
							if(expr1.getLeftExpression().equals(expr2.getLeftExpression()))
							{
								checktwo=true;
							}
						}
					}
					
					SQLStatement sstmt = (SQLStatement)Parser.parseInformixTableSupport(GlobalVariable.getRightCmpsource().getMainsqlsb().toString());
					List<String> sc = new ArrayList<String>();
					
					for(String pk : GlobalVariable.getRightCmpsource().pk)
					{
						sc.add(pk);//todo
						Builder.addGroupby(sstmt,pk);
					}
					Builder.removeSelectItem(sstmt, sc);
					Builder.addSelectExpression(sstmt, new SqlObject("Column","0"));
					Builder.addSelectExpression(sstmt, new SqlObject("Column","count(*)"));
					//Builder.buildCount(sstmt, null);
					//Builder.addGroupby(sstmt,"s.servno");//todo
					
					String inputrange = rightrangeidx;
					if(expr2 != null)
					{
						Builder.addWhere(sstmt, Builder.OP_AND,inputrange+expr2.getStringExpression()+expr2.getRightExpression());
						if(checktwo)
						{
							if(expr1!=null)
								Builder.addWhere(sstmt, Builder.OP_AND,inputrange+expr1.getStringExpression()+expr1.getRightExpression());
						}
					}
					if(GlobalVariable.getRightCmpsource().getDBType().equals(genericCmpDataConstant.CONSTANT_DBVENDOR_INFORMIX))
					{
						brright.write(InformixBuilder.fixTableForInformix(sstmt));
					}
					else
						brright.write(sstmt.toString());
					brright.newLine();
				}
				br.close();
				brright.close();

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			
		}
	}
	
	public static void initRightFirstSql()
	{
		SQLStatement sstmt;
		if(GlobalVariable.getRightCmpsource().getDBType().equals(genericCmpDataConstant.CONSTANT_DBVENDOR_INFORMIX))
			sstmt= (SQLStatement)Parser.parseInformixTableSupport(GlobalVariable.getRightCmpsource().getMainsqlsb().toString());
		else
			sstmt= (SQLStatement)Parser.parse(GlobalVariable.getRightCmpsource().getMainsqlsb().toString());
		for(String pk : GlobalVariable.getRightCmpsource().pk)
		{
			String realcolumn=genericCmpDataUtil.getRealColumn(pk);

			ColumnMeta cmp = genericCmpDataUtil.findCmpPair(genericCmpDataConstant.CmpRightElm, realcolumn);
			if(cmp==null)
			{
				throw new genericCmpDataException("cmpdata003", "findCmpPair:"+realcolumn+" undefined");
			}
			//String realpk = Builder.checkalias(sstmt,pk);
			//Builder.addWhere(sstmt, Builder.OP_AND, realpk+"="+cmp.getDefaultWhere());//右資料的datameta尚未實作，因此假設左右兩邊資料形態一樣，抓左邊
		}
		Builder.setFirst(sstmt, 1);
		if(GlobalVariable.getRightCmpsource().getDBType().equals(genericCmpDataConstant.CONSTANT_DBVENDOR_INFORMIX))
			rightfirstsql.append(InformixBuilder.fixTableForInformix(sstmt));
		else
			rightfirstsql.append(sstmt.toString());
	}
	
	public static String applySessionVariable(String input)
	{
		if(input == null)
			return input;
		Pattern pattern = Pattern.compile("\\$\\{(\\w+)\\}");
		Matcher matcher = pattern.matcher(input);
		while (matcher.find())
		{
			String sv = GlobalVariable.getSessionVariable().get(matcher.group(1));
			if(sv != null)
			{
				String matchergr1 = PathManipulation.filterRegex(matcher.group(1));
				input=input.replaceAll("\\$\\{("+matchergr1+")\\}",sv);
			}
		}
		
		return input;
	}
	
	private static void setPKCmpPair(List<String> leftpk, List<String> rightpk)
	{
		if(leftpk.size() != rightpk.size())
			throw new genericCmpDataException("cmpdata006","PK size mismatch");
		int i=0;
		for(String pk : leftpk)
		{
			CmpPair cp = genericCmpDataUtil.findCmpPair(pk);
			if(cp == null)
			{
				ColumnMeta leftColumnMeta = ColumnMeta.getLeftColumn(genericCmpDataUtil.getRealColumn(pk));
				ColumnMeta rightColumnMeta = ColumnMeta.getRightColumn(genericCmpDataUtil.getRealColumn(rightpk.get(i++)));
				
				//leftColumnMeta.setNullcheckProps();
				//rightColumnMeta.setNullcheckProps();
				if(leftColumnMeta == null || rightColumnMeta == null)
					throw new genericCmpDataException("cmpdata004", "xxxcol pk defined error:"+pk);
				
				cp = new CmpPair(leftColumnMeta,rightColumnMeta, transform_main.threadcnt);
				cp.enableCmp=false;
				cp.setName("PK_"+pk);
				CmpPair.addCmpPair(cp);
			}
			else
			{
				ColumnMeta leftColumnMeta = ColumnMeta.getLeftColumn(genericCmpDataUtil.getRealColumn(pk));
				ColumnMeta rightColumnMeta = ColumnMeta.getRightColumn(genericCmpDataUtil.getRealColumn(rightpk.get(i++)));
				if(!rightColumnMeta.getName().equals(cp.getRightcm().getName()))
				{
					cp = new CmpPair(leftColumnMeta,rightColumnMeta, transform_main.threadcnt);
					cp.enableCmp=false;
					cp.setName("PK_"+pk);
					CmpPair.addCmpPair(cp);
				}
			}
		}
	}
	
	public static void readinput(String file) throws IOException
	{
		file = PathManipulation.filterFilePath(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
		StringBuffer str=new StringBuffer();
		String s;
		while((s=br.readLine())!=null)
		{
			str.append(s).append("\n");
		}
		br.close();
	      ByteArrayInputStream in = new ByteArrayInputStream(str.toString().getBytes("UTF-8"));
	      org.jdom2.input.SAXBuilder bldr = new org.jdom2.input.SAXBuilder();

	      
	      try {
	        org.jdom2.Document doc = bldr.build(in);
	        Element root_elm = doc.getRootElement();
	        String selm;
	        
	        selm=getelmvalue(root_elm, "thread");
	        if(selm !=null)
	        	threadcnt = Integer.parseInt(selm);
	        
	        selm=getelmvalue(root_elm, "fetchsize");
	        if(selm !=null)
	        	GlobalVariable.setFetchsize(Integer.parseInt(selm));
	        
	        selm=getelmvalue(root_elm, "cmpunitcnt");
	        if(selm !=null)
	        	GlobalVariable.setCmpunitcnt(Integer.parseInt(selm));
	        
	        selm=getelmvalue(root_elm, "leftcharset");
	        if(selm !=null)
	        	GlobalVariable.setLeftCharset(selm);
	        
	        selm=getelmvalue(root_elm, "rightcharset");
	        if(selm !=null)
	        	GlobalVariable.setRightCharset(selm);
	        
	        selm=getelmvalue(root_elm, "forceexit");
	        if(selm !=null)
	        {
	        	if(selm.equals("NO"))
	        		transform_main.isForceExit=false;
	        }
	        
	        selm=getelmvalue(root_elm, "write_reg_cmpdata");
	        if(selm !=null)
	        {
	        	if(selm.equals("NO"))
	        		RstWriting.enable_write_reg_cmpdata=false;
	        }
	        
	        getLogformat(root_elm, "logformat");
	        
	        Element elm;
	        selm=getelmvalue(root_elm, "leftdbtype");
	        if(selm ==null)
	        {
	        	System.out.println("leftdbtype undefined set leftdbtype=oracle");
	        	GlobalVariable.setLeftCmpsource(new Cmpsource(genericCmpDataConstant.CONSTANT_DBVENDOR_ORACLE));
	        }
	        else
	        {
	        	GlobalVariable.setLeftCmpsource(new Cmpsource(selm));
	        }
	        
	        selm=getelmvalue(root_elm, "rightdbtype");
	        if(selm ==null)
	        {
	        	System.out.println("rightdbtype undefined set rightdbtype=informix");
	        	GlobalVariable.setRightCmpsource(new Cmpsource(genericCmpDataConstant.CONSTANT_DBVENDOR_INFORMIX));
	        }
	        else
	        {
	        	GlobalVariable.setRightCmpsource(new Cmpsource(selm));
	        }
	        
	        
	        elm=getelm(root_elm, "leftsqldef");
	        if(elm !=null)
	        {
	        	GlobalVariable.getLeftCmpsource().range = Range_input.parseXml(elm);
	        }
	        
	        selm=getelmvalue(root_elm, "resultsetsize");
	        if(selm !=null)
	        {
	        	resultsetsize=Integer.parseInt(selm);
	        }
	        
	        GlobalVariable.setSessionVariable(getVariable(root_elm, "variables"));
	        
	        selm=getelmvalue(root_elm, "leftdb");
	        if(selm ==null)
	        {
	        	throw new Exception("leftdb undefined");
	        }
	        GlobalVariable.getLeftCmpsource().dbconn=applySessionVariable(selm);
	        
	        selm=getelmvalue(root_elm, "rightdb");
	        if(selm ==null)
	        {
	        	cmptype=2;
	        }
	        else
	        	GlobalVariable.getRightCmpsource().dbconn=applySessionVariable(selm);
	        
	        selm=getelmvalue(root_elm, "lefttitle");
	        if(selm ==null)
	        {
	        	GlobalVariable.getLeftCmpsource().setTable("");
	        }
	        else
	        	GlobalVariable.getLeftCmpsource().setTable(applySessionVariable(selm));
	        
	        selm=getelmvalue(root_elm, "righttitle");
	        if(selm ==null)
	        {
	        	GlobalVariable.getRightCmpsource().setTable("");
	        }
	        else
	        	GlobalVariable.getRightCmpsource().setTable(applySessionVariable(selm));
	        
	        selm=getelmvalue(root_elm, "leftsqlcountcolumn");
	        if(selm !=null)
	        	GlobalVariable.getLeftCmpsource().sqlcountcolumn=selm;
	        
	        String sleftcolelm=getelmvalue(root_elm, "leftcol");
	        if(sleftcolelm ==null)
	        {
	        	throw new genericCmpDataException("cmpdata004", "leftcol undefined");
	        }
	        String srightcolelm=getelmvalue(root_elm, "rightcol");
	        if(srightcolelm ==null)
	        {
	        	srightcolelm="";
	        	cmptype=2;
	        }
	        initCmpPair(sleftcolelm, srightcolelm);

	        getCols(root_elm, "col");
	        //getColDefs(root_elm, "col");
	        
	        selm=getelmvalue(root_elm, "leftpk");
	        if(selm ==null)
	        {
	        	throw new genericCmpDataException("cmpdata004", "leftpk undefined");
	        }
	        GlobalVariable.getLeftCmpsource().pk=new ArrayList<String>(Arrays.asList(selm.split(",")));
	        
	        selm=getelmvalue(root_elm, "rightpk");
	        if(selm ==null)
	        {
	        	throw new genericCmpDataException("cmpdata002", "rightpk undefined");
	        }
	        else
	        	GlobalVariable.getRightCmpsource().pk=new ArrayList<String>(Arrays.asList(selm.split(",")));
	        
	        //setPKCmpPair(GlobalVariable.getLeftCmpsource().pk, GlobalVariable.getRightCmpsource().pk);
	        
	        selm=getelmvalue(root_elm, "leftsql");
	        if(selm ==null)
	        {
	        	throw new genericCmpDataException("cmpdata004", "leftsql undefined");
	        }
	        GlobalVariable.getLeftCmpsource().getMainsqlsb().append(applySessionVariable(selm));
			
			selm=getelmvalue(root_elm, "rightsql");
	        if(selm ==null)
	        {
	        	cmptype=2;
	        	selm="";
	        }
	        GlobalVariable.getRightCmpsource().getMainsqlsb().append(applySessionVariable(selm));

	      }
	      catch (Exception ex) {
	        DBManagerRuntimeException e = new DBManagerRuntimeException("cmpdata003", ex);
	        throw e;
	      }
	}
	
	public static ResultSet Set_rrs(Connection conn, String sql)
	{
		PreparedStatement pstmtrrs;
		ResultSet ret = null;
		sqltestconn=conn;
		try {
			if(init==0)
			{
				if(GlobalVariable.getLeftCmpsource().range==null)
				{
					if(GlobalVariable.getLeftCmpsource().getMainsqlsb().toString().startsWith("file://"))
					{
						String []firstNowSql = getNextSqlScript();
						firstNowSql[0]=applySessionVariable(firstNowSql[0]);
						firstNowSql[1]=applySessionVariable(firstNowSql[1]);
						transform_main.firstNowStmt[0] = Parser.parseStatement(firstNowSql[0]);
						firstNowSql[0] = PathManipulation.filterSQL(firstNowSql[0]);
						pstmtrrs = conn.prepareStatement(firstNowSql[0]);
					}
					else
					{
						String []firstNowSql = new String[2];
						firstNowSql[0]=applySessionVariable(sqltest[sqltestcnt++]);
						firstNowSql[1]=applySessionVariable(GlobalVariable.getRightCmpsource().getMainsqlsb().toString());
						transform_main.firstNowStmt[0] = Parser.parseStatement(firstNowSql[0]);
						pstmtrrs = conn.prepareStatement(firstNowSql[0]);
					}
				}
				else
				{
					String firstNowSql;
					firstNowSql = GlobalVariable.getLeftCmpsource().range.getSchemaString();
					firstNowSql=applySessionVariable(firstNowSql);
					transform_main.firstNowStmt[0] = Parser.parseStatement(firstNowSql);
					firstNowSql = PathManipulation.filterSQL(firstNowSql);
					pstmtrrs = conn.prepareStatement(firstNowSql);
				}
			}
			else
				pstmtrrs = conn.prepareStatement(sql,ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
			
			pstmtrrs.setFetchSize(GlobalVariable.getFetchsize());
			ret = pstmtrrs.executeQuery();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

	public static int sqltestcnt=0;
	public static String []sqltest;

	public static Connection sqltestconn;

	public static void OpenSqlScripts(String path)
	{
		if(GlobalVariable.getLeftCmpsource().range==null)
		{
			try {
				sqlscriptfile = new BufferedReader(new FileReader(path));

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public static void CloseSqlScripts()
	{
		if(GlobalVariable.getLeftCmpsource().range==null)
		{
			try {
				sqlscriptfile.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	synchronized public static String[] getNextSqlScript()
	{
		String []ret =new String[2];
		if(GlobalVariable.getLeftCmpsource().range==null)
		{
			try {
				ret[0] = sqlscriptfile.readLine();

				//ScriptAPI.errpk_println(ret);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			ret[0] = GlobalVariable.getLeftCmpsource().range.getNextRangeString();
		}
		return ret;
	}
	
	private static void init_openlogfile(String _inputxml_path, String _errpkfile_path) throws UnsupportedEncodingException, FileNotFoundException
	{
		String inputxml_path = PathManipulation.filterFilePath(_inputxml_path);
		File inputxml = new File(inputxml_path);
		String errpkfile_path = PathManipulation.filterFilePath(_errpkfile_path);
		File errpkfile = new File(errpkfile_path);
		errpk = new PrintWriter(new OutputStreamWriter(new FileOutputStream(errpkfile.getParent()+"/"+inputxml.getName()+errpkfile.getName()), "UTF-8"));
		//errpk = new PrintWriter(new File(errpkfile.getParent()+"/"+inputxml.getName()+errpkfile.getName()));
		errpk_summary = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(errpkfile_path+".summary"),true), "UTF-8"));
	}
	
	private static void init_getMeta() throws SQLException
	{
		Object connobj = DSManager.getInstance().getConn(GlobalVariable.getLeftCmpsource().dbconn);
		
		if(connobj instanceof Connection)
		{
			Connection db_connora=(Connection)connobj;
			Connection rightconnobj = (Connection)DSManager.getInstance().getConn(GlobalVariable.getRightCmpsource().dbconn);
			PreparedStatement rightpstmt = rightconnobj.prepareStatement(rightfirstsql.toString());
			ResultSet rightrs = rightpstmt.executeQuery();
			ResultSet leftrs = null;
			
			String leftmainsqlfile=null;
			if(GlobalVariable.getLeftCmpsource().getMainsqlsb().toString().startsWith("file://"))
			{//分段
				leftmainsqlfile = GlobalVariable.getLeftCmpsource().getMainsqlsb().toString().substring("file://".length());
				OpenSqlScripts(leftmainsqlfile);
			}
			
			leftrs = Set_rrs(db_connora,GlobalVariable.getLeftCmpsource().getMainsqlsb().toString());
			  if(GlobalVariable.getLeftCmpsource().getMainsqlsb().toString().startsWith("file://"))
			  {
				  CloseSqlScripts();
				  OpenSqlScripts(leftmainsqlfile);
			  }
			  else
			  {
				  //不分段
				  sqltest = new String[1];
				  sqltest[0]=GlobalVariable.getLeftCmpsource().getMainsqlsb().toString();
				  System.out.println("Only 1 sql part defined! Set threadcnt to 1");
				  threadcnt=1;//thread一律為1
			  }
			  //init_lcmpk(rrs.getMetaData());
			  setCmpPair(leftrs.getMetaData(), rightrs.getMetaData());
			  leftrs.close();//todo 不分段
			  rightrs.close();
			  rightpstmt.close();
			  rightconnobj.close();
		}
	}
	
	private static void init_handleargs(String []args) throws ClassNotFoundException, IOException
	{
		if(args.length <2 || args[0]==null || args[1]==null)
		{
			System.out.println("windows:   java -cp javalib/*;javalib/jdom-2.0.6/*;genericCmpData.jar com.cmpdata.cmpdata confing file_path logfile_path [customized_class_file_path or jar_file_path] [entry main customized class uri]");
			System.out.println("unix like: java -cp javalib/*:javalib/jdom-2.0.6/*:genericCmpData.jar com.cmpdata.cmpdata confing file_path logfile_path [customized_class_file_path or jar_file_path] [entry main customized class uri]");
			return;
		}
		if(args.length==3)
		{
			if(args[2].equals("YES"))
				dbgmode=1;
			else if(args[2].equals("1000"))
				dbgmode=1000;
		}

		if(args.length>3 && (args[2]!=null || args[3]!=null))
		{
			if(args[2]==null || args[3]==null)
			{
				System.out.println("windows:   java -cp javalib/*;javalib/jdom-2.0.6/*;genericCmpData.jar com.cmpdata.cmpdata confing file_path logfile_path [customized_class_file_path or jar_file_path] [entry main customized class uri]");
				System.out.println("unix like: java -cp javalib/*:javalib/jdom-2.0.6/*:genericCmpData.jar com.cmpdata.cmpdata confing file_path logfile_path [customized_class_file_path or jar_file_path] [entry main customized class uri]");
				return;
			}
			ScriptAPI.initScriptAPI(args[2],args[3]);
		}
	}
	
	private static Thread [] init_startCmpTask() throws InterruptedException
	{
		cmptasks = new CmpTask[threadcnt];
		Thread []transform_main = new Thread[threadcnt];
		for(int ii=0;ii<threadcnt;ii++)
   	  	{
			transform_main[ii] = new Thread(new transform_main(ii));
			transform_main[ii].start();
		}
		
		for(int ii=0;ii<threadcnt;ii++)
		{
			while(cmptasks[ii]==null || transform_main[ii]==null)
			{
				Thread.sleep(2000);
			}
		}
		
		return transform_main;
	}
	private static boolean mainthread_monitorCmpTask(Thread []genericCmpDataThread)
	{
		boolean done= true;
		for(int ii=0;ii<threadcnt;ii++)
		{//System.out.println("cmptasks["+ii+"]:");TestMemUtil.printObjectSize(cmptasks[ii]);
			//if(cmptasks[ii]!=null && cmptasks[ii].state.get()!=-1)
			if(genericCmpDataThread[ii]!=null && genericCmpDataThread[ii].isAlive())	
				done=false;
		}
		return done;
	}
	
	private static int mainthread_monitorProgress(int previousprogress)
	{
		int prtcycle=100000;
		int now = getrrsc();
		if((now-previousprogress)>prtcycle)
		{
			System.out.println("progress:"+now+" "+new Date());
			System.out.flush();
			previousprogress=now;
		}
		return previousprogress;
	}
	
	private static void mainthread_summaryresult()
	{
		System.out.println("end process"+" "+new Date());
	  	  AllSummary.endtime = new Date();
	  	  SummarizedResult();
	  	  RstWriting.writeSummary(format, GlobalVariable.getLeftCmpsource(), GlobalVariable.getRightCmpsource(), AllSummary);
	  	  ScriptAPI.summary_println(transform_main.errpk, transform_main.errpk_summary,"leftcount:"+AllSummary.leftcount+" rightcount:"+AllSummary.rightcount);
	  	  ScriptAPI.summary_println(transform_main.errpk, transform_main.errpk_summary,"case1bcount:"+AllSummary.case1bcount+" case2count:"+AllSummary.case2count+" case3count:"+AllSummary.case3count+" case4count:"+AllSummary.case4count+" case2rechckscriptcount:"+AllSummary.case2rechckscriptcount);
	  	  ScriptAPI.summary_println(transform_main.errpk, transform_main.errpk_summary,"error column summary:");
	  	  for(CmpPair cp :CmpPair.getlcmppair())
		  {
	  		  int colerr = cp.getErrcnt();
			  if(colerr>0)
			  {
				  ScriptAPI.summary_println(cp+" error:"+colerr);
			  }
		  }
	}
	
	public static void main(String[] args) throws Exception {
		
		init_handleargs(args);
		AllSummary.begintime = new Date();
		System.out.println("begin: "+AllSummary.begintime);
		
		init_openlogfile(args[0], args[1]);
		readinput(args[0]);//todo for transform
		//if(!customrightcountref)
			//initRightCountRef(rightcountrefidx);//todo for transform
		initRightFirstSql();
		format = RstFormat.initMeta("1", GlobalVariable.getLeftCmpsource().pk, GlobalVariable.getRightCmpsource().pk);
		//todo for transform
		init_getMeta();
		int i = GlobalVariable.getrrsc();
		GlobalVariable.setController
		(
				transform_controller.transform_controllerfactory
				(
						GlobalVariable.getLeftCmpsource(),
						GlobalVariable.getRightCmpsource(),
						firstNowStmt[0],
						firstNowStmt[1],
						threadtaskmap,
						threadrunidmap,
						logformat,
						logformatsplitter,
						errpk,
						format,
						dbgmode	
				)
		);
		Thread []genericCmpDataThread = init_startCmpTask();

		while(true)
		{
			i=mainthread_monitorProgress(i);
			if(mainthread_monitorCmpTask(genericCmpDataThread))
				break;
			//TestMemUtil.printMemoryStatus();
  		  	Thread.sleep(2000);
  	  	}
		
		mainthread_summaryresult();

	}
}
