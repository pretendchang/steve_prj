package com.cmpdata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.cmpdata.columnprop.ExecOnPropMethod_AND;
import com.cmpdata.datatype.DatatypeDefinition;
import com.cmpdata.dbvendor.DBInterface;
import com.sqlparse.SQLExpression;
import com.sqlparse.SQLOrderBy;

public final class ColumnMeta {
	private static List<ColumnMeta> left = new ArrayList<ColumnMeta>();
	private static List<ColumnMeta> right = new ArrayList<ColumnMeta>();
	
	private static Map<String, ColumnMeta> mapleft = new TreeMap<String, ColumnMeta>(String.CASE_INSENSITIVE_ORDER);
	private static Map<String, ColumnMeta> mapright = new TreeMap<String, ColumnMeta>(String.CASE_INSENSITIVE_ORDER);
	
	public static void addLeftCol(ColumnMeta c)
	{
		left.add(c);
		mapleft.put(c.name, c);
	}
	
	public static void addRightCol(ColumnMeta c)
	{
		right.add(c);
		mapright.put(c.name, c);
	}
	
	public static ColumnMeta getLeftColumn(String elm)
	{
		return getColumn(mapleft, elm);
	}
	
	public static ColumnMeta getRightColumn(String elm)
	{
		return getColumn(mapright, elm);
	}
	
	public static ColumnMeta getLeftColumn(int idx)
	{
		return getColumn(left, idx);
	}
	
	public static ColumnMeta getRightColumn(int idx)
	{
		return getColumn(right, idx);
	}
	
	public static int getRightColumnSize()
	{
		return right.size();
	}
	
	public static int getLeftColumnSize()
	{
		return left.size();
	}
	
	private static ColumnMeta getColumn(Map<String, ColumnMeta> mapLeftRight, String elm)
	{
		return mapLeftRight.get(elm);
	}
	
	private static ColumnMeta getColumn(List<ColumnMeta> leftright, int idx)
	{
		return leftright.get(idx);
	}
	
	private final String name;
	private List<String> props;
	private DatatypeDefinition type;
	
	private boolean propsset=false;
	private boolean typeset=false;
	
	public ColumnMeta(String n)
	{
		name=n;
	}
	
	public static ColumnMeta findColumn(String name, int leftright)
	{
		List<ColumnMeta> lcm;
		if(leftright == genericCmpDataConstant.CmpLeftElm)
		{
			lcm = left;
		}
		else
		{
			lcm = right;
		}
		for(ColumnMeta cm : lcm)
		{
			if(cm.getName().equals(name))
				return cm;
		}
		return null;
	}
	
	public String getName()
	{
		return name;
	}
	
	public List<String> getProps()
	{
		return props;
	}
	
	public void setProps(String _props)
	{
		if(!propsset)
		{
			propsset=true;
			props = Arrays.asList(_props.split(","));
		}
		else
		{
			System.out.println("setProps has been set");
		}
	}
	
	public void setNullcheckProps()
	{
		if(props == null)
		{
			props = new ArrayList<String>();
			props.add(ExecOnPropMethod_AND.PROP_NULLCHECK);
			return;
		}
		if(props.contains(ExecOnPropMethod_AND.PROP_NULLCHECK))
			props.add(ExecOnPropMethod_AND.PROP_NULLCHECK);
	}
	
	public void setType(DatatypeDefinition _type)
	{
		if(!typeset)
		{	
			typeset=true;
			type = _type;
		}
		else
		{
			System.out.println("setType has been set");
		}
	}
	
	public int compare(String s1, String s2)
	{
		if(type == null)
		{
			System.out.println("ColumnMeta:"+this.name+" type null");
		}
		return type.compare(s1, s2);
	}
	
	public String getDefaultWhere()
	{
		return type.getDefaultWhere();
	}
	
	public String getCheckPKWhere_Nullcheck(String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		return type.getPKWhere_Nullcheck(ColumnMetaName, dbinterface, nullmark);
	}
	
	public String getCheckPKWhere_default(String ColumnMetaName)
	{
		return type.getPKWhere_default(ColumnMetaName, null, null);
	}
	
