package com.steve.db.data.source.support;

import java.io.*;
import java.sql.*;
import javax.sql.*;

import org.apache.commons.configuration.*;
import org.apache.commons.dbcp.datasources.*;
import org.apache.torque.*;
import org.apache.torque.dsfactory.*;
import com.steve.db.data.manager.*;
import com.steve.db.data.source.*;

public class SharedPoolDataSourceSupport implements DSO{

 private DataSource ds;
 private String configFilePath;

 public synchronized Object getConn(UrlDS _url_ds) throws DBManagerRuntimeException
 {
   Connection conn = null;
    try {
      conn = ds.getConnection();
    }
    catch (SQLException ex)
    {
      DBManagerRuntimeException e = new DBManagerRuntimeException("cmpdata002",ex);
      throw e;
    }
     return (Object) conn;
  }

 public SharedPoolDataSourceSupport(String _configfilepath) throws DBManagerRuntimeException
 {
   if(ds == null)
   {
	   org.apache.torque.dsfactory.SharedPoolDataSourceFactory ds_fctr = null;
    try {
    	org.apache.commons.configuration.PropertiesConfiguration cfg = new PropertiesConfiguration(_configfilepath);
      cfg.setFileName(_configfilepath);
      ds_fctr = new org.apache.torque.dsfactory.SharedPoolDataSourceFactory();
      ds_fctr.initialize(cfg);
    }
    catch (TorqueException ex) {
      DBManagerRuntimeException e = new DBManagerRuntimeException("cmpdata006",ex);
      throw e;
    }
    catch (IOException ex) {
      DBManagerRuntimeException e = new DBManagerRuntimeException("cmpdata006",ex);
      throw e;
    }
     ds = ds_fctr.getDataSource();
    }


   if(ds instanceof SharedPoolDataSource)
   {
     this.configFilePath = _configfilepath;
   }
   else
   {
     DBManagerRuntimeException e = new DBManagerRuntimeException("cmpdata006");
     throw e;
   }
 }

 public int getMaxActive()
 {
   if (this.ds != null)
     return ((SharedPoolDataSource)this.ds).getMaxActive();
   return -1;
 }

 public int getMaxIdle()
 {
   if (this.ds != null)
     return ((SharedPoolDataSource)this.ds).getMaxIdle();
   return -1;
 }

 public int getActive()
 {
   if (this.ds != null)
     return ((SharedPoolDataSource)this.ds).getNumActive();
   return -1;
 }

 public int getIdle()
 {
   if (this.ds != null)
     return ((SharedPoolDataSource)this.ds).getNumIdle();
   return -1;
 }

 public int getMaxWait()
 {
   if (this.ds != null)
     return ((SharedPoolDataSource)this.ds).getMaxWait();
   return -1;
 }
 public String getValidationQuery() {
   if (this.ds != null)
     return ( (SharedPoolDataSource)this.ds).getValidationQuery();
   return "-1";
 }

}
