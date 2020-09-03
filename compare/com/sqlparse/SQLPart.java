package com.sqlparse;

import java.io.Serializable;

public interface SQLPart extends Serializable {
	public boolean isnull();
	public void setnull();
}