	public String getPKWhere(String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		List<String> props =this.getProps();

		//String sb = (String) ExecOnPropMethod_AND.exec(ExecOnPropImpl_getPKWhere.impls, props, new Object[]{this, ColumnMetaName, dbinterface, nullmark});
		//return sb;

		if(props==null)
		{
			return getCheckPKWhere_default(ColumnMetaName);
		}
		else if(props.indexOf(ExecOnPropMethod_AND.PROP_NULLCHECK)!=-1 || props.indexOf(ExecOnPropMethod_AND.PROP_NULLCHECK.toLowerCase())!=-1)
		{
			return getCheckPKWhere_Nullcheck(ColumnMetaName, dbinterface, nullmark);
		}
		else if(props.indexOf("servattrvalnullcheck")!=-1)//pks.get(j).equals("olddata"))
		{
			return "NVL((case when "+ColumnMetaName+"='F ' then 'F' else "+ColumnMetaName+" end),'${NULL}') = ?";
		}
		else
		{
			return getCheckPKWhere_default(ColumnMetaName);
		}
		
	}
	
	public SQLExpression buildPKWherePtn_Nullcheck(String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		return type.buildPKWherePtn_Nullcheck(ColumnMetaName, dbinterface, nullmark);
	}
	
	public SQLExpression buildPKWherePtn_default(String ColumnMetaName)
	{
		return type.buildPKWherePtn_default(ColumnMetaName, null, null);
	}
	
	public SQLExpression buildPKWherePtn(String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		List<String> props =this.getProps();
		
		if(props==null)
		{
			return buildPKWherePtn_default(ColumnMetaName);
		}
		else if(props.indexOf(ExecOnPropMethod_AND.PROP_NULLCHECK)!=-1 || props.indexOf(ExecOnPropMethod_AND.PROP_NULLCHECK.toLowerCase())!=-1)
		{
			return buildPKWherePtn_Nullcheck(ColumnMetaName, dbinterface, nullmark);
		}
		else
		{
			return buildPKWherePtn_default(ColumnMetaName);
		}
		//return (SQLExpression) ExecOnPropMethod_AND.exec(ExecOnPropImpl_buildPKWherePtn.impls, props, new Object[]{this, ColumnMetaName, dbinterface, nullmark});
	}
	
	public String getPKOrder_Nullcheck(String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		return type.getPKOrder_Nullcheck(ColumnMetaName, dbinterface, nullmark);
	}
	
	public String getPKOrder_default(String ColumnMetaName)
	{
		return type.getPKOrder_default(ColumnMetaName, null, null);
	}
	
	public String getPKOrder(String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		List<String> props =this.getProps();
		
		//String sb = (String) ExecOnPropMethod_AND.exec(ExecOnPropImpl_getPKOrder.impls, props, new Object[]{this, ColumnMetaName, dbinterface, nullmark});
		//return sb;
		
		if(props==null)
		{
			return getPKOrder_default(ColumnMetaName);
		}
		else if(props.indexOf(ExecOnPropMethod_AND.PROP_NULLCHECK)!=-1 || props.indexOf(ExecOnPropMethod_AND.PROP_NULLCHECK.toLowerCase())!=-1)
		{
			return getPKOrder_Nullcheck(ColumnMetaName, dbinterface, nullmark);
		}
		else
		{
			return getPKOrder_default(ColumnMetaName);
		}
	}
	
	public List<SQLOrderBy> getPKOrder_jsql_Nullcheck(String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		return type.getPKOrder_jsql_Nullcheck(ColumnMetaName, dbinterface, nullmark);
	}
	
	public List<SQLOrderBy> getPKOrder_jsql_default(String ColumnMetaName)
	{
		return type.getPKOrder_jsql_default(ColumnMetaName, null, null);
	}
	
	public List<SQLOrderBy> getPKOrder_jsql(String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		List<String> props =this.getProps();
		
		//List<SQLOrderBy> ret = (List<SQLOrderBy>)ExecOnPropMethod_AND.exec(ExecOnPropImpl_getPKOrder_jsql.impls, props, new Object[]{this, ColumnMetaName, dbinterface, nullmark});
		//return ret;
		
		if(props==null)
		{
			return getPKOrder_jsql_default(ColumnMetaName);
		}
		else if(props.indexOf(ExecOnPropMethod_AND.PROP_NULLCHECK)!=-1 || props.indexOf(ExecOnPropMethod_AND.PROP_NULLCHECK.toLowerCase())!=-1)
		{
			return getPKOrder_jsql_Nullcheck(ColumnMetaName, dbinterface, nullmark);
		}
		else
		{
			return getPKOrder_jsql_default(ColumnMetaName);
		}
	}
	
	public String toString()
	{
		return name;
	}

}
