package com.cmpdata;

import java.util.List;
import java.util.TreeMap;

import com.cmpdata.rst.PKDisplay;

import com.cmpdata.logic.CmpPair;

public class CmpContext {	
	//global context
	private List<String> leftpk;
	private List<String> rightpk;
	
	private String tablename;
	
	//row context
	private TreeMap<String, Object> left;
	private TreeMap<String, Object> right;
	
	private String leftvalue;
	private String rightvalue;
	
	//error info
	public final static int ERRTYPE_CASE1=1;
	public final static int ERRTYPE_CASE2=2;
	public final static int ERRTYPE_CASE3=3;
	public final static int ERRTYPE_DATACNT_DIFFERENCE=4;
	
	private int errtype;
	private CmpPair errColumn;
	
	private PKDisplay pk;
	public void setLeft(TreeMap<String, Object> _left)
	{
		left = _left;
	}
	
	public TreeMap<String, Object> getLeft()
	{
		return left;
	}
	
	public void setRight(TreeMap<String, Object> _right)
	{
		right = _right;
	}
	
	public TreeMap<String, Object> getRight()
	{
		return right;
	}
	
	public void setLeftPK(List<String> _leftpk)
	{
		leftpk = _leftpk;
	}
	
	public List<String> getLeftPK()
	{
		return leftpk;
	}
	
	public void setRightPK(List<String> _rightpk)
	{
		rightpk = _rightpk;
	}
	
	public List<String> getRightPK()
	{
		return rightpk;
	}
	
	public String getTableName()
	{
		return tablename;
	}
	
	public void setTableName(String _tablename)
	{
		tablename = _tablename;
	}
	
	public void setErrColumn(CmpPair cp)
	{
		errColumn = cp;
	}
	
	public CmpPair getErrColumn()
	{
		return errColumn;
	}
	
	public void setErrType(int type)
	{
		errtype = type;
	}
	
	public int getErrType()
	{
		return errtype;
	}
	
	public void setLeftValue(String _lvalue)
	{
		leftvalue = _lvalue;
	}
	
	public String getLeftValue()
	{
		return leftvalue;
	}
	
	public void setRightValue(String _rvalue)
	{
		rightvalue = _rvalue;
	}
	
	public String getRightValue()
	{
		return rightvalue;
	}
	
	public PKDisplay getPKDisplay()
	{
		return pk;
	}
	
	public void setPKDisplay(PKDisplay _pk)
	{
		pk = _pk;
	}
}
