package com.cmpdata.logic;

public class CmpLogicRst {
	public boolean rst;
	public String left;
	public String right;
	
	public CmpLogicRst()
	{
		left=new String();
		right=new String();
	}
	
	public void setResult(String _left, String _right)
	{
		left = _left;
		right = _right;
	}
}
