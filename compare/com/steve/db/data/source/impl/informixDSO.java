package com.steve.db.data.source.impl;

import com.steve.db.data.manager.DBManagerRuntimeException;
import com.steve.db.data.source.*;
import com.steve.db.data.source.support.*;

import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import java.sql.*;
import java.util.Properties;
/**
<pre>
    This class is used for accessing data from database data sources.
    A configuration file 'db.cfg' is needed to be set.
    Put the file in the path : '/opt/bom/etc'.
    The configuration sample is as the following :

pool.defaultMaxActive=30
pool.testOnBorrow=true
pool.validationQuery=SELECT 1

connection.driver = com.mysql.jdbc.Driver
connection.url = jdbc:mysql://localhost/bom?useUnicode=true&autoReconnect=true&maxReconnects=100&initialTimeout=10
connection.user = user
connection.password = password
</pre>

*/
public class informixDSO extends DbDSO
{
//  private static com.steve.util.Logger log = com.steve.util.Logger.getLogger(mysqlDSO.class);

  public synchronized Object getConn(UrlDS _url_ds) {
      Connection con;
      try
      {
    	  Properties pr = new Properties();
          pr.put("IFX_USEPUT","1");
    	  Class.forName ("com.informix.jdbc.IfxDriver");//System.out.println("String:"+getConfig());
    	  con = DriverManager.getConnection(getConfig(), pr);
    	  //con = DriverManager.getConnection("jdbc:informix-sqli://10.28.19.135:2953/bcmdb_trdb:informixserver=cb1server;NEWCODESET=big5,8859-1,819;IFX_USE_STRENC=true", "blmmqc", "3edc$RFV5tgb");
    	  //con = DriverManager.getConnection("jdbc:informix-sqli://10.97.22.158:2953/bcmdb_dev:informixServer=cb1server;NEWCODESET=big5,8859-1,819", "blmmqc", "1qaz@WSX3edc");

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
