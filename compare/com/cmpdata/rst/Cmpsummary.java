package com.cmpdata.rst;

import java.util.Date;

public class Cmpsummary {
	public int leftcount;
	public int rightcount;
	
	public int case1bcount;//��Ƥ��e���~
	public int case2count;//�s�t�εL��ơA�¨t�Φ����
	public int case2rechckscriptcount;//�s�t�εL��ơA�¨t�Φ���ƮɡA����case2check�᪺���T���
	public int case3count;//�¨t�εL��ơA�s�t�Φ����
	public int case4count;//�s�¨t�θ�Ƥ�Ƥ���(��-�k)
	
	public Date begintime;
	public Date endtime;
	
	public Cmpsummary()
	{
		leftcount = rightcount = case1bcount = case2count = case3count = case4count =0;
	}
}
