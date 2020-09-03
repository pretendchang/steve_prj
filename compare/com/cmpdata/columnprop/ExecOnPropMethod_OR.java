package com.cmpdata.columnprop;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;

public interface ExecOnPropMethod_OR {
	public abstract Object yesfun(Object value,Object ...methodargs);
	public abstract Object nofun(Object value, Object ...methodargs);
	
	static final String PROP_DEFAULT = "NULL";
	static final String PROP_CHINESE = "CHINESE";
	static final String PROP_UNTRIM = "UNTRIM";
	public static Object exec(TreeMap<String, ExecOnPropMethod_OR> impl, List<String> prop, Object value, Object []methodargs)
	{
		Set<String> keys = impl.keySet();
		if(prop == null)
		{
			value = impl.get(PROP_DEFAULT).yesfun(value, methodargs);;
			return value;
		}
		Object ret = value;
		
		for(String k : keys)
		{
			if(k.equalsIgnoreCase(PROP_DEFAULT))
				continue;
			ExecOnPropMethod_OR exec = impl.get(k);

			if(prop.indexOf(k.toLowerCase())!=-1)
			{
				ret = exec.yesfun(ret, methodargs);
			}
			else if(prop.indexOf(k.toUpperCase())!=-1)
			{
				ret = exec.yesfun(ret, methodargs);
			}
			else
			{
				ret = exec.nofun(ret, methodargs);
			}
		}
		return ret;
	}
}
