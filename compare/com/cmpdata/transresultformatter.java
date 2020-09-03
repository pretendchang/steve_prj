package com.cmpdata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import com.security.PathManipulation;
import com.steve.db.data.manager.DSManager;

public class transresultformatter {

	private static void buildOutput(String templatepath, String outputpathtemplate, HashMap<String, String> mapping, String outputkey)
	{
		try {
			templatepath = PathManipulation.filterFilePath(templatepath);
			BufferedReader templatereader = new BufferedReader(new FileReader(templatepath));
			String replacepath=null;
			if(outputkey.startsWith("$"))
				replacepath="\\"+outputkey;
			else
				replacepath=outputkey;
			
			replacepath = PathManipulation.filterRegex(replacepath);
			String outputpath = outputpathtemplate.replaceAll(replacepath,mapping.get(outputkey));
			outputpath = PathManipulation.filterFilePath(outputpath);
			PrintWriter out = new PrintWriter(new FileOutputStream(new File(outputpath), false));
			String s = null;
			
			while((s=templatereader.readLine())!=null)
			{
				for(String key : mapping.keySet())
				{
					String replacereg=null;
					if(key.startsWith("$"))
						replacereg="\\"+key;
					else
						replacereg=key;
					replacereg = PathManipulation.filterRegex(replacereg);
					s=s.replaceAll(replacereg, mapping.get(key));
					
				}
				out.print(s);
				out.print("\n");
			}
			templatereader.close();
			out.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void buildSql(String templatepath, String outputpathtemplate, HashMap<String, String> mapping, String outputkey, int from, int to, int interval)
	{
		try {
			templatepath = PathManipulation.filterFilePath(templatepath);
			BufferedReader templatereader = new BufferedReader(new FileReader(templatepath));
			String replacepath=null;
			if(outputkey.startsWith("$"))
				replacepath="\\"+outputkey;
			else
				replacepath=outputkey;
			replacepath=PathManipulation.filterRegex(replacepath);
			String outputpath = outputpathtemplate.replaceAll(replacepath,mapping.get(outputkey));
			
			
			String s = null;
			
			String r1 = mapping.get("$r1");
			String r2 = mapping.get("$YYYYMM");
			
			while((s=templatereader.readLine())!=null)
			{
				System.out.println("write to "+outputpath);
				cmpRangeCutter.cmpRangeCutter(r1, r2, from,to,interval,outputpath, true, s);
			}
			templatereader.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void buildTransformatter(String templatepath, String outputpathtemplate, HashMap<String, String> mapping, String outputkey)
	{
		try {
			templatepath = PathManipulation.filterFilePath(templatepath);
			BufferedReader templatereader = new BufferedReader(new FileReader(templatepath));

			outputpathtemplate = PathManipulation.filterFilePath(outputpathtemplate);
			PrintWriter out = new PrintWriter(new FileOutputStream(new File(outputpathtemplate), false));
			if(outputpathtemplate.contains(".sh"))
			{
				out.print("#!/bin/sh -x");
				out.print("\n");
				out.print("NOWTIMESTAMP=$(date '+%s')");
				out.print("\n");
			}
			String s = null;
			
			DateFormat df = new SimpleDateFormat("yyyyMM");
			String from = mapping.get("$YYYYMMVALUEFROM");
			String to = mapping.get("$YYYYMMVALUETO");
			try {
				while((s=templatereader.readLine())!=null)
				{
					Date dtfrom = df.parse(from);
					Date dtto = df.parse(to);
					while(dtfrom.before(dtto) || dtfrom.equals(dtto))
					{
						String rs=s.replaceAll("\\$YYYYMMVALUE", df.format(dtfrom));	
						out.print(rs);
						out.print("\n");
	
						Calendar c = Calendar.getInstance(); 
						c.setTime(dtfrom); 
						c.add(Calendar.MONTH, 1);
						dtfrom = c.getTime();
					}
				}
				
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			templatereader.close();
			out.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void buildBatch(String templatepath, String outputpath, HashMap<String, String> mapping, String outputkey)
	{
		buildOutput(templatepath, outputpath, mapping, outputkey);//*.bat
	}
	
	public static void buildXml(String templatepath, String outputpath, HashMap<String, String> mapping, String outputkey)
	{
		buildOutput(templatepath, outputpath, mapping, outputkey);
	}
	
	public static void buildLeftSql(String templatepath, String outputpath, HashMap<String, String> mapping, String outputkey, int from, int to, int interval)
	{
		buildSql(templatepath, outputpath, mapping, outputkey, from, to, interval);
	}
	
	public static void buildRightCountRefSql(String templatepath, String outputpath, HashMap<String, String> mapping, String outputkey, int from, int to, int interval)
	{
		buildSql(templatepath, outputpath, mapping, outputkey, from, to, interval);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/*
		if(args.length != 6)
		{
			System.out.println("mode:batch, xml, left, right   templatepath outputpath outputkey  key(逗號分隔)   value (逗號分隔)");
			return;
		}
		*/
		
		
		
		System.out.println("input:"+args[0]+" "+args[1]+" "+args[2]+" "+args[3]+" "+args[4]+" "+args[5]);
		String []keys = args[4].split(",");
		String []values = args[5].split(",");
		
		if(keys.length != values.length)
		{
			System.out.println("key value 數目不符");
			System.out.println("mode:batch, xml, left, right   templatepath outputpath outputkey   key(逗號分隔)   value (逗號分隔)");
			return;
		}
		
		HashMap<String, String> map = new HashMap<String, String>();
		int i=0;
		boolean checkargs3=false;
		for(String s : keys)
		{
			if(s.equalsIgnoreCase(args[3].toLowerCase()))
				checkargs3=true;
			map.put(s, values[i++]);
		}
		
		if(!args[0].equalsIgnoreCase("formatter") && !checkargs3)
		{
			System.out.println("outputkey not found in the key: "+args[3]);
			System.out.println("mode:batch, xml, left, right   templatepath outputpath outputkey   key(逗號分隔)   value (逗號分隔)");
			return;
		}
		
		if(args[0].equalsIgnoreCase("batch"))
		{
			buildBatch(args[1], args[2], map, args[3]);
		}
		else if(args[0].equalsIgnoreCase("xml"))
		{
			buildXml(args[1], args[2], map, args[3]);
		}
		else if(args[0].equalsIgnoreCase("left"))
		{
			int from = Integer.parseInt(args[6]);
			int to = Integer.parseInt(args[7]);
			int interval = Integer.parseInt(args[8]);
			buildLeftSql(args[1], args[2], map, args[3], from, to, interval);
		}
		else if(args[0].equalsIgnoreCase("right"))
		{
			int from = Integer.parseInt(args[6]);
			int to = Integer.parseInt(args[7]);
			int interval = Integer.parseInt(args[8]);
			buildRightCountRefSql(args[1], args[2], map, args[3], from, to, interval);
		}
		else if(args[0].equalsIgnoreCase("formatter"))
		{
			buildTransformatter(args[1], args[2], map, args[3]);
		}
		else
			System.out.println("mode:batch, xml, left, right   templatepath outputpath outputkey   key(逗號分隔)   value (逗號分隔)");
	}
	
	

}
