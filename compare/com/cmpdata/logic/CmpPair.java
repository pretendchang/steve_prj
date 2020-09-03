package com.cmpdata.logic;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.script.Invocable;

import com.cmpdata.datatype.DatatypeDefinition;
import com.cmpdata.dbvendor.DBInterface;
import com.cmpdata.scripting.CheckInterfaceScript;
import com.cmpdata.scripting.CheckScript;
import com.sqlparse.SQLExpression;
import com.sqlparse.SQLOrderBy;

import com.cmpdata.ColumnMeta;
import com.cmpdata.genericCmpDataConstant;
import com.cmpdata.genericCmpDataException;


public class CmpPair {
	//private static List<CmpPair> lcmppair = new ArrayList<CmpPair>();
	//private static Map<ColumnMeta, CmpPair> lleftcmppair = new HashMap<ColumnMeta, CmpPair>();
	//private static Map<ColumnMeta, CmpPair> lrightcmppair = new HashMap<ColumnMeta, CmpPair>();
	private static Map<String, CmpPair> lcmppair = new HashMap<String, CmpPair>();//cp.name當key, 不可重複
	public static void addCmpPair(CmpPair cp)//無法後蓋前
	{
		if(lcmppair.get(cp.name) == null)
			lcmppair.put(cp.name, cp);
		/*
		if(lleftcmppair.get(cp.getLeftcm())==null && lrightcmppair.get(cp.getRightcm())==null)
		{//新定義的cmppair
			lleftcmppair.put(cp.getLeftcm(), cp);
			lrightcmppair.put(cp.getRightcm(), cp);
			
			if(cp.name==null || cp.name.equals(""))//pk欄位沒有定義在colext的情況
				cp.setName("_v_"+cp.getLeftcm().getName());
		}
		else if(lleftcmppair.get(cp.getLeftcm())==null || lrightcmppair.get(cp.getRightcm())==null)
			throw new genericCmpDataException("cmpdata004", "PK defined error");
			*/
	}
	
	public static List<CmpPair> getlcmppair()
	{
		return new ArrayList<CmpPair>(lcmppair.values());
		//return new ArrayList<CmpPair>(lcmppair);
	}
	
	public static CmpPair factory(String name, String leftname, String rightname, int threadcnt)
	{
		if(leftname==null && rightname==null)
		{//不可同時為null
			throw new genericCmpDataException("cmpdata004", "colext/col/left and right undefined");
		}
		ColumnMeta leftColumnMeta = null;
		ColumnMeta rightColumnMeta = null;
		
		if(leftname != null)
			leftColumnMeta = ColumnMeta.getLeftColumn(leftname);
		
		if(rightname != null)
			rightColumnMeta = ColumnMeta.getRightColumn(rightname);
		
		if(leftColumnMeta == null && rightColumnMeta == null)
			throw new genericCmpDataException("cmpdata004", "xxxcol tag defined error:"+name);
		
		CmpPair cp = new CmpPair(leftColumnMeta,rightColumnMeta, threadcnt);
		cp.setName(name);
		
		return cp;
	}
	
	private ColumnMeta leftcm;
	private ColumnMeta rightcm;

	
	//比對
	CheckInterfaceScript<CmpLogic> cmpScript;
	//key轉換
	CheckScript keytransformScript;
	//轉檔
	CheckInterfaceScript<TransLogic> transScript;
	
	public boolean enableCmp=true;
	//public List<String> props;
	public String name="";
	public boolean init = false;
	public boolean lazyinitrefs=false;

	private AtomicInteger errcnt;//統計該欄位錯誤幾筆資料

	private String customtype;

	@SuppressWarnings("unchecked")
	public CmpPair(ColumnMeta c1, ColumnMeta c2, int _threadcnt)
	{
		leftcm = c1;
		rightcm= c2;

		errcnt = new AtomicInteger();
	}
	public CmpPair(String c1, String c2)
	{
		leftcm = new ColumnMeta(c1);
		rightcm= new ColumnMeta(c2);

	}
	/*
	public CmpPair(CmpPair.ColumnMeta c1, CmpPair.ColumnMeta c2, CmpLogic cl)
	{
		leftcm = c1;
		rightcm= c2;
		logic = cl;
	}
	*/
	public List<String> getProps(int leftright)
	{
		if(leftright == 0)
			return leftcm.getProps();
		else
			return rightcm.getProps();
	}
	
	private ColumnMeta getMeta(int leftright)
	{
		if(leftright == 0)
			return leftcm;
		else
			return rightcm;
	}
	
	public void setProps(String _leftprops, String _rightprops, int threadcnt)
	{
		/*
		if(logic != null)
			System.out.println("CmpPair.logic is not null. You have to setprop first then set logic");
			*/
		//設定預設logic
		if(_rightprops!= null && _rightprops.contains("chinese"))
		{
			setCheckColumnValueScript(CmpLogicDefaultImpl.ByteArrayEqual, null, null, threadcnt);
			setTransValueScript(TransLogicDefaultImpl.StringEqual, null, null, threadcnt);
		}
		else
		{
			setCheckColumnValueScript(CmpLogicDefaultImpl.StringEuqal, null, null, threadcnt);
			setTransValueScript(TransLogicDefaultImpl.StringEqual, null, null, threadcnt);
		}
		if(_leftprops!=null)
			leftcm.setProps(_leftprops);
		
		if(_rightprops != null)
		rightcm.setProps(_rightprops);
	}
	
