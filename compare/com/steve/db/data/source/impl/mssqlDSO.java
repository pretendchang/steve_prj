package com.steve.db.data.source.impl;

import com.steve.db.data.source.*;
import com.steve.db.data.source.support.*;



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
public class mssqlDSO extends DbDSO
{
  private SharedPoolDataSourceSupport dsSupport;
  private static com.steve.util.Logger log = com.steve.util.Logger.getLogger(mssqlDSO.class);

  public synchronized Object getConn(UrlDS _url_ds) {
    log.info(getConfigFilePath());
    if(dsSupport == null)
    {
      dsSupport = new SharedPoolDataSourceSupport(getConfigFilePath());
    }

    return dsSupport.getConn(_url_ds);
  }

  public int getMaxActive() {
    return dsSupport.getMaxActive();
  }

  public int getMaxIdle() {
    return dsSupport.getMaxIdle();
  }

  public int getActive() {
    return dsSupport.getActive();
  }

  public int getIdle() {
    return dsSupport.getIdle();
  }

  public int getMaxWait() {
    return dsSupport.getMaxWait();
  }

  public String getValidationQuery() {
    return dsSupport.getValidationQuery();
  }
}
