package com.steve.db.data.manager;

/*
 * 
cmpdata001  ���k�����~
cmpdata002 ��Ʈw�������~
cmpdata003 Ūds_manager.cfg���~
cmpdata004 ���JDBDSO���~
cmpdata005 ��Ʈwtx���~
cmpdata006 DBDSO�]�w��Ū�����~
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
