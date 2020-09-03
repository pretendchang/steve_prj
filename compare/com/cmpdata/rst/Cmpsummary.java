package com.cmpdata.rst;

import java.util.Date;

public class Cmpsummary {
	public int leftcount;
	public int rightcount;
	
	public int case1bcount;//資料內容錯誤
	public int case2count;//新系統無資料，舊系統有資料
	public int case2rechckscriptcount;//新系統無資料，舊系統有資料時，執行case2check後的正確比數
	public int case3count;//舊系統無資料，新系統有資料
	public int case4count;//新舊系統資料比數不符(左-右)
	
	public Date begintime;
	public Date endtime;
	
	public Cmpsummary()
	{
		leftcount = rightcount = case1bcount = case2count = case3count = case4count =0;
	}
}
