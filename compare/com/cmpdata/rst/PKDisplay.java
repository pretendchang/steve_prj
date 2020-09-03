package com.cmpdata.rst;

import java.util.List;
import java.util.TreeMap;

public interface PKDisplay {
	public StringBuffer Display(List<String> pks, TreeMap<String,Object> rs, String splitter);
}
