package com.cmpdata.range;

import java.util.ArrayList;
import java.util.List;

public final class Range_fbms_equipnumber extends Range {
	
	private List<String> equipnumber = new ArrayList<String>();
	private List<String> equipnumber_exclude = new ArrayList<String>();
	private int nowRange;
	
	public Range_fbms_equipnumber(Range_input _input)
	{
		super(_input);
		nowRange=0;
		
		equipnumber_generator(input.rangetype);
		equipnumber_exclude_generator(input.dbtype, input.keyname, input.rangetype);
	}
	
	public String getNextRangeString()
	{
		String ret = null;
		if(nowRange < equipnumber.size())
		{
			String e = equipnumber.get(nowRange);
			StringBuffer cond1= new StringBuffer();
			if(input.dbtype==1)
				cond1.append("substr(").append(input.keyname).append(",0,").append(e.length()).append(")='").append(e).append("'");
			else if(input.dbtype==2)
				cond1.append("left(").append(input.keyname).append(",").append(e.length()).append(")='").append(e).append("'");
			
			ret = input.template.replaceAll(input.replacekeyword, cond1.toString());
		}
		else if((nowRange >= equipnumber.size()) && (nowRange<(equipnumber.size() + equipnumber_exclude.size())))
		{
			ret = input.template.replaceAll(input.replacekeyword, "  ("+equipnumber_exclude.get(nowRange-equipnumber.size())+")");
		}

		nowRange++;
		return ret;
	}
	
