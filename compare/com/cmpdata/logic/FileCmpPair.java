package com.cmpdata.logic;

import java.util.ArrayList;
import java.util.List;



public class FileCmpPair {
	public int type;//1: common byte  2:composite bytecnt is repeat cnt
	private int bytecnt;
	public int from;
	public int to;
	public String name;
	public int repeat=1;
	
	public List<FileCmpPair> ldetail = new ArrayList<FileCmpPair>();//°j°érepeat meta
	
	public FileCmpPair(int i, int _f, String n)
	{
		type=1;
		bytecnt = i;
		name=n;
		from = _f;
		to +=_f+i;
	}
	public FileCmpPair(int i, int _f, String n, int r)
	{
		type=2;
		bytecnt = i;
		name=n;
		repeat=r;
		
		from = _f;
		to +=_f+i;
	}
	
	
	public void addcmp(FileCmpPair c)
	{
		ldetail.add(c);
	}
	
	public void setBytecnt(int c)
	{
		bytecnt+=c;
		to+=c*repeat;
	}
	
	public int getBytecnt()
	{
		return bytecnt*repeat;
	}
	
	public int getsingleBytecnt()
	{
		return bytecnt;
	}
}
