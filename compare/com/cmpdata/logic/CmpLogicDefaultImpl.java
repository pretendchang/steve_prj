package com.cmpdata.logic;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.Collator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.TreeMap;

import com.cmpdata.controller.GlobalVariable;

public class CmpLogicDefaultImpl {
	public static CmpLogic GetDefaultImpl(String fun)
	{
		if(fun.equalsIgnoreCase("String1Check"))
			return String1Check;
		else if(fun.equalsIgnoreCase("StringConstantCheck"))
			return StringConstantCheck;
		else if(fun.equalsIgnoreCase("SubStringEuqalVar"))
			return SubStringEuqalVar;
		else if(fun.equalsIgnoreCase("StringEuqalNotExact"))
			return StringEuqalNotExact;
		else if(fun.equalsIgnoreCase("StringEqualNotExact"))
			return StringEuqalNotExact;
		else if(fun.equalsIgnoreCase("StringEuqal_Transresult1061121"))
			return StringEuqal_Transresult1061121;
		else if(fun.equalsIgnoreCase("StringTrimEqual"))
			return StringTrimEqual;
		else if(fun.equalsIgnoreCase("ByteArrayEqual"))
			return ByteArrayEqual;
		else if(fun.equalsIgnoreCase("ByteArrayUntrimSpaceEqual"))
			return ByteArrayUntrimSpaceEqual;
		else if(fun.equalsIgnoreCase("StringIgnoreWidthEuqal"))
			return StringIgnoreWidthEuqal;
		else if(fun.equalsIgnoreCase("DecimalEuqal"))
			return DecimalEqual;
		else
			return StringEuqal;
	}
	
	public static CmpLogic StringEuqal = new CmpLogic(){
		public CmpLogicRst result = new CmpLogicRst();
		public boolean cmpvalue(Object o1, Object o2, Object []args, int runid, Object []vars, CmpLogic logic)
		{
			String s1 = (String)o1;
			String s2 = (String)o2;
			//result.setResult(s1, s2);

			return s1.equals(s2);
		}
	};
	
	public static CmpLogic StringIgnoreWidthEuqal = new CmpLogic(){
		public CmpLogicRst result = new CmpLogicRst();
		public boolean cmpvalue(Object o1, Object o2, Object []args, int runid, Object []vars, CmpLogic logic)
		{
			Collator collate = Collator.getInstance(Locale.US);
	        collate.setStrength(Collator.IDENTICAL);

	        collate.setDecomposition(Collator.FULL_DECOMPOSITION);
	        return collate.equals((String)o1, (String)o2);
		}
	};
	
	public static CmpLogic StringTrimEqual = new CmpLogic(){
		public CmpLogicRst result = new CmpLogicRst();
		public boolean cmpvalue(Object o1, Object o2, Object []args, int runid, Object []vars, CmpLogic logic)
		{
			String s1 = (String)o1;
			String s2 = (String)o2;
			//result.setResult(s1, s2);

			return s1.trim().equals(s2.trim());
		}
	};
	public static CmpLogic StringEuqalNotExact = new CmpLogic(){
		public CmpLogicRst result = new CmpLogicRst();
		public boolean cmpvalue(Object o1, Object o2, Object []args, int runid, Object []vars, CmpLogic logic)
		{
			String s1 = (String)o1;
			String s2 = (String)o2;
			//result.setResult(s1, s2);
			if(!s1.equals(s2))
			{
				if(s1.equals("${NULL}") && s2.equals(""))
					return true;
				else if(s2.equals("${NULL}") && s1.equals(""))
					return true;
				else
					return false;
				
			}
			return true;
		}
	};
	public static CmpLogic StringEuqal_Transresult1061121 = new CmpLogic(){
		public CmpLogicRst result = new CmpLogicRst();
		public boolean cmpvalue(Object o1, Object o2, Object []args, int runid, Object []vars, CmpLogic logic)
		{
			String s1 = (String)o1;
			String s2 = (String)o2;
			//result.setResult(s1, s2);
			if(!s1.equals(s2))
			{
				if(s1.equals("${NULL}") && s2.equals(""))
					return true;
				else if(s2.equals("${NULL}") && s1.equals(""))
					return true;
				else if(s2.equals(" ") && s1.equals("${NULL}"))
					return true;
				else
					return false;
				
			}
			return true;
		}
	};
	public static CmpLogic SubStringEuqal = new CmpLogic(){
		public CmpLogicRst result = new CmpLogicRst();
		public boolean cmpvalue(Object o1, Object o2, Object []args, int runid, Object []vars, CmpLogic logic)
		{
			String s1 = (String)o1;
			String s2 = (String)o2;
			//result.setResult(s1, s2);
			return s1.substring(0,10).equals(s2);
		}
	};
	public static CmpLogic SubStringEuqalVar = new CmpLogic(){//var1 from  var2 to   
		public CmpLogicRst result = new CmpLogicRst();
		public boolean cmpvalue(Object o1, Object o2, Object []args, int runid, Object []vars, CmpLogic logic)
		{
			String s1 = o1.toString();
			String s2 = o2.toString();
			//result.setResult(s1, s2);
			if(s1.equals("${NULL}"))
			{
				return s2.equals("${NULL}");
			}
			else if(s2.equals("${NULL}"))
			{
				return s1.equals("${NULL}");
			}
			if(vars==null || vars.length!=2)
			{
				ScriptAPI.errpk_println("SubStringEuqalVar vars defined error! Do StringEuqal");
				return s1.equals(s2);
			}
			int from=0, to=0;
			try {
				from = Integer.parseInt((String)vars[0]);
				to = Integer.parseInt((String)vars[1]);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				ScriptAPI.errpk_println("SubStringEuqalVar vars defined error! Do StringEuqal");
				return s1.equals(s2);
			}
			if(s1.length()<to)
			{
				ScriptAPI.errpk_println("SubStringEuqalVar s1 length error Do StringEuqal "+s1);
				return s1.equals(s2);
			}
			if(s2.length()<to)
			{
				ScriptAPI.errpk_println("SubStringEuqalVar s2 length error Do StringEuqal "+s2);
				return s1.equals(s2);
			}
			
			return s1.substring(from,to).equals(s2.substring(from,to));
		}
	};
	public static CmpLogic StringConstantCheck = new CmpLogic(){
		public CmpLogicRst result = new CmpLogicRst();
		public boolean cmpvalue(Object o1, Object o2, Object []args, int runid, Object []vars, CmpLogic logic)
		{
			String s1 = (String)o1;
			String s2 = (String)o2;
			//result.setResult(s1, s2);
			if(args[1]!=null)
			{
				if(s1.equals(s2) && s1.equals(args[1]))
						return true;
			}
			ScriptAPI.errpk_println(s1+" "+s2+"  "+args[1]);
			return false;
		}
	};
	public static CmpLogic String1Check = new CmpLogic(){
		public CmpLogicRst result = new CmpLogicRst();
		@SuppressWarnings("unchecked")
		public boolean cmpvalue(Object o1, Object o2, Object []args, int runid, Object []vars, CmpLogic logic)
		{
			String s1 = (String)o1;
			String s2 = (String)o2;
			//result.setResult(s1, s2);
			String cmps = null;
			if(!args[1].toString().equals("${NULL}"))
			{
				String []cols = ((TreeMap<String,String>)args[1]).get("${col}").split(",");
				cmps = ((TreeMap<String,String>)args[1]).get(cols[0].toLowerCase());
				boolean ret = s1.equals(cmps);
				if(!ret)
					ScriptAPI.errpk_println(s1+" "+cmps);
				return ret;
			}
			else
			{
				if(s1.equals("${NULL}"))
					return true;
				else
				{
					ScriptAPI.errpk_println(s1+" ${NULL} find nothing");
				}
				//ScriptAPI.errpk_println(s1+" ${NULL} find nothing");
			}
			return false;
		}
	};
	