	private List<String> equipnumber_exclude_generator(int dbtype, String key1, int type)//type=1:less 1000  2:more 10000 3:100 least
	{
		if(type==1 || type==2 || type==3)
			equipnumber_exclude.add(exclude(key1, dbtype).toString());
		
		if(type==4)
			equipnumber_exclude.add(excludeType4(key1, dbtype).toString());
		
		if(type==1 || type==2)
			equipnumber_exclude.add(excludeM(key1, dbtype).toString());
		else if(type==3)
		{
			equipnumber_exclude.add(excludeMtype31(key1, dbtype).toString());
			equipnumber_exclude.add(excludeMtype32(key1, dbtype).toString());
			equipnumber_exclude.add(excludeMtype33(key1, dbtype).toString());
		}
			
		if(type==1 || type==2 || type==3)
			equipnumber_exclude.add(excludeY(key1, dbtype).toString());
		
		if(type==1 || type==2)
		{
			equipnumber_exclude.add(excludeD(key1, dbtype).toString());
			equipnumber_exclude.add(excludeA(key1, dbtype).toString());
			equipnumber_exclude.add(excludeG(key1, dbtype).toString());
		}

		return equipnumber_exclude;
	}
	
	
	private List<String> equipnumber_generator(int type)//type=1:less 1000  2:more 10000 3:100 least
	{
		if(type==1 || type==2 || type == 3)
		{
			equipnumber.add("E");
			equipnumber.add("N");
			equipnumber.add("Z");
			equipnumber.add("S");
			equipnumber.add("P");
			equipnumber.add("C");
			equipnumber.add("T");
			equipnumber.add("K");
			equipnumber.add("B");
			equipnumber.add("X");
		}
		//D
		if(type==1 || type==2)
		{
		for(int i=0;i<10;i++)
			equipnumber.add("D0"+i);
		for(int i=1;i<=7;i++)
			equipnumber.add("D"+i);
		for(int i=0;i<10;i++)
			equipnumber.add("D8"+i);
		for(int i=0;i<10;i++)
			equipnumber.add("D9"+i);
		}
		else if(type==3)
		{
			equipnumber.add("D");
		}

		//Y
		if(type==1)
		{
			//Y0
			for(int i=0;i<1000;i++)
					equipnumber.add("Y"+String.format("%04d",i));
			//Y1 Y2
			for(int i=100;i<300;i++)
				equipnumber.add("Y"+String.format("%03d",i));
		}
		else if(type==2)
		{
			for(int i=0;i<10000;i++)
				equipnumber.add("Y"+String.format("%05d",i));
			for(int i=1000;i<3000;i++)
				equipnumber.add("Y"+String.format("%04d",i));
		}
		else if(type==3)
		{
			/*全資料比對
			for(int i=0;i<100;i++)
				equipnumber.add("Y"+String.format("%03d",i));
				*/
			//da整測 篩選比對
			for(int i=0;i<10;i++)
				equipnumber.add("Y"+String.format("%02d",i));
			
			for(int i=10;i<30;i++)
				equipnumber.add("Y"+String.format("%02d",i));
		}
		
		
		if(type==1 || type==2)
		{
			for(int i=0;i<10;i++)
			{
				equipnumber.add("Y3"+i);
			}
			for(int i=0;i<10;i++)
			{
				equipnumber.add("Y4"+i);
			}
		}
		else if(type==3)
		{
			equipnumber.add("Y3");
			equipnumber.add("Y4");
		}

		if(type==1 || type==2)
		{
			//Y900
			for(int i=0;i<10;i++)
					equipnumber.add("Y900"+i);
			//Y901~Y909
			for(int i=1;i<10;i++)
				equipnumber.add("Y90"+i);
			//Y91~Y99
			for(int i=1;i<10;i++)
				equipnumber.add("Y9"+i);
		}
		else if(type==3)
		{
			equipnumber.add("Y9");
		}

		if(type==1 || type==2 || type == 3)
		{
		equipnumber.add("Y5");
		equipnumber.add("YD");
		equipnumber.add("YV");
		}
		
		if(type==4)
		{
			for(int i=0;i<10;i++)
				equipnumber.add("Y"+i);
		}
		//other Y
		
		
		//M
		if(type==1)
		{
			//M000~M012
			for(int i=0;i<130;i++)
				equipnumber.add("M0"+String.format("%03d",i));
			//M013~M019
			for(int i=3;i<9;i++)
				equipnumber.add("M01"+i);
			//M100~M117
			for(int i=0;i<180;i++)
				equipnumber.add("M1"+String.format("%03d",i));
			equipnumber.add("M118");equipnumber.add("M119");
			//M500~M506
			for(int i=0;i<70;i++)
				equipnumber.add("M5"+String.format("%03d",i));
			equipnumber.add("M507");equipnumber.add("M508");equipnumber.add("M509");
			//M700~M708
			for(int i=0;i<90;i++)
				equipnumber.add("M7"+String.format("%03d",i));
			equipnumber.add("M709");
			//M800~M802
			for(int i=0;i<30;i++)
				equipnumber.add("M8"+String.format("%03d",i));
			//M900~M912
			for(int i=0;i<130;i++)
				equipnumber.add("M9"+String.format("%03d",i));

		}
		else if(type==2)
		{
			//M000~M012
			for(int i=0;i<1300;i++)
				equipnumber.add("M0"+String.format("%04d",i));
			//M013~M019
			for(int i=3;i<9;i++)
				equipnumber.add("M01"+i);
			//M100~M117
			for(int i=0;i<1800;i++)
				equipnumber.add("M1"+String.format("%04d",i));
			equipnumber.add("M118");equipnumber.add("M119");
			//M500~M506
			for(int i=0;i<700;i++)
				equipnumber.add("M5"+String.format("%04d",i));
			equipnumber.add("M507");equipnumber.add("M508");equipnumber.add("M509");
			//M700~M708
			for(int i=0;i<900;i++)
				equipnumber.add("M7"+String.format("%04d",i));
			equipnumber.add("M709");
			//M800~M802
			for(int i=0;i<300;i++)
				equipnumber.add("M8"+String.format("%04d",i));
			//M900~M912
			for(int i=0;i<1300;i++)
				equipnumber.add("M9"+String.format("%04d",i));

		}
		else if(type==3)
		{
			//M000~M012
			for(int i=0;i<13;i++)
				equipnumber.add("M0"+String.format("%02d",i));

			//M100~M117
			for(int i=0;i<18;i++)
				equipnumber.add("M1"+String.format("%02d",i));
			//M500~M506
			for(int i=0;i<7;i++)
				equipnumber.add("M5"+String.format("%02d",i));
			//M700~M708
			for(int i=0;i<9;i++)
				equipnumber.add("M7"+String.format("%02d",i));
			//M800~M802
			for(int i=0;i<3;i++)
				equipnumber.add("M8"+String.format("%02d",i));
			//M900~M912
			for(int i=0;i<13;i++)
				equipnumber.add("M9"+String.format("%02d",i));
		}
		else if(type==4)
		{
			for(int i=0;i<10;i++)
				equipnumber.add("M"+i);
		}
	
		
		if(type==1 || type==2)
		{
			for(int i=0;i<10;i++)
				equipnumber.add("G"+i);
			
			//A
			for(int i=0;i<10;i++)
				equipnumber.add("A0"+i);
			for(int i=1;i<=9;i++)
				equipnumber.add("A"+i);
		}
		else if(type==3)
		{
			equipnumber.add("G");
			equipnumber.add("A");
		}

		
		return equipnumber;
	}
	private StringBuffer exclude(String key1, int dbtype)
	{
		StringBuffer sb = new StringBuffer();
		if(dbtype==1)
			sb.append("substr(").append(key1).append(",0,1) in (")
			  .append("'F',")
			  .append("'H',")
			  .append("'I',")
			  .append("'J',")
			  .append("'L',")
			  .append("'O',")
			  .append("'Q',")
			  .append("'R',")
			  .append("'U',")
			  .append("'V',")
			  .append("'W',")
			  .append("'0',")
			  .append("'1',")
			  .append("'2',")
			  .append("'3',")
			  .append("'4',")
			  .append("'5',")
			  .append("'6',")
			  .append("'7',")
			  .append("'8',")
			  .append("'9'").append(")");
		
		else if(dbtype==2)
		sb.append("left(").append(key1).append(",1) in (")
		  .append("'F',")
		  .append("'H',")
		  .append("'I',")
		  .append("'J',")
		  .append("'L',")
		  .append("'O',")
		  .append("'Q',")
		  .append("'R',")
		  .append("'U',")
		  .append("'V',")
		  .append("'W',")
		  .append("'0',")
		  .append("'1',")
		  .append("'2',")
		  .append("'3',")
		  .append("'4',")
		  .append("'5',")
		  .append("'6',")
		  .append("'7',")
		  .append("'8',")
		  .append("'9'").append(")");
		return sb;
	}
	private StringBuffer excludeType4(String key1, int dbtype)
	{
		StringBuffer sb = new StringBuffer();
		if(dbtype==1)
			sb.append("substr(").append(key1).append(",0,2) not in(")
			  .append("'M0',")
			  .append("'M1',")
			  .append("'M2',")
			  .append("'M3',")
			  .append("'M4',")
			  .append("'M5',")
			  .append("'M6',")
			  .append("'M7',")
			  .append("'M8',")
			  .append("'M9',")
			  .append("'Y0',")
			  .append("'Y1',")
			  .append("'Y2',")
			  .append("'Y3',")
			  .append("'Y4',")
			  .append("'Y5',")
			  .append("'Y6',")
			  .append("'Y7',")
			  .append("'Y8',")
			  .append("'Y9')");
		if(dbtype==2)
			sb.append("left(").append(key1).append(",2) not in(")
			  .append("'M0',")
			  .append("'M1',")
			  .append("'M2',")
			  .append("'M3',")
			  .append("'M4',")
			  .append("'M5',")
			  .append("'M6',")
			  .append("'M7',")
			  .append("'M8',")
			  .append("'M9',")
			  .append("'Y0',")
			  .append("'Y1',")
			  .append("'Y2',")
			  .append("'Y3',")
			  .append("'Y4',")
			  .append("'Y5',")
			  .append("'Y6',")
			  .append("'Y7',")
			  .append("'Y8',")
			  .append("'Y9')");
		return sb;
	}
	
