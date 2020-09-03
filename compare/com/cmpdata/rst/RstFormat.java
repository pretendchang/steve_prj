package com.cmpdata.rst;

import java.util.List;

import com.cmpdata.TreeMapUtil;

public class RstFormat {
//The meta of the meta
	private String formatid;
	public String metaid;
	public String batchno;
	private RstFormat(String formatid)
	{
		this.formatid = formatid;
		if(RstWriting.enable_write_reg_cmpdata)
		{
			this.metaid = String.valueOf(RstWriting.queryNextMeta());
			this.batchno = String.valueOf(RstWriting.queryNextBatchno());
		}
	}
	private void writeMeta1(List<String> leftkeynames, List<String> rightkeynames)
	{
		if(formatid.equals("1"))
		{
			if(RstWriting.enable_write_reg_cmpdata)
			{
				int metaid=Integer.parseInt(this.metaid);
				//write meta keycount keyname
				writeMetaDetail1(metaid, "1", leftkeynames);
				writeMetaDetail1(metaid, "2", rightkeynames);
			}
		}
	}
	private void writeMetaDetail1(int metaid, String side, List<String> keynames)
	{
		int i=1;
		int keycount = keynames.size();
		RstWriting.writeMeta(String.valueOf(metaid), side, "keycount", String.valueOf(keycount));
		for(String keyname : keynames)
		{
			RstWriting.writeMeta(String.valueOf(metaid), side, String.format("keyfield%02d",i++), keyname);
		}
	}
	
	private void writeRaw1(List<List<Object>> RstWritingRaw,int rowno, RstWriting rst)
	{
		//chinese byte to string bug 
		if(formatid.equals("1"))
		{
			if(rst.leftrawdata != null)
			{
				for(String key :rst.leftrawdata.keySet())
				{
					String s = TreeMapUtil.getString(rst.leftrawdata.get(key));
					if(s != null)
					{
						int rawno = RstWriting.queryNextRawno();
						RstWriting.writeRaw(RstWritingRaw,new Integer(rawno).toString(),new Integer(rowno).toString(), "1", key, s);
					}
				}
			}
			
			if(rst.rightrawdata != null)
			{
				for(String key :rst.rightrawdata.keySet())
				{
					String s = TreeMapUtil.getString(rst.rightrawdata.get(key));//chinese byte to string bug 
					if(s != null)
					{
						int rawno = RstWriting.queryNextRawno();
						RstWriting.writeRaw(RstWritingRaw,new Integer(rawno).toString(),new Integer(rowno).toString(), "2", key, s);
					}
				}
			}
			

		}
	}
	
	protected void writeRaw(List<List<Object>> RstWritingRaw, int rowno, RstWriting rst)
	{
		if(formatid.equals("1"))
		{
			writeRaw1(RstWritingRaw, rowno, rst);
		}
	}
	
	public static RstFormat initMeta(String format, List<String> leftkeynames, List<String> rightkeynames)
	{
		RstFormat rstformat = new RstFormat(format);
		if(format.equals("1"))
		{
			rstformat.writeMeta1(leftkeynames, rightkeynames);
		}
		
		return rstformat;
	}
}
