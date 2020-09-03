package com.steve.db.data.manager;

import java.lang.reflect.*;
public class TransactionProxy implements InvocationHandler{
    private Object obj;

    public static Object newInstance(Object obj) {
        return java.lang.reflect.Proxy.newProxyInstance(
                obj.getClass().getClassLoader(),
                obj.getClass().getInterfaces(),
                new TransactionProxy(obj));
    }

    private TransactionProxy(Object obj) {
        this.obj = obj;
    }

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
      String methodname = m.getName();

      //若是作搜尋(利用方法名稱的開頭作判斷)，則不加入transaction
      if(methodname.startsWith("get") ||
         methodname.startsWith("load") ||
         methodname.startsWith("search"))
      {
        return this.invokeWithoutTx(proxy, m, args);
      }
      else
      {
        return this.invokeWithTx(proxy, m, args);
      }
    }

    private void rollbackHandling()
    {
      DSManager.getInstance().rollbackTransaction("sql://localhost");
    }

    private void commitHandling()
    {
      DSManager.getInstance().commitTransaction("sql://localhost");
    }

    private Object invokeWithTx(Object proxy, Method m, Object[] args) throws Throwable
    {
      Object result;
      boolean init_state = DSManager.getInstance().getTransactionState("sql://localhost");
      try {
              DSManager.getInstance().beginTransaction("sql://localhost");
              result = m.invoke(obj, args);
              if(!init_state)
                commitHandling();
      } catch (InvocationTargetException e) {
        rollbackHandling();
        throw e.getTargetException();
      } catch (Exception e) {
        rollbackHandling();
        throw new RuntimeException("unexpected invocation exception: " + e.getMessage());
      } finally {
        if (init_state == false)
          DSManager.getInstance().endTransaction("sql://localhost");
      }
        return result;
    }

    private Object invokeWithoutTx(Object proxy, Method m, Object[] args) throws Throwable
    {
      Object result;
      try {
        result = m.invoke(obj, args);
      }
      catch (InvocationTargetException e) {
        throw e.getTargetException();
      }
      catch (Exception e) {
        throw new RuntimeException("unexpected invocation exception: " + e.getMessage());
      }
      return result;
    }
}
