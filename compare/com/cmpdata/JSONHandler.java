package com.cmpdata;

import com.cmpdata.genericCmpDataException;



public class JSONHandler {
	
	
	public static String getColumnValueStringNull(String ColumnValueString)
	{
		if(ColumnValueString == null)
			return "${NULL}";
		return ColumnValueString;
	}
	public static String getRTrimColumnValueStringNull(String ColumnValueString)
	{
		if(ColumnValueString == null)
			return "${NULL}";
		if(ColumnValueString.equals(" "))
			return " ";
		String rtrim = ColumnValueString.replaceAll("\\s+$","");
		return rtrim;
		//return ColumnValueString.trim();
	}
	public static String getTrimColumnValueStringNull(String ColumnValueString)
	{
		if(ColumnValueString == null)
			return "${NULL}";
		return ColumnValueString.trim();
	}
	public static String getTrimColumnValueString(String ColumnValueString)
	{
		return getColumnValueString(ColumnValueString, true);
	}
	public static String getColumnValueString(String ColumnValueString)
	{
		return getColumnValueString(ColumnValueString, false);
	}
	private static String getColumnValueString(String ColumnValueString, boolean istrim)
	{
		if(ColumnValueString == null)
			return "${NULL}";
		if(ColumnValueString.compareTo("")==0)
			return "${NOVALUE}";
		if(ColumnValueString.equals(" "))
		{
			ColumnValueString="${SPACE}";
		}
		if(ColumnValueString.contains(" "))
		{
			if(istrim)
			{
				String aftertrim = ColumnValueString.trim();
				if(aftertrim.equals(""))
				{//若資料只含一堆space，不可trim，保留原貌
					ColumnValueString = ColumnValueString.replace(" ", "${SPACE}");
				}
				else
					ColumnValueString=aftertrim;
			}
			else
				ColumnValueString = ColumnValueString.replace(" ", "${SPACE}");
		}
		return ColumnValueString;
	}
	
	
	public static String rollbackColumnValueString(String ColumnValueString)
	{
		if(ColumnValueString.equals("${SPACE}"))
			return " ";
		else if(ColumnValueString.contains("${SPACE}"))
		{
			ColumnValueString=ColumnValueString.trim();
			ColumnValueString = ColumnValueString.replace("${SPACE}", " ");
			return ColumnValueString;
		}
		else if(ColumnValueString.equals("${NOVALUE}"))
			return "";
		return ColumnValueString;
	}
	
	public static String replaceSpecialCharacter(String str)
	{
		return str.replace("／", "%").replace("╱","%")
				  .replace("＼", "%").replace("╲","%")
				  .replace("～","\u223c")
				  .replace("\ufffd","%");
	}
	public static boolean isSQLLikeCmp(String str)
	{
		if(  str.contains("／") 
		   ||str.contains("＼") 
		   || str.contains("╱")
		   || str.contains("╲")
		   || str.contains("\ufffd"))
		return true;
		
		return false;
	}
	
	public static boolean cmpColumnValueString(String ColumnValueString, Object javaObj)
	{
		if(ColumnValueString == null)
		{
			throw new genericCmpDataException("cmp007","cmpColumnValueString->ColumnValueString is null");
		}
		if(ColumnValueString.equals("${NULL}"))
		{
			if(javaObj==null || javaObj.equals("${NULL}"))//javaObj.equals("${NULL}") table vs. table
				return true;
			return false;
		}
		else if(ColumnValueString.equals("${NOVALUE}"))
		{
			if(javaObj.equals(""))
				return true;
			return false;
		}
		else if(ColumnValueString.contains("${SPACE}"))
		{
			String s = ((String)javaObj).replace(" ", "${SPACE}");
			if(new String(replaceSpecialCharacter(ColumnValueString).getBytes()).equals(new String(replaceSpecialCharacter(s).getBytes())))
				return true;
			return false;
		}
		else
			return new String(replaceSpecialCharacter(ColumnValueString).getBytes()).equals(new String(replaceSpecialCharacter(javaObj.toString()).getBytes()));

	}

}
