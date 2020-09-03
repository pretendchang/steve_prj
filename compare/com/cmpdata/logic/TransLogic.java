package com.cmpdata.logic;

public interface TransLogic {
	 public <Val>Val transvalue(Object s1, Object []refs, int runid, Object []logicvars, Class<Val> clazz, TransLogic logic);
}
