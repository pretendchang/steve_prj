package com.cmpdata;

public class genericCmpDataException extends RuntimeException {
	//cmpdata001 db type not match
	//cmpdata002 db column not defined
	//cmpdata003 leftcol no definition
	//cmpdata004 xml definition wrong
	//cmpdata005 data format wrong
	//cmpdata006 column definition error
	//cmpdata007 TreeMapUtil error
	//cmpdata008 right sql error
	//cmpdata009 �]�w�ɩw�q��pk�bresultsetmeta�䤣��
	//cmpdata010 SQLException �s�u����
	//cmpdata011 ScriptAPI class����l��
	//cmpdata012 cmptype error only supports t1t3/nameaddr
	String desc;
	public genericCmpDataException(String _msg, String _desc)
	{
		StringBuffer sb = new StringBuffer();
		sb.append("id:").append(_msg).append("/").append(_desc);
	    System.out.println(sb);
	    desc = sb.toString();
	}
}
