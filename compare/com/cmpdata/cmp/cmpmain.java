package com.cmpdata.cmp;


import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptException;

import org.jdom2.Element;

import com.cmpdata.*;


import com.cmpdata.controller.GlobalVariable;
import com.cmpdata.datatype.DatatypeDefinition;
import com.cmpdata.range.Range_input;
import com.cmpdata.rst.Cmpsummary;
import com.cmpdata.rst.PKDisplay;
import com.cmpdata.rst.RstFormat;
import com.cmpdata.rst.RstWriting;
import com.cmpdata.scripting.CheckInterfaceScript;
import com.cmpdata.scripting.CheckScript;

import com.security.PathManipulation;
import com.sqlparse.*;
import com.steve.db.data.manager.DBManagerRuntimeException;
import com.steve.db.data.manager.DSManager;

import com.cmpdata.logic.*;

	public class cmpmain implements Runnable  {

		//final static int CmpLeftElm = 0;
		//final static int CmpRightElm = 1;
		//public static TreeMap<String, String> SessionVariable;
		public cmpmain(){runid=-1;}
		public cmpmain(int id)
		{ 
			runid = id;
		}
		int runid;
		public void run() {
			int parcnt=GlobalVariable.getCmpunitcnt();//basic duplicate parcnt 10  thread 16 ~ 24    
	//left連線錯誤時，把當下出問題的part重做
	//right連線出錯時，當下出問題的part捨棄(or 呼叫left重作)，且須重建temp table
			try {
					if(cmptype==1)
					{
						cmptasks[runid] = new CmpTask(GlobalVariable.getLeftCmpsource().pk, GlobalVariable.getLeftCmpsource().dbconn);
					}
					
					GlobalVariable.threadtaskmap.put(Long.toString(Thread.currentThread().getId(), 10), cmptasks[runid]);
					GlobalVariable.threadrunidmap.put(Long.toString(Thread.currentThread().getId(), 10), runid);
					boolean redo=false;
					while(cmptasks[runid].getState()==0)
					{
						boolean eof=false;
						if(!redo)
						{
							//cmptasks[runid].begintime=new Date().getTime();
							eof=GetResultSet(cmptasks[runid], parcnt);//todo 不分段
						}
						if(eof)
							break;
						redo = false;
					}
				//db_conninfo.close();
			}
			catch(genericCmpDataException e)
			{
				e.printStackTrace();
				cmpmain.ForceExit();
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				cmpmain.ForceExit();
			}
			finally
			{
				RstWriting.batchwritePartSummary();
				RstWriting.batchwriteRst(cmptasks[runid].RstWritingRst);
				RstWriting.batchwriteRaw(cmptasks[runid].RstWritingRaw);
				System.out.println("task"+runid+" exit:"+GlobalVariable.getrrsc()+" "+new Date());
				System.out.flush();
				cmpmain.errpk_summary.flush();
				cmpmain.errpk.flush();
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
			if(task.countrightmode || cmpmain.strategy==3)
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
		
		public static int compare(TreeMap<String,Object> o1, TreeMap<String,Object> o2, List<String> pk, int leftright)
		{
			if(GlobalVariable.dbgmode==1)System.out.println("allpk:"+pk);
			  for(int i=0;i<pk.size(); i++)
			  {
				  String pkname = genericCmpDataUtil.getRealColumn(pk.get(i));
				  if(GlobalVariable.dbgmode==1)System.out.println("nowcmp:"+pkname);
				  ColumnMeta cm = genericCmpDataUtil.findCmpPair(leftright, pkname);
				  if(cm == null)
					  throw new genericCmpDataException("cmpdata002","findCmpPair pkname null:"+pkname);
				  String realcolumn = genericCmpDataUtil.getRealColumn(cm.getName());
				  
				  int cmprst = cm.compare(TreeMapUtil.getString(o1.get(realcolumn)), TreeMapUtil.getString(o2.get(realcolumn)));
				  if(cmprst==0)
					  continue;
				  else
				  {
					  return cmprst;
				  }
			  }
			  if(o1!=o2)
				  ScriptAPI.errpk_println("pk get duplicate data:"+o1.toString());
			  return 0;
		}   
		
		public static int compare(TreeMap<String,Object> o1, TreeMap<String,Object> o2, List<String> pk, int leftright, int a)
		{
			  for(int i=0;i<pk.size(); i++)
			  {
				  String pkname = genericCmpDataUtil.getRealColumn(pk.get(i));
				  
				  CmpPair cmp = genericCmpDataUtil.findCmpPair(pkname);
				  if(cmp == null)
					  throw new genericCmpDataException("cmpdata002","findCmpPair pkname null:"+pkname);

				  String o1realcolumn = genericCmpDataUtil.getRealColumn(cmp.getLeftcm().getName());
				  String o2realcolumn = genericCmpDataUtil.getRealColumn(cmp.getRightcm().getName());

				  ColumnMeta cm = genericCmpDataUtil.findCmpPair(leftright, pkname);
				  int cmprst = cm.compare(TreeMapUtil.getString(o1.get(o1realcolumn)), TreeMapUtil.getString(o2.get(o2realcolumn)));
				  if(cmprst==0)
					  continue;
				  else
				  {
					  return cmprst;
				  }
				  
			  }
			  return 0;
		}   
		public static Set<TreeMap<String,Object>> QueryResultSet(Connection conn, PreparedStatement pstmt, String sqlscript, int leftright, CmpTask task) throws SQLException
		{
			Cmpsource src = (leftright==genericCmpDataConstant.CmpLeftElm)?GlobalVariable.getLeftCmpsource():GlobalVariable.getRightCmpsource();
			Set<TreeMap<String, Object>> ret = new TreeSet<TreeMap<String, Object>>(
					new Comparator<TreeMap<String,Object>>(){
						  @Override
						  public int compare(TreeMap<String,Object> o1, TreeMap<String,Object> o2) {
							  return cmpmain.compare(o1,o2,src.pk,leftright);
						  }   
					  }
					);
			
			pstmt.setQueryTimeout(1200);
			ResultSet rs = pstmt.executeQuery();
			rs.setFetchSize(GlobalVariable.getFetchsize());
			
			if(GlobalVariable.dbgmode==1)
			{
				System.out.println("GetResultSet executeQuery:"+sqlscript);
			}
			
			while(rs.next())
			{
				TreeMap<String,Object> r = new TreeMap<String,Object>(String.CASE_INSENSITIVE_ORDER);
				//for(int i=0; i<src.cols.size(); i++)
				for(CmpPair col:CmpPair.getlcmppair())	
				{
					String columnname = (leftright==genericCmpDataConstant.CmpLeftElm)?col.getLeftcm().getName():col.getRightcm().getName();
					if(columnname.startsWith("#Constant"))
						continue;
					
					CmpPair cp = col;
					  try {
						  int columnidx = rs.findColumn(columnname);
						  List<String> props =cp.getProps(leftright);
						  if(props==null)
						  {
							  //r.put(right.cols.get(i).toString(),JSONHandler.getRTrimColumnValueStringNull(task.rs.getString(right.cols.get(i).toString())));
							  String v = JSONHandler.getRTrimColumnValueStringNull(GlobalVariable.getLeftCmpsource().getResultSetValue(rs, columnidx));
							  if(GlobalVariable.dbgmode==1)System.out.println("1get:"+columnname+":"+v);
							  r.put(columnname,v);
						  }
						  else if(props.contains("untrim"))
						  {
							  //r.put(right.cols.get(i).toString(),JSONHandler.getColumnValueStringNull(task.rs.getString(right.cols.get(i).toString())));
							  String v = JSONHandler.getColumnValueStringNull(GlobalVariable.getLeftCmpsource().getResultSetValue(rs, columnidx));
							  if(GlobalVariable.dbgmode==1)
							  {
								  System.out.println("2get:"+columnname+":"+v);
								  byte []bs = rs.getBytes(columnname);
								  System.out.println("2get:"+columnname+":"+bs.length);
								  //printbyte(bs);
							  }
							  r.put(columnname,v);
						  }
						  else if(props.contains("chinese"))
						  {
							  byte []bs = rs.getBytes(columnname);
							  if(GlobalVariable.dbgmode==1)
							  {
								  System.out.println("get:"+columnname+":");
								  //printbyte(bs);
							  }
							  r.put(columnname, bs);
							  continue;
						  }
						  else
						  {
							  //r.put(right.cols.get(i).toString(),JSONHandler.getRTrimColumnValueStringNull(task.rs.getString(right.cols.get(i).toString())));
							  String v = JSONHandler.getRTrimColumnValueStringNull(GlobalVariable.getLeftCmpsource().getResultSetValue(rs, columnidx));
							  if(GlobalVariable.dbgmode==1)System.out.println("3get:"+columnname+":"+v);
							  r.put(columnname,v);
						  }
						  
						//r.put(loldcol.get(i).toString(),JSONHandler.getColumnValueString(task.rs.getString(loldcol.get(i).toString())));
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						throw new genericCmpDataException("cmpdata002", "rrs.getString column:"+columnname+" is not defined in the db");
					}
				}
				if(GlobalVariable.dbgmode==1)
				{
					System.out.println("GetResultSet before add r:"+ret.size());
				}
				ret.add(r);
				if(GlobalVariable.dbgmode==1)
				{
					System.out.println("GetResultSet after add r:"+ret.size());
					System.out.println("GetResultSet:"+sqlscript);
					System.out.println("GetResultSet rs:"+r);
				}
				/*
				if(left.sqlcountcolumn!=null)
				{
					int rscnt = rs.getInt(left.sqlcountcolumn);
					if(task!=null)
						task.nowsqlcnt_plus(rscnt);
				}
				else
				{
					if(task!=null)
						task.nowsqlcnt_plus();
				}*/
			}
			rs.close();
			pstmt.close();
			
			return ret;
		}
		static class rightresultquery implements Runnable
		{
			Connection conn;
			PreparedStatement pstmt;
			String sqlscript;
			CyclicBarrier barrier;
			Set<TreeMap<String,Object>> ret;
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					ret = QueryResultSet(conn, pstmt, sqlscript,genericCmpDataConstant.CmpRightElm,null);
					barrier.await();
				} catch (InterruptedException | BrokenBarrierException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}
		
		public static void case2handling(CmpTask task, TreeMap<String,Object> rs) throws Exception
		{
			if(cmpmain.case2checkScript != null)
			  {
				    List<Object> refs = new ArrayList<Object>();
					List<TreeMap<String,Object>> thisrow = new ArrayList<TreeMap<String,Object>>();
					thisrow.add(rs);
					//thisrow.add(null);
					refs.add(thisrow);//加入thisrow left		
					if(cmpmain.case2checkScript.getCheckscriptrefs() != null)
					{
						if(!case2lazyInit)
							refs.addAll(CheckScript.generateRef(GlobalVariable.getSessionVariable(), thisrow, cmpmain.case2checkScript.getCheckscriptrefs()));//加入設定檔定義的ref
						else
							refs.add(cmpmain.case2checkScript.getCheckscriptrefs());
					}
				  
					boolean ret = (Boolean)cmpmain.case2checkScript.InvokeCheckScript(new Object[] { refs.toArray()});
					if(!ret)
					{
						  StringBuffer pk = subcompare_setPKDisplay(GlobalVariable.getLeftCmpsource().pk,rs);
						  synchronized(cmpmain.errpk)
						  {
							  if(!task.countrightmode)
							  {
								  if(GlobalVariable.logformat != null)
								  {
									  ScriptAPI.errpk_report(rs, null, GlobalVariable.getLeftCmpsource().pk, null, GlobalVariable.getRightCmpsource().getTable(), CmpContext.ERRTYPE_CASE2, null, null, null, GlobalVariable.logformatsplitter, null);
								  }
								  else
								  {
										  ScriptAPI.errpk_println("Left find no right ref data: pk:"+pk);
								  }
							  }
							  
						  }
						  ScriptAPI.writeRegTable(task.RstWritingRst, task.RstWritingRaw, -1, -1, format, "case2", null, null, GlobalVariable.getLeftCmpsource().pk, GlobalVariable.getRightCmpsource().pk, rs, null, "", "");
						  
						  if(GlobalVariable.getLeftCmpsource().sqlcountcolumn!=null)
						  {
							  int rscnt = Integer.parseInt((String)rs.get(GlobalVariable.getLeftCmpsource().sqlcountcolumn));
							  task.nowsqlfindnorightcnt_plus(rscnt);
						  }
						  else
							  task.nowsqlfindnorightcnt_plus();
					}
					else
					{
						//StringBuffer pk = subcompare_setPKDisplay(left.pk,rs);
					    //ScriptAPI.errpk_println("Left find no right ref data but recheck script ok: pk:"+pk);
					    
					    if(GlobalVariable.getLeftCmpsource().sqlcountcolumn!=null)
						  {
							  int rscnt = Integer.parseInt((String)rs.get(GlobalVariable.getLeftCmpsource().sqlcountcolumn));
							  task.nowsqlfindnorightrechckscripcnt_plus(rscnt);
						  }
						  else
							  task.nowsqlfindnorightrechckscripcnt_plus();
					}
				}
			  else
			  {
				  StringBuffer pk = subcompare_setPKDisplay(GlobalVariable.getLeftCmpsource().pk,rs);
				  synchronized(cmpmain.errpk)
				  {
					  if(!task.countrightmode)
					  {
						  if(GlobalVariable.logformat != null)
						  {
							  ScriptAPI.errpk_report(rs, null, GlobalVariable.getLeftCmpsource().pk, null, GlobalVariable.getRightCmpsource().getTable(), CmpContext.ERRTYPE_CASE2, null, null, null, GlobalVariable.logformatsplitter, null);
						  }
						  else
						  {
							  ScriptAPI.errpk_println("Left find no right ref data: pk:"+pk);
						  }
					  }
				  }
				  ScriptAPI.writeRegTable(task.RstWritingRst, task.RstWritingRaw, -1, -1, format, "case2", null, null, GlobalVariable.getLeftCmpsource().pk, GlobalVariable.getRightCmpsource().pk, rs, null, "", "");
				  
				  if(GlobalVariable.getLeftCmpsource().sqlcountcolumn!=null)
				  {
					  int rscnt = Integer.parseInt((String)rs.get(GlobalVariable.getLeftCmpsource().sqlcountcolumn));
					  task.nowsqlfindnorightcnt_plus(rscnt);
				  }
				  else
					  task.nowsqlfindnorightcnt_plus();
			  }
		}
		
		public static void case3handling(CmpTask task, TreeMap<String,Object> rsdata)
		{
			boolean case3check=false;
			if(cmpmain.case3checkScript != null)
			  {
				  List<Object> refs = new ArrayList<Object>();
					List<TreeMap<String,Object>> thisrow = new ArrayList<TreeMap<String,Object>>();
					thisrow.add(rsdata);

					refs.add(thisrow);//加入thisrow right		
					try {
						if(cmpmain.case3checkScript.getCheckscriptrefs() != null)
						{
							if(!cmpmain.case3lazyInit)
								refs.addAll(CheckScript.generateRef(GlobalVariable.getSessionVariable(), thisrow, cmpmain.case3checkScript.getCheckscriptrefs()));//加入設定檔定義的ref
							else
								refs.add(cmpmain.case3checkScript.getCheckscriptrefs());
						}

						case3check = (Boolean)cmpmain.case3checkScript.InvokeCheckScript(new Object[] { refs.toArray()});
					} catch (NoSuchMethodException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ScriptException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			
			
			if(!case3check)
			{
				if(GlobalVariable.getRightCmpsource().sqlcountcolumn!=null)
				{
					int rscnt = Integer.parseInt((String)rsdata.get(GlobalVariable.getRightCmpsource().sqlcountcolumn));
					task.nowsqlfindnorightcnt_plus(rscnt);
				}
				else
					task.nowsqlfindnoleft_plus();
				
				synchronized(cmpmain.errpk)
				{
					if(GlobalVariable.logformat != null)
					{
						  ScriptAPI.errpk_report(null, rsdata, null, GlobalVariable.getRightCmpsource().pk, GlobalVariable.getRightCmpsource().getTable(), CmpContext.ERRTYPE_CASE3, null, null, null, GlobalVariable.logformatsplitter, null);
					}
					else
					{
						StringBuffer pk = subcompare_setPKDisplay(GlobalVariable.getRightCmpsource().pk,rsdata);
						ScriptAPI.errpk_println("Right find no left ref data: pk:"+pk);
					}
				}
				ScriptAPI.writeRegTable(task.RstWritingRst, task.RstWritingRaw, -1, -1, cmpmain.format, "case3", null, null,GlobalVariable.getLeftCmpsource().pk, GlobalVariable.getRightCmpsource().pk, null, rsdata, "", "");
			}
		}
		
		public static void leftcounting(CmpTask task, TreeMap<String,Object> rsdata, String sqlcountcolumn)
		{
			if(sqlcountcolumn!=null)
			{
				int rscnt = Integer.parseInt((String)rsdata.get(sqlcountcolumn));
				task.nowsqlcnt_plus(rscnt);
			}
			else
				task.nowsqlcnt_plus();
		}
		
		public static void rightcounting(CmpTask task, TreeMap<String,Object> rsdata, String sqlcountcolumn)
		{
			if(sqlcountcolumn!=null)
			{
				int rscnt = Integer.parseInt((String)rsdata.get(sqlcountcolumn));
				task.getsummary().rightcount+=rscnt;
				task.getallsummary().rightcount+=rscnt;
			}
			else
			{
				task.getsummary().rightcount+=1;
				task.getallsummary().rightcount+=1;
			}
		}
		
		public static boolean GetResultSet(CmpTask task, int lmt)
		{
			int leftcnt=0;
			try {
				
					LeftGetter_GetNowSql(task);
				
					if(task.nowsql[0] == null || task.nowsql[1] == null)
						return true;

					task.setZeroNowSql();
					String filters = PathManipulation.filterSQL(task.nowsql[0]);
					if(GlobalVariable.dbgmode==1)
					{
						System.out.println("GetResultSet executeQuery:"+filters);
					}
					CyclicBarrier barrier = new CyclicBarrier(2);
					rightresultquery query = new rightresultquery();
					query.conn=(Connection) DSManager.getInstance().getConn(GlobalVariable.getRightCmpsource().dbconn);
					query.pstmt=query.conn.prepareStatement(task.nowsql[1]);
					query.sqlscript=task.nowsql[1];
					query.barrier=barrier;
					Thread tquery = new Thread(query);
					tquery.start();
					
					task.pstmt = ((Connection) task.conn).prepareStatement(task.nowsql[0]);
					Set<TreeMap<String, Object>> leftret = QueryResultSet((Connection)task.conn, task.pstmt, filters, genericCmpDataConstant.CmpLeftElm, task);
					
					barrier.await();
					tquery.join();
					Set<TreeMap<String, Object>> rightret = query.ret;

					if(GlobalVariable.dbgmode==1)
					{
						System.out.println("left:"+leftret);
						System.out.println("right:"+rightret);
					}
				//比對leftret rightret
					Iterator<TreeMap<String, Object>> leftite = leftret.iterator();
					Iterator<TreeMap<String, Object>> rightite = rightret.iterator();
					TreeMap<String, Object> leftr = null;
					TreeMap<String, Object> rightr = null;
					boolean nextleft=true;
					boolean nextright=true;
					while(leftite.hasNext() || rightite.hasNext())
					{
						if(nextleft)
						{
							leftr = (leftite.hasNext())?leftite.next():null;
						}
						
						if(nextright)
						{
							rightr = (rightite.hasNext())?rightite.next():null;
						}
						
						//compare leftr and rightr
						if(leftr==null)
						{
							nextright=true;
							nextleft=false;
							rightcounting(task, rightr, GlobalVariable.getRightCmpsource().sqlcountcolumn);
							case3handling(task,rightr);
							continue;
						}
						else if(rightr==null)
						{
							nextleft=true;
							nextright=false;
							leftcounting(task, leftr, GlobalVariable.getLeftCmpsource().sqlcountcolumn);
							leftcnt++;
							case2handling(task,leftr);
							continue;
						}
						
						int cmpr = cmpmain.compare(leftr,rightr,GlobalVariable.getLeftCmpsource().pk,genericCmpDataConstant.CmpLeftElm, 11);
						
						
						StringBuffer pk = pkdisplay.Display(GlobalVariable.getLeftCmpsource().pk, leftr, "_");
						if(cmpr ==0)
						{
							if(!subcompare_checkColumnValue(task, pk.toString(), leftr, rightr))
							{
								task.nowsqlcmpproblemcnt_plus();
							}
							leftcounting(task, leftr, GlobalVariable.getLeftCmpsource().sqlcountcolumn);
							leftcnt++;
							rightcounting(task, rightr, GlobalVariable.getRightCmpsource().sqlcountcolumn);
							nextleft=true;
							nextright=true;
						}
						else if(cmpr>0)
						{
							nextright=true;
							nextleft=false;
							rightcounting(task, rightr, GlobalVariable.getRightCmpsource().sqlcountcolumn);
							case3handling(task,rightr);
						}
						else if(cmpr<0)
						{
							nextleft=true;
							nextright=false;
							leftcounting(task, leftr, GlobalVariable.getLeftCmpsource().sqlcountcolumn);
							leftcnt++;
							case2handling(task,leftr);
						}
						
						
					}
				
				
				if(GlobalVariable.getLeftCmpsource().getMainsqlsb().toString().startsWith("file://"))
				{
					//檢查duplicate 舊系統沒資料新系統有資料的狀況
					//task.checkRightcnttable(left.pk,right.pk);
					//紀錄上一個sql執行結果
					//if(checkBalance(task))
					{
						if(task.nowsql!=null && task.nowsql[0] != null)
						{
							if(task.nowsql[1]!=null)
							{
								ScriptAPI.summary_println(task.nowsql[0]+"|"+task.getsummary().leftcount+"|"+task.getsummary().case1bcount+"|"+task.getsummary().case2count+"|"+task.getsummary().case3count+"|"+task.getsummary().case4count+"|"+task.getsummary().rightcount+"|"+task.nowsql[1]);
								RstWriting.writePartSummary(format, task.nowsql[0], task.nowsql[1], task.getsummary());
							}
							else
							{
								ScriptAPI.summary_println(task.nowsql[0]+"|"+task.getsummary().leftcount+"|"+task.getsummary().case1bcount+"|"+task.getsummary().case2count+"|"+task.getsummary().case3count+"|"+task.getsummary().case4count+"|"+task.getsummary().rightcount);
								RstWriting.writePartSummary(format, task.nowsql[0], "", task.getsummary());
							}
						}
					}

					//task.truncateRightcnttable(right.dbconn);

				}
				else
				{
					if(task.nowsql!=null && task.nowsql[0]!=null && task.nowsql[0].equals(GlobalVariable.getLeftCmpsource().getMainsqlsb().toString()))
					{//已經比完了
						if(!GlobalVariable.getLeftCmpsource().getMainsqlsb().toString().startsWith("file://"))
						{
							ScriptAPI.summary_println(task.nowsql[0]+"|"+task.getsummary().leftcount+"|"+task.getsummary().case1bcount+"|"+task.getsummary().case2count+"|"+task.getsummary().case3count+"|"+task.getsummary().case4count+"|"+task.getsummary().rightcount);
							RstWriting.writePartSummary(format, task.nowsql[0], "", task.getsummary());
						}
					}
					task.nowsql=new String[2];
					task.nowsql[0] = GlobalVariable.getLeftCmpsource().getMainsqlsb().toString();
				}
					
			} catch (SQLException e) {
				System.out.println("retry connection:"+GlobalVariable.getLeftCmpsource().dbconn+" sql:"+task.nowsql[0]);
				ScriptAPI.errpk_println(e.toString());
				e.printStackTrace();
				//int repeat=0;
				//leftcnt=0;rightcnt=0;//drop this redo
				task.rollbackAllSummary();
				task.setZeroNowSql();
				
				RetryDBConnection retrydbconn = new RetryDBConnection((Connection)task.conn, task.pstmt, task.rs);
				retrydbconn = retrydbconn.Retry(GlobalVariable.getLeftCmpsource().dbconn, task.nowsql[0]);
				task.conn = retrydbconn.conn;task.pstmt = retrydbconn.pstmt;task.rs=retrydbconn.rs;
				return false;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (BrokenBarrierException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			plus_rrsc(leftcnt);
			//task.allsummary.rightcount+=rightcnt;
			return false;
		}
		
		private static boolean subcompare_checkColumnValue(CmpTask task, String pk, TreeMap<String,Object> rs, TreeMap<String,Object> rsinfodata) throws Exception
		{
			boolean ret=true;
			int cntcol=0;
			int wrongrowno=-1;
			for(CmpPair col:CmpPair.getlcmppair())
			{
				if(!col.init || !col.enableCmp)//不用比
					continue;
				List<String> props = col.getProps(genericCmpDataConstant.CmpRightElm);
				if(col.getCheckColumnValueScript()!=null)
				{
					List<Object> refs = new ArrayList<Object>();
					List<TreeMap<String,Object>> thisrow = new ArrayList<TreeMap<String,Object>>();
					thisrow.add(rs);
					thisrow.add(rsinfodata);
					refs.add(thisrow);//加入thisrow left
					try {				
						if(col.getCheckColumnValueScriptrefs() != null)
						{
							if(!col.lazyinitrefs)
								refs.addAll(CheckScript.generateRef(GlobalVariable.getSessionVariable(), thisrow, col.getCheckColumnValueScriptrefs()));//加入設定檔定義的ref
							else
								refs.add(col.getCheckColumnValueScriptrefs());
						}
						String leftvalue=null;
						String rightvalue=null;
						if(props==null || !props.contains("chinese"))
						{
							TreeMap<String, String> leftrightvalue = subcompare_getLeftRightValue(col, rs, rsinfodata);
							leftvalue = leftrightvalue.get("leftvalue");
							rightvalue = leftrightvalue.get("rightvalue");
							Integer runid = GlobalVariable.threadrunidmap.get(Long.toString(Thread.currentThread().getId(), 10));
							col.clearColumnValueScriptRst(runid);
							//if(!JSONHandler.cmpColumnValueString(rs.get(col.rightcm.toString()),right))
							if(!col.getCheckColumnValueScript().cmpvalue(leftvalue,rightvalue,refs.toArray(), runid, col.getcmpLogicvars(),null))
							{
								String refleftvalue, refrightvalue;
								col.addErrcnt();
								if(col.getCheckColumnValueScriptRst(runid, genericCmpDataConstant.CmpLeftElm).length()==0)
									refleftvalue= JSONHandler.getColumnValueString(leftvalue);
								else
									refleftvalue=JSONHandler.getColumnValueString(col.getCheckColumnValueScriptRst(runid, genericCmpDataConstant.CmpLeftElm));
								
								if(col.getCheckColumnValueScriptRst(runid, genericCmpDataConstant.CmpRightElm).length()==0)
									refrightvalue= JSONHandler.getColumnValueString(rightvalue);
								else
									refrightvalue=JSONHandler.getColumnValueString(col.getCheckColumnValueScriptRst(runid, genericCmpDataConstant.CmpRightElm));
								synchronized(errpk)
								{
									if(GlobalVariable.logformat != null)
									{
										ScriptAPI.errpk_report(rs, rsinfodata, GlobalVariable.getLeftCmpsource().pk, GlobalVariable.getRightCmpsource().pk, GlobalVariable.getRightCmpsource().getTable(), CmpContext.ERRTYPE_CASE1, col, refleftvalue, refrightvalue, GlobalVariable.logformatsplitter, pkdisplay);
									}
									else
									{
										ScriptAPI.errpk_println("pk:"+pk+" columninfo:"+col+", leftvalue:"+refleftvalue+" rightvalue:"+refrightvalue);
									}
								}
								wrongrowno = ScriptAPI.writeRegTable(task.RstWritingRst, task.RstWritingRaw, wrongrowno, cntcol, format, "case1b", (col.getLeftcm()==null)?genericCmpDataConstant.CONSTANT_STRING_NULL:col.getLeftcm().toString(), (col.getRightcm()==null)?genericCmpDataConstant.CONSTANT_STRING_NULL:col.getRightcm().toString(), GlobalVariable.getLeftCmpsource().pk, GlobalVariable.getRightCmpsource().pk, rs, rsinfodata,refleftvalue, refrightvalue);
								ret = false;
								cntcol++;
							} 
						}
						else
						{//chinese屬性的欄位，無法自訂script
							Integer runid = GlobalVariable.threadrunidmap.get(Long.toString(Thread.currentThread().getId(), 10));
							col.clearColumnValueScriptRst(runid);
							if(!col.getCheckColumnValueScript().cmpvalue(TreeMapUtil.getBytes(rs.get(col.getLeftcm().toString()),GlobalVariable.getLeftCharset()), TreeMapUtil.getBytes(rsinfodata.get(col.getLeftcm().toString()),GlobalVariable.getRightCharset()),refs.toArray(), runid, col.getcmpLogicvars(),null))
							//if(!CmpLogicDefaultImpl.ByteArrayEqual.cmpvalue(TreeMapUtil.getBytes(rs.get(col.leftcm.toString())), TreeMapUtil.getBytes(rsinfodata.get(col.rightcm.toString())),refs.toArray(),col.logicvars,null))
							//if(!Arrays.equals(TreeMapUtil.getBytes(rs.get(col.leftcm.toString())), TreeMapUtil.getBytes(rsinfodata.get(col.rightcm.toString()))))
							{
								String refleftvalue, refrightvalue;
								col.addErrcnt();
								if(col.getCheckColumnValueScriptRst(runid, genericCmpDataConstant.CmpLeftElm).length()==0)
									refleftvalue= JSONHandler.getColumnValueString((String)rs.get(col.getLeftcm().toString()));
								else
									refleftvalue=JSONHandler.getColumnValueString(col.getCheckColumnValueScriptRst(runid, genericCmpDataConstant.CmpLeftElm));
								
								if(col.getCheckColumnValueScriptRst(runid, genericCmpDataConstant.CmpRightElm).length()==0)
									refrightvalue= JSONHandler.getColumnValueString(new String(TreeMapUtil.getBytes(rsinfodata.get(col.getRightcm().toString()),GlobalVariable.getRightCharset())));
								else
									refrightvalue=JSONHandler.getColumnValueString(col.getCheckColumnValueScriptRst(runid, genericCmpDataConstant.CmpRightElm));
								
								synchronized(errpk)
								{
									if(GlobalVariable.logformat != null)
									{
										ScriptAPI.errpk_report(rs, rsinfodata, GlobalVariable.getLeftCmpsource().pk, GlobalVariable.getRightCmpsource().pk, GlobalVariable.getRightCmpsource().getTable(), CmpContext.ERRTYPE_CASE1, col, refleftvalue, refrightvalue, GlobalVariable.logformatsplitter, pkdisplay);
									}
									else
									{
										ScriptAPI.errpk_println("pk:"+pk+" columninfo:"+col+", leftvalue:"+refleftvalue+" rightvalue:"+refrightvalue);
									}
								}
								
								wrongrowno = ScriptAPI.writeRegTable(task.RstWritingRst, task.RstWritingRaw, wrongrowno, cntcol, format, "case1b", col.getLeftcm().toString(), col.getRightcm().toString(), GlobalVariable.getLeftCmpsource().pk, GlobalVariable.getRightCmpsource().pk, rs, rsinfodata,refleftvalue, refrightvalue);
								ret = false;
								cntcol++;
							}
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						if(e instanceof NullPointerException)
						{
							System.out.println("col.logic:"+col.getCheckColumnValueScript());
							System.out.println("refs:"+refs);
							System.out.println("col:"+col);
						}
						else
						{
							System.out.println("column:"+col.toString()+" scripting error");
						}
						e.printStackTrace();
					}
				}
			}
			return ret;
		}
		private static TreeMap<String, String> subcompare_getLeftRightValue(CmpPair col, TreeMap<String,Object> leftrs, TreeMap<String,Object> rightrs)
		{
			TreeMap<String, String> ret = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
			String leftvalue, rightvalue;
			if((col.getRightcm()!=null && col.getRightcm().toString().startsWith("#Constant")) && (col.getLeftcm()!=null && col.getLeftcm().toString().startsWith("#Constant")))
			{
				//throw exception
			}
			if((col.getRightcm()!=null && col.getRightcm().toString().startsWith("#Constant")) || (col.getLeftcm()!=null && col.getLeftcm().toString().startsWith("#Constant")))
			{//常數比對一律把待檢查字串放在left和right, 執行checkvalue時，將呼叫string constant check，會由ref值比對
				if(!col.getRightcm().toString().startsWith("#Constant"))
					leftvalue =TreeMapUtil.getString(rightrs.get(col.getRightcm().toString()));
				else
					leftvalue =TreeMapUtil.getString(leftrs.get(col.getLeftcm().toString()));
					
				rightvalue=leftvalue;
			}
			else
			{//一般比對
				leftvalue =(col.getLeftcm()==null)?genericCmpDataConstant.CONSTANT_STRING_NULL:TreeMapUtil.getString(leftrs.get(col.getLeftcm().toString()));
				rightvalue =(col.getRightcm()==null)?genericCmpDataConstant.CONSTANT_STRING_NULL:TreeMapUtil.getString(rightrs.get(col.getRightcm().toString()));
			}
			
			ret.put("leftvalue", leftvalue);
			ret.put("rightvalue", rightvalue);
			return ret;
		}
		public static StringBuffer subcompare_setPKDisplay(List<String> pks, TreeMap<String,Object> rs, String splitter)
		{
			StringBuffer pk = new StringBuffer();
			  for(int i=0;i<pks.size();i++)
			  {
				  if(i>0)
					  pk.append(splitter);
				  
				  String realcolumn=genericCmpDataUtil.getRealColumn(pks.get(i));

				  pk.append(JSONHandler.getColumnValueString(TreeMapUtil.getString(rs.get(realcolumn))));
			  }
			  return pk;
		}
		private static StringBuffer subcompare_setPKDisplay(List<String> pks, TreeMap<String,Object> rs)
		{
			return subcompare_setPKDisplay(pks, rs, "_");
		}
		public static StringBuffer subcompare_setPKDisplay(List<String> pks, int leftright, ResultSet rs) throws SQLException
		{
			Cmpsource src = (leftright==genericCmpDataConstant.CmpLeftElm)?GlobalVariable.getLeftCmpsource():GlobalVariable.getRightCmpsource();
			StringBuffer pk = new StringBuffer();
			  for(int i=0;i<pks.size();i++)
			  {
				  if(i>0)
					  pk.append("_");
				  
				  String realcolumn=genericCmpDataUtil.getRealColumn(pks.get(i));
					
					ColumnMeta cmp = genericCmpDataUtil.findCmpPair(leftright, realcolumn);
					  if(cmp==null)
					  {
						  //throw exception
					  }
					  int columnidx = rs.findColumn(realcolumn);
					List<String> props = cmp.getProps();
					
					//String o1 = right.getResultSetValue(rs, columnidx);
					//byte []o2 = right.getResultSetByte_ignoreException(rs, columnidx);
					//String value = (String) ExecOnPropMethod_OR.exec(ExecOnPropImpl_setPKDisplay.impls, props, o1, new Object[]{o2});
					  
					if(props==null)
					  {
						  pk.append(JSONHandler.getTrimColumnValueString(src.getResultSetValue(rs, columnidx)));
						  continue;
					  }
					  String value=null;
					  if(props.indexOf("chinese")!=-1)
					  {
						  try {
							  byte []v = rs.getBytes(realcolumn);
							  if(v != null)
								  value = new String(v,"ms950");
							  else
								  value="${NULL}";
							//pk.append(new String(rs.getBytes(realcolumn),"ms950"));
						} catch (UnsupportedEncodingException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					  }
					  else
						  value=src.getResultSetValue(rs, columnidx);
					  
					  if(props.indexOf("untrim")!=-1)
					  {
						  value = JSONHandler.getColumnValueString(value);
					  }
					  else
						  value=JSONHandler.getTrimColumnValueString(value);
					  pk.append(value);
			  }
			  return pk;
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
		public static boolean customrightcountref=false;

		public static int strategy=1;//1 for basic(10萬 15.99s)   2 for duplicate(10萬16.45s)   3 for countright(10萬 35.73s)

		public static PrintWriter errpk;
		public static PrintWriter errpk_summary;
		public static Cmpsummary AllSummary = new Cmpsummary();

		public static int threadcnt=8;
		public static boolean isExactEqual=true;//true for StringEuqal   false for StringEuqalNotExact
		//public static boolean isForceExit=true;//true for 錯太多中斷   false for 跑到比完

		static CmpTask [] cmptasks;
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
		
		//public static int dbgmode=0;//0 no dbg 1 dbg
		
		//public static String logformat;//${left.offcode}|${left.equipnum}|${right.equipoffcode}|${right.equipnumber}|#caseconstant('m3', 'm1', 'm2', 'm4')|${tablename}|#columnname(1)|${leftvalue}| ${rightvalue}| #pk(',')
		//public static String logformatsplitter=",";
		public static int resultsetsize=0;
		
		static PKDisplay pkdisplay = new PKDisplay()
		 {
			public StringBuffer Display(List<String> pks, TreeMap<String,Object> rs, String splitter)
			{
				return subcompare_setPKDisplay(pks, rs, splitter);
			}
		 };
		
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
				CheckInterfaceScript<VarScript> vsimpl = CheckScript.CheckInterfaceScriptFactory("varscript", getelmvalue(vs,"script"), null, VarScript.class, null, cmpmain.errpk, cmpmain.threadcnt);
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
				cmpmain.case2lazyInit=true;
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
				cmpmain.case3lazyInit=true;
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
				if(GlobalVariable.dbgmode==1)
					System.out.println(script);
				try {
					cp.setkeytransformScript(script, null, getRefs(elmcolext, "ref"), cmpmain.errpk, cmpmain.threadcnt);
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
				/*
				if(leftname == null)
					throw new genericCmpDataException("cmpdata004", "colext/col/left undefined");
				
				if(rightname == null)
					throw new genericCmpDataException("cmpdata004", "colext/col/right undefined");
				
				CmpPair cp = null;
				//由left尋找 導致left欄位不能重複被引用，重複引用會找錯
				//String selm = getelmvalue(e, "left");
				ColumnMeta leftColumnMeta = ColumnMeta.getLeftColumn(leftname);
				ColumnMeta rightColumnMeta = ColumnMeta.getRightColumn(rightname);
				
				if(leftColumnMeta == null || rightColumnMeta == null)
					throw new genericCmpDataException("cmpdata004", "xxxcol tag defined error:"+leftname);
				
				cp = new CmpPair(leftColumnMeta,rightColumnMeta, genericCmpData.threadcnt);
				cp.setName(name);
				*/
				CmpPair cp = CmpPair.factory(name, leftname, rightname, cmpmain.threadcnt);
				
				String rselm = getelmvalue(e, "rightprop");
				String lselm = getelmvalue(e, "leftprop");
				cp.setProps(lselm, rselm, cmpmain.threadcnt);//set StringEuqal algorithm
				
				if(!cmpmain.isExactEqual)
					cp.setNotExactLogic(cmpmain.threadcnt);
				
				String script = getelmvalue(e, "script");
				if(script!=null)
					try {
						script=applySessionVariable(script);
						cp.setCheckColumnValueScript(script, null, getRefs(e, "ref"), cmpmain.errpk, cmpmain.threadcnt);
					} catch (NullPointerException e1) {
						throw new genericCmpDataException("cmpdata003", "col:"+name+" leftcol no definition");
					} catch (Exception e2) {
						e2.printStackTrace();
						throw new genericCmpDataException("cmpdata003", "col:"+name+" script syntax error");
					}
				
				String defaultlogic = getelmvalue(e, "defaultlogic");
				if(defaultlogic!=null)
				{
					cp.setCheckColumnValueScript(CmpLogicDefaultImpl.GetDefaultImpl(defaultlogic), 
												 getDefaultLogicVars(getelmvalue(e, "defaultlogicvars")),
												 getRefs(e, "ref"),
												 cmpmain.threadcnt);
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
			
			String dbg = (elmcolext.getAttribute("dbg")==null)?null:elmcolext.getAttribute("dbg").getValue();
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
					if(dbg!=null)
						ref.put("dbg",dbg);
					else
						ref.put("dbg","NO");
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
				GlobalVariable.logformat= elmpattern;
			}
			
			String elmsplitter = getelmvalue(elmlog,"splitter");
			if(elmsplitter != null)
			{
				GlobalVariable.logformatsplitter= elmsplitter;
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
			GlobalVariable.getRightCmpsource().setFirst(sstmt, 1);
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
		
		private static void setExtraCmpPair(List<String> leftpk, List<String> rightpk)
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
					
					cp = new CmpPair(leftColumnMeta,rightColumnMeta, cmpmain.threadcnt);
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
						cp = new CmpPair(leftColumnMeta,rightColumnMeta, cmpmain.threadcnt);
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
		        
		        selm=getelmvalue(root_elm, "init");
		        if(selm !=null)
		        	init = Integer.parseInt(selm);
		        
		        selm=getelmvalue(root_elm, "thread");
		        if(selm !=null)
		        	threadcnt = Integer.parseInt(selm);
		        
		        selm=getelmvalue(root_elm, "fetchsize");
		        if(selm !=null)
		        	GlobalVariable.setFetchsize(Integer.parseInt(selm));
		        
		        selm=getelmvalue(root_elm, "cmpunitcnt");
		        if(selm !=null)
		        	GlobalVariable.setCmpunitcnt(Integer.parseInt(selm));
		        
		        selm=getelmvalue(root_elm, "exactequal");
		        if(selm !=null)
		        {
		        	if(selm.equals("NO"))
		        		cmpmain.isExactEqual=false;
		        }
		        
		        selm=getelmvalue(root_elm, "forceexit");
		        if(selm !=null)
		        {
		        	if(selm.equals("NO"))
		        		GlobalVariable.isForceExit=false;
		        }
		        GlobalVariable.ForceExitimpl = new GlobalVariable.ForceExit()
		        {
		        	public void ForceExit()
		        	{
		        		cmpmain.ForceExit();
		        	}
		        };
		        
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
		        
		        elm=getelm(root_elm, "rightsqldef");
		        if(elm !=null)
		        {
		        	GlobalVariable.getRightCmpsource().range = Range_input.parseXml(elm);
		        }
		        
		        selm=getelmvalue(root_elm, "resultsetsize");
		        if(selm !=null)
		        {
		        	resultsetsize=Integer.parseInt(selm);
		        }
		        
		        GlobalVariable.setSessionVariable(getVariable(root_elm, "variables"));
		        
		        getCase2check(root_elm, "case2check");
		        getCase3check(root_elm, "case3check");
		        
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
		        
		        selm=getelmvalue(root_elm, "leftcheckduplicatesql");
		        if(selm !=null)
		        	GlobalVariable.getLeftCmpsource().checkduplicatesql=selm;
		        
		        selm=getelmvalue(root_elm, "rightcheckduplicatesql");
		        if(selm !=null)
		        	GlobalVariable.getRightCmpsource().checkduplicatesql=selm;
		        
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
		        
		        setExtraCmpPair(GlobalVariable.getLeftCmpsource().pk, GlobalVariable.getRightCmpsource().pk);
		        
		        
		        selm=getelmvalue(root_elm, "leftsqlcountcolumn");
		        if(selm !=null)
		        	GlobalVariable.getLeftCmpsource().sqlcountcolumn=selm;
		        
		        selm=getelmvalue(root_elm, "rightsqlcountcolumn");
		        if(selm !=null)
		        	GlobalVariable.getRightCmpsource().sqlcountcolumn=selm;
		        
		        if(GlobalVariable.getLeftCmpsource().sqlcountcolumn!=null && GlobalVariable.getRightCmpsource().sqlcountcolumn!=null)
		        {
		        	List<String> left = new ArrayList<String>();
		        	List<String> right = new ArrayList<String>();
		        	left.add(GlobalVariable.getLeftCmpsource().sqlcountcolumn);
		        	right.add(GlobalVariable.getRightCmpsource().sqlcountcolumn);
		        	setExtraCmpPair(left, right);
		        }
		        
		        
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
				
				selm=getelmvalue(root_elm, "strategy");
		        if(selm ==null)
		        {
		        	strategy=1;
		        }
		        else
		        {
		        	strategy=getStartegy(selm);
		        }
				
				selm=getelmvalue(root_elm, "rightcountref");
		        if(selm !=null)
		        {
		        	rightcountref.append(selm);
		        	customrightcountref = true;
		        }
		        else
		        {
			        if(GlobalVariable.getLeftCmpsource().getMainsqlsb().indexOf("file://")!=-1)
					{
						String leftmainsqlfile = GlobalVariable.getLeftCmpsource().getMainsqlsb().toString().substring("file://".length());
						rightcountref.append(leftmainsqlfile+"right");
					}
			        else
			        {
			        	
			        }
		        }
		        
		        selm=getelmvalue(root_elm, "rightcountref_usegroupby");
		        if(selm !=null)
		        {
		        	if(selm.equals("YES"))
		        		GlobalVariable.getRightCmpsource().rightcountref_usegroupby=true;
		        	else
		        		GlobalVariable.getRightCmpsource().rightcountref_usegroupby=false;
		        }
		        
		        selm=getelmvalue(root_elm, "rightcountscript");
		        if(selm !=null)
		        {
		        	GlobalVariable.getRightCmpsource().setCountscript(selm, cmpmain.errpk, cmpmain.threadcnt);
		        }
		        
		        selm=getelmvalue(root_elm, "leftFirstUnionSelect");
		        if(selm !=null)
		        {
		        	if(selm.equals("YES"))
		        		GlobalVariable.getLeftCmpsource().usefirstunionselect=true;
		        	else
		        		GlobalVariable.getLeftCmpsource().usefirstunionselect=false;
		        }
		        
		        selm=getelmvalue(root_elm, "leftduplicatepk");
		        if(selm !=null)
		        {
		        	GlobalVariable.getLeftCmpsource().duplicatepk = Arrays.asList(selm.split(","));
		        }
		        /*
		        selm=getelmvalue(root_elm, "rightfirstsql");
		        if(selm ==null)
		        {
		        	if(strategy==3)
		        		throw new cmpdataexception("cmpdata004", "countright strategy should define rightfirstsql");
		        	selm="";
		        }
		        rightfirstsql.append(selm);
		        */

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
							cmpmain.firstNowStmt[0] = Parser.parseStatement(firstNowSql[0]);
							cmpmain.firstNowStmt[1] = Parser.parseStatement(firstNowSql[1]);
							firstNowSql[0] = PathManipulation.filterSQL(firstNowSql[0]);
							pstmtrrs = conn.prepareStatement(firstNowSql[0]);
						}
						else
						{
							String []firstNowSql = new String[2];
							firstNowSql[0]=applySessionVariable(sqltest[sqltestcnt++]);
							firstNowSql[1]=applySessionVariable(GlobalVariable.getRightCmpsource().getMainsqlsb().toString());
							cmpmain.firstNowStmt[0] = Parser.parseStatement(firstNowSql[0]);
							cmpmain.firstNowStmt[1] = Parser.parseStatement(firstNowSql[1]);
							pstmtrrs = conn.prepareStatement(firstNowSql[0]);
						}
					}
					else
					{
						String []firstNowSql = new String[2];
						firstNowSql[0] = GlobalVariable.getLeftCmpsource().range.getSchemaString();
						firstNowSql[1] = GlobalVariable.getRightCmpsource().range.getSchemaString();
						firstNowSql[0]=applySessionVariable(firstNowSql[0]);
						firstNowSql[1]=applySessionVariable(firstNowSql[1]);
						cmpmain.firstNowStmt[0] = Parser.parseStatement(firstNowSql[0]);
						cmpmain.firstNowStmt[1] = Parser.parseStatement(firstNowSql[1]);
						firstNowSql[0] = PathManipulation.filterSQL(firstNowSql[0]);
						pstmtrrs = conn.prepareStatement(firstNowSql[0]);
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
					
					if(!rightcountref.equals(""))
						sqlscriptfilerightcnt = new BufferedReader(new FileReader(rightcountref.toString()));
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
					if(!rightcountref.equals(""))
			    		  sqlscriptfilerightcnt.close();
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
		
					if(!rightcountref.equals(""))
						ret[1] = sqlscriptfilerightcnt.readLine();
					//ScriptAPI.errpk_println(ret);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else
			{
				ret[0] = GlobalVariable.getLeftCmpsource().range.getNextRangeString();
				if(!rightcountref.equals(""))
					ret[1] = GlobalVariable.getRightCmpsource().range.getNextRangeString();
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
			new com.qa.log.errpk(errpk, errpk_summary);
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
					GlobalVariable.dbgmode=1;
				else if(args[2].equals("1000"))
					GlobalVariable.dbgmode=1000;
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
			Thread []genericCmpDataThread = new Thread[threadcnt];
			for(int ii=0;ii<threadcnt;ii++)
	   	  	{
				genericCmpDataThread[ii] = new Thread(new cmpmain(ii));
		   		genericCmpDataThread[ii].start();
			}
			
			for(int ii=0;ii<threadcnt;ii++)
			{
				while(cmptasks[ii]==null || genericCmpDataThread[ii]==null)
				{
					Thread.sleep(2000);
				}
			}
			
			return genericCmpDataThread;
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
		  	  ScriptAPI.summary_println("leftcount:"+AllSummary.leftcount+" rightcount:"+AllSummary.rightcount);
		  	  ScriptAPI.summary_println("case1bcount:"+AllSummary.case1bcount+" case2count:"+AllSummary.case2count+" case3count:"+AllSummary.case3count+" case4count:"+AllSummary.case4count+" case2rechckscriptcount:"+AllSummary.case2rechckscriptcount);
		  	  ScriptAPI.summary_println("error column summary:");
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
			initRightFirstSql();
			format = RstFormat.initMeta("1", GlobalVariable.getLeftCmpsource().pk, GlobalVariable.getRightCmpsource().pk);
			//todo for transform
			init_getMeta();
			int i = GlobalVariable.getrrsc();

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



