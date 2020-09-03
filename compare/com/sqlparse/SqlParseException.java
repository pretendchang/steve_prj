package com.sqlparse;

import com.steve.util.Logger;


public class SqlParseException extends RuntimeException {
	/**
	 * 001 unimplemented exception
	 */
	private static final long serialVersionUID = 1L;
	String desc;

	public SqlParseException(String _msg, Exception e)
	{
		StringBuffer sb = new StringBuffer();
		sb.append("error id:").append(_msg).append("/").append(e.toString()).append(":").append(e.getMessage());
	    desc = sb.toString();
		
	}
	public SqlParseException(String _msg, String _desc)
	{
		StringBuffer sb = new StringBuffer();
		sb.append("error id:").append(_msg).append("/").append(_desc);
	    desc = sb.toString();
	}
}
