package com.steve.db.data.manager;

import java.sql.*;
import java.util.*;

import com.security.PathManipulation;
import com.steve.db.data.source.*;

import java.io.*;
import java.lang.reflect.Method;

import org.jdom2.*;
import com.steve.util.ResourceFileReader;


public class DSManagerDAO
{
  private static final String DEF_CFG_FILE = "";
  private static Map dsMap = new HashMap();

  private static com.steve.util.Logger log = com.steve.util.Logger.getLogger(DSManagerDAO.class);
  
  static
  {
      String str = null;
      /*
      if(RecheckUCO.autotesting.equals("true"))
      {
    	  log.info("autotesting is true read ds_manager_auto.cfg");
    	  str=ResourceFileReader.readResourceFile("com.steve.db.data.manager.DSManager","ds_manager_auto.cfg");
      }
      else*/
      {
    	  //log.info("autotesting is true read ds_manager.cfg");
    	  str = ResourceFileReader.readResourceFile("com.steve.db.data.manager.DSManager","ds_manager.cfg");
      }

      ByteArrayInputStream in = new ByteArrayInputStream(str.getBytes());
      org.jdom2.input.SAXBuilder bldr = new org.jdom2.input.SAXBuilder();

      
      try {
        org.jdom2.Document doc = bldr.build(in);
        Element root_elm = doc.getRootElement();
        Element elmWs = root_elm.getChild("DsMap");
        List listchildren = elmWs.getChildren("Ds");
        Iterator it = listchildren.iterator();
        while (it.hasNext()) {
          Element elmWsMap = (Element) it.next();
          dsMap.put(elmWsMap.getAttributeValue("name"), elmWsMap);
        }
      }
      catch (Exception ex) {
        DBManagerRuntimeException e = new DBManagerRuntimeException("cmpdata003", ex);
        throw e;
      }

  }


  public static Map getSiteDS(String _site_id)
  {
    Map map = (Map) DSManager.getInstance().getSiteDS().get(_site_id);

    if(map == null)
    {
      map = new Hashtable();
      DSManager.getInstance().setSiteDS(_site_id, map);
    }

    return map;
  }


  public static Object getConn(UrlDS url_ds)
  {
	  if(url_ds.getProtocol().equals("sql"))
	  {
	      DbDSO dso = getDbDSO(url_ds.getUrl());
	      Object conn = dso.getConn(new UrlDS());
	      return conn;
	  }
	  else if(url_ds.getProtocol().equals("file"))
	  {
		  String path = url_ds.getPath();
		  if(url_ds.checkPath(path))
		  {
			  String filters = PathManipulation.filterFilePath(url_ds.getPath());
			  File file = new File(filters);
			  return file;
		  }
	  }
	  throw new com.steve.db.data.manager.DBManagerRuntimeException("cmpdata007", "unknown protocol:"+url_ds.getProtocol());
  }

  public static Object getDataSource(String _servername)
  {
      DbDSO dso = getDbDSO(_servername);
      dso.getConn(new UrlDS());
      Object ds = dso.getDataSource();

      return ds;
  }

  private static DbDSO getDbDSO(String _servername) throws DBManagerRuntimeException
  {
    Element ws = (Element) dsMap.get(_servername);
    if(ws == null)
    	throw new DBManagerRuntimeException("cmpdata004",_servername+" not found");
    String cls = ws.getAttributeValue("classname");

//    log.trace("get ds_manager.cfg information");
//    log.trace("input server name:"+_servername);
//    log.trace("mapping implementation DbDSO class:"+cls);
    try {
      DbDSO dso = (DbDSO) Class.forName(cls).newInstance();
      dso.setConfigFilePath(ws.getAttributeValue("config"));
      if(ws.getAttributeValue("usr") !=null && ws.getAttributeValue("pwd") !=null)
      {
    	  dso.setUsrPwd(ws.getAttributeValue("usr"), ws.getAttributeValue("pwd"));
      }
//      log.trace("implement DbDSO configuration file:"+ws.getAttributeValue("config"));

      return dso;
    }
    catch (ClassNotFoundException ex) {
      DBManagerRuntimeException e = new DBManagerRuntimeException("cmpdata004",ex);
      throw e;
    }
    catch (IllegalAccessException ex) {
      DBManagerRuntimeException e = new DBManagerRuntimeException("cmpdata004",ex);
      throw e;
    }
    catch (InstantiationException ex) {
      DBManagerRuntimeException e = new DBManagerRuntimeException("cmpdata004",ex);
      throw e;
    }
  }


  public static boolean beginTransaction(String _url)
  {
    Connection conn = (Connection) DSManager.getInstance().getConn(_url);

    try
    {
      conn.setAutoCommit(false);
      return true;
    }
    catch(SQLException e)
    {
      DBManagerRuntimeException ex = new DBManagerRuntimeException("cmpdata005",e);
    }

    return false;
  }


  public static boolean endTransaction(String _url)
  {
    Connection conn = (Connection) DSManager.getInstance().getConn(_url);

    try
    {
      conn.setAutoCommit(true);
      return true;
    }
    catch(SQLException e)
    {
      DBManagerRuntimeException ex = new DBManagerRuntimeException("cmpdata005",e);
    }

    return false;
  }


