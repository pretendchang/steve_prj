package com.cmpdata;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import com.cmpdata.controller.GlobalVariable;
import com.cmpdata.rst.Cmpsummary;
import com.steve.db.data.manager.DSManager;

import com.cmpdata.logic.ScriptAPI;
public class CmpTask {
	public Object conn;//left conn
	public Object getConn()
	{
		return conn;
	}
	
	public PreparedStatement pstmt;
	public PreparedStatement getPstmt()
	{
		return pstmt;
	}
	public ResultSet rs;
	public ResultSet getRs()
	{
		return rs;
	}
	public void setRs(ResultSet _rs)
	{
		rs = _rs;
	}
	public String []nowsql;
	/*
	nowsql目前固定用兩個string
	nowsql[0]放置getresultset(left)查詢要使用的sql
	nowsql[1]放置計算right比數的sql，這sql用在兩個地方
	   A: basic模式時，於checkRightcnttable累計right的比數，某些特殊需求(ex:service M 一筆對多筆)的比對，會要留下group by的敘述
	   B: Countright模式時，於insertrightcnttable取得每一個pk的比數
	*/
	//public HashSQLStatement hashSqlStatement;
	//public SQLStatement []nowstmt;
	public String nowrightcntsql;
	private AtomicInteger state = new AtomicInteger();
	public int getState()
	{
		return state.get();
	}
	public synchronized void setState(int s)
	{
		state.set(s);
	}
	public boolean countrightmode;//0 is default   1 is in countright mode  
	//當part 比對 leftcount和rightcount比數不符，countright將被設成true，該part以countright重新比對
	//重比前，先呼叫rollbackAllsummary countrightmode=true; 本次比對的partsummary不寫結果。
	//再次比對完成後將partsummary結果寫入，並在note欄位註記，比對結束後 countrightmode設為false
	
	private Cmpsummary summary;
	public Cmpsummary getsummary()
	{
		return summary;
	}
	private Cmpsummary allsummary;//累計數量
	public Cmpsummary getallsummary()
	{
		return allsummary;
	}
	private int allsummaryerror=0;//累計allsummary的case1 case2 case3的錯誤，超過threashold終止程式 checkErrorThreashold
	private static final int errorthreadshold=10000;
	//int nowsqlcnt;
	//int nowsqlrightcnt;//新系統資料數
	//int nowsqlcmpproblemcnt;
	//int nowsqlfindnorightcnt;//舊系統有  新系統沒有 case3
	//int nowsqlrightcntproblem;//兩邊個數不一 case4
	int nowsqlduplicateright;//no use debug
	//int nowsqlfindnoleft;//新系統有舊系統沒有 case2
	/*
	public String tableindex;
	
	public String rightcnttablename;
	public String rightcntdburl;
	
	public StringBuffer leftprevinfo;
	public StringBuffer leftnowinfo;
	public StringBuffer rightprevinfo;
	public StringBuffer rightnowinfo;
	*/
	public List<List<Object>> RstWritingPartSummary;
	public List<List<Object>> RstWritingRst;
	public List<List<Object>> RstWritingRaw;

