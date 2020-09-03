package com.cmpdata.range;

public abstract class Range {
	protected Range_input input;
	
	private Range()
	{
		
	}
	
	protected Range(Range_input _input)
	{
		input = _input;
	}
	
	public abstract String getNextRangeString();
	public String getSchemaString()
	{
		String ret = null;
		StringBuffer cond1= new StringBuffer();
	
		cond1.append(input.keyname).append("<").append(input.keyfrom);
		ret = input.template.replaceAll(input.replacekeyword, cond1.toString());
		
		return ret;
	}
}
