package com.cmpdata.controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import com.cmpdata.CmpTask;
import com.cmpdata.genericCmpData;
import com.security.PathManipulation;
import com.steve.db.data.manager.DSManager;

import com.cmpdata.logic.ScriptAPI;
import com.cmpdata.logic.VarScript;

public class Init {
	protected static void init_openlogfile(String _inputxml_path, String _errpkfile_path) throws UnsupportedEncodingException, FileNotFoundException
	{
		String inputxml_path = PathManipulation.filterFilePath(_inputxml_path);
		File inputxml = new File(inputxml_path);
		String errpkfile_path = PathManipulation.filterFilePath(_errpkfile_path);
		File errpkfile = new File(errpkfile_path);
		GlobalVariable.errpk = new PrintWriter(new OutputStreamWriter(new FileOutputStream(errpkfile.getParent()+"/"+inputxml.getName()+errpkfile.getName()), "UTF-8"));
		//errpk = new PrintWriter(new File(errpkfile.getParent()+"/"+inputxml.getName()+errpkfile.getName()));
		GlobalVariable.errpk_summary = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(errpkfile_path+".summary"),true), "UTF-8"));
	}
	
	protected static void init_getMeta() throws SQLException
	{
		Object connobj = DSManager.getInstance().getConn(GlobalVariable.left.dbconn);
		
		if(connobj instanceof Connection)
		{
			Connection db_connora=(Connection)connobj;
			Connection rightconnobj = (Connection)DSManager.getInstance().getConn(GlobalVariable.right.dbconn);
			PreparedStatement rightpstmt = rightconnobj.prepareStatement(GlobalVariable.rightfirstsql.toString());
			ResultSet rightrs = rightpstmt.executeQuery();
			
			String leftmainsqlfile=null;
			if(GlobalVariable.left.getMainsqlsb().toString().startsWith("file://"))
			{//分段
				leftmainsqlfile = GlobalVariable.left.getMainsqlsb().toString().substring("file://".length());
				OpenSqlScripts(leftmainsqlfile);
			}
			
			Set_rrs(db_connora,GlobalVariable.left.getMainsqlsb().toString());
			  if(GlobalVariable.left.getMainsqlsb().toString().startsWith("file://"))
			  {
				  CloseSqlScripts();
				  OpenSqlScripts(leftmainsqlfile);
			  }
			  else
			  {
				  //不分段
				  sqltest = new String[1];
				  sqltest[0]=GlobalVariable.left.getMainsqlsb().toString();
				  System.out.println("Only 1 sql part defined! Set threadcnt to 1");
				  threadcnt=1;//thread一律為1
			  }
			  //init_lcmpk(rrs.getMetaData());
			  setCmpPair(rrs.getMetaData(), rightrs.getMetaData());
			  rrs.close();//todo 不分段
			  rightrs.close();
			  rightpstmt.close();
			  rightconnobj.close();
		}
	}
	
	protected static void init_handleargs(String []args) throws ClassNotFoundException, IOException
	{
		if(args.length <2 || args[0]==null || args[1]==null)
		{
			System.out.println("windows:   java -cp javalib/*;javalib/jdom-2.0.6/*;genericCmpData.jar com.cmpdata.cmpdata confing file_path logfile_path [customized_class_file_path or jar_file_path] [entry main customized class uri]");
			System.out.println("unix like: java -cp javalib/*:javalib/jdom-2.0.6/*:genericCmpData.jar com.cmpdata.cmpdata confing file_path logfile_path [customized_class_file_path or jar_file_path] [entry main customized class uri]");
			return;
		}
		if(args.length==3)
		{
			if(args[2].equals("YES"))
				dbgmode=1;
			else if(args[2].equals("1000"))
				dbgmode=1000;
		}

		if(args.length>3 && (args[2]!=null || args[3]!=null))
		{
			if(args[2]==null || args[3]==null)
			{
				System.out.println("windows:   java -cp javalib/*;javalib/jdom-2.0.6/*;genericCmpData.jar com.cmpdata.cmpdata confing file_path logfile_path [customized_class_file_path or jar_file_path] [entry main customized class uri]");
				System.out.println("unix like: java -cp javalib/*:javalib/jdom-2.0.6/*:genericCmpData.jar com.cmpdata.cmpdata confing file_path logfile_path [customized_class_file_path or jar_file_path] [entry main customized class uri]");
				return;
			}
			ScriptAPI.initScriptAPI(args[2],args[3]);
		}
	}
	
	protected static Thread [] init_startCmpTask() throws InterruptedException
	{
		cmptasks = new CmpTask[threadcnt];
		Thread []genericCmpDataThread = new Thread[threadcnt];
		for(int ii=0;ii<threadcnt;ii++)
   	  	{
			genericCmpDataThread[ii] = new Thread(new genericCmpData(ii));
	   		genericCmpDataThread[ii].start();
		}
		
		for(int ii=0;ii<threadcnt;ii++)
		{
			while(cmptasks[ii]==null || genericCmpDataThread[ii]==null)
			{
				Thread.sleep(2000);
			}
		}
		
		return genericCmpDataThread;
	}
}
