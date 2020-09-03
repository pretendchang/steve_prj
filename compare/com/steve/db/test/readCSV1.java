package com.steve.db.test;

import java.io.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.*;
import com.houseagent.*;
import com.houseagent.data.util;
import com.steve.db.data.manager.DSManager;
public class readCSV1 {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception
	{
		read();
	}
	
	public static void read() throws Exception
	{
		// TODO Auto-generated method stub
		String fileName = "c:\\g.txt";
		String strhousesql;

	    FileReader fr = new FileReader( fileName );//讀檔

	    BufferedReader stdin = new BufferedReader( fr );
	        //將fr置入BufferedReader, 只有BufferedReader能readLine()



	    StringTokenizer stoken = null;
	    
	    Connection db_conn = (Connection) DSManager.getInstance().getConn("sql://localhost");
	    
	    try
	    {int aa=0;
	    	while( stdin.ready() ){
	    		System.out.println(aa++);
	    		PreparedStatement stmt = 
	        		db_conn.prepareStatement("insert into house (DataCreatedTime,govarea1,govarea2,govarea3,BuildingNumber1,BuildingNumber2,HouseRegisteredTime,HouseLocRoad,HouseLocAlley,HouseLocAlleyNo,HouseLocNo,HouseLocDashNo,HouseLocNoDDash,HouseLocFloor,HouseLocFloorDash,HouseLocOddEven,HouseType,MainBuilding,BusinessArea,MainUsage,MainMaterial,HouseFinishedTime,TotalFloor,TotalAreaOfMainBuilding,TotalAreaOfExtBuilding,TotalAreaOfPublic,TotalAreaOfHouse,HouseLocRoadSec) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

//	    		strhousesql="insert into house (`DataCreatedTime`,`govarea1`,`govarea2`,`govarea3`,`BuildingNumber1`,`BuildingNumber2`,
	    		//`HouseRegisteredTime`,`HouseLocRoad`(8),`HouseLocAlley`,`HouseLocAlleyNo`,`HouseLocNo`,`HouseLocDashNo`,`HouseLocNoDDash`,`HouseLocFloor`,`HouseLocFloorDash`,`HouseLocOddEven`,
	    		//`HouseType`(17),`MainBuilding`,`BusinessArea`,`MainUsage`,`MainMaterial`,`HouseFinishedTime`,`TotalFloor`,
	    		//`TotalAreaOfMainBuilding(24)`,`TotalAreaOfExtBuilding`,`TotalAreaOfPublic`,`TotalAreaOfHouse`,HouseLocRoadSec) values ";
	    		String []arrs = stdin.readLine().split("	");

	        		if(arrs[1].equals(new String("")))
	        			stmt.setDate(1,  new java.sql.Date(util.String2Date("2012/4/22").getTime()));
	        		else
	        			stmt.setDate(1,  new java.sql.Date(util.String2Date(arrs[1]).getTime()));
	        		
	        		arrs[2]=arrs[2].trim();
	        		int regionid1 = com.houseagent.data.AddressRegion.getDataID(1, arrs[2]);
	        		arrs[3]=arrs[3].trim();
	        		int regionid2 = com.houseagent.data.AddressRegion.getDataID(2, arrs[3]);
	        		arrs[4]=arrs[4].trim();
	        		int regionid3 = com.houseagent.data.AddressRegion.getDataID(3, arrs[4]);
	        		
	        		if(regionid1==-1)
	        			regionid1 = com.houseagent.data.AddressRegion.create(arrs[2], 1, 0);
	        		stmt.setInt(2, regionid1);
	        		
	        		if(regionid2==-1)
	        			regionid2 = com.houseagent.data.AddressRegion.create(arrs[3], 2, regionid1);
	        		stmt.setInt(3, regionid2);
	        		
	        		if(regionid3==-1)
	        			regionid3 = com.houseagent.data.AddressRegion.create(arrs[4], 3, regionid2);
	        		stmt.setInt(4, regionid3);
	        		
	        		String []as = arrs[5].split("-");
	        		stmt.setString(5,as[0]);
	        		stmt.setString(6,as[1]);
	        		stmt.setDate(7, new java.sql.Date(util.String2Date(arrs[14]).getTime()));
	        		stmt.setString(8, arrs[41]);
	        		stmt.setString(9, null);
	        		stmt.setInt(10, -1);
	        		stmt.setInt(11, -1);
	        		stmt.setInt(12, -1);
	        		stmt.setInt(13, -1);
	        		stmt.setInt(14, -1);
	        		stmt.setInt(15, -1);
	        		stmt.setInt(16, -1);
	        		stmt.setInt(28, -1);

	        		for(int i=42;i<arrs.length;i++)
	        		{
		        		if(arrs[i]!=null)
		        			setstmt(stmt,arrs[i]);
	        		}

	        		stmt.setInt(17, 1);//todo housetype
	        		stmt.setString(18, null);
	        		stmt.setString(19, arrs[0]);
	        		stmt.setString(20, arrs[7]);
	        		stmt.setString(21, arrs[8]);//主要建材
	        		stmt.setDate(22, new java.sql.Date(util.String2Date(arrs[14]).getTime()));
	        		if(arrs[9].equals(new String("")))
	        			stmt.setInt(23, 0);
	        		else
	        			stmt.setInt(23, Integer.parseInt(arrs[9]));
	        		
	        		if(arrs[12].equals(new String("")))
	        			stmt.setBigDecimal(24, new BigDecimal("0"));
	        		else
	        			stmt.setBigDecimal(24, new BigDecimal(arrs[12]));//主建物
	        		
	        		if(arrs[19].equals(new String("")))
	        			stmt.setBigDecimal(25, new BigDecimal("0"));
	        		else
	        			stmt.setBigDecimal(25, new BigDecimal(arrs[19]));
	        		
	        		if(arrs[33].equals(new String("")))
	        			stmt.setBigDecimal(26, new BigDecimal("0"));
	        		else
	        			stmt.setBigDecimal(26, new BigDecimal(arrs[33]));
	        		
	        		if(arrs[13].equals(new String("")))
	        			stmt.setBigDecimal(27, new BigDecimal("0"));
	        		else
	        			stmt.setBigDecimal(27, new BigDecimal(arrs[13]));
	        		stmt.executeUpdate();
	        		stmt.close();
	        		
	        		stmt = db_conn.prepareStatement("select dataid from house where buildingnumber1=?");
	        		stmt.setString(1, as[0]);
	        		int dataid=0;
	        		ResultSet rs = stmt.executeQuery();
	        		while(rs.next())
	        		{
	        			dataid=rs.getInt("dataid");
	        		}
	        		rs.close();
	        		stmt.close();
	        		
	        		String []areaas;
	        		if(arrs[6].indexOf("：")!=-1)
	        			areaas= arrs[6].split("：");
	        		else
	        			areaas= arrs[6].split(" ");
	        		for(int i=0;i<areaas.length;i++)
	        		{
		        		stmt = db_conn.prepareStatement("insert into houseloc (houseid, areasection,areanumber) values(?,?,?)");
		        		stmt.setInt(1, dataid);
		        		stmt.setString(2, arrs[4]);
		        		stmt.setString(3, areaas[i]);
		        		stmt.executeUpdate();
		        		stmt.close();
	        		}
	        		
	        		stmt = db_conn.prepareStatement("insert into houselayer (houseid, layername,layerarea) values(?,?,?)");
	        		stmt.setInt(1, dataid);
	        		stmt.setString(2, arrs[10]);
	        		if(arrs[13].equals(new String("")))
	        			stmt.setBigDecimal(3, new BigDecimal("0"));
	        		else
	        			stmt.setBigDecimal(3, new BigDecimal(arrs[13]));
	        		stmt.executeUpdate();
	        		stmt.close();
	        		
	        		BigDecimal tmp;
	        		if(arrs[16].equals(new String("")))
	        			tmp=new BigDecimal("0");
	        		else
	        			tmp= new BigDecimal(arrs[16]);
	        		if(tmp.floatValue()>0)
	        		{
		        		stmt = db_conn.prepareStatement("insert into HouseExtBuilding (houseid, extbuildingusage,extbuildingarea) values(?,?,?)");
		        		stmt.setInt(1, dataid);
		        		stmt.setString(2, "陽台");
		        		stmt.setBigDecimal(3, new BigDecimal(arrs[16]));
		        		stmt.executeUpdate();
		        		stmt.close();
	        		}
	        		if(arrs[18].equals(new String("")))
	        			tmp=new BigDecimal("0");
	        		else
	        			tmp = new BigDecimal(arrs[18]);
	        		if(tmp.floatValue()>0)
	        		{
		        		stmt = db_conn.prepareStatement("insert into HouseExtBuilding (houseid, extbuildingusage,extbuildingarea) values(?,?,?)");
		        		stmt.setInt(1, dataid);
		        		stmt.setString(2, "其他");
		        		stmt.setBigDecimal(3, new BigDecimal(arrs[18]));
		        		stmt.executeUpdate();
		        		stmt.close();
	        		}
	        		if(arrs[23].equals(new String("")))
	        			tmp=new BigDecimal("0");
	        		else
	        			tmp = new BigDecimal(arrs[23]);
	        		if(tmp.floatValue()>0)
	        		{
		        		stmt = db_conn.prepareStatement("insert into houseshare (houseid, shareno,sharearea,sharepart,sharedividedby) values(?,?,?,?,?)");
		        		stmt.setInt(1, dataid);
		        		stmt.setString(2, null);
		        		stmt.setBigDecimal(3, new BigDecimal(tmp.floatValue()*0.3025));
		        		stmt.setBigDecimal(4, new BigDecimal(arrs[21]));
		        		stmt.setBigDecimal(5, new BigDecimal(arrs[22]));
		        		stmt.executeUpdate();
		        		stmt.close();
	        		}
	        		
	        		if(arrs[27].equals(new String("")))
	        			tmp=new BigDecimal("0");
	        		else
	        			tmp = new BigDecimal(arrs[27]);
	        		if(tmp.floatValue()>0)
	        		{
		        		stmt = db_conn.prepareStatement("insert into houseshare (houseid, shareno,sharearea,sharepart,sharedividedby) values(?,?,?,?,?)");
		        		stmt.setInt(1, dataid);
		        		stmt.setString(2, null);
		        		stmt.setBigDecimal(3, new BigDecimal(tmp.floatValue()*0.3025));
		        		stmt.setBigDecimal(4, new BigDecimal(arrs[25]));
		        		stmt.setBigDecimal(5, new BigDecimal(arrs[26]));
		        		stmt.executeUpdate();
		        		stmt.close();
	        		}
	        		if(arrs[31].equals(new String("")))
	        			tmp=new BigDecimal("0");
	        		else
	        			tmp = new BigDecimal(arrs[31]);
	        		if(tmp.floatValue()>0)
	        		{
		        		stmt = db_conn.prepareStatement("insert into houseshare (houseid, shareno,sharearea,sharepart,sharedividedby) values(?,?,?,?,?)");
		        		stmt.setInt(1, dataid);
		        		stmt.setString(2, null);
		        		stmt.setBigDecimal(3, new BigDecimal(tmp.floatValue()*0.3025));
		        		stmt.setBigDecimal(4, new BigDecimal(arrs[29]));
		        		stmt.setBigDecimal(5, new BigDecimal(arrs[30]));
		        		stmt.executeUpdate();
		        		stmt.close();
	        		}
	        		
	        		String []owneras = arrs[38].split("：");
	        		for(int i=0;i<owneras.length;i++)
	        		{
		        		stmt = db_conn.prepareStatement("insert into houseowner (houseid, RegisteredOrder,RegisteredTime,RegisteredReason,RegisteredReasonTime,OwnerName,OwnerPart,OwnerDividedBy,govarea1,govarea2,HouseLocAddr) values(?,?,?,?,?,?,?,?,?,?,?)");
		        		stmt.setInt(1, dataid);
		        		if(arrs[35].equals(new String("")))
		        			stmt.setInt(2, 1);
		        		else
		        			stmt.setInt(2, Integer.parseInt(arrs[35]));
		        		stmt.setDate(3, new java.sql.Date(util.String2Date(arrs[37]).getTime()));
		        		stmt.setString(4, arrs[36]);
		        		stmt.setDate(5, new java.sql.Date(util.String2Date(arrs[37]).getTime()));
		        		
		        		stmt.setString(6, owneras[i]);//name
		        		
		        		String []ownerdivas = arrs[40].split("：");
		        		if(ownerdivas.length==owneras.length)
		        		{
		        			String []tmps=ownerdivas[i].split("/");
		        			stmt.setInt(7, Integer.parseInt(tmps[0]));//part
		        			stmt.setInt(8, Integer.parseInt(tmps[1]));//div
		        		}
		        		else
		        		{
		        			String []tmps;
		        			if(i>(ownerdivas.length-1))
		        			{
		        				tmps=ownerdivas[ownerdivas.length-1].split("/");
			        			stmt.setInt(7, Integer.parseInt(tmps[0]));//part
			        			stmt.setInt(8, Integer.parseInt(tmps[1]));//div
		        			}
		        			else
		        			{
			        			tmps=ownerdivas[i].split("/");
			        			stmt.setInt(7, Integer.parseInt(tmps[0]));//part
			        			stmt.setInt(8, Integer.parseInt(tmps[1]));//div
		        			}
		        		}
		        		stmt.setInt(9, 1);//gov1
		        		stmt.setInt(10, 1);//gov2
		        		
		        		String []owneraddras;
		        		owneraddras= arrs[39].split("：");
		        		if(owneraddras.length==owneras.length)
		        		{
		        			stmt.setString(11, owneraddras[i]);//addr
		        		}
		        		else
		        		{
		        			if(i>(owneraddras.length-1))
		        			{
		        				stmt.setString(11, owneraddras[owneraddras.length-1]);//addr
		        			}
		        			else
		        				stmt.setString(11, owneraddras[i]);//addr
		        		}
		        		stmt.executeUpdate();
		        		
		        		stmt.close();
	        		}
	        		
	        		
	        }
	    }catch(Exception e)
	    {
	    	e.printStackTrace();
	    }
	}
	
	public static void setstmt(PreparedStatement _stmt, String str) throws Exception
	{
		if(str.indexOf("段")!=-1)
		{
			String s = str.replace('段', '\0');
			s=s.trim();
			int locno=-1;
			if(s.matches("一"))
				locno=1;
			else if(s.matches("二"))
				locno=2;
			else if(s.matches("三"))
				locno=3;
			else if(s.matches("四"))
				locno=4;
			else if(s.matches("五"))
				locno=5;
			else
				locno = Integer.parseInt(s);
			_stmt.setInt(28, locno);
			return;
		}
		else if(str.indexOf("弄")!=-1)
		{
			String s = str.replace('弄', '\0');
			s=s.trim();
			int locno = Integer.parseInt(s);
			_stmt.setInt(10, locno);
			return;
		}
		else if(str.indexOf("巷")!=-1)
		{
			String s = str.replace('巷', '\0');
			s=s.trim();
			_stmt.setString(9, s);
			return;
		}
		else if(str.indexOf("樓")!=-1)
		{
			String s = str.replace('樓', '\0');
			s=s.trim();
			int locno;
			if(!s.matches("B1"))
				locno = Integer.parseInt(s);
			else
				locno=1001;
			_stmt.setInt(14, locno);
			return;
		}
		else if(str.indexOf("之")!=-1 && str.indexOf("號")!=-1)
		{//todo
			String s = str.replace('之', ' ');
			s = s.replace('號', '\0');
			s=s.trim();
			int locno;
			if(!s.matches("B1"))
				locno = Integer.parseInt(s);
			else
				locno=1001;
			_stmt.setInt(12, locno);
			return;
		}
		else if(str.indexOf("號")!=-1)
		{
			String s = str.replace('號', '\0');
			s=s.trim();
			int locno = Integer.parseInt(s);
    		_stmt.setInt(11, locno);
    		_stmt.setInt(16, locno%2);
			return;
		}
		else if(str.indexOf("之")!=-1 && str.indexOf("號")==-1)
		{
			String s = str.replace('之', '\0');
			s=s.trim();
			int locno = Integer.parseInt(s);
    		_stmt.setInt(15, locno);
			return;
		}
		else
		{
			int locno = Integer.parseInt(str);
    		_stmt.setInt(12, locno);
    		_stmt.setInt(16, locno%2);
			return;
		}
	}

}