	public CmpTask(List<String> lpk, String dburl)/*, String rightcntdburl, String _rightcnttablename, String referencesql)*/
	{
		conn=DSManager.getInstance().getConn(dburl);
		state.set(0);countrightmode=false;
		summary = new Cmpsummary();
		allsummary = new Cmpsummary();
		//nowstmt = new SQLStatement[2];

		/*
		SQLExpression pkwhereptn = controller.subcompare_buildPKWherePtn(right.pk, genericCmpDataConstant.CmpRightElm,1);
  	  	hashSqlStatement = HashSQLStatement.buildHashSQLStatement(right.getMainsqlsb().toString(), pkwhereptn);
  	  	List<SQLOrderBy> pkorder = controller.subcompare_setPKOrder_jsql(right.pk, genericCmpDataConstant.CmpRightElm);
  	  	Builder.addOrderby(hashSqlStatement.getStmt(), pkorder);
		
		hashSqlStatement = HashSQLStatement.buildHashSQLStatement(right.getMainsqlsb().toString(), pkwhereptn);
		setZeroNowSql();
		
		leftprevinfo = new StringBuffer();
		leftnowinfo  = new StringBuffer();
		rightprevinfo= new StringBuffer();
		rightnowinfo = new StringBuffer();
		*/
		RstWritingPartSummary = new ArrayList<List<Object>>();
		RstWritingRaw = new ArrayList<List<Object>>();
		RstWritingRst = new ArrayList<List<Object>>();

		//initRightcnttable(lpk, rightcntdburl, _rightcnttablename, referencesql);
	}
	/*
	public CmpTask(List<String> lpk, String dburl, transform_controller _controller)
	{
		conn=DSManager.getInstance().getConn(dburl);
		state.set(0);countrightmode=false;
		summary = new Cmpsummary();
		allsummary = new Cmpsummary();
		//nowstmt = new SQLStatement[2];
		
		//SQLExpression pkwhereptn = tcontroller.subcompare_buildPKWherePtn(right.pk, genericCmpDataConstant.CmpRightElm,1);
  	  	//hashSqlStatement = HashSQLStatement.buildHashSQLStatement(right.getMainsqlsb().toString(), pkwhereptn);
  	  	//List<SQLOrderBy> pkorder = controller.subcompare_setPKOrder_jsql(right.pk, genericCmpDataConstant.CmpRightElm);
  	  	//Builder.addOrderby(hashSqlStatement.stmt, pkorder);
		
		//hashSqlStatement = HashSQLStatement.buildHashSQLStatement(right.getMainsqlsb().toString(), pkwhereptn);
		setZeroNowSql();
		
		leftprevinfo = new StringBuffer();
		leftnowinfo  = new StringBuffer();
		rightprevinfo= new StringBuffer();
		rightnowinfo = new StringBuffer();
		
		RstWritingPartSummary = new ArrayList<List<Object>>();
		RstWritingRaw = new ArrayList<List<Object>>();
		RstWritingRst = new ArrayList<List<Object>>();

		//initRightcnttable(lpk, rightcntdburl, _rightcnttablename, referencesql);
	}
	*/
	public void setZeroNowSql()
	{
		summary.leftcount = summary.case1bcount = summary.case3count = summary.case4count = nowsqlduplicateright=summary.case2count=summary.rightcount=0;
		summary.begintime=new java.util.Date();
	}
	
	public void nowsqlcnt_plus()
	{
		summary.leftcount++;
		allsummary.leftcount++;
	}
	
	public void nowsqlcnt_plus(int cnt)
	{
		summary.leftcount+=cnt;
		allsummary.leftcount+=cnt;
	}
	
	public void nowsqlcmpproblemcnt_plus()
	{
		summary.case1bcount++;
		allsummary.case1bcount++;
		checkErrorThreashold();
	}
	
	public void nowsqlfindnorightcnt_plus()
	{//舊有新沒有
		summary.case2count++;
		allsummary.case2count++;
		checkErrorThreashold();
	}
	
	public void nowsqlfindnorightcnt_plus(int cnt)
	{//舊有新沒有
		summary.case2count+=cnt;
		allsummary.case2count+=cnt;
		checkErrorThreashold();
	}
	
	public void nowsqlfindnorightrechckscripcnt_plus()
	{//舊有新沒有
		summary.case2rechckscriptcount++;
		allsummary.case2rechckscriptcount++;
		allsummaryerror--;
	}
	
	public void nowsqlfindnorightrechckscripcnt_plus(int cnt)
	{//舊有新沒有
		summary.case2rechckscriptcount+=cnt;
		allsummary.case2rechckscriptcount+=cnt;
		allsummaryerror--;
	}
	
	public void nowsqlrightcntproblem_plus(int difference)
	{
		summary.case4count+=difference;
		allsummary.case4count+=difference;
		checkErrorThreashold();
	}
	
	public void nowsqlduplicateright_plus()
	{
		nowsqlduplicateright++;
	}
	
	public void nowsqlfindnoleft_plus()
	{//新有舊沒有
		summary.case3count++;
		allsummary.case3count++;
		checkErrorThreashold();
	}
	
	public void nowsqlfindnoleft_plus(int cnt)
	{//新有舊沒有
		summary.case3count+=cnt;
		allsummary.case3count+=cnt;
		checkErrorThreashold();
	}
	
	public void rollbackAllSummary()
	{//case1 case2 doesn't checked again in countright mode
		allsummary.leftcount-=summary.leftcount;
		allsummary.rightcount-=summary.rightcount;
		//allsummary.case1bcount-=summary.case1bcount;
		//allsummary.case2count-=summary.case2count;
		allsummary.case3count-=summary.case3count;
		allsummary.case4count-=summary.case4count;
		
		//allsummaryerror-=summary.case1bcount;
		//allsummaryerror-=summary.case2count;
		allsummaryerror-=summary.case3count;
		allsummaryerror-=summary.case4count;
				
	}
	
	private void checkErrorThreashold()
	{
		if(GlobalVariable.isForceExit)
		{
			allsummaryerror++;
			if(allsummaryerror >=errorthreadshold)
			{
				ScriptAPI.errpk_println("Too many errors exit program forcily");
				GlobalVariable.ForceExit();
				//System.exit(1);
			}
		}
	}
}
