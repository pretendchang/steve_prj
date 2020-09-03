package com.steve.db.data.source;

import java.util.*;


public class UrlDS
{
  private String protocol = "";
  private String user = "";
  private String password = "";
  private String host = "";
  private String port = "";
  private String path = "";
  private String file = "";
  private String query = "";
  private String url = "";
  
  
  public UrlDS()
  {
  }
  
  
  public UrlDS(String _url)
  {
    String str;
    url = _url;
    HashMap map = parse(_url);
    
    if(map == null)
      return;
    
    if((str = (String) map.get("Protocol")) != null)
      protocol = str;
    
    if((str = (String) map.get("User")) != null)
      user = str;
    
    if((str = (String) map.get("Password")) != null)
      password = str;
    
    if((str = (String) map.get("Host")) != null)
      host = str;

    if((str = (String) map.get("Port")) != null)
      port = str;

    if((str = (String) map.get("Path")) != null)
      path = str;
    
    if((str = (String) map.get("File")) != null)
      file = str;

    if((str = (String) map.get("Query")) != null)
      query = str;
  }
  
  
  /**
      <pre>
      The method parses the input URL string into a HashMap.
      The HashMap contains the following keys :
        Protocol, User, Password, Host, Port, Path, File, Query.
        
      'parse("ldap://user:password@local:8080")' will result in 
      '{Password=password, Host=local, Protocol=ldap, User=user, Port=8080}'.
      
      'parse("ldap://user:password@local:8080/")' will result in 
      '{Password=password, Host=local, Protocol=ldap, Path=/, User=user, Port=8080}'.
      
      'parse("ldap://host/path/path2")' will result in 
      '{Host=host, Protocol=ldap, Path=/path/path2}'.
      
      'parse("ldap://host/path/path2/")' will result in 
      '{Host=host, Protocol=ldap, Path=/path, File=path2}'.
      </pre>
  */
  public static HashMap parse(String _url)
  {
    // parsing for protocol.
    int pos = _url.indexOf("://");
    HashMap map;
    
    if(pos == -1) return null;
    else map = new HashMap();
    
    map.put("Protocol", _url.substring(0, pos));

    
    // parsing for user and password.
    pos += 3;
    
    if(pos >= _url.length())
      return map;
    
    if(map.get("Protocol").equals("file"))
    {
    	map.put("Path", _url.substring(pos));
    	return map;
    }
    
    String str = _url.substring(pos);
    pos = 0;
    int pos2 = str.indexOf('@');
    int pos3;
    
    if(pos2 > 0)
    {
      String user_str = str.substring(pos, pos2);
      pos3 = user_str.indexOf(':');
      
      if(pos3 > 0)
      {
        map.put("User", user_str.substring(pos, pos3));
        if(pos3 < user_str.length() - 1)
          map.put("Password", user_str.substring(pos3 + 1));
      }
      else if(pos3 == 0)
      {
        if(pos3 < user_str.length() - 1)
          map.put("Password", user_str.substring(pos3 + 1));
      }
      else
      {
        if(user_str.length() > 0)
          map.put("User", user_str);
      }
      
      pos = pos2 + 1;
    }
    else if(pos2 == 0)
      pos = pos2 + 1;
    
    
    // parsing for host and port.
    if(pos >= str.length())
      return map;
    
    str = str.substring(pos);
    pos = 0;
    pos2 = str.indexOf('/');
    pos3 = str.indexOf(':');
    
    if(pos3 >= 0)
    {
      if(pos3 < pos2 - 1)
        map.put("Port", str.substring(pos3 + 1, pos2));
      else if(pos3 < str.length() - 2)
        map.put("Port", str.substring(pos3 + 1));
      
      if(pos3 > 0)
        map.put("Host", str.substring(pos, pos3));
    }
    else
    {
      if(pos2 > 0)
        map.put("Host", str.substring(pos, pos2));
      else if(pos2 < 0)
        map.put("Host", str.substring(pos));
    }
    
    
    // parsing for path.
    if(pos2 >= 0)
    {
      pos = pos2;
      
      if(pos >= str.length() - 1)
      {
        map.put("Path", "/");
        return map;
      }
    }
    else
      return map;
    
    str = str.substring(pos);
    pos = 0;
    pos2 = str.lastIndexOf('/');
    
    if(pos2 > 0)
      map.put("Path", str.substring(pos, pos2));
    else  // pos2 == 0
      map.put("Path", "/");
    
    
    // parsing for file.
    pos = pos2 + 1;
    
    if(pos >= str.length())
      return map;
    else
      str = str.substring(pos);
    
    pos = 0;
    pos2 = str.indexOf('?');
    
    if(pos2 > 0)
      map.put("File", str.substring(pos, pos2));
    else if(pos2 < 0)
    {
      map.put("File", str.substring(pos));
      return map;
    }
    
    
    // parsing for query string.
    pos = pos2 + 1;
    
    if(pos >= str.length())
      return map;
    
    map.put("Query", str.substring(pos));
    
    
    return map;
  }
  
  
  /**
      The method returns the connection string with the following format :
        &lt;protocol&gt;://&lt;user&gt;:&lt;password&gt;@&lt;host&gt;:&lt;port&gt;
  */
  public String getConnString()
  {
    StringBuffer str = new StringBuffer();
    
    if(protocol.equals(""))
      return null;
    else
      str.append(protocol + "://");
    
    if(!user.equals(""))
      str.append(user);
    
    if(!password.equals(""))
      str.append(":" + password);

    if(!host.equals(""))
      if(!user.equals("") || !password.equals(""))
        str.append("@" + host);
      else
        str.append(host);

    if(!port.equals(""))
      str.append(":" + port);
    
    return str.toString();
  }
  
  
  public String getProtocol()
  {
    return protocol;
  }
  
  
  public String getHost()
  {
    return host;
  }
  
  public String getUrl()
  {
	  return url;
  }
  
  public String getPath()
  {
	  return path;
  }
  
  public boolean checkPath(String path)
  {
	  if(!path.contains("~"))
		  return true;
	  return false;
  }
  
  public static void main(String[] args)
  {
	  UrlDS ds1 = new UrlDS("file://c:/test/ffdf/abc.txt");
	  
	  //System.out.println(map.get("Host"));
	  System.out.println(ds1.getPath());
	  System.out.println(ds1.getProtocol());
	  
	  UrlDS ds2 = new UrlDS("sql://informix");
	  System.out.println(ds2.getHost());
	  System.out.println(ds2.getProtocol());

  }
}
