package com.cmpdata.scripting;

import java.io.PrintWriter;
import java.util.List;
import java.util.TreeMap;

import javax.script.Invocable;

import com.cmpdata.logic.CmpLogic;
import com.cmpdata.logic.CmpLogicRst;

public class CheckInterfaceScript<Interface> extends CheckScript {

	Interface logic;
	
	CheckInterfaceScript(Invocable _inv, String _functionname, Class<Interface> clazz, Object []_logicvars, List<TreeMap<String, Object>> refs, PrintWriter writer, int threadcnt)
	{
		super(_inv, _functionname, _logicvars, refs, writer, threadcnt);
		logic = _inv.getInterface(clazz);
	}
	
	CheckInterfaceScript(Interface _inv, String _functionname, Object [] _logicvars, List<TreeMap<String, Object>> refs, int threadcnt)
	{
		super(null, _functionname, _logicvars, refs, threadcnt);
		logic = _inv;
		try {
			java.lang.reflect.Field f = logic.getClass().getField("result");
			f.setAccessible(true);
			for(int i=0;i<threadcnt;i++)
			{
				rst[i] = (CmpLogicRst) f.get(logic);
			}
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Interface getInterface()
	{
		return logic;
	}
	
	public static void main(String[] args) {

	}

}