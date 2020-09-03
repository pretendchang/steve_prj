package com.qa.log;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import com.cmpdata.controller.GlobalVariable;

public class errpk implements LogPrintln {
	
	static PrintWriter pw = null;
	static PrintWriter pw_summary = null;
	public errpk()
	{
		
	}
	public errpk(PrintWriter _pw, PrintWriter _pw_summary)
	{
		if(pw==null)
			pw = _pw;
		if(pw_summary==null)
			pw_summary = _pw_summary;
	}
	public errpk(PrintWriter _pw)
	{
		if(pw==null)
			pw = _pw;
	}
	
	public void println(String s) {
		if(pw == null)
		{
			GlobalVariable.errpk.println(s);
			GlobalVariable.errpk.flush();
		}
		else
		{
			pw.println(s);
			pw.flush();
		}
	}
	public void print(String s) {
		if(pw == null)
		{
			GlobalVariable.errpk.print(s);
			GlobalVariable.errpk.flush();
		}
		else
		{
			pw.print(s);
			pw.flush();
		}
	}
	public void summary_println(String s) {
		if(pw_summary == null)
		{
			GlobalVariable.errpk_summary.println(s);
			GlobalVariable.errpk_summary.flush();
		}
		else
		{
			pw_summary.println(s);
			pw_summary.flush();
		}
	}

}
