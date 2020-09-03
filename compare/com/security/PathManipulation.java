package com.security;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class PathManipulation {
	private static HashMap<String, String> base64codechecking;
	
	private static String filter(char c)
	{
		String s = base64codechecking.get(c);
		if(s != null)
			return s;
		else
			return Character.toString(c);
	}
	
	private static String encodeBase64(String s)
	{
		BASE64Encoder encoder = new BASE64Encoder();
		String encodedText = null;
		try {
			encodedText = encoder.encode(s.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return encodedText;
	}
	
	private static String decodeBase64(String s)
	{
		BASE64Decoder decoder = new BASE64Decoder();
		String decodedText=null;
		try {
			byte [] decodedTextByte = decoder.decodeBuffer(s);
			decodedText = new String(decodedTextByte, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return decodedText;
	}
	
	private static String filter(String s)
	{
		/*
		String encodedText = encodeBase64(s);
		
		StringBuffer ret = new StringBuffer();		
		char []chars = encodedText.toCharArray();
		for(int i=0;i<chars.length;i++)
		{
			ret.append(filter(chars[i]));
		}
		
		String decodedText=decodeBase64(ret.toString());
		
		return decodedText;
		*/
		return s;
	}
	
	public static String filterSQL(String s)
	{
		return filter(s);
	}
	
	public static String filterFilePath(String s)
	{
		return filter(s);
	}
	
	public static String filterUrl(String s)
	{
		return filter(s);
	}
	
	public static String filterRegex(String s)
	{
		return filter(s);
	}
	
	public static String filterShellCmd(String s)
	{
		return filter(s);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	static
	{
		base64codechecking = new HashMap<String, String>();
		/*
		base64codechecking.put("\n","\n");
		base64codechecking.put("\r","\r");
		base64codechecking.put("+","+");
		base64codechecking.put("=","=");
		
		base64codechecking.put("0","0");
		base64codechecking.put("1","1");
		base64codechecking.put("2","2");
		base64codechecking.put("3","3");
		base64codechecking.put("4","4");
		base64codechecking.put("5","5");
		base64codechecking.put("6","6");
		base64codechecking.put("7","7");
		base64codechecking.put("8","8");
		base64codechecking.put("9","9");
		
		base64codechecking.put("A","A");
		base64codechecking.put("B","B");
		base64codechecking.put("C","C");
		base64codechecking.put("D","D");
		base64codechecking.put("E","E");
		base64codechecking.put("F","F");
		base64codechecking.put("G","G");
		base64codechecking.put("H","H");
		base64codechecking.put("I","I");
		base64codechecking.put("J","J");
		base64codechecking.put("K","K");
		base64codechecking.put("L","L");
		base64codechecking.put("M","M");
		base64codechecking.put("N","N");
		base64codechecking.put("O","O");
		base64codechecking.put("P","P");
		base64codechecking.put("Q","Q");
		base64codechecking.put("R","R");
		base64codechecking.put("S","S");
		base64codechecking.put("T","T");
		base64codechecking.put("U","U");
		base64codechecking.put("V","V");
		base64codechecking.put("W","W");
		base64codechecking.put("X","X");
		base64codechecking.put("Y","Y");
		base64codechecking.put("Z","Z");
		
		base64codechecking.put("a","a");
		base64codechecking.put("b","b");
		base64codechecking.put("c","c");
		base64codechecking.put("d","d");
		base64codechecking.put("e","e");
		base64codechecking.put("f","f");
		base64codechecking.put("g","g");
		base64codechecking.put("h","h");
		base64codechecking.put("i","i");
		base64codechecking.put("j","j");
		base64codechecking.put("k","k");
		base64codechecking.put("l","l");
		base64codechecking.put("m","m");
		base64codechecking.put("n","n");
		base64codechecking.put("o","o");
		base64codechecking.put("p","p");
		base64codechecking.put("q","q");
		base64codechecking.put("r","r");
		base64codechecking.put("s","s");
		base64codechecking.put("t","t");
		base64codechecking.put("u","u");
		base64codechecking.put("v","v");
		base64codechecking.put("w","w");
		base64codechecking.put("x","x");
		base64codechecking.put("y","y");
		base64codechecking.put("z","z");*/
	}

}