	public void setNotExactLogic(int threadcnt)
	{
		if(rightcm.getProps()!= null && rightcm.getProps().contains("chinese"))
			setCheckColumnValueScript(CmpLogicDefaultImpl.ByteArrayEqual, null, null, threadcnt);
		else
			setCheckColumnValueScript(CmpLogicDefaultImpl.StringEuqalNotExact, null, null, threadcnt);
	}
	
	public void setName(String _name)
	{
		name=_name;
	}
	
	public String toString()
	{
		return new StringBuffer().append("name:").append(name)
								 .append(" ").append("left:").append((leftcm==null)?"":leftcm.getName())
								 .append(" ").append("right:").append((rightcm==null)?"":rightcm.getName()).toString();
	}
	
	public CmpPair find(String _name)
	{
		if(_name.equalsIgnoreCase(name))
			return this;
		if(leftcm!=null && _name.equalsIgnoreCase(leftcm.getName()))
			return this;
		if(rightcm!=null && _name.equalsIgnoreCase(rightcm.getName()))
			return this;
		return null;
	}

	
	public static class CmpRef
	{
		String db;
		String sql;
		HashMap<String, String> refdata=new HashMap<String,String>();
	}
	
	public void addErrcnt()
	{
		errcnt.incrementAndGet();
	}
	
	public int getErrcnt()
	{
		return errcnt.get();
	}
	
	public ColumnMeta getLeftcm()
	{
		return leftcm;
	}
	
	public ColumnMeta getRightcm()
	{
		return rightcm;
	}
	
	public void setkeytransformScript(String script, Object []logicvars, List<TreeMap<String, Object>> refs, PrintWriter writer, int threadcnt)
	{
		keytransformScript = CheckScript.CheckScriptFactory("KeyTransformScript", script, logicvars, refs, writer, threadcnt);
	}
	
	public void setCheckColumnValueScript(String script, Object []logicvars, List<TreeMap<String, Object>> refs, PrintWriter writer, int threadcnt)
	{
		cmpScript = CheckScript.CheckInterfaceScriptFactory("checkcolumnvalue", script, logicvars, CmpLogic.class, refs, writer, threadcnt);
	}
	
	public void setCheckColumnValueScript(CmpLogic logic, Object []logicvars, List<TreeMap<String, Object>> refs, int threadcnt)
	{
		cmpScript = CheckScript.CheckInterfaceScriptFactory("checkcolumnvalue", logic, logicvars, refs, threadcnt);
	}
	
	public List<TreeMap<String, Object>> getCheckColumnValueScriptrefs()
	{
		return cmpScript.getCheckscriptrefs();
	}
	
	public CmpLogic getCheckColumnValueScript()
	{
		return cmpScript.getInterface();
	}
	
	public String getCheckColumnValueScriptRst(int runid, int leftright)
	{
		return cmpScript.getRst(runid, leftright);
	}
	
	public void clearColumnValueScriptRst(int runid)
	{
		cmpScript.clearRst(runid);
	}
	
	public void setTransValueScript(String script, Object []logicvars, List<TreeMap<String, Object>> refs, PrintWriter writer, int threadcnt)
	{
		transScript = CheckScript.CheckInterfaceScriptFactory("transvaluescript", script, logicvars, TransLogic.class, refs, writer, threadcnt);
	}
	
	public void setTransValueScript(TransLogic logic, Object []logicvars, List<TreeMap<String, Object>> refs, int threadcnt)
	{
		transScript = CheckScript.CheckInterfaceScriptFactory("transvaluescript", logic, logicvars, refs, threadcnt);
	}
	
	public List<TreeMap<String, Object>> getTransValueScriptrefs()
	{
		return transScript.getCheckscriptrefs();
	}
	
	public TransLogic getTransValueScript()
	{
		return transScript.getInterface();
	}
	
	public String getTransValueScriptRst(int runid, int leftright)
	{
		return transScript.getRst(runid, leftright);
	}
	
	public Object [] getTransLogicvars()
	{
		return transScript.getLogicvars();
	}
	
	public CheckScript getKeytransformScript()
	{
		return this.keytransformScript;
	}
	
	public List<TreeMap<String, Object>> getKeytransformScriptrefs()
	{
		return keytransformScript.getCheckscriptrefs();
	}
	
	public void setRightCustomType(String _customtype)
	{
		customtype = _customtype;
	}

	
	public Object [] getcmpLogicvars()
	{
		return cmpScript.getLogicvars();
	}
	
	public static void main(String []args)
	{
		ColumnMeta left = new ColumnMeta("a");
		ColumnMeta right = new ColumnMeta("b");
		
		CmpPair cp = new CmpPair(left,right, 1);
		cp.enableCmp=false;
		cp.setName("aaa");
		CmpPair.addCmpPair(cp);
		//cp.setName("aaa");
		System.out.println(CmpPair.getlcmppair());
		CmpPair cp1 = new CmpPair(left,right, 1);
		CmpPair.addCmpPair(cp1);
		System.out.println(CmpPair.getlcmppair());
	}
}
