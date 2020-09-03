package com.cmpdata.columnprop;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;

public interface ExecOnPropMethod_AND {
	public abstract Object yesfun(Object ...methodargs);
	
	static final String PROP_DEFAULT = "NULL";
	static final String PROP_CHINESE = "CHINESE";
	static final String PROP_UNTRIM = "UNTRIM";
	static final String PROP_NULLCHECK = "NULLCHECK";
	public static Object exec(TreeMap<String, ExecOnPropMethod_AND> impl, List<String> prop, Object []methodargs)
	{
		Set<String> keys = impl.keySet();
		if(prop == null)
		{
			return impl.get(PROP_DEFAULT).yesfun(methodargs);
		}
		
		for(String k : keys)
		{
			if(k.equalsIgnoreCase(PROP_DEFAULT))
				continue;
			ExecOnPropMethod_AND exec = impl.get(k);

			if(prop.indexOf(k.toLowerCase())!=-1)
			{
				return exec.yesfun(methodargs);
			}
			else if(prop.indexOf(k.toUpperCase())!=-1)
			{
				return exec.yesfun(methodargs);
			}
		}
		return impl.get(PROP_DEFAULT).yesfun(methodargs);
	}
}
