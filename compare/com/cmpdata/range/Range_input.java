package com.cmpdata.range;

import org.jdom2.Element;

import com.cmpdata.range.Range;
import com.cmpdata.range.Range_fbms_equipnumber;
import com.cmpdata.range.Range_input;
import com.cmpdata.range.Range_normal_distribution;
import com.cmpdata.range.Range_numbermod;

public class Range_input {
	public String keyname;
	public String replacekeyword;
	public String template;
	
	public int dbtype;//1:oracle 2:informix
	public int rangetype;//type=1:less 1000  2:more 10000 3:100 least
	
	//for normal distribution
	public int keyfrom;
	public int keyto;
	public int interval;
	public String [] excludesql;
	public String [] includesql;
	public String rangetypeAlgorithm;
	
	public static Range RangeFactory(Range_input ri, String Rangetype) throws Exception
	{
		if(Rangetype.equalsIgnoreCase("Range_fbms_equipnumber"))
			return new Range_fbms_equipnumber(ri);
		else if(Rangetype.equalsIgnoreCase("Range_normal_distribution"))
			return new Range_normal_distribution(ri);
		else if(Rangetype.equalsIgnoreCase("Range_numbermod"))
			return new Range_numbermod(ri);
		else
			throw new Exception("Unknown Rangetype:"+Rangetype);
	}
	
	public static Range parseXml(Element sqldef) throws Exception
	{
		Element elm;
		
		elm = sqldef.getChild("keyname");
		if(elm==null)
			throw new Exception();
		String keyname = elm.getValue().trim();
		
		elm = sqldef.getChild("sql");
		if(elm==null)
			throw new Exception();
		String sql = elm.getValue().trim();
		
		elm = sqldef.getChild("rangetype");
		int rangetype=0;
		if(elm!=null)
			rangetype = Integer.parseInt(elm.getValue().trim());
		
		elm = sqldef.getChild("includesql");
		String includesql="";
		if(elm!=null)
			includesql = elm.getValue().trim();
		
		elm = sqldef.getChild("excludesql");
		String excludesql="";
		if(elm!=null)
			excludesql = elm.getValue().trim();
		
		elm = sqldef.getChild("keyfrom");
		int keyfrom=0;
		if(elm!=null)
			keyfrom = Integer.parseInt(elm.getValue().trim());
		
		elm = sqldef.getChild("keyto");
		int keyto=0;
		if(elm!=null)
			keyto = Integer.parseInt(elm.getValue().trim());
		
		elm = sqldef.getChild("keyinterval");
		int keyinterval=0;
		if(elm!=null)
			keyinterval = Integer.parseInt(elm.getValue().trim());
		
		elm = sqldef.getChild("rangetypeAlgorithm");
		String rangetypeAlgorithm="Range_numbermod";
		if(elm!=null)
			rangetypeAlgorithm = elm.getValue().trim();
		
		Range_input ri = new Range_input();
		ri.dbtype=1;
		ri.keyname=keyname;
		ri.template=sql;
		ri.replacekeyword="\\$r1";
		ri.rangetype=rangetype;
		ri.excludesql=new String[1];
		ri.excludesql[0]=excludesql;
		ri.includesql=new String[1];
		ri.includesql[0]=includesql;
		ri.rangetypeAlgorithm=rangetypeAlgorithm;
		ri.keyfrom=keyfrom;
		ri.keyto=keyto;
		ri.interval=keyinterval;
		
		return RangeFactory(ri, ri.rangetypeAlgorithm);
	}
	
	public static void main(String s[]) {
		/*
		  Range_input ri = new Range_input();
		  ri.dbtype=1;ri.interval=600000;ri.keyfrom=15000000;ri.keyname="t.equipno";ri.keyto=29000000;
		  ri.replacekeyword="\\$r1";ri.rangetype=3;
		  ri.template="select c.*,t.offcode,t.equipnum from Invoprocess c, telequip t , ACCOUNT a where t.equipno = a.billequipno and c.ACCTNO = a.ACCTNO and (t.EQUIPBUSSID in ('A','B','D','E','K','T','X','Y','Z') or t.EQUIPNUM like 'CL%') and t.EQUIPNUM not like 'Z0%' and t.EQUIPEFFDATE is not NULL and t.BILLUSEMODE is not NULL and c.INVEFFDATE is not null and a.ACCTNUM LIKE 'Z%' ${leftequipfiler} and $r1";
		  //Range rtest = new Range_normal_distribution(ri);
		  Range rtest = new Range_fbms_equipnumber(ri);
		  String s1;
		  while((s1=rtest.getNextRangeString())!=null)
		  {
			 System.out.println(s1);
		  }
		*/
		
		  Range_input ri = new Range_input();
		  ri.dbtype=1;ri.keyname="t.equipno";
		  ri.replacekeyword="\\$r1";ri.rangetype=2;
		  ri.excludesql=new String[1];
		  ri.excludesql[0]="";

		  ri.template="select c.*,t.offcode,t.equipnum from Invoprocess c, telequip t , ACCOUNT a where t.equipno = a.billequipno and c.ACCTNO = a.ACCTNO and (t.EQUIPBUSSID in ('A','B','D','E','K','T','X','Y','Z') or t.EQUIPNUM like 'CL%') and t.EQUIPNUM not like 'Z0%' and t.EQUIPEFFDATE is not NULL and t.BILLUSEMODE is not NULL and c.INVEFFDATE is not null and a.ACCTNUM LIKE 'Z%' ${leftequipfiler} and $r1";
		  //Range rtest = new Range_normal_distribution(ri);
		  Range rtest = new Range_numbermod(ri);
		  String s1;
		  System.out.println(rtest.getSchemaString());
		  while((s1=rtest.getNextRangeString())!=null)
		  {
			 System.out.println(s1);
		  }
	  }
	
}
