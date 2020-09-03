package com.cmpdata.range;

public final class Range_numbermod extends Range {
	private int nowRange;
	private boolean eof=false;
	private int modint=0;
	//rangetype 1:20  2:more 200 3:2000 4:10000
	protected Range_numbermod(Range_input _input) {
		super(_input);
		nowRange = 0;
		
		if(input.rangetype==1)
			modint=20;
		else if(input.rangetype==2)
			modint=200;
		else if(input.rangetype==3)
			modint=2000;
		else if(input.rangetype==4)
			modint=10000;
		else if(input.rangetype>4)
			modint=input.rangetype;
	}

	@Override
	public String getNextRangeString() {
		if(eof)
			return null;
		String ret = null;
		StringBuffer cond1= new StringBuffer();
		
		if(nowRange<modint)
		{
			if(input.includesql!=null && input.includesql[0]!=null && !input.includesql[0].equals(""))
			{
				cond1.append(input.includesql[0]).append(" and ");
			}
			cond1.append("mod(").append(input.keyname).append(",").append(modint).append(")=").append(nowRange);
		}
		else
			cond1.append(input.excludesql[nowRange-modint]);
			
		nowRange++;
		if(nowRange>=modint)
		{
			if(input.excludesql==null || input.excludesql[0].equals(""))
			{
				eof=true;
				//return null;
			}
			else if(nowRange==(modint+input.excludesql.length))
			{
				eof=true;
			}
		}

		ret = input.template.replaceAll(input.replacekeyword, cond1.toString());
		
		return ret;
	}
	
	@Override
	public String getSchemaString()
	{
		String ret = null;
		StringBuffer cond1= new StringBuffer();
	
		cond1.append(input.keyname).append("=").append(nowRange);
		ret = input.template.replaceAll(input.replacekeyword, cond1.toString());
		
		return ret;
	}

}