	private StringBuffer excludeM(String key1, int dbtype)
	{
		StringBuffer sb = new StringBuffer();
		if(dbtype==1)
			sb.append("((substr(").append(key1).append(",0,1)='M'").append(" and ")
			  .append("substr(").append(key1).append(",0,2) not in(")
			  .append("'M0',")
			  .append("'M1',")
			  .append("'M5',")
			  .append("'M7',")
			  .append("'M8',")
			  .append("'M9'))").append(" or ")
	   		  .append("substr(").append(key1).append(",0,3) in (")
	   		  .append("'M02',")
	   		  .append("'M03',")
	   		  .append("'M04',")
	   		.append("'M05',")
	   		.append("'M06',")
	   		.append("'M07',")
	   		.append("'M08',")
	   		.append("'M09',")
	   		.append("'M12',")
	   		.append("'M13',")
	   		.append("'M14',")
	   		.append("'M15',")
	   		.append("'M16',")
	   		.append("'M17',")
	   		.append("'M18',")
	   		.append("'M19',")
	   		.append("'M51',")
	   		.append("'M52',")
	   		.append("'M53',")
	   		.append("'M54',")
	   		.append("'M55',")
	   		.append("'M56',")
	   		.append("'M57',")
	   		.append("'M58',")
	   		.append("'M59',")
	   		.append("'M71',")
	   		.append("'M72',")
	   		.append("'M73',")
	   		.append("'M74',")
	   		.append("'M75',")
	   		.append("'M76',")
	   		.append("'M77',")
	   		.append("'M78',")
	   		.append("'M79',")
	   		.append("'M81',")
	   		.append("'M82',")
	   		.append("'M83',")
	   		.append("'M84',")
	   		.append("'M85',")
	   		.append("'M86',")
	   		.append("'M87',")
	   		.append("'M88',")
	   		.append("'M89',")
	   		.append("'M92',")
	   		.append("'M93',")
	   		.append("'M94',")
	   		.append("'M95',")
	   		.append("'M96',")
	   		.append("'M97',")
	   		.append("'M98',")
	   		.append("'M99')").append(" or ")
	   		  .append("substr(").append(key1).append(",0,4) in (")
	   		.append("'M803',")
	   		.append("'M804',")
	   		.append("'M805',")
	   		.append("'M806',")
	   		.append("'M807',")
	   		.append("'M808',")
	   		.append("'M809',")
	   		.append("'M913',")
	   		.append("'M914',")
	   		.append("'M915',")
	   		.append("'M916',")
	   		.append("'M917',")
	   		.append("'M918',")
	   		.append("'M919'))");
		
		else if(dbtype==2)
		sb.append("((left(").append(key1).append(",1)='M'").append(" and ")
		  .append("left(").append(key1).append(",2) not in (")
		  .append("'M0',")
		  .append("'M1',")
		  .append("'M5',")
		  .append("'M7',")
		  .append("'M8',")
		  .append("'M9'))").append(" or ")
		  .append("left(").append(key1).append(",3) in (")
		  .append("'M02',")
   		  .append("'M03',")
   		  .append("'M04',")
   		.append("'M05',")
   		.append("'M06',")
   		.append("'M07',")
   		.append("'M08',")
   		.append("'M09',")
   		.append("'M12',")
   		.append("'M13',")
   		.append("'M14',")
   		.append("'M15',")
   		.append("'M16',")
   		.append("'M17',")
   		.append("'M18',")
   		.append("'M19',")
   		.append("'M51',")
   		.append("'M52',")
   		.append("'M53',")
   		.append("'M54',")
   		.append("'M55',")
   		.append("'M56',")
   		.append("'M57',")
   		.append("'M58',")
   		.append("'M59',")
   		.append("'M71',")
   		.append("'M72',")
   		.append("'M73',")
   		.append("'M74',")
   		.append("'M75',")
   		.append("'M76',")
   		.append("'M77',")
   		.append("'M78',")
   		.append("'M79',")
   		.append("'M81',")
   		.append("'M82',")
   		.append("'M83',")
   		.append("'M84',")
   		.append("'M85',")
   		.append("'M86',")
   		.append("'M87',")
   		.append("'M88',")
   		.append("'M89',")
   		.append("'M92',")
   		.append("'M93',")
   		.append("'M94',")
   		.append("'M95',")
   		.append("'M96',")
   		.append("'M97',")
   		.append("'M98',")
   		.append("'M99')").append(" or ")
   		  .append("left(").append(key1).append(",4) in (")
   		.append("'M803',")
   		.append("'M804',")
   		.append("'M805',")
   		.append("'M806',")
   		.append("'M807',")
   		.append("'M808',")
   		.append("'M809',")
   		.append("'M913',")
   		.append("'M914',")
   		.append("'M915',")
   		.append("'M916',")
   		.append("'M917',")
   		.append("'M918',")
   		.append("'M919'))");
   		 
		return sb;
	}
	private StringBuffer excludeMtype31(String key1, int dbtype)
	{
		StringBuffer sb = new StringBuffer();
		if(dbtype==1)
			sb.append("(substr(").append(key1).append(",0,1)='M'").append(" and ")
			  .append("substr(").append(key1).append(",0,2) not in (")
			  .append("'M0',")
			  .append("'M1',")
			  .append("'M5',")
			  .append("'M7',")
			  .append("'M8',")
			  .append("'M9'))");
		else if(dbtype==2)
			sb.append("(left(").append(key1).append(",1)='M'").append(" and ")
			  .append("left(").append(key1).append(",2) not in (")
			  .append("'M0',")
			  .append("'M1',")
			  .append("'M5',")
			  .append("'M7',")
			  .append("'M8',")
			  .append("'M9'))");;
		
		return sb;
	}
	private StringBuffer excludeMtype32(String key1, int dbtype)
	{
		StringBuffer sb = new StringBuffer();
		if(dbtype==1)
			sb.append("substr(").append(key1).append(",0,4) in (")
			.append("'M013',")
			.append("'M014',")
			.append("'M015',")
			.append("'M016',")
			.append("'M017',")
			.append("'M018',")
			.append("'M019',")
			.append("'M118',")
			.append("'M119',")
			.append("'M507',")
			.append("'M508',")
			.append("'M509')").append(" or ")
			.append("substr(").append(key1).append(",0,3) in (")
			.append("'M02',")
			.append("'M03',")
			.append("'M04',")
			.append("'M05',")
			.append("'M06',")
			.append("'M07',")
			.append("'M08',")
			.append("'M09',")
			.append("'M12',")
			.append("'M13',")
			.append("'M14',")
			.append("'M15',")
			.append("'M16',")
			.append("'M17',")
			.append("'M18',")
			.append("'M19',")
			.append("'M51',")
			.append("'M52',")
			.append("'M53',")
			.append("'M54',")
			.append("'M55',")
			.append("'M56',")
			.append("'M57',")
			.append("'M58',")
			.append("'M59')");;
		
		if(dbtype==2)
			sb.append("left(").append(key1).append(",4) in (")
			.append("'M013',")
			.append("'M014',")
			.append("'M015',")
			.append("'M016',")
			.append("'M017',")
			.append("'M018',")
			.append("'M019',")
			.append("'M118',")
			.append("'M119',")
			.append("'M507',")
			.append("'M508',")
			.append("'M509')").append(" or ")
	 		  .append("left(").append(key1).append(",3) in (")
 		  	.append("'M02',")
			.append("'M03',")
			.append("'M04',")
			.append("'M05',")
			.append("'M06',")
			.append("'M07',")
			.append("'M08',")
			.append("'M09',")
			.append("'M12',")
			.append("'M13',")
			.append("'M14',")
			.append("'M15',")
			.append("'M16',")
			.append("'M17',")
			.append("'M18',")
			.append("'M19',")
			.append("'M51',")
			.append("'M52',")
			.append("'M53',")
			.append("'M54',")
			.append("'M55',")
			.append("'M56',")
			.append("'M57',")
			.append("'M58',")
			.append("'M59')");;
		
		return sb;
	}
	private StringBuffer excludeMtype33(String key1, int dbtype)
	{
		StringBuffer sb = new StringBuffer();
		if(dbtype==1)
			sb.append("substr(").append(key1).append(",0,3) in (")
			.append("'M71',")
			.append("'M72',")
			.append("'M73',")
			.append("'M74',")
			.append("'M75',")
			.append("'M76',")
			.append("'M77',")
			.append("'M78',")
			.append("'M79',")
			.append("'M81',")
			.append("'M82',")
			.append("'M83',")
			.append("'M84',")
			.append("'M85',")
			.append("'M86',")
			.append("'M87',")
			.append("'M88',")
			.append("'M89',")
			.append("'M92',")
			.append("'M93',")
			.append("'M94',")
			.append("'M95',")
			.append("'M96',")
			.append("'M97',")
			.append("'M98',")
			.append("'M99')").append(" or ")
			.append("substr(").append(key1).append(",0,4) in (")
			.append("'M803',")
			.append("'M804',")
			.append("'M805',")
			.append("'M806',")
			.append("'M807',")
			.append("'M808',")
			.append("'M809',")
			.append("'M913',")
			.append("'M914',")
			.append("'M915',")
			.append("'M916',")
			.append("'M917',")
			.append("'M918',")
			.append("'M919')");
		

		if(dbtype==2)
			sb.append("left(").append(key1).append(",3) in (")
			.append("'M71',")
			.append("'M72',")
			.append("'M73',")
			.append("'M74',")
			.append("'M75',")
			.append("'M76',")
			.append("'M77',")
			.append("'M78',")
			.append("'M79',")
			.append("'M81',")
			.append("'M82',")
			.append("'M83',")
			.append("'M84',")
			.append("'M85',")
			.append("'M86',")
			.append("'M87',")
			.append("'M88',")
			.append("'M89',")
			.append("'M92',")
			.append("'M93',")
			.append("'M94',")
			.append("'M95',")
			.append("'M96',")
			.append("'M97',")
			.append("'M98',")
			.append("'M99')").append(" or ")
			.append("left(").append(key1).append(",4) in (")
	 		.append("'M803',")
			.append("'M804',")
			.append("'M805',")
			.append("'M806',")
			.append("'M807',")
			.append("'M808',")
			.append("'M809',")
			.append("'M913',")
			.append("'M914',")
			.append("'M915',")
			.append("'M916',")
			.append("'M917',")
			.append("'M918',")
			.append("'M919')");
		
		return sb;
	}
	private StringBuffer excludeY(String key1, int dbtype)
	{
		StringBuffer sb = new StringBuffer();
		if(dbtype==1)
			sb.append("substr(").append(key1).append(",0,1)='Y'").append(" and ")
			  .append("substr(").append(key1).append(",0,2) not in (")
			  .append("'Y0',")
			  .append("'Y1',")
			  .append("'Y2',")
			  .append("'Y3',")
			  .append("'Y4',")
			  .append("'Y5',")
			  .append("'Y9',")
			  .append("'YD',")
			  .append("'YV'").append(")");
		
		else if(dbtype==2)
			sb.append("left(").append(key1).append(",1)='Y'").append(" and ")
			  .append("left(").append(key1).append(",2) not in (")
			  .append("'Y0',")
			  .append("'Y1',")
			  .append("'Y2',")
			  .append("'Y3',")
			  .append("'Y4',")
			  .append("'Y5',")
			  .append("'Y9',")
			  .append("'YD',")
			  .append("'YV'").append(")");
		return sb;
	}
	private StringBuffer excludeD(String key1, int dbtype)
	{
		StringBuffer sb = new StringBuffer();
		if(dbtype==1)
			sb.append("substr(").append(key1).append(",0,1)='D'").append(" and ")
			  .append("substr(").append(key1).append(",0,2) not in (")
			  .append("'D0',")
			  .append("'D1',")
			  .append("'D2',")
			  .append("'D3',")
			  .append("'D4',")
			  .append("'D5',")
			  .append("'D6',")
			  .append("'D7',")
			  .append("'D8',")
			  .append("'D9')");
		
		else if(dbtype==2)
		sb.append("left(").append(key1).append(",1)='D'").append(" and ")
		  .append("left(").append(key1).append(",2) not in(")
		  .append("'D0',")
		  .append("'D1',")
		  .append("'D2',")
		  .append("'D3',")
		  .append("'D4',")
		  .append("'D5',")
		  .append("'D6',")
		  .append("'D7',")
		  .append("'D8',")
		  .append("'D9')");
		return sb;
	}
	private StringBuffer excludeA(String key1, int dbtype)
	{
		StringBuffer sb = new StringBuffer();
		if(dbtype==1)
			sb.append("substr(").append(key1).append(",0,1)='A'").append(" and ")
			  .append("substr(").append(key1).append(",0,2) not in (")
			  .append("'A0',")
			  .append("'A1',")
			  .append("'A2',")
			  .append("'A3',")
			  .append("'A4',")
			  .append("'A5',")
			  .append("'A6',")
			  .append("'A7',")
			  .append("'A8',")
			  .append("'A9')");
		
		else if(dbtype==2)
		sb.append("left(").append(key1).append(",1)='A'").append(" and ")
		  .append("left(").append(key1).append(",2) not in (")
		  .append("'A0',")
		  .append("'A1',")
		  .append("'A2',")
		  .append("'A3',")
		  .append("'A4',")
		  .append("'A5',")
		  .append("'A6',")
		  .append("'A7',")
		  .append("'A8',")
		  .append("'A9')");
		return sb;
	}
	private StringBuffer excludeG(String key1, int dbtype)
	{
		StringBuffer sb = new StringBuffer();
		if(dbtype==1)
			sb.append("substr(").append(key1).append(",0,1)='G'").append(" and ")
			  .append("substr(").append(key1).append(",0,2) not in (")
			  .append("'G0',")
			  .append("'G1',")
			  .append("'G2',")
			  .append("'G3',")
			  .append("'G4',")
			  .append("'G5',")
			  .append("'G6',")
			  .append("'G7',")
			  .append("'G8',")
			  .append("'G9')");
		
		else if(dbtype==2)
		sb.append("left(").append(key1).append(",1)='G'").append(" and ")
		  .append("left(").append(key1).append(",2) not in (")
		  .append("'G0',")
		  .append("'G1',")
		  .append("'G2',")
		  .append("'G3',")
		  .append("'G4',")
		  .append("'G5',")
		  .append("'G6',")
		  .append("'G7',")
		  .append("'G8',")
		  .append("'G9')");
		return sb;
	}
}
