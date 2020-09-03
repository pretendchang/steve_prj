package com.steve.db.jmx.db;

import java.lang.management.*;
import javax.management.*;

public class perform {

  public static void main(String[] main) throws Exception {
    bind();
    // Wait forever
    System.out.println("Waiting forever...");
    Thread.sleep(Long.MAX_VALUE);

  }

  public static void bind() {
    try {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

      // Construct the ObjectName for the MBean we will register
      ObjectName name = new ObjectName("BOMKernel:type=資料庫,name=連線");

      // Create the Hello World MBean
      DBManagement mbean = new DBManagement();

      // Register the Hello World MBean
      mbs.registerMBean(mbean, name);

    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}
