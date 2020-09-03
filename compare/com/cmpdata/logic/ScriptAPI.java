package com.cmpdata.logic;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.net.URL;
import java.net.URLClassLoader;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.cmpdata.genericCmpDataException;

import com.cmpdata.CmpContext;

import com.cmpdata.genericCmpDataUtil;
import com.cmpdata.controller.GlobalVariable;
import com.cmpdata.rst.PKDisplay;
import com.cmpdata.rst.ResultExecutor;
import com.cmpdata.rst.RstFormat;
import com.cmpdata.rst.RstWriting;
import com.cmpdata.scripting.CheckScript;

import com.qa.log.errpk;
import com.security.PathManipulation;


import jdk.nashorn.api.scripting.ScriptObjectMirror;


public class ScriptAPI {
	static Class<?> cls;
	private static Class<?> LoadClassesFromJar(String pathToJar, String entryclassname) throws IOException, ClassNotFoundException
	{
		Class<?> ret = null;
		pathToJar = PathManipulation.filterFilePath(pathToJar);
		JarFile jarFile=null;
		try
		{
			jarFile= new JarFile(pathToJar);
			Enumeration<JarEntry> e = jarFile.entries();
	
			URL[] urls = { new URL("jar:file:" + pathToJar+"!/") };
			URLClassLoader cl = URLClassLoader.newInstance(urls);
	
			while (e.hasMoreElements()) {
			    JarEntry je = e.nextElement();
			    if(je.isDirectory() || !je.getName().endsWith(".class")){
			        continue;
			    }
			    // -6 because of .class
			    String className = je.getName().substring(0,je.getName().length()-6);
			    className = className.replace('/', '.');
			    if(className.equals(entryclassname))
			    {
			    	ret = cl.loadClass(className);
			    }
			    else
			    	cl.loadClass(className);
	
			}
		}
		finally
		{
			if(jarFile != null)
				jarFile.close();
		}
		return ret;
	}
	public static void initScriptAPI(String jarpath, String customizedclassname) throws ClassNotFoundException, IOException
	{
		cls = LoadClassesFromJar(jarpath, customizedclassname);
	}
	
	public static Object invokeCustomMethod(String methodname, ScriptObjectMirror arg)
	{
		List<Object> listResult = new ArrayList<Object>();
		Integer length = (Integer)arg.get("length");
		for (int i = 0; i < length; i++) {
			listResult.add((arg.get("" + i)));
		}
		Object []args = listResult.subList(1, listResult.size()).toArray();
		if(cls == null)
			throw new genericCmpDataException("cmpdata011","ScriptAPI has not been initialized");

	    Method m =null;
	    Class<?> []argclass=new Class<?>[args.length];
	    for(int i=0;i<args.length;i++)
	    {
	    	argclass[i]=args[i].getClass();
	    	
	    	if(argclass[i].getName().equalsIgnoreCase("java.lang.integer"))
	    		argclass[i]=int.class;
	    	else if(argclass[i].getName().equalsIgnoreCase("java.lang.float"))
	    		argclass[i]=float.class;
	    	else if(argclass[i].getName().equalsIgnoreCase("java.lang.double"))
	    		argclass[i]=double.class;
	    	else if(argclass[i].getName().equalsIgnoreCase("java.lang.boolean"))
	    		argclass[i]=boolean.class;
	    }
	    
		try {
			m = cls.getDeclaredMethod(methodname,argclass);
		} catch (NoSuchMethodException e) {
			throw new genericCmpDataException("cmpdata011","ScriptAPI invokeCustomMethod NoSuchMethodException:"+methodname);
		} catch (SecurityException e) {
			e.printStackTrace();
			throw new genericCmpDataException("cmpdata011","ScriptAPI invokeCustomMethod SecurityException");
		}
	    
	    try {
			return m.invoke(null,args);
		} catch (IllegalAccessException e) {
			throw new genericCmpDataException("cmpdata011","ScriptAPI invokeCustomMethod IllegalAccessException");
		} catch (IllegalArgumentException e) {
			throw new genericCmpDataException("cmpdata011","ScriptAPI invokeCustomMethod IllegalArgumentException");
		} catch (InvocationTargetException e) {
			throw new genericCmpDataException("cmpdata011","ScriptAPI invokeCustomMethod InvocationTargetException");
		}
	}
	
	public static void printHello(String name)
	{
		System.out.println("hello "+name);
	}
	//outpatn left.equipoffcode,left.equipnumber,right.equipoffcode,right.equipnumber, 空缺用空白隔開
	public static void errpk_printspecified(TreeMap<String, Object> left, TreeMap<String, Object> right, String outpatn)
	{
		String []cols = outpatn.split(",");
		for(String c:cols)
		{
			if(!c.equals(" ") && !c.equals(""))//空缺用空白隔開
			{
				String []s = c.split("\\.");
				if(s[0].equals("left"))
				{
					if(left==null)
						new errpk().print(",");
					else
						new errpk().print(left.get(s[1])+",");
				}
				else if(s[0].equals("right"))
				{
					if(right==null)
						new errpk().print(",");
					else
						new errpk().print(right.get(s[1])+",");
				}
			}
			else
			{
				new errpk().print(",");
			}
		}
	}
	public static void errpk_report(TreeMap<String, Object> left, TreeMap<String, Object> right, 
			List<String> leftpk, List<String> rightpk, 
			String tablename, int errtype, CmpPair errcolumn, String splitter, PKDisplay pk)
	{
		CmpContext context = new CmpContext();
		context.setLeft(left);
		context.setRight(right);
		context.setLeftPK(leftpk);
		context.setRightPK(rightpk);
		context.setTableName(tablename);
		context.setErrColumn(errcolumn);
		context.setErrType(errtype);
		context.setPKDisplay(pk);
		errpk_println(ResultExecutor.executeResultFormat(context, GlobalVariable.logformat, "\\|", splitter));
	}
	
