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
public class readCSV {

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
		String fileName = "c:\\f.csv";
		String strhousesql;

	    FileReader fr = new FileReader( fileName );//讀檔

	    BufferedReader stdin = new BufferedReader( fr );
	        //將fr置入BufferedReader, 只有BufferedReader能readLine()



	    StringTokenizer stoken = null;
	    
	    Connection db_conn = (Connection) DSManager.getInstance().getConn("sql://localhost");
	    
	    try
	    {
	    	while( stdin.ready() ){
	    		PreparedStatement stmt = 
	        		db_conn.prepareStatement("insert into house (DataCreatedTime,govarea1,govarea2,govarea3,BuildingNumber1,BuildingNumber2,HouseRegisteredTime,HouseLocRoad,HouseLocAlley,HouseLocAlleyNo,HouseLocNo,HouseLocDashNo,HouseLocNoDDash,HouseLocFloor,HouseLocFloorDash,HouseLocOddEven,HouseType,MainBuilding,BusinessArea,MainUsage,MainMaterial,HouseFinishedTime,TotalFloor,TotalAreaOfMainBuilding,TotalAreaOfExtBuilding,TotalAreaOfPublic,TotalAreaOfHouse,HouseLocRoadSec) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

//	    		strhousesql="insert into house (`DataCreatedTime`,`govarea1`,`govarea2`,`govarea3`,`BuildingNumber1`,`BuildingNumber2`,
	    		//`HouseRegisteredTime`,`HouseLocRoad`(8),`HouseLocAlley`,`HouseLocAlleyNo`,`HouseLocNo`,`HouseLocDashNo`,`HouseLocNoDDash`,`HouseLocFloor`,`HouseLocFloorDash`,`HouseLocOddEven`,
	    		//`HouseType`(17),`MainBuilding`,`BusinessArea`,`MainUsage`,`MainMaterial`,`HouseFinishedTime`,`TotalFloor`,
	    		//`TotalAreaOfMainBuilding(24)`,`TotalAreaOfExtBuilding`,`TotalAreaOfPublic`,`TotalAreaOfHouse`) values ";
	    		String []arrs = stdin.readLine().split(",");

	        		
	        		stmt.setDate(1,  new java.sql.Date(util.String2Date(arrs[1]).getTime()));
	        		stmt.setInt(2, 1);
	        		stmt.setInt(3, 3);
	        		stmt.setInt(4, 6);
	        		
	        		String []as = arrs[5].split("-");
	        		stmt.setString(5,as[0]);
	        		stmt.setString(6,as[1]);
	        		stmt.setDate(7, new java.sql.Date(util.String2Date(arrs[6]).getTime()));
	        		stmt.setString(8, arrs[7]+"路");
	        		stmt.setString(9, null);
	        		stmt.setInt(10, -1);
	        		stmt.setInt(28, Integer.parseInt(arrs[8]));
	        		int locno = Integer.parseInt(arrs[9]);
	        		stmt.setInt(11, locno);
	        		stmt.setInt(12, -1);
	        		stmt.setInt(13, -1);
	        		stmt.setInt(14, Integer.parseInt(arrs[10].trim()));
	        		stmt.setInt(15, Integer.parseInt(arrs[11].trim()));
	        		stmt.setInt(16, locno%2);
	        		stmt.setInt(17, 1);
	        		stmt.setString(18, arrs[0]);
	        		stmt.setString(19, null);
	        		stmt.setString(20, arrs[14]);
	        		stmt.setString(21, arrs[15]);//主要建材
	        		stmt.setDate(22, new java.sql.Date(util.String2Date(arrs[20]).getTime()));
	        		stmt.setInt(23, Integer.parseInt(arrs[16]));
	        		stmt.setBigDecimal(24, new BigDecimal(arrs[19]));//主建物
	        		stmt.setBigDecimal(25, new BigDecimal(arrs[25]));
	        		stmt.setBigDecimal(26, new BigDecimal(arrs[39]));
	        		stmt.setBigDecimal(27, new BigDecimal(arrs[41]));
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
	        		
	        		stmt = db_conn.prepareStatement("insert into houseloc (houseid, areasection,areanumber) values(?,?,?)");
	        		stmt.setInt(1, dataid);
	        		stmt.setString(2, "四維段");
	        		stmt.setString(3, arrs[13]);
	        		stmt.executeUpdate();
	        		stmt.close();
	        		
	        		stmt = db_conn.prepareStatement("insert into houselayer (houseid, layername,layerarea) values(?,?,?)");
	        		stmt.setInt(1, dataid);
	        		stmt.setString(2, arrs[17]);
	        		stmt.setBigDecimal(3, new BigDecimal(arrs[19]));
	        		stmt.executeUpdate();
	        		stmt.close();
	        		
	        		stmt = db_conn.prepareStatement("insert into HouseExtBuilding (houseid, extbuildingusage,extbuildingarea) values(?,?,?)");
	        		stmt.setInt(1, dataid);
	        		stmt.setString(2, "陽台");
	        		stmt.setBigDecimal(3, new BigDecimal(arrs[22]));
	        		stmt.executeUpdate();
	        		stmt.close();
	        		
	        		stmt = db_conn.prepareStatement("insert into HouseExtBuilding (houseid, extbuildingusage,extbuildingarea) values(?,?,?)");
	        		stmt.setInt(1, dataid);
	        		stmt.setString(2, "其他");
	        		stmt.setBigDecimal(3, new BigDecimal(arrs[24]));
	        		stmt.executeUpdate();
	        		stmt.close();
	        		
	        		stmt = db_conn.prepareStatement("insert into houseshare (houseid, shareno,sharearea,sharepart,sharedividedby) values(?,?,?,?,?)");
	        		stmt.setInt(1, dataid);
	        		stmt.setString(2, null);
	        		stmt.setBigDecimal(3, new BigDecimal(arrs[42]));
	        		stmt.setBigDecimal(4, new BigDecimal(arrs[27]));
	        		stmt.setBigDecimal(5, new BigDecimal(arrs[28]));
	        		stmt.executeUpdate();
	        		stmt.close();
	        		
	        		stmt = db_conn.prepareStatement("insert into houseshare (houseid, shareno,sharearea,sharepart,sharedividedby) values(?,?,?,?,?)");
	        		stmt.setInt(1, dataid);
	        		stmt.setString(2, null);
	        		stmt.setBigDecimal(3, new BigDecimal(arrs[43]));
	        		stmt.setBigDecimal(4, new BigDecimal(arrs[31]));
	        		stmt.setBigDecimal(5, new BigDecimal(arrs[32]));
	        		stmt.executeUpdate();
	        		stmt.close();
	        		
	        		stmt = db_conn.prepareStatement("insert into houseshare (houseid, shareno,sharearea,sharepart,sharedividedby) values(?,?,?,?,?)");
	        		stmt.setInt(1, dataid);
	        		stmt.setString(2, null);
	        		stmt.setBigDecimal(3, new BigDecimal(arrs[44]));
	        		stmt.setBigDecimal(4, new BigDecimal(arrs[35]));
	        		stmt.setBigDecimal(5, new BigDecimal(arrs[36]));
	        		stmt.executeUpdate();
	        		stmt.close();
	        		
	        		stmt = db_conn.prepareStatement("insert into houseowner (houseid, RegisteredOrder,RegisteredTime,RegisteredReason,RegisteredReasonTime,OwnerName,OwnerPart,OwnerDividedBy,govarea1,govarea2,HouseLocAddr) values(?,?,?,?,?,?,?,?,?,?,?)");
	        		stmt.setInt(1, dataid);
	        		stmt.setInt(2, Integer.parseInt(arrs[45]));
	        		stmt.setDate(3, new java.sql.Date(util.String2Date(arrs[46]).getTime()));
	        		stmt.setString(4, arrs[47]);
	        		stmt.setDate(5, new java.sql.Date(util.String2Date(arrs[48]).getTime()));
	        		stmt.setString(6, arrs[49]);
	        		stmt.setInt(7, Integer.parseInt(arrs[52]));
	        		stmt.setInt(8, Integer.parseInt(arrs[53]));
	        		stmt.setInt(9, 1);
	        		stmt.setInt(10, 1);
	        		stmt.setString(11, arrs[50]);
	        		stmt.executeUpdate();
	        		System.out.println(arrs.length);
	        		if(arrs.length>56)
	        		{
	        			stmt.setInt(1, dataid);
		        		stmt.setInt(2, Integer.parseInt(arrs[45]));
		        		stmt.setDate(3, new java.sql.Date(util.String2Date(arrs[46]).getTime()));
		        		stmt.setString(4, arrs[47]);
		        		stmt.setDate(5, new java.sql.Date(util.String2Date(arrs[48]).getTime()));
		        		stmt.setString(6, arrs[56]);
		        		stmt.setInt(7, Integer.parseInt(arrs[52]));
		        		stmt.setInt(8, Integer.parseInt(arrs[53]));
		        		stmt.setInt(9, 1);
		        		stmt.setInt(10, 1);
		        		stmt.setString(11, arrs[57]);
		        		stmt.executeUpdate();
	        		}
	        		stmt.close();

	        		
	        		
	        }
	    }catch(Exception e)
	    {
	    	e.printStackTrace();
	    }
	}

}
