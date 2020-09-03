package com.cmpdata.range;


public final class Range_normal_distribution extends Range{
	private int nowRange;
	private boolean eof=false;
	
	public Range_normal_distribution(Range_input _input)
	{
		super(_input);
		nowRange=input.keyfrom;
	}
	
	public String getNextRangeString()
	{
		if(eof)
			return null;
		String ret = null;
		StringBuffer cond1= new StringBuffer();
		
		if(nowRange == input.keyfrom)
			cond1.append(input.keyname).append("<").append(input.keyfrom);
		else if(nowRange > input.keyfrom && nowRange<input.keyto)
			cond1.append(input.keyname).append(">=").append(nowRange-input.interval).append(" and ").append(input.keyname).append("<").append(nowRange);
		else if(nowRange>=input.keyto)
		{
			cond1.append(input.keyname).append(">=").append(nowRange-input.interval);
			eof = true;
		}
		
		nowRange+=input.interval;
		ret = input.template.replaceAll(input.replacekeyword, cond1.toString());
		
		return ret;
	}
}
