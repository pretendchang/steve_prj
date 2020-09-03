package com.cmpdata;

import java.sql.*;

import com.security.PathManipulation;
import com.steve.db.data.manager.DBManagerRuntimeException;
import com.steve.db.data.manager.DSManager;

public class RetryDBConnection {
	public Connection conn;
	public PreparedStatement pstmt;
	public ResultSet rs;
	
	public RetryDBConnection(Connection _conn, PreparedStatement _pstmt, ResultSet _rs)
	{
		conn = _conn;
		pstmt = _pstmt;
		rs = _rs;
	}
	
	public RetryDBConnection Retry(String dbconn, String sql)
	{
		int repeat=0;
		while(repeat<10)
		{
			try {
				if(rs!=null && !rs.isClosed())
				{
					rs.close();
				}
				/*
				if(pstmt!=null && !pstmt.isClosed())
					pstmt.close();
				*/
				conn.close();
				conn = (Connection)DSManager.getInstance().getConn(dbconn);
				
				if(sql != null)
				{
					sql = PathManipulation.filterSQL(sql);
					pstmt= conn.prepareStatement(sql);
					rs = pstmt.executeQuery();
				}
				break;
			} 
			catch(DBManagerRuntimeException e2)
			{
				e2.printStackTrace();
				rs=null;
				pstmt=null;					
			}
			catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				rs=null;
				pstmt=null;
			}
			try {
				repeat++;
				Thread.sleep(10000);
			} catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		try {
			if(conn.isClosed())
			{
				throw new DBManagerRuntimeException("cmpdata002","dbconnection error:"+dbconn);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this;
	}
}