	public static CmpLogic ByteArrayEqual = new CmpLogic(){
		public CmpLogicRst result = new CmpLogicRst();
		public boolean cmpvalue(Object o1, Object o2, Object []args, int runid, Object []vars, CmpLogic logic)
		{
			byte[] s1 = (byte[])o1;
			byte[] s2 = (byte[])o2;
			if(s1.length==0 || s1[0]==0 || new String(s1).equals("${NULL}"))
			{
				if(s2.length==0 || s2[0]==0 || new String(s2).equals("${NULL}"))
					return true;
			}
			String s_s1 = ScriptAPI.GetString_LeftCharset(s1);//new String(s1, "ms950");
			String s_s2 = ScriptAPI.GetString_RightCharset(s2);//new String(s2, "ms950");
			if(s_s1==null || s_s2==null)
				return false;
			//result.setResult(s_s1, s_s2);
			s_s1 = s_s1.trim();
			s_s2 = s_s2.trim();
			
			boolean ret = Arrays.equals(ScriptAPI.GetBytes_RightCharset(s_s1), ScriptAPI.GetBytes_RightCharset(s_s2));
			if(!ret)
			{
				return false;
			}
			return true;
		}
	};
	
	public static CmpLogic ByteArrayUntrimSpaceEqual = new CmpLogic(){
		public CmpLogicRst result = new CmpLogicRst();
		public boolean cmpvalue(Object o1, Object o2, Object []args, int runid, Object []vars, CmpLogic logic)
		{
			byte[] s1 = (byte[])o1;
			byte[] s2 = (byte[])o2;
			if(s1.length==0 || s1[0]==0 || new String(s1).equals("${NULL}"))
			{
				if(new String(s2).equals(" "))
					return true;
			}
			if(ScriptAPI.checkCharsetSame())
				return Arrays.equals(s1, s2);
			String s_s1 = ScriptAPI.GetString_LeftCharset(s1);//new String(s1, "ms950");
			String s_s2 = ScriptAPI.GetString_RightCharset(s2);//new String(s2, "ms950");
			if(s_s1==null || s_s2==null)
				return false;
			return Arrays.equals(ScriptAPI.GetBytes_RightCharset(s_s1), ScriptAPI.GetBytes_RightCharset(s_s2));
		}
	};
	
	public static CmpLogic DecimalEqual = new CmpLogic(){
		public CmpLogicRst result = new CmpLogicRst();
		public boolean cmpvalue(Object o1, Object o2, Object []args, int runid, Object []vars, CmpLogic logic)
		{
			BigDecimal do1 = new BigDecimal((String)o1);
	    	BigDecimal do2 = new BigDecimal((String)o2);
			return (do1.compareTo(do2)==0);
		}
	};
}
