package com.cmpdata.datatype;


public final class DatatypeTransform {
	private final DatatypeDefinition lefttype;
	private final DatatypeDefinition righttype;
	
	private DatatypeTransform(DatatypeDefinition l, DatatypeDefinition r)
	{
		lefttype = l;
		righttype= r;
	}
	public String getTypeTransformValue(String input)
	{
		if(input == null)
			return null;
		
		if(lefttype.getDatatype()==DatatypeDefinition.DATATYPE_DATETIME && righttype.getDatatype()==DatatypeDefinition.DATATYPE_DATE)
		{//timestamp to date  
			if(input.length()>=10)
				return input.substring(0,10);
			else
				return input;
		}
		else
			return input;
	}
}
