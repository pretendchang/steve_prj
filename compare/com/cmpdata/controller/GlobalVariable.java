package com.cmpdata.controller;

import java.io.BufferedReader;
import java.io.PrintWriter;

import java.util.HashMap;

import java.util.TreeMap;

import com.cmpdata.rst.Cmpsummary;
import com.cmpdata.rst.RstFormat;
import com.cmpdata.scripting.CheckScript;
import com.cmpdata.transform.transform_controller;
import com.sqlparse.SQLStatement;

import com.cmpdata.CmpTask;
import com.cmpdata.Cmpsource;



public class GlobalVariable {
	private static TreeMap<String, String> SessionVariable;
	public static TreeMap<String, String> getSessionVariable()
	{
		return SessionVariable;
	}
	public static void setSessionVariable(TreeMap<String, String> _SessionVariable)
	{
		SessionVariable = _SessionVariable;
	}
	
	private static int fetchsize=500;
	public static int getFetchsize()
	{
		return fetchsize;
	}
	public static void setFetchsize(int _fetchsize)
	{
		fetchsize = _fetchsize;
	}
	
	private static int cmpunitcnt=20;
	public static int getCmpunitcnt()
	{
		return cmpunitcnt;
	}
	public static void setCmpunitcnt(int _cmpunitcnt)
	{
		cmpunitcnt = _cmpunitcnt;
	}
	
	public static int rrsc=0;
	public static int getrrsc()
	{
		return rrsc;
	}
	public synchronized static void plusrrsc(int plus)
	{
		rrsc += plus;
	}
	
	public static BufferedReader rrs_br;
	
	private static Cmpsource left = null;
	public static Cmpsource getLeftCmpsource()
	{
		return left;
	}
	public static void setLeftCmpsource(Cmpsource src)
	{
		left = src;
	}
	private static Cmpsource right = null;
	public static Cmpsource getRightCmpsource()
	{
		return right;
	}
	public static void setRightCmpsource(Cmpsource src)
	{
		right = src;
	}
	
	private static String leftCharset="UTF-8";
	public static String getLeftCharset()
	{
		return leftCharset;
	}
	public static void setLeftCharset(String charset)
	{
		leftCharset = charset;
	}
	private static String rightCharset="MS950";
	public static String getRightCharset()
	{
		return rightCharset;
	}
	public static void setRightCharset(String charset)
	{
		rightCharset = charset;
	}
	
	private static transform_controller tcontroller = null;
	public static void setController(transform_controller _controller)
	{
		tcontroller = _controller;
	}
	public static transform_controller getTController()
	{
		return tcontroller;
	}
	public static StringBuffer rightcountref = new StringBuffer();
	public static StringBuffer rightfirstsql = new StringBuffer();
	public static String rightcountrefidx;
	public static boolean customrightcountref=false;

	public static int strategy=1;//1 for basic(10�U 15.99s)   2 for duplicate(10�U16.45s)   3 for countright(10�U 35.73s)

	public static PrintWriter errpk;
	public static PrintWriter errpk_summary;
	public static Cmpsummary AllSummary = new Cmpsummary();

	public static int threadcnt=8;
	public static boolean isExactEqual=true;//true for StringEuqal   false for StringEuqalNotExact
	public static boolean isForceExit=true;//true for ���Ӧh���_   false for �]���
	public interface ForceExit
	{
		void ForceExit();
	}
	public static ForceExit ForceExitimpl;
	public static void ForceExit()
	{
		ForceExitimpl.ForceExit();
	}

	static CmpTask [] cmptasks;
	public static HashMap<String,CmpTask> threadtaskmap = new HashMap<String,CmpTask>();
	public static HashMap<String,Integer> threadrunidmap = new HashMap<String,Integer>();
	public static int init=0;
	
	public static BufferedReader sqlscriptfile;
	public static BufferedReader sqlscriptfilerightcnt;
	
	public static int cmptype=1;
	//1 is default db vs. db   
	//2. db value verification  db(with script) �]�w�ɨS�� rightdb, rightpk, rightcol
	//3. db vs. file leftdb�H "file://"�_�Y 
	
	public static RstFormat format;
	
	
	public static SQLStatement []firstNowStmt = new SQLStatement[2];
	//case2 case3 refs ���Υ[left right�Aref�@�ߥ[�bidx=0���a��
	//public static Invocable case2checkscript;
	//case2���~���[�����, ��o�{case2���D�A�A�[�禹script�T�{�O�_���T�A
	//�Y�����T�A�p�Ƥ��M�|�N�����p�J(�׶}checkbalance�����D)�A���|�A��rlog���O�L���D�A�åB���N�����~detail�g�Jlog
	private static CheckScript case2checkScript;
	//public static List<TreeMap<String, Object>> case2checkscriptrefs;
	private static boolean case2lazyInit=false;

	//public static Invocable case3checkscript;
	//case3���~���[�����, ��o�{case3���D�A�A�[�禹script�T�{�O�_���T�A
	protected static CheckScript case3checkScript;
	//public static List<TreeMap<String, Object>> case3checkscriptrefs;//�u��reference right pk
	protected static boolean case3lazyInit=false;
	
	public static int dbgmode=0;//0 no dbg 1 dbg
	
	//public static String customloginfo;//log���~�T�����e�m��T  left.equipoffcode, left.equipnumber, right.equipoffcode, right.equipnumber,
	public static String logformat;//${left.offcode}|${left.equipnum}|${right.equipoffcode}|${right.equipnumber}|#caseconstant('m3', 'm1', 'm2', 'm4')|${tablename}|#columnname(1)|${leftvalue}| ${rightvalue}| #pk(',')
	public static String logformatsplitter=",";
	public static int resultsetsize=0;
	
}
