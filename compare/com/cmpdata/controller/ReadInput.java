package com.cmpdata.controller;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import org.jdom2.Element;

import com.cmpdata.Cmpsource;
import com.cmpdata.ColumnMeta;
import com.cmpdata.genericCmpData;
import com.cmpdata.genericCmpDataConstant;
import com.cmpdata.genericCmpDataException;
import com.cmpdata.range.Range_input;
import com.cmpdata.rst.RstWriting;
import com.cmpdata.scripting.CheckScript;
import com.security.PathManipulation;
import com.steve.db.data.manager.DBManagerRuntimeException;

import com.cmpdata.logic.CmpLogicDefaultImpl;
import com.cmpdata.logic.CmpPair;
import com.cmpdata.logic.ScriptAPI;
import com.cmpdata.logic.VarScript;

public class ReadInput {
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
	
	
	public static void readinput(String file) throws IOException
	{
		file = PathManipulation.filterFilePath(file);
		BufferedReader br = new BufferedReader(new FileReader(file));
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
	        		genericCmpData.isExactEqual=false;
	        }
	        
	        selm=getelmvalue(root_elm, "forceexit");
	        if(selm !=null)
	        {
	        	if(selm.equals("NO"))
	        		genericCmpData.isForceExit=false;
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
	        	left = new Cmpsource(genericCmpDataConstant.CONSTANT_DBVENDOR_ORACLE);
	        }
	        else
	        {
	        	left = new Cmpsource(selm);
	        }
	        
	        selm=getelmvalue(root_elm, "rightdbtype");
	        if(selm ==null)
	        {
	        	System.out.println("rightdbtype undefined set rightdbtype=informix");
	        	right = new Cmpsource(genericCmpDataConstant.CONSTANT_DBVENDOR_INFORMIX);
	        }
	        else
	        {
	        	right = new Cmpsource(selm);
	        }
	        
	        
	        elm=getelm(root_elm, "leftsqldef");
	        if(elm !=null)
	        {
	        	left.range = Range_input.parseXml(elm);
	        }
	        
