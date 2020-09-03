package com.steve.db.data.source.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.steve.db.data.manager.DBManagerRuntimeException;
import com.steve.db.data.source.DbDSO;
import com.steve.db.data.source.UrlDS;

public class h2dbDSO extends DbDSO {
	public synchronized Object getConn(UrlDS _url_ds) {
	      Connection con;
	      try
	      {
	    	  Class.forName ("org.h2.Driver");
	    	  con = DriverManager.getConnection(getConfig());
	    	  return con;
	      }
	      catch(SQLException e)
	      {
	    	  DBManagerRuntimeException ex = new DBManagerRuntimeException("cmpdata002", e);
	          throw ex;
	      }
	      catch(Exception e)
	      {
	    	  DBManagerRuntimeException ex = new DBManagerRuntimeException("cmpdata001", e);
	          throw ex;
	      }
	  }
}
