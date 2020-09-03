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
	nowsql�ثe�T�w�Ψ��string
	nowsql[0]��mgetresultset(left)�d�߭n�ϥΪ�sql
	nowsql[1]��m�p��right��ƪ�sql�A�osql�Φb��Ӧa��
	   A: basic�Ҧ��ɡA��checkRightcnttable�֭pright����ơA�Y�ǯS��ݨD(ex:service M �@����h��)�����A�|�n�d�Ugroup by���ԭz
	   B: Countright�Ҧ��ɡA��insertrightcnttable���o�C�@��pk�����
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
	//��part ��� leftcount�Mrightcount��Ƥ��šAcountright�N�Q�]��true�A��part�Hcountright���s���
	//����e�A���I�srollbackAllsummary countrightmode=true; ������諸partsummary���g���G�C
	//�A����粒����Npartsummary���G�g�J�A�æbnote�����O�A��ﵲ���� countrightmode�]��false
	
	private Cmpsummary summary;
	public Cmpsummary getsummary()
	{
		return summary;
	}
	private Cmpsummary allsummary;//�֭p�ƶq
	public Cmpsummary getallsummary()
	{
		return allsummary;
	}
	private int allsummaryerror=0;//�֭pallsummary��case1 case2 case3�����~�A�W�Lthreashold�פ�{�� checkErrorThreashold
	private static final int errorthreadshold=10000;
	//int nowsqlcnt;
	//int nowsqlrightcnt;//�s�t�θ�Ƽ�
	//int nowsqlcmpproblemcnt;
	//int nowsqlfindnorightcnt;//�¨t�Φ�  �s�t�ΨS�� case3
	//int nowsqlrightcntproblem;//����ӼƤ��@ case4
	int nowsqlduplicateright;//no use debug
	//int nowsqlfindnoleft;//�s�t�Φ��¨t�ΨS�� case2
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
	{//�¦��s�S��
		summary.case2count++;
		allsummary.case2count++;
		checkErrorThreashold();
	}
	
	public void nowsqlfindnorightcnt_plus(int cnt)
	{//�¦��s�S��
		summary.case2count+=cnt;
		allsummary.case2count+=cnt;
		checkErrorThreashold();
	}
	
	public void nowsqlfindnorightrechckscripcnt_plus()
	{//�¦��s�S��
		summary.case2rechckscriptcount++;
		allsummary.case2rechckscriptcount++;
		allsummaryerror--;
	}
	
	public void nowsqlfindnorightrechckscripcnt_plus(int cnt)
	{//�¦��s�S��
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
	{//�s���¨S��
		summary.case3count++;
		allsummary.case3count++;
		checkErrorThreashold();
	}
	
	public void nowsqlfindnoleft_plus(int cnt)
	{//�s���¨S��
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
