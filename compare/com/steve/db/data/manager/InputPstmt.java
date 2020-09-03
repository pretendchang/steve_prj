package com.steve.db.data.manager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface InputPstmt {
	public void PstmtMapping(PreparedStatement pstmt) throws SQLException;
	public <T> T DB2ObjectMapping(ResultSet rs) throws SQLException;
}
