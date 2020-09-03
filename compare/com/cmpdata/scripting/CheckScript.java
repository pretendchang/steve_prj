package com.cmpdata.scripting;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.jdom2.Element;
import org.jdom2.JDOMException;

import com.cmpdata.genericCmpDataConstant;

import com.cmpdata.genericCmpDataUtil;
import com.steve.util.ResourceFileReader;

import jdk.nashorn.api.scripting.JSObject;
import com.cmpdata.logic.CmpLogic;
import com.cmpdata.logic.CmpLogicRst;
import com.cmpdata.logic.ScriptAPI;
import com.cmpdata.logic.VarScript;

public class CheckScript {
		final private static String SCRIPT_TAG_BODY = "BODY";
		private Invocable checkscript;
		private List<TreeMap<String, Object>> checkscriptrefs;
		private String scriptfunctionname;
		private Object []logicvars;
		private PrintWriter writer;
		protected CmpLogicRst []rst;
		
		private static int dbgmode=0;
		
		private static TreeMap<String, String> scriptmap = new TreeMap<String, String>();
		private static TreeMap<String, String> functionnamemap = new TreeMap<String, String>();
		static
		{
			try {
				String str = ResourceFileReader.readResourceFile("com.cmpdata.scripting.CheckScript","CheckScriptImpl.rsc");
				
				ByteArrayInputStream in = new ByteArrayInputStream(str.toString().getBytes("UTF-8"));
			    org.jdom2.input.SAXBuilder bldr = new org.jdom2.input.SAXBuilder();

			   
			    org.jdom2.Document doc = bldr.build(in);
			    Element root_elm = doc.getRootElement();
			    List<Element> lelm = root_elm.getChildren("Script");
				for(Element elm : lelm)
				{
					scriptmap.put(elm.getChild("name").getText(), elm.getChild("code").getText().trim());
					functionnamemap.put(elm.getChild("name").getText(), elm.getChild("functionname").getText().trim());
				}
			    
			    
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (JDOMException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public static CheckScript CheckScriptFactory(String scripttype, String script, Object []_logicvars, List<TreeMap<String, Object>> refs, PrintWriter writer, int threadcnt)
		{
			String script_r = script.replaceAll("\\$","\\\\\\$");
			String code = scriptmap.get(scripttype);
			String newcode = null;
			Pattern pattern = Pattern.compile("\\$\\{(\\w+)\\}");
			Matcher matcher = pattern.matcher(code);
			while (matcher.find())
			{
				newcode = code.replaceAll("\\$\\{("+SCRIPT_TAG_BODY+")\\}",script_r);
			}

			Invocable inv = null;
	        try {
	        	ScriptEngine e = GetScriptEngine();
	        	e.eval(newcode);
				inv = (Invocable)e;
			} catch (ScriptException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        CheckScript ret = new CheckScript(inv, functionnamemap.get(scripttype),_logicvars, refs, writer, threadcnt);
			return ret;
		}
		
		public static <Interface> CheckInterfaceScript<Interface> CheckInterfaceScriptFactory(String scripttype, String script, Object []_logicvars, Class<Interface> clazz, List<TreeMap<String, Object>> refs, PrintWriter writer, int threadcnt)
		{
			String script_r = script.replaceAll("\\$","\\\\\\$");
			String code = scriptmap.get(scripttype);
			String newcode = null;
			Pattern pattern = Pattern.compile("\\$\\{(\\w+)\\}");
			Matcher matcher = pattern.matcher(code);
			while (matcher.find())
			{
				newcode = code.replaceAll("\\$\\{("+SCRIPT_TAG_BODY+")\\}",script_r);
			}

	        Invocable inv = null;
	        try {
	        	ScriptEngine e = GetScriptEngine();
	        	e.eval(newcode);
				inv = (Invocable)e;
			} catch (ScriptException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        CheckInterfaceScript<Interface> ret = new CheckInterfaceScript<Interface>(inv, functionnamemap.get(scripttype), clazz, _logicvars, refs, writer, threadcnt);
			return ret;
		}
		
		private static ScriptEngine GetScriptEngine() throws ScriptException
		{
			ScriptEngineManager manager = new ScriptEngineManager();
	        ScriptEngine e = manager.getEngineByName("JavaScript");
	        // Get original load function
	        final JSObject loadFn = (JSObject)e.get("load");
	        // Get global. Not really necessary as we could use null too, just for
	        // completeness.
	        final JSObject thiz = (JSObject)e.eval("(function() { return this; })()");

	        // Define a new "load" function
	        final Function<Object, Object> newLoad = (source) -> {
	            if (source instanceof String) {
	                final String strSource = (String)source;
	                if (strSource.startsWith("cmpdata:")) {
	                    // handle "cmpdata:"
	                	String []ss = strSource.split(":");
	                    return loadFn.call(thiz, loadCmpdataInclude(ss[1]));
	                }
	            }
	            // Fall back to original load for everything else
	            return loadFn.call(thiz, source);
	        };

	        // Replace built-in load with our load
	        e.put("load", newLoad);
	        
	        return e;
		}
		
		private static Object loadCmpdataInclude(final String source)
	    {
	    	String str = ResourceFileReader.readResourceFile("com.cmpdata.scripting.CheckScript",source);
			
			final Map<String, String> sourceMap = new HashMap<>();
	        sourceMap.put("name", "cmpdatainclude");
	        sourceMap.put("script", str);
	        return sourceMap;
	    }
		
		public static <Interface> CheckInterfaceScript<Interface> CheckInterfaceScriptFactory(String scripttype, Interface script, Object []_logicvars, List<TreeMap<String, Object>> refs, int threadcnt)
		{
	        CheckInterfaceScript<Interface> ret = new CheckInterfaceScript<Interface>(script, functionnamemap.get(scripttype), _logicvars, refs, threadcnt);
			return ret;
		}
		
		protected CheckScript(Invocable _inv, String _functionname, Object []_logicvars, List<TreeMap<String, Object>> refs, PrintWriter _writer, int threadcnt)
		{
			checkscript = _inv;
			writer = _writer;
			logicvars = _logicvars;
			rst = new CmpLogicRst[threadcnt];
			for(int i=0;i<threadcnt;i++)
			{
				rst[i] = new CmpLogicRst();
			}
			scriptfunctionname = _functionname;
			checkscriptrefs = refs;
			
			((ScriptEngine)checkscript).put("result", rst);
			((ScriptEngine)checkscript).put("writer", writer);
		}
		
		protected CheckScript(Invocable _inv, String _functionname, Object []_logicvars, List<TreeMap<String, Object>> refs, int threadcnt)
		{//��CheckInterfaceScript(Interface _inv, ...)�I�s�Arst��l�ƥ�subclass�B�z
			checkscript = _inv;
			logicvars = _logicvars;
			rst = new CmpLogicRst[threadcnt];
			scriptfunctionname = _functionname;
			checkscriptrefs = refs;
			if(checkscript != null)
			((ScriptEngine)checkscript).put("result", rst);
		}
		
		public List<TreeMap<String, Object>> getCheckscriptrefs()
		{
			return checkscriptrefs;
		}
		
		public Object InvokeCheckScript(Object [] objs)
		{
			Object ret = null;
			try {
				ret = checkscript.invokeFunction(scriptfunctionname, objs);
			} catch (NoSuchMethodException | ScriptException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return ret;
		}
		
		public <T> T getInterface(Class<T> dataType)
		{
			return checkscript.getInterface(dataType);
		}
		
		public String getRst(int runid, int leftright)
		{
			return (leftright==genericCmpDataConstant.CmpLeftElm) ? rst[runid].left : rst[runid].right;
		}
		
		public void clearRst(int runid)
		{
			rst[runid].left = rst[runid].right = "";
		}
		
		public Object [] getLogicvars()
		{
			return logicvars;
		}
		
		public static List<Object> generateRef(TreeMap<String, String> ConstantVal, List<TreeMap<String,Object>> thisrow, List<TreeMap<String,Object>> refs) throws Exception
		{
			List<Object> ret = new ArrayList<Object>();
			for(TreeMap<String,Object> ref : refs)
			{
				if(ref.get("val")==null)
				{
					TreeMap<String,String> r;
					String []vars = ((String)ref.get("var")).split(",");
					String []pk = new String[vars.length];
					for(int i=0;i<vars.length;i++)
					{
						if(dbgmode==1)
						{
							System.out.println("execute var:"+vars[i]);
						}
						if(vars[i].startsWith("$"))
						{
							String SessionVariableObj = ConstantVal.get(vars[i].substring(1));
							if(SessionVariableObj== null)
							{
								@SuppressWarnings("unchecked")
								TreeMap<String, CheckInterfaceScript<VarScript>> vsimpl= (TreeMap<String, CheckInterfaceScript<VarScript>>)ref.get("varscripts");
								if(vsimpl == null)
									throw new Exception(vars[i]+" vsimpl unimplemented");
								Object []os = {thisrow};
								try {
									pk[i]=(String)(vsimpl.get(vars[i].substring(1)).getInterface().InitVar(os));
									if(dbgmode==1)
									{
										System.out.println("varscript:"+vars[i].substring(1)+"/"+(pk[i])+" len:"+pk[i].length());
									}
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							else
								pk[i] = SessionVariableObj;
						}
						else
						{//left.xxx   right.xxx  �S����left or right�ɡA�w�]��left.xxx
							if(!vars[i].contains("."))
								pk[i]=ScriptAPI.left(thisrow, vars[i]);
							else
							{
								String realcolumn = genericCmpDataUtil.getRealColumn(vars[i]);
								if(vars[i].contains("right"))
								{
									pk[i]=ScriptAPI.right(thisrow, realcolumn);
								}
								else
								{
									pk[i]=ScriptAPI.left(thisrow, realcolumn);
								}
									
							}
							if(!vars[i].equals("") && pk[i]==null)
							{
								System.out.println("col:"+vars[i]+" get null value");
							}
						}
					}
					TreeMap<String,Object> refclone = new TreeMap<String,Object>(ref);
					if(ref.get("multi")!=null && ref.get("multi").equals("true"))
					{
						ret.add(CheckScriptRef.QueryRefMulti(refclone, pk));
					}
					else
					{
						r = CheckScriptRef.QueryRef(refclone, pk);
						if(r!=null)
						{
							try {
								r.put("${col}",(String)ref.get("col"));
							} catch (Exception e) {
								// TODO Auto-generated catch block
								System.out.println(r);System.out.println(ref);System.out.println(ref.get("col"));
								e.printStackTrace();
							}
							ret.add(r);
						}
						else
						{
							//ScriptAPI.errpk_println(colname+" ref find nothing:"+ref.get("sql")+" var:"+ref.get("var")+"("+thisrow.get(ref.get("var"))+")");
							ret.add("${NULL}");
						}
					}
				}
				else
				{
					String val = (String)ref.get("val");
					if(!val.startsWith("${") && val.startsWith("$"))
					{
						val = ConstantVal.get(val.substring(1));
					}
					ret.add(val);
				}
			}
			return ret;
		}
		
		public static void main(String []args) throws ClassNotFoundException, IOException
		{
			PrintWriter errpk = new PrintWriter(new OutputStreamWriter(new FileOutputStream("d://tmp//aaa.txt"), "UTF-8"));
			TreeMap<String, String> t1 = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
			TreeMap<String, String> t2 = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
			TreeMap<String, String> t3 = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
			
			List<TreeMap<String, String>> lrefs = new ArrayList<TreeMap<String, String>>();
			TreeMap<String, String> ref = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
			ref.put("db", "sql://informix");
			ref.put("sql", "select * from bcmdb_qcdb:user where userid=?");
			ref.put("var", "a1");
			ref.put("dbg", "NO");
			ref.put("col", "pacustname");lrefs.add(ref);
			List<TreeMap<String, String>> thisrow = new ArrayList<TreeMap<String, String>>();
			List<Object> lref = new ArrayList<Object>();
			
			t1.put("a1", "1513");
			t2.put("a1", "a1rightvalue");
			t3.put("b1", "b1value");
			thisrow.add(t1);thisrow.add(t2);
			lref.add(thisrow); lref.add(lrefs);
			
			StringBuffer sb = new StringBuffer();
			sb.append("printlog(\"aaa\");")
			  .append("printlog(getLeftValue(\"a1\"));")
			  .append("printlog(getRightValue(\"a1\"));")
			  .append("printlog(getRefValue(0, \"pacustname\"));")
			  .append("invokeMethod('test1', 'sssabc', 4, 5);");
			
			ScriptAPI.initScriptAPI("d:/testapi.jar", "scriptplugin.testapi");
			
			
			CheckScript sc = CheckScript.CheckScriptFactory("checkcolumnvalue", sb.toString(), null, null, errpk, 1);
			sc.getInterface(CmpLogic.class).cmpvalue("","",lref.toArray(),1,null,null);
			errpk.close();
		}
		/*
�D�\��
1. ����
2. �������
 A. ��鷺�e�B��Ƽ�

�\��S��
1. ������ƪ�
  A. ���ɪ�����(�ǲΪ���ƪ�)
  B. �{�����W�u�e������

2. configuration as a service
  A. �ݨD�������P��X
  B. ���ѨϥΪ̥Hscript�w�q�����W�h(���Ѩ���script�ѽs��)
  C. �u�@�ܼơA�u�ʩw�q�C�����u�@���Ѽ�

3. plugin�[�c�i��X�Ȥ�J��domain lib

4. �䴩�U�ظ�Ʈw�t�P���[�c


�į�
		 */
}