	public static void errpk_report(TreeMap<String, Object> left, TreeMap<String, Object> right, 
			List<String> leftpk, List<String> rightpk, 
			String tablename, int errtype, CmpPair errcolumn, String leftvalue, String rightvalue, String splitter, PKDisplay pk)
	{
		CmpContext context = new CmpContext();
		context.setLeft(left);
		context.setRight(right);
		context.setLeftPK(leftpk);
		context.setRightPK(rightpk);
		context.setTableName(tablename);
		context.setErrColumn(errcolumn);
		context.setErrType(errtype);
		context.setLeftValue(leftvalue);
		context.setRightValue(rightvalue);
		context.setPKDisplay(pk);
		errpk_println(ResultExecutor.executeResultFormat(context, GlobalVariable.logformat, "\\|", splitter));
	}
	public static void errpk_println(String s)
	{
		new errpk().println(s);
	}
	
	public static void summary_println(String s)
	{
		new errpk().summary_println(s);
	}
	
	public static void errpk_println(PrintWriter _pw, PrintWriter _pw_summary, String s)
	{
		new errpk(_pw, _pw_summary).println(s);
	}
	
	public static void errpk_println(PrintWriter _pw, String s)
	{
		new errpk(_pw).println(s);
	}
	
	public static void summary_println(PrintWriter _pw, PrintWriter _pw_summary, String s)
	{
		new errpk(_pw, _pw_summary).summary_println(s);
	}
	
	public static int writeRegTable(List<List<Object>> RstWritingRst, List<List<Object>> RstWritingRaw, int wrongrowno, int cnt, RstFormat format, String casetype, 
										String leftfieldname, String rightfieldname, 
										List<String> lpk, List<String> rpk, 
										TreeMap<String, Object> leftdata, TreeMap<String, Object> rightdata,
										String leftvalue, String rightvalue)
	{
		RstWriting rst = new RstWriting(format);//meta
		rst.casetype=casetype;

		if(!casetype.equals("case3"))//左有右沒有  只寫左資料
		{
			for(String pk:lpk)
			{
				String realcolumn = genericCmpDataUtil.getRealColumn(pk);
				rst.leftrawdata.put(realcolumn, leftdata.get(realcolumn));
			}
			if(casetype.equals("case1b"))
			{
				if(cnt>=0)
					rst.leftrawdata.put(String.format("fieldname%02d",cnt), leftfieldname);
				else
					rst.leftrawdata.put("fieldname", leftfieldname);
				rst.leftrawdata.put(leftfieldname, leftvalue);
			}
		}
		
		if(!casetype.equals("case2"))//右有左沒有  只寫右資料
		{
			for(String pk:rpk)
			{
				String realcolumn = genericCmpDataUtil.getRealColumn(pk);
				rst.rightrawdata.put(realcolumn, rightdata.get(realcolumn));
			}
			if(casetype.equals("case1b"))
			{
				if(cnt>=0)
					rst.rightrawdata.put(String.format("fieldname%02d",cnt), rightfieldname);
				else
					rst.rightrawdata.put("fieldname", rightfieldname);
				rst.rightrawdata.put(rightfieldname, rightvalue);
			}
		}
		return RstWriting.writeWrongData(RstWritingRst, RstWritingRaw, rst, wrongrowno);
	}
	
	public static int writeRegTable(List<List<Object>> RstWritingRst,List<List<Object>> RstWritingRaw, int wrongrowno, int cnt, RstFormat format, String casetype, 
			String leftfieldname, String rightfieldname, 
			String leftlinenumber, String rightlinenumber,
			String leftvalue, String rightvalue)
	{
		RstWriting rst = new RstWriting(format);//meta
		rst.casetype=casetype;
		
		if(!casetype.equals("case3"))
		{
			rst.leftrawdata.put("linenumber", leftlinenumber);
			if(casetype.equals("case1b"))
			{
				if(cnt>=0)
					rst.leftrawdata.put(String.format("fieldname%02d",cnt), leftfieldname);
				else
					rst.leftrawdata.put("fieldname", leftfieldname);
				rst.leftrawdata.put(leftfieldname, leftvalue);
			}
		}
		
		if(!casetype.equals("case2"))
		{
			rst.rightrawdata.put("linenumber", rightlinenumber);
			if(casetype.equals("case1b"))
			{
				if(cnt>=0)
					rst.rightrawdata.put(String.format("fieldname%02d",cnt), rightfieldname);
				else
					rst.rightrawdata.put("fieldname", rightfieldname);
				rst.rightrawdata.put(rightfieldname, rightvalue);
			}
		}
		return RstWriting.writeWrongData(RstWritingRst, RstWritingRaw, rst, wrongrowno);
	}
	
