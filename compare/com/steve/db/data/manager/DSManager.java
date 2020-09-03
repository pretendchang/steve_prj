package com.steve.db.data.manager;

import com.steve.db.data.source.*;

import java.sql.*;
import java.util.*;



public class DSManager
{
  /**
      Set the default site ID to be 'local'.
      The default site ID is used to find data sources in the default site.
  */
  public static final String DEF_SITE_ID = "sql://localhost";

  private static DSManager instance;
  private Hashtable site_ds = new Hashtable();
  private Hashtable url_conn_map = new Hashtable();
  static
  {

  }


  private DSManager()
  {
    Hashtable map = new Hashtable();
    site_ds.put(DEF_SITE_ID, map);
  }


  public static DSManager getInstance()
  {
    if(instance == null)
      instance = new DSManager();

    return instance;
  }


  public Map getSiteDS()
  {
    return site_ds;
  }


  public Map setSiteDS(String _site_id, Map _ds)
  {
    site_ds.put(_site_id, _ds);
    return site_ds;
  }

  public Hashtable getConns()
  {
    return url_conn_map;
  }

  public synchronized Object getConn(String _url, String _uid)
  {
    if(_url == null || _url.equals(""))
      _url = DEF_SITE_ID;
    UrlDS url_ds = new UrlDS(_url);
    String key = url_ds.getConnString();

    Object conn = null;
    boolean use_new = false;

    Hashtable conn_map = (Hashtable) url_conn_map.get(key);

    if(conn_map == null)
    {
      conn_map = new Hashtable();
      url_conn_map.put(key, conn_map);
    }
    else
      conn = conn_map.get(_uid);

    try
    {
      if(conn == null)
        use_new = true;
      else if(((Connection) conn).isClosed())
        use_new = true;
    }
    catch(SQLException e)
    {
      DBManagerRuntimeException ex = new DBManagerRuntimeException("cmpdata002",e);
      throw ex;
    }


    if(use_new)
    {
      conn = DSManagerDAO.getConn(url_ds);

      if(conn == null)
        return null;
      else
      {
        conn_map.put(_uid, conn);
        return conn;
      }
    }
    else
      return conn;
  }

  /**
      Gets the connection by the hash code of the current thread.
  */
  public synchronized Object getConn(String _url)
  {
    if(_url==null || _url.equals(""))
      _url = DEF_SITE_ID;
    return getConn(_url, Integer.toString(Thread.currentThread().hashCode()));
  }

  public synchronized Object getDataSource(String _url)
  {
    if(_url==null || _url.equals(""))
      _url = DEF_SITE_ID;
    return getDataSource(_url, Integer.toString(Thread.currentThread().hashCode()));
  }

  public synchronized Object getDataSource(String _url, String _uid)
  {
    if(_url == null || _url.equals(""))
      _url = DEF_SITE_ID;
    UrlDS url_ds = new UrlDS(_url);
    String key = url_ds.getConnString();

    Object conn = null;
    boolean use_new = false;

    Hashtable conn_map = (Hashtable) url_conn_map.get(key);

    if(conn_map == null)
    {
      conn_map = new Hashtable();
      url_conn_map.put(key, conn_map);
    }
    else
      conn = conn_map.get(_uid);

    try
    {
      if(conn == null)
        use_new = true;
      else if(((Connection) conn).isClosed())
        use_new = true;
    }
    catch(SQLException e)
    {
      DBManagerRuntimeException ex = new DBManagerRuntimeException("cmpdata002",e);
      return null;
    }


    if(use_new)
    {
      conn = DSManagerDAO.getDataSource(_url);

      if(conn == null)
        return null;
      else
      {
        conn_map.put(_uid, conn);
        return conn;
      }
    }
    else
      return conn;
  }


  public Object setConn(String _url, String _uid, Object _conn)
  {
    UrlDS url_ds = new UrlDS(_url);
    String key = url_ds.getConnString();

    Hashtable conn_map = (Hashtable) url_conn_map.get(key);

    if(conn_map == null)
      conn_map = new Hashtable();

    conn_map.put(_uid, _conn);

    return _conn;
  }


  public boolean beginTransaction(String _url)
  {
    return DSManagerDAO.beginTransaction(_url);
  }


  public boolean endTransaction(String _url)
  {
    return DSManagerDAO.endTransaction(_url);
  }


  public boolean commitTransaction(String _url)
  {
    return DSManagerDAO.commitTransaction(_url);
  }


  public boolean rollbackTransaction(String _url)
  {
    return DSManagerDAO.rollbackTransaction(_url);
  }


  /**
      Gets the transaction state of a connection with URL.
  */
  public boolean getTransactionState(String _url)
  {
    return DSManagerDAO.getTransactionState(_url);
  }
  
  public <T> List<T> getallobject(String _url, String _sql, Class<T> typeParameterClass, InputPstmt input)
  {
	  return DSManagerDAO.getallobject(_url, _sql, typeParameterClass,input);
  }

  public <T> T getobject(String _url, String _sql, Class<T> typeParameterClass, InputPstmt input)
  {
	  return DSManagerDAO.getobject(_url, _sql, typeParameterClass, input);
  }
  public void execute(String _url, String _sql, InputPstmt input)
  {
	  DSManagerDAO.execute(_url, _sql, input);
  }
}