	        elm=getelm(root_elm, "rightsqldef");
	        if(elm !=null)
	        {
	        	right.range = Range_input.parseXml(elm);
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
	        left.dbconn=applySessionVariable(selm);
	        
	        selm=getelmvalue(root_elm, "rightdb");
	        if(selm ==null)
	        {
	        	cmptype=2;
	        }
	        else
	        	right.dbconn=applySessionVariable(selm);
	        
	        selm=getelmvalue(root_elm, "lefttitle");
	        if(selm ==null)
	        {
	        	left.setTable("");
	        }
	        else
	        	left.setTable(applySessionVariable(selm));
	        
	        selm=getelmvalue(root_elm, "righttitle");
	        if(selm ==null)
	        {
	        	right.setTable("");
	        }
	        else
	        	right.setTable(applySessionVariable(selm));
	        
	        selm=getelmvalue(root_elm, "leftsqlcountcolumn");
	        if(selm !=null)
	        	left.sqlcountcolumn=selm;
	        
	        selm=getelmvalue(root_elm, "rightsqlcountcolumn");
	        if(selm !=null)
	        	right.sqlcountcolumn=selm;
	        
	        selm=getelmvalue(root_elm, "leftcheckduplicatesql");
	        if(selm !=null)
	        	left.checkduplicatesql=selm;
	        
	        selm=getelmvalue(root_elm, "rightcheckduplicatesql");
	        if(selm !=null)
	        	right.checkduplicatesql=selm;
	        
	        
	        selm=getelmvalue(root_elm, "leftpk");
	        if(selm ==null)
	        {
	        	throw new genericCmpDataException("cmpdata004", "leftpk undefined");
	        }
	        left.pk=new ArrayList<String>(Arrays.asList(selm.split(",")));
	        
	        selm=getelmvalue(root_elm, "rightpk");
	        if(selm ==null)
	        {
	        	cmptype=2;
	        }
	        else
	        	right.pk=new ArrayList<String>(Arrays.asList(selm.split(",")));
	        
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
	        
	        selm=getelmvalue(root_elm, "leftsql");
	        if(selm ==null)
	        {
	        	throw new genericCmpDataException("cmpdata004", "leftsql undefined");
	        }
			left.getMainsqlsb().append(applySessionVariable(selm));
			
			selm=getelmvalue(root_elm, "rightsql");
	        if(selm ==null)
	        {
	        	cmptype=2;
	        	selm="";
	        }
			right.getMainsqlsb().append(applySessionVariable(selm));
			
			selm=getelmvalue(root_elm, "strategy");
	        if(selm ==null)
	        {
	        	strategy=1;
	        }
	        else
	        {
	        	strategy=getStartegy(selm);
	        }
	        selm=getelmvalue(root_elm, "rightcountrefidx");
	        if(selm ==null)
	        {
	        	selm="";
	        }
	        rightcountrefidx=selm;
			
			selm=getelmvalue(root_elm, "rightcountref");
	        if(selm !=null)
	        {
	        	rightcountref.append(selm);
	        	customrightcountref = true;
	        }
	        else
	        {
		        if(left.getMainsqlsb().indexOf("file://")!=-1)
				{
					String leftmainsqlfile = left.getMainsqlsb().toString().substring("file://".length());
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
	        		right.rightcountref_usegroupby=true;
	        	else
	        		right.rightcountref_usegroupby=false;
	        }
	        
	        selm=getelmvalue(root_elm, "rightcountscript");
	        if(selm !=null)
	        {
	        	right.countscript = ScriptAPI.EvalSourceCountScript(selm);
	        }
	        
	        selm=getelmvalue(root_elm, "leftFirstUnionSelect");
	        if(selm !=null)
	        {
	        	if(selm.equals("YES"))
	        		left.usefirstunionselect=true;
	        	else
	        		left.usefirstunionselect=false;
	        }
	        
	        selm=getelmvalue(root_elm, "leftduplicatepk");
	        if(selm !=null)
	        {
	        	left.duplicatepk = Arrays.asList(selm.split(","));
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
		CheckScript.CheckScriptFactory("case2", script, null, getRefs(elmcolext, "ref"), errpk, threadcnt);
		
		String lazyInit = getelmvalue(elmcolext, "lazyInit");
		if(lazyInit!=null && lazyInit.equals("YES"))
			genericCmpData.case2lazyInit=true;
	}
	public static void getCase3check(Element rootelm, String elmname) throws Exception
	{
		Element elmcolext = rootelm.getChild("case3check");
		if(elmcolext == null)
		{
			return;
		}
		String script = getelmvalue(elmcolext, "script");
		CheckScript.CheckScriptFactory("case3", script, null, getRefs(elmcolext, "ref"), errpk, threadcnt);
		
		String lazyInit = getelmvalue(elmcolext, "lazyInit");
		if(lazyInit!=null && lazyInit.equals("YES"))
			genericCmpData.case3lazyInit=true;
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
				cp.setkeytransformScript(script, null, getRefs(elmcolext, "ref"), genericCmpData.errpk, genericCmpData.threadcnt);
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
			if(leftname == null)
				throw new genericCmpDataException("cmpdata004", "colext/col/left undefined");
			
			String rightname = getelmvalue(e, "right");
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
			
			String rselm = getelmvalue(e, "rightprop");
			String lselm = getelmvalue(e, "leftprop");
			cp.setProps(lselm, rselm, genericCmpData.threadcnt);//set StringEuqal algorithm
			
			if(!genericCmpData.isExactEqual)
				cp.setNotExactLogic(genericCmpData.threadcnt);
			
			String script = getelmvalue(e, "script");
			if(script!=null)
				try {
					script=applySessionVariable(script);
					cp.setCheckColumnValueScript(script, null, getRefs(e, "ref"), genericCmpData.errpk, genericCmpData.threadcnt);
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
											 genericCmpData.threadcnt);
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
				TreeMap<String, VarScript> varscripts = getVarScripts(e);
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
	
	public static TreeMap<String, VarScript> getVarScripts(Element rootelm)
	{
		TreeMap<String, VarScript> varscripts = new TreeMap<String, VarScript>(String.CASE_INSENSITIVE_ORDER);
		List<Element> lvs = rootelm.getChildren("varscript");
		if(lvs.size() == 0)
			return null;
		
		for(Element vs : lvs)
		{
			VarScript vsimpl = ScriptAPI.EvalVarScript(getelmvalue(vs,"script"));
			varscripts.put(getelmvalue(vs,"name"), vsimpl);
		}
		return varscripts;
		
	}
}
