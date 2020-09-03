package com.cmpdata;

import java.io.UnsupportedEncodingException;

import com.cmpdata.genericCmpDataException;

public class TreeMapUtil {
	public static byte[] getBytes(Object obj, String charset)
	{
		try {
			if(obj==null)
			{
				return JSONHandler.getColumnValueString((String)obj).getBytes(charset);
			}
			if(obj.getClass().getName().equals("[B"))
			{
				return (byte[])obj;
			}
			if(obj.getClass().getName().equals("java.lang.String"))
			{
				byte [] ret=null;
				try {
					ret = JSONHandler.rollbackColumnValueString((String)obj).getBytes(charset);
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return ret;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		throw new genericCmpDataException("cmpdata007","TreeMapUtil error unimplemented getBytes datatype:"+obj.getClass().getName());
	}
	
	public static String getString(Object obj)
	{
		try {
			if(obj==null)
			{
				return JSONHandler.getColumnValueString((String)obj);
			}
			if(obj.getClass().getName().equals("[B"))
			{
				try {
					return new String((byte[])obj, "ms950");
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(obj.getClass().getName().equals("java.lang.String"))
			return ((String)obj);
		
		throw new genericCmpDataException("cmpdata007","TreeMapUtil error unimplemented getBytes datatype:"+obj.getClass().getName());
	}
}