  public static boolean commitTransaction(String _url)
  {
    Connection conn = (Connection) DSManager.getInstance().getConn(_url);

    try
    {
      conn.commit();
      return true;
    }
    catch(SQLException e)
    {
      DBManagerRuntimeException ex = new DBManagerRuntimeException("cmpdata005",e);
    }

    return false;
  }


  public static boolean rollbackTransaction(String _url)
  {
    Connection conn = (Connection) DSManager.getInstance().getConn(_url);

    try
    {
      conn.rollback();
      return true;
    }
    catch(SQLException e)
    {
      DBManagerRuntimeException ex = new DBManagerRuntimeException("cmpdata005",e);
    }

    return false;
  }

  public static int getConnectionPoolCount()
  {
    return dsMap.size();
  }

  public static Map getConnectionPool()
  {
    return dsMap;
  }

  public static boolean getTransactionState(String _url)
  {
    Connection conn = (Connection) DSManager.getInstance().getConn(_url);

    try
    {
      return !conn.getAutoCommit();
    }
    catch(SQLException e)
    {
      DBManagerRuntimeException ex = new DBManagerRuntimeException("cmpdata005",e);
    }

    return true;
  }
  
  public static <T> List<T> getallobject(String _url, String _sql, Class<T> typeParameterClass, InputPstmt input)
  {
	  	Connection db_conn = (Connection) DSManager.getInstance().getConn(_url);
		List<T> ret = new ArrayList<T>();
		
		try
	    {
	      if(db_conn != null)
	      {
	    	  _sql = PathManipulation.filterSQL(_sql);
	    	PreparedStatement pstmt = db_conn.prepareStatement(_sql);
	    	if(input !=null)
	    	input.PstmtMapping(pstmt);

	    	ResultSet rs = pstmt.executeQuery();
	        while(rs.next())
	        {
	        	//DB2ObjectMapper mapper = DB2ObjectBuilder.build(typeParameterClass.getName());
	        	//mapper.DB2ObjectMapping(rs);
	        	T obj = (T)input.DB2ObjectMapping(rs);
	        	ret.add(obj);
	        }
	        rs.close();
	        pstmt.close();

	        return ret;
	      }
	    }
	    catch(com.steve.db.data.manager.DBManagerRuntimeException e)
		{
			throw e;
		}
	    catch(Exception e)
	    {
	    	throw new com.steve.db.data.manager.DBManagerRuntimeException("cmpdata001", e);
	    }
	    finally
	    {/*
	    	try {
				db_conn.close();
			} catch (SQLException e) {
				throw new com.steve.db.data.manager.DBManagerRuntimeException("cmpdata002", e);
			}
			*/
	    }
		return null;
  }

  public static <T> T getobject(String _url, String _sql, Class<T> typeParameterClass, InputPstmt input)
  {
	  Connection db_conn = (Connection) DSManager.getInstance().getConn(_url);
	  T obj = null;	
		try
	    {
	      if(db_conn != null)
	      {
	    	PreparedStatement pstmt = db_conn.prepareStatement(_sql);
	    	if(input !=null)
		    	input.PstmtMapping(pstmt);

	    	ResultSet rs = pstmt.executeQuery();

	        if(rs.next())
	        {
	        	obj = (T)input.DB2ObjectMapping(rs);
	        }
	        
	        rs.close();
	        pstmt.close();

	        return obj;
	      }
	    }
		catch(com.steve.db.data.manager.DBManagerRuntimeException e)
		{
			System.out.println("sql:"+_sql);
			throw e;
		}
	    catch(Exception e)
	    {
	    	System.out.println("sql:"+_sql);
	    	throw new com.steve.db.data.manager.DBManagerRuntimeException("cmpdata001", e);
	    }
	    finally
	    {/*
	    	try {
				db_conn.close();
			} catch (SQLException e) {
				throw new com.steve.db.data.manager.DBManagerRuntimeException("cmpdata002", e);
			}*/
	    }
		return null;
  }
  public static void execute(String _url, String _sql, InputPstmt input)
  {
	  Connection db_conn = (Connection) DSManager.getInstance().getConn(_url);
	  PreparedStatement pstmt=null;
		try
	    {
	      if(db_conn != null)
	      {
	    	  _sql = PathManipulation.filterSQL(_sql);
	    	pstmt = db_conn.prepareStatement(_sql);
	    	if(input !=null)
		    	input.PstmtMapping(pstmt);

	    	pstmt.executeUpdate();

	      }
	    }
		catch(com.steve.db.data.manager.DBManagerRuntimeException e)
		{
			System.out.println("sql:"+_sql);
			throw e;
		}
	    catch(Exception e)
	    {
	    	System.out.println("sql:"+_sql);
	    	throw new com.steve.db.data.manager.DBManagerRuntimeException("cmpdata001", e);
	    }
	    finally
	    {
	    	try {
	    		if(pstmt!=null)
	    			pstmt.close();
				db_conn.close();
			} catch (SQLException e) {
				throw new com.steve.db.data.manager.DBManagerRuntimeException("cmpdata002", e);
			}
	    }
  }
}
