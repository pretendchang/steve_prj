package com.steve.db.data.source.impl;

import com.steve.db.data.manager.DBManagerRuntimeException;
import com.steve.db.data.source.*;
import com.steve.db.data.source.support.*;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import java.sql.*;
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
public class jbossDSDSO extends DbDSO
{
//  private static com.steve.util.Logger log = com.steve.util.Logger.getLogger(mysqlDSO.class);

  public synchronized Object getConn(UrlDS _url_ds) {
	  Context ctx = null;
	  
      Connection con;
      try
      {
    	  
    	  ctx = new InitialContext();

	      //DataSource ds = (DataSource) ctx.lookup("java:jboss/datasource/vnvAUTOTEST");  // ***測試區(新舊fBMS)
		  DataSource ds = (DataSource) ctx.lookup(getConfig()); // ***正式區(新fBMS)
		  con = ds.getConnection();
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