	@SuppressWarnings("unchecked")
	public static String thisrow(Object rs, String colname)
	{
		return ((TreeMap<String,String>)rs).get(colname);
	}
	
	@SuppressWarnings("unchecked")
	public static String left(Object rs, String key)
	{
		//return ((TreeMap<String,String>)rs).get(key);
		return ((List<TreeMap<String,String>>)rs).get(0).get(key);
	}
	
	@SuppressWarnings("unchecked")
	public static String right(Object rs, String key)
	{
		return ((List<TreeMap<String,String>>)rs).get(1).get(key);
	}
	/*
	 * ref[0]為this row
	 * 之後的ref陣列元素依照xml設定檔refs次序
	 */
	
	public static String getToday(String format)
	{
		SimpleDateFormat sdfDate = new SimpleDateFormat(format);//dd/MM/yyyy
	    Date now = new Date();
	    String strDate = sdfDate.format(now);
	    return strDate;
	}

		
	//d1 type yyyy-mm-dd
	public static int DateDifference(String d1, String d2)
	{
		if(d1 == null)
		{
			throw new genericCmpDataException("cmpdata005","d1 is null");
		}
		if(d2 == null)
		{
			throw new genericCmpDataException("cmpdata005","d2 is null");
		}
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date dd1 = dateFormat.parse(d1);
            Date dd2 = dateFormat.parse(d2);
            long diff = dd1.getTime()-dd2.getTime();
            
            return (int) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
        } catch (ParseException e) {
            throw new genericCmpDataException("cmpdata005","ScriptAPI.DateDifference ParseException:"+d1+"  "+d2);
        }
	}
	
	//d1 type 民國年 yyyMM  d2-d1 in months
		public static int MonthDifference(String d1, String d2)
		{
			if(d1 == null)
			{
				throw new genericCmpDataException("cmpdata005","d1 is null");
			}
			if(d2 == null)
			{
				throw new genericCmpDataException("cmpdata005","d2 is null");
			}
			
            Calendar startCalendar = ROCYear2CommonYear(d1);
            Calendar endCalendar = ROCYear2CommonYear(d2);

            int diffYear = endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR);
            int diffMonth = diffYear * 12 + endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);
            
            return diffMonth;

		}
		public static String byte2string(byte[] b, String codename)
		{
			try {
				return new String(b, codename);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		public static boolean contains(String s, String contains, String s_charset) throws UnsupportedEncodingException
		{
			return s.contains(new String(contains.getBytes(s_charset)));
		}
		private static Calendar ROCYear2CommonYear(String sd)
		{
			Calendar ret = null;
			try {
				if(sd.length()==5)
				{
					DateFormat dateFormat = new SimpleDateFormat("yyyMM");
					Date d = dateFormat.parse(sd);
					ret = new GregorianCalendar();
					ret.setTime(d);
					ret.set(Calendar.YEAR, ret.get(Calendar.YEAR)+1911);
					
				}
				else if(sd.length()==4)
				{
					DateFormat dateFormat = new SimpleDateFormat("yyMM");
					Date d = dateFormat.parse(sd);
					ret = new GregorianCalendar();
					ret.setTime(d);
					ret.set(Calendar.YEAR, ret.get(Calendar.YEAR)+11);
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				throw new genericCmpDataException("cmpdata005","ScriptAPI.ROCYear2CommonYear ParseException:"+sd);
			}
			
			return ret;
		}
		
		@SuppressWarnings("unchecked")
		public static Object[] LazyRefInit(Object []refs)
		{
			try {
				if(refs==null || refs.length!=2)
				{
					throw new genericCmpDataException("cmpdata011","LazyRefInit: refs is null");
				}
				List<Object> lo = CheckScript.generateRef(GlobalVariable.getSessionVariable(), (List<TreeMap<String,Object>>)refs[0], (List<TreeMap<String, Object>>)refs[1]);
				if(lo != null)
					return lo.toArray();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new genericCmpDataException("cmpdata013","LazyRefInit execution error");
			}
			return null;
		}
		
		public static String GetString_LeftCharset(byte []bs)
		{
			try {
				return new String(bs, GlobalVariable.getLeftCharset());
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
	
		public static String GetString_RightCharset(byte []bs)
		{
			try {
				return new String(bs, GlobalVariable.getRightCharset());
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		
		public static byte[] GetBytes_LeftCharset(String s)
		{
			try {
				return s.getBytes(GlobalVariable.getLeftCharset());
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		
		public static byte[] GetBytes_RightCharset(String s)
		{
			try {
				return s.getBytes(GlobalVariable.getRightCharset());
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
		
		public static boolean checkCharsetSame()
		{
			return (GlobalVariable.getLeftCharset()==GlobalVariable.getRightCharset());
		}

}
