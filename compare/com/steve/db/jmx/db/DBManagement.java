package com.steve.db.jmx.db;

import java.sql.*;
import java.util.*;
import com.steve.db.data.manager.*;
public class DBManagement implements DBManagementMBean
{
    public void testConnection(String url) throws Exception
    {
      Connection con = (Connection)DSManager.getInstance().getConn(url);
      if(con == null)
        throw new Exception("資料庫連線:"+url+"錯誤");
    }
    public int getDBPoolSize()
    {
      return DSManagerDAO.getConnectionPoolCount();
    }
    public Map queryDBPool()
    {
      return DSManagerDAO.getConnectionPool();
    }
}
