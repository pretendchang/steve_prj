package com.cmpdata;

import java.io.UnsupportedEncodingException;
import java.util.TreeMap;

import com.cmpdata.ColumnMeta;
import com.cmpdata.genericCmpDataConstant;
import com.cmpdata.logic.CmpPair;

public class genericCmpDataUtil {
	public static String getRealColumn(String col)
	{
		String ret;
		if(col.contains("."))
		{
			ret = col.substring(col.indexOf(".")+1);
		}
		else
			ret = col;
		
		return ret;
	}
	
	public static ColumnMeta findCmpPair(int leftright, String _name)
	{
		/*
		CmpPair cp = findCmpPair(_name);
		if(cp == null)
			return null;
		ColumnMeta cm = (leftright == genericCmpDataConstant.CmpLeftElm)? 
				cp.getLeftcm():cp.getRightcm();
				
		return cm;
		*/
		ColumnMeta cm = (leftright == genericCmpDataConstant.CmpLeftElm)? 
		ColumnMeta.getLeftColumn(_name):ColumnMeta.getRightColumn(_name);
		
		return cm;
	}
	
	public static CmpPair findCmpPair(String _name)//lcmppair
	{
		String n = getRealColumn(_name);
		for(CmpPair cp: CmpPair.getlcmppair())
		{
			if(cp.find(n)!=null)
				return cp;
		}
		return null;
	}
	
	public static byte[] TranscodeCharset(byte[] s, String leftcharset, String rightcharset)
	{
		if(leftcharset.equalsIgnoreCase(rightcharset))
			return s;
		else
		{
			byte[] ret=null;
			try {
				ret = new String(s, leftcharset).getBytes(rightcharset);
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return ret;
		}
	}
	
	public static void printbyte(byte []bs)
	{
		System.out.print("[");
		for(int i=0;i<bs.length; i++)
		{
			System.out.print(bs[i]+",");
		}
		System.out.println("]");
	}
	public static void showTreeMap(TreeMap<String,Object> t)
	{
		System.out.print("{");
		for(String key :t.keySet())
		{
			Object o = t.get(key);
			if(o==null)
			{
				System.out.print(key+"="+o+",");
			}
			else if(o.getClass().getName().equals("java.lang.String"))
			{
				System.out.print(key+"="+o+",");
			}
			else
			{
				System.out.print(key+"="+new String((byte[])o)+",");
			}
		}
		System.out.println("}");
	}
}
