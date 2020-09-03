package com.steve.db.jmx.db;
import javax.management.*;
import java.util.Map;
public interface DBManagementMBean
{
    public void testConnection(String url) throws Exception;
    public int getDBPoolSize();
    public Map queryDBPool();
}
