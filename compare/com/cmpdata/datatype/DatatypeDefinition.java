package com.cmpdata.datatype;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import com.cmpdata.dbvendor.DBInterface;
import com.sqlparse.Builder;
import com.sqlparse.SQLExpression;
import com.sqlparse.SQLOrderBy;

import com.cmpdata.genericCmpDataConstant;

public final class DatatypeDefinition {
	public static final int DATATYPE_CHAR = 1;
	public static final int DATATYPE_NUMBER = 2;
	public static final int DATATYPE_DATE = 3;
	public static final int DATATYPE_DATETIME = 4;
	public static final int DATATYPE_DECIMAL = 5;
	
	//oracle jdbc driver sql type and jdbc type mismatch
	private static final String COLUMNTYPENAME_DATE="DATE";
	
	private int datatype;
	private int SQLColumnType;
	private String JDBCColumnTypeName;
	private int SQLPrecision;
	private int SQLScale;
	
	protected int getDatatype()
	{
		return datatype;
	}
	
	public static DatatypeDefinition factory(int ColumnType, String ColumnTypeName, int _sqlprecision, int _sqlscale)
	{
		return new DatatypeDefinition(ColumnType, ColumnTypeName, _sqlprecision, _sqlscale);
	}
	
	protected DatatypeDefinition(int ColumnType, String ColumnTypeName, int _sqlprecision, int _sqlscale)
	{
		SQLColumnType = ColumnType;
		JDBCColumnTypeName = ColumnTypeName;
		SQLPrecision = _sqlprecision;
		SQLScale = _sqlscale;
		switch(ColumnType)
		{
			case Types.CHAR:
			case Types.LONGNVARCHAR:
			case Types.LONGVARCHAR:
			case Types.NCHAR:
			case Types.NVARCHAR:
			case Types.VARCHAR:
			//case Types.DATE:
			//case Types.TIME:
				datatype=DATATYPE_CHAR;
					break;
			case Types.DATE:
			case Types.TIME:
				datatype=DATATYPE_DATE;
					break;
			case Types.TIMESTAMP:
				if(ColumnTypeName.equalsIgnoreCase(COLUMNTYPENAME_DATE))
					datatype=DATATYPE_DATE;
				else
					datatype=DATATYPE_DATETIME;
				break;
			case Types.NUMERIC:
				datatype=DATATYPE_DECIMAL;
				break;	
			default:
				datatype=DATATYPE_NUMBER;
		}
	}
	
