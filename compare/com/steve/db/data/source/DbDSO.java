package com.steve.db.data.source;

import javax.sql.*;

public abstract class DbDSO implements DSO
{
   String filepath = "";
   String usr="";
   String pwd="";
   static DataSource ds;


  public String getConfigFilePath() {
    return filepath;
  }
  public void setConfigFilePath(String _filepath)
  {
    filepath = _filepath;
  }
  public void setUsrPwd(String _usr, String _pwd)
  {
    usr = _usr;
    pwd = _pwd;
  }

  
  public String getConfig()
  {
    return filepath;
  }
  public String getUsr()
  {
    return usr;
  }
  public String getPwd()
  {
    return pwd;
  }

  public final DataSource getDataSource()
  {
    return ds;
  }

}

