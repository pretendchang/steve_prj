package com.steve.db.data.manager;

/*
 * 
cmpdata001  未歸類錯誤
cmpdata002 資料庫相關錯誤
cmpdata003 讀ds_manager.cfg錯誤
cmpdata004 載入DBDSO錯誤
cmpdata005 資料庫tx錯誤
cmpdata006 DBDSO設定黨讀取錯誤
cmpdata007 unknown url protocol
 */
public class DBManagerRuntimeException extends RuntimeException
{
	public String desc;
  public DBManagerRuntimeException(String _msg, Exception e)
  {
	  StringBuffer sb = new StringBuffer();
		sb.append("error id:").append(_msg).append("/").append(e.toString()).append(":").append(e.getMessage());
		System.out.println(sb);
		this.initCause(e);
		//e.printStackTrace();
	    desc = sb.toString();
  }
  public DBManagerRuntimeException(String _msg, String _desc)
  {
	  StringBuffer sb = new StringBuffer();
		sb.append("error id:").append(_msg).append("/").append(_desc);
		System.out.println(sb);
	    desc = sb.toString();
  }
}