	public int compare(String s1, String s2)
	{
		switch(datatype)
		{
		case DATATYPE_CHAR:
		case DATATYPE_DATE:	
		case DATATYPE_DATETIME:	
			int ords = s1.compareTo(s2);
			if(ords!=0)
				return ords;
			break;
			
		case DATATYPE_NUMBER:
		case DATATYPE_DECIMAL:
			try {
				  if(s1.equals(genericCmpDataConstant.CONSTANT_STRING_NULL) || s2.equals(genericCmpDataConstant.CONSTANT_STRING_NULL))
				  {
					  return s1.compareTo(s2);
				  }
				  
				BigDecimal i1 = new BigDecimal(s1);
				BigDecimal i2 = new BigDecimal(s2);
				int ordi = i1.compareTo(i2);
				if(ordi!=0)
					return ordi;
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			break;	
		}
		return 0;
	}
	
	public String buildCeateTableColumnDefinition()
	{
		StringBuilder sb = new StringBuilder();
		if(JDBCColumnTypeName.equals("varchar"))
			sb.append(JDBCColumnTypeName).append("(").append(SQLPrecision).append(")");
		else if(JDBCColumnTypeName.equals("decimal"))
			sb.append(JDBCColumnTypeName).append("(").append(SQLPrecision).append(",").append(SQLScale).append(")");
		else if(JDBCColumnTypeName.equals("char"))
		{
			sb.append(JDBCColumnTypeName).append("(").append(SQLPrecision).append(")");
		}
		else if(JDBCColumnTypeName.equals("nvarchar"))
			sb.append(JDBCColumnTypeName).append("(").append(SQLPrecision).append(")");
		else
			sb.append(JDBCColumnTypeName);
		
		return sb.toString();
	}
	
	public String getDefaultWhere()
	{
		switch(datatype)
		{
		case DATATYPE_CHAR:
			return "''";
		case DATATYPE_NUMBER:
			return "0";
		case DATATYPE_DATE:
			return "2100-01-01";
		
		default:
			return "''";
		}
	}
	
	public void setPstmtValue(PreparedStatement pstmt, int columnidx, Object value)
	{/*
		switch(datatype)
		{
		case DATATYPE_CHAR:
			pstmt.setString(columnidx, (String) value);
			break;	
		case DATATYPE_DATE:
			break;	
		case DATATYPE_DATETIME:
			break;	
		case DATATYPE_NUMBER:
			break;	
		case DATATYPE_DECIMAL:
			break;	
		}
		*/
	}
	
	public String getPKWhere_Nullcheck(String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		StringBuffer sb = new StringBuffer();
		
		  if(datatype==DATATYPE_CHAR || datatype==DATATYPE_NUMBER)//text
		  {
			  sb.append("NVL("+ColumnMetaName+",'"+nullmark+"') = ?");
		  }
		  else if(datatype==DATATYPE_DATE)//date
		  {
			  sb.append("NVL(TO_CHAR("+ColumnMetaName+",'"+dbinterface.getDateFormatString()+"'),'"+nullmark+"')=?");//todo date  
		  }
		  else if(datatype==DATATYPE_DATETIME)//date
		  {
			  sb.append("NVL(TO_CHAR("+ColumnMetaName+",'"+dbinterface.getDateTimeFormatString()+"'),'"+nullmark+"')=?");//todo date	  
		  }
		  else if(datatype==DATATYPE_DECIMAL)//numberic
		  {
			  sb.append("NVL(TO_CHAR("+ColumnMetaName+"),'"+nullmark+"')=?");//todo date
		  }
		  else
			  sb.append(ColumnMetaName).append("=?");
		  
		  return sb.toString();
	}
	
	public String getPKWhere_default(String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		StringBuffer sb = new StringBuffer();
		
		sb.append(ColumnMetaName).append("=?");
		return sb.toString();
	}
	
	public SQLExpression buildPKWherePtn_Nullcheck(String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		SQLExpression pkslayer = null;
		  if(datatype==DATATYPE_CHAR || datatype==DATATYPE_NUMBER)//text
		  {
			  pkslayer=Builder.BuildExpression("NVL("+ColumnMetaName+",'"+nullmark+"')","?", Builder.OP_EQUAL);
		  }
		  else if(datatype==DATATYPE_DATE)//date
		  {
			  pkslayer=Builder.BuildExpression("NVL(TO_CHAR("+ColumnMetaName+",'"+dbinterface.getDateFormatString()+"'),'"+nullmark+"')","?", Builder.OP_EQUAL);
		  }
		  else if(datatype==DATATYPE_DATETIME)//date
		  {
			  pkslayer=Builder.BuildExpression("NVL(TO_CHAR("+ColumnMetaName+",'"+dbinterface.getDateTimeFormatString()+"'),'"+nullmark+"')","?", Builder.OP_EQUAL);
		  }
		  else if(datatype==DATATYPE_DECIMAL)//numberic
		  {
			  pkslayer=Builder.BuildExpression("NVL(TO_CHAR("+ColumnMetaName+"),'"+nullmark+"')","?", Builder.OP_EQUAL);
		  }
		  else
			  pkslayer=Builder.BuildExpression(ColumnMetaName,"?", Builder.OP_EQUAL);
		  
		  return pkslayer;
	}
	
	public SQLExpression buildPKWherePtn_default(String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		return Builder.BuildExpression(ColumnMetaName,"?", Builder.OP_EQUAL);
	}
	
	public String getPKOrder_Nullcheck(String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		StringBuffer sb = new StringBuffer();
		
		  if(datatype==DATATYPE_CHAR || datatype==DATATYPE_NUMBER)//text
		  {
			  sb.append("NVL("+ColumnMetaName+",'"+nullmark+"')");
		  }
		  else if(datatype==DATATYPE_DATE)//date
		  {
			  sb.append("NVL(TO_CHAR("+ColumnMetaName+",'"+dbinterface.getDateFormatString()+"'),'"+nullmark+"')");//todo date  
		  }
		  else if(datatype==DATATYPE_DATETIME)//date
		  {
			  sb.append("NVL(TO_CHAR("+ColumnMetaName+",'"+dbinterface.getDateTimeFormatString()+"'),'"+nullmark+"')");//todo date	  
		  }
		  else if(datatype==DATATYPE_DECIMAL)//numberic
		  {
			  sb.append("NVL(TO_CHAR("+ColumnMetaName+"),'"+nullmark+"')");//todo date
		  }
		  else
			  sb.append(ColumnMetaName);
		  
		  return sb.toString();
	}
	
	public String getPKOrder_default(String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		StringBuffer sb = new StringBuffer();
		
		sb.append(ColumnMetaName);
		return sb.toString();
	}
	
	public List<SQLOrderBy> getPKOrder_jsql_Nullcheck(String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		List<SQLOrderBy> ret = new ArrayList<SQLOrderBy>();
		  if(datatype==DATATYPE_CHAR || datatype==DATATYPE_NUMBER)//text
		  {
			  ret.add(new SQLOrderBy(true, "NVL("+ColumnMetaName+",'"+nullmark+"')"));
		  }
		  else if(datatype==DATATYPE_DATE)//date
		  {
			  ret.add(new SQLOrderBy(true, "NVL(TO_CHAR("+ColumnMetaName+",'"+dbinterface.getDateFormatString()+"'),'"+nullmark+"')"));
		  }
		  else if(datatype==DATATYPE_DATETIME)//date
		  {
			  ret.add(new SQLOrderBy(true, "NVL(TO_CHAR("+ColumnMetaName+",'"+dbinterface.getDateTimeFormatString()+"'),'"+nullmark+"')"));
		  }
		  else if(datatype==DATATYPE_DECIMAL)//numberic
		  {
			  ret.add(new SQLOrderBy(true, "NVL(TO_CHAR("+ColumnMetaName+"),'"+nullmark+"')"));
		  }
		  else
			  ret.add(new SQLOrderBy(true, ColumnMetaName));
		  
		  return ret;
	}
	
	public List<SQLOrderBy> getPKOrder_jsql_default(String ColumnMetaName, DBInterface dbinterface, String nullmark)
	{
		List<SQLOrderBy> ret = new ArrayList<SQLOrderBy>();
		ret.add(new SQLOrderBy(true, ColumnMetaName));
		return ret;
	}
}
