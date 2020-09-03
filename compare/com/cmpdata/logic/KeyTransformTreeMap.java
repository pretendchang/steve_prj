package com.cmpdata.logic;

import java.util.TreeMap;

public class KeyTransformTreeMap {
	private TreeMap<String,Object> orgdata = new TreeMap<String,Object>(String.CASE_INSENSITIVE_ORDER);
	private TreeMap<String,Object> newdata = new TreeMap<String,Object>(String.CASE_INSENSITIVE_ORDER);
	
	public KeyTransformTreeMap(TreeMap<String,Object> _orgdata)
	{
		orgdata=_orgdata;
	}
	
	public void setNewData(TreeMap<String,Object> _newdata)
	{
		newdata = _newdata;
	}
	
	public TreeMap<String,Object> getNewData(TreeMap<String,Object> _orgdata)
	{
		if(_orgdata == orgdata)
			return newdata;
		else
			return null;
	}
}
