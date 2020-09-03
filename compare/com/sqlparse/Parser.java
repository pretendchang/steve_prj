package com.sqlparse;

import com.steve.util.Logger;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

public class Parser {
	public static Logger logger = Logger.getLogger(Parser.class);
	public static SQLStatement parseStatement(String stmt)
	{
		try {
			if(stmt.contains(":"))
			{
				return (SQLStatement)parseInformixTableSupport(stmt);
			}
			else
				return new SQLStatement(CCJSqlParserUtil.parse(stmt));
		} catch (JSQLParserException e) {
			throw new SqlParseException("SqlParse001", e);
		}
	}
	
	public static SQLExpression parseCondExpression(String stmt)
	{
		try {
			return new SQLExpression(CCJSqlParserUtil.parseCondExpression(stmt));
		} catch (JSQLParserException e) {
			throw new SqlParseException("SqlParse001", e);
		}
	}
	
	public static SQLPart parseInformixTableSupport(String sstmt)
	{
		int chk = checkstmt(sstmt);
		if(chk==1)
		{
			String s = InformixBuilder.fixInformixTableForCompatible(sstmt);
			SQLStatement stmt = (SQLStatement)parseInformixSelectStatement(s);
			return stmt;
		}
		return parse(sstmt);
	}
	
	public static SQLPart parse(String stmt)
	{
		try {
			int chk = checkstmt(stmt);
			if(chk==1)
			{
				return new SQLStatement(CCJSqlParserUtil.parse(stmt));
			}
			else if(chk==2)
			{
				return new SQLExpression(CCJSqlParserUtil.parseCondExpression(stmt));
			}
			else
				return new SQLExpression(CCJSqlParserUtil.parseExpression(stmt));
				
		} catch (JSQLParserException e) {
			logger.debug("parse error:"+stmt);
			System.out.println("parse error:"+stmt);
			throw new SqlParseException("SqlParse001 parse:", stmt);
		}
	}
	
	private static SQLPart parseInformixSelectStatement(String stmt)
	{
		try {
			return new SQLStatement(CCJSqlParserUtil.parse(stmt), SQLStatement.CONSTANT_DBVENDOR_INFORMIX);
				
		} catch (JSQLParserException e) {
			logger.debug("parse error:"+stmt);
			System.out.println("parse error:"+stmt);
			throw new SqlParseException("SqlParse001 parse:", stmt);
		}
	}
	
	private static int checkstmt(String stmt)
	{
		if(stmt.toLowerCase().contains("select"))
			return 1;
		if(stmt.toLowerCase().contains("="))
			return 2;
		return 3;
	}
}
