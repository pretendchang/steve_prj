package com.sqlparse;

import java.util.ArrayList;
import java.util.List;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.ComparisonOperator;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.select.First.Keyword;
import net.sf.jsqlparser.util.SelectUtils;
import net.sf.jsqlparser.util.TablesNamesFinder;
public class Builder {
	final public static String OP_AND="And";
	final public static String OP_OR="Or";
	final public static String OP_EQUAL="Equal";
	
	
	public static SQLStatement buildBlankSQLStatement()
	{
		SQLStatement stmt = null;
		Select stmtblank = new Select();
	    PlainSelect plainSelect = new PlainSelect();
	    stmtblank.setSelectBody(plainSelect);
	    stmt = new SQLStatement(stmtblank);
		return stmt;
	}
	public static SQLExpression buildBlankSQLExpression()
	{
		return null;//new SQLExpression((Expression)new BinaryExpression());
	}
	
	public static void addSelectExpression(SQLStatement stmt, SqlObject express)
	{
		Select selectStatement = (Select) stmt.getStatement();
		if(express.objecttype.equals("Column"))
		{
			SelectExpressionItem si = new SelectExpressionItem();
			try {
				si.setExpression(CCJSqlParserUtil.parseExpression((String)express.value));
				Alias a = new Alias(express.alias);
				a.setUseAs(false);
				si.setAlias(a);
			} catch (JSQLParserException e) {
				throw new SqlParseException("SqlParse001", e);
			}
			((PlainSelect)selectStatement.getSelectBody()).addSelectItems(si);
		}
		else if(express.objecttype.equals("AllTableColumn"))
		{
			AllTableColumns si = new AllTableColumns(new Table((String)express.value));
			((PlainSelect)selectStatement.getSelectBody()).addSelectItems(si);
		}
		else if(express.objecttype.equals("Top"))
		{
			Top t = new Top();
			try {
				t.setExpression(CCJSqlParserUtil.parseExpression((String)express.value));
			} catch (JSQLParserException e) {
				// TODO Auto-generated catch block
				throw new SqlParseException("SqlParse001", e);
			}
			((PlainSelect)selectStatement.getSelectBody()).setTop(t);
		}
		else
			throw new SqlParseException("err01", "addSelectExpression:unimplemented SqlObject "+express.objecttype);
	}
	
	public static void addFrom(SQLStatement stmt, SqlObject express)
	{
		Select selectStatement = (Select) stmt.getStatement();
		PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();
		FromItem fi = plainSelect.getFromItem();
		if(fi==null)
		{
			if(express.objecttype.equals("Table"))
			{
				if(express.value.getClass().getName().equals("java.lang.String"))
				{
					fi = new Table((String)express.value);
					if(express.alias!=null && !express.alias.equals(""))
					{
						Alias as = new Alias(express.alias);
						as.setUseAs(false);
						fi.setAlias(as);
					}
					plainSelect.setFromItem(fi);
				}
				else
					throw new SqlParseException("err01", "addFrom:unimplemented Table SqlObject value type "+express.value.getClass().getName());
			}
			else if(express.objecttype.equals("Subselect"))
			{
				SQLStatement substmt = null;
				if(express.value.getClass().getName().equals("java.lang.String"))
				{
					substmt = Parser.parseStatement((String)express.value);
				}
				else if(express.value.getClass().getName().equals("com.sqlparse.SQLStatement"))
				{
					substmt = (SQLStatement)express.value;
				}
				else
					throw new SqlParseException("err01", "addFrom:unimplemented Subselect SqlObject value type "+express.value.getClass().getName());
			
				SubSelect subs = new SubSelect();
				if(express.alias!=null && !express.alias.equals(""))
				{
					Alias as = new Alias(express.alias);
					as.setUseAs(false);
					subs.setAlias(as);
				}
				subs.setSelectBody(((Select)substmt.getStatement()).getSelectBody());
				plainSelect.setFromItem(subs);
			}
			else
				throw new SqlParseException("err01", "addFrom:unimplemented SqlObject objecttype "+express.objecttype);
		}
		else
		{
			List<Join> js = plainSelect.getJoins();
			if(js==null)
			{
				js=new ArrayList<Join>();
				plainSelect.setJoins(js);
			}
			Join jn = new Join();
			jn.setSimple(true);
			if(express.objecttype.equals("Table"))
			{
				if(express.value.getClass().getName().equals("java.lang.String"))
				{
					Table t = new Table((String)express.value);
					if(express.alias!=null && !express.alias.equals(""))
					{
						Alias as = new Alias(express.alias);
						as.setUseAs(false);
						t.setAlias(as);
					}
					jn.setRightItem(t);
				}
				else
					throw new SqlParseException("err01", "addFrom:unimplemented Table SqlObject value type "+express.value.getClass().getName());
			}
			else if(express.objecttype.equals("Subselect"))
			{
				SQLStatement substmt = null;
				if(express.value.getClass().getName().equals("java.lang.String"))
				{
					substmt = Parser.parseStatement((String)express.value);
				}
				else if(express.value.getClass().getName().equals("com.sqlparse.SQLStatement"))
				{
					substmt = (SQLStatement)express.value;
				}
				else
					throw new SqlParseException("err01", "addFrom:unimplemented Subselect SqlObject value type "+express.value.getClass().getName());
			
				SubSelect subs = new SubSelect();
				if(express.alias!=null && !express.alias.equals(""))
				{
					subs.setAlias(new Alias(express.alias));
				}
				subs.setSelectBody(((Select)substmt.getStatement()).getSelectBody());
				jn.setRightItem(subs);
			}
			else
				throw new SqlParseException("err01", "addFrom:unimplemented SqlObject objecttype "+express.objecttype);
			
			js.add(jn);
		}
		//SelectUtils.addJoin(selectStatement, new Table("select * from aabs"), Parser.parseCondExpression("test=rr").getExpress());
	}
	
	public static SQLExpression addExpression(SQLExpression exp1, String op, SQLExpression exp2)
	{
		if(exp1 == null)
		{
			if(exp2 != null)
				return exp2;
			//exception
		}
		else if(exp2 == null)
		{
			if(exp1 != null)
				return exp1;
		}
		return addExpression(exp1.getExpress(), op, exp2.getExpress());
	}
	
	private static SQLExpression addExpression(Expression exp1, String op, Expression exp2)
	{
		if(op.equals(OP_AND))
		{	
		    AndExpression and = new AndExpression(exp1, exp2);
		    return new SQLExpression(and);
		    
		}
		else if(op.equals(OP_OR))
		{	
		    OrExpression or = new OrExpression(exp1, exp2);
		    return new SQLExpression(or);
		    
		}
		return null;

	}
	
	public static void addWhere(SQLStatement stmt, String op, String express)
	{
		Expression condexp;
		try {
			condexp = CCJSqlParserUtil.parseCondExpression(express);
		} catch (JSQLParserException e) {
			e.printStackTrace();
			throw new SqlParseException("SqlParse001", e);
		}
		addWhere(stmt, op, condexp);
	}
	
	public static void addWhere(SQLStatement stmt, String op, SQLExpression express)
	{
		addWhere(stmt, op, express.getExpress());
	}
	
	private static void addWhere(SQLStatement stmt, String op, Expression express)
	{
		List<PlainSelect> plainSelects = getPlainSelect(stmt);
		
		for(PlainSelect ps : plainSelects)
		{
			Expression where = ps.getWhere();
			if(where ==null)
			{
				where = express;
				ps.setWhere(where);
			}
			else
			{
				if(op.equals(OP_AND))
				{	
				    AndExpression and = new AndExpression(where, express);
				    ps.setWhere(and);
				}
			}
		}
	}
	
	private static List<PlainSelect> getPlainSelect(SQLStatement stmt)
	{
		List<PlainSelect> ret = new ArrayList<PlainSelect>();
		Select selectStatement1 = (Select) stmt.getStatement();
		SelectBody selectBody = selectStatement1.getSelectBody();
		if (selectBody instanceof PlainSelect) {  
			ret.add((PlainSelect)selectBody);
		}
		else
		{ 
			List<SelectBody> operationList = ((SetOperationList) selectBody).getSelects();  
	        for(SelectBody sb : operationList)
	        {
	        	ret.add((PlainSelect)sb);
	        } 
		}
		return ret;
	}
	
	public static void addWhere(SQLExpression stmt, String addtype, SQLExpression express)
	{
		addWhere(stmt, addtype, express.getExpress());
	}
	
	private static void addWhere(SQLExpression exp, String addtype, Expression express)
	{
		Expression mainexp = exp.getExpress();
		
		if(mainexp == null)
		{
			exp.setExpress(express);
		}
		else
		{
			if(addtype.equals("And"))
			{	
				mainexp = new AndExpression(mainexp, express);
				exp.setExpress(mainexp);
			}
		}
	}
	
	public static void addWhere(SQLExpression exp, String addtype, String express)
	{
		Expression condexp;
		try {
			condexp = CCJSqlParserUtil.parseCondExpression(express);
		} catch (JSQLParserException e) {
			e.printStackTrace();
			throw new SqlParseException("SqlParse001", e);
		}
		addWhere(exp, addtype, condexp);
	}
	
	public static void addOrderby(SQLStatement stmt, List<SQLOrderBy> l)
	{
		Select selectStatement = (Select) stmt.getStatement();
		
		PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();
		List<OrderByElement> lobe = new ArrayList<OrderByElement>();
		for(SQLOrderBy sob : l)
		{
			lobe.add(sob.getOrderByElement());
		}
		plainSelect.setOrderByElements(lobe);
	}
	
	public static void addOrderby(SQLStatement stmt, boolean isASC, String column)
	{
		Select selectStatement = (Select) stmt.getStatement();
		
		PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();
		List<OrderByElement> lob = plainSelect.getOrderByElements();
		if(lob == null)
		{
			lob = new ArrayList<OrderByElement>();
		}
		
		OrderByElement o = new OrderByElement();
		o.setAscDescPresent(true);
		o.setAsc(isASC);
		o.setExpression(new Column(column));
		lob.add(o);
		plainSelect.setOrderByElements(lob);
	}
	
	public static void addGroupby(SQLStatement stmt, String column)
	{
		Select selectStatement = (Select) stmt.getStatement();
		
		PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();
		plainSelect.addGroupByColumnReference(new Column(column));
	}
	
	public static void buildSum(SQLStatement stmt, String countname)
	{
		Select selectStatement = (Select) stmt.getStatement();
		List<SelectItem> lsi = ((PlainSelect)selectStatement.getSelectBody()).getSelectItems();
		List<SelectItem> newlsi = new ArrayList<SelectItem>();
		for(SelectItem si : lsi)
		{
			SelectExpressionItem sei = (SelectExpressionItem)si;
			if(sei.getAlias()!=null && sei.getAlias().toString().contains(countname))
			{
				if(sei.toString().toLowerCase().contains("sum") || sei.toString().toLowerCase().contains("count"))
				{//若countname欄位已經是  count() or sum() 不再外包sum，以免產生sum(sum(1)) aggregate SQLException
					newlsi.add(sei);
				}
				else
				{
					Function f = new Function();
					f.setName("sum");
					List<Expression> lpara = new ArrayList<Expression>();
					lpara.add(sei.getExpression());
					f.setParameters(new ExpressionList(lpara));
					SelectExpressionItem ssi = new SelectExpressionItem();
					ssi.setExpression(f);
					ssi.setAlias(sei.getAlias());
					newlsi.add(ssi);
				}
				break;
				//newlsi.add(f);
			}
		}
		lsi.clear();
		lsi.addAll(newlsi);
	}
	
	public static void buildCount(SQLStatement stmt, String countname)
	{
		Select selectStatement1 = (Select) stmt.getStatement();
		
		SelectExpressionItem si = new SelectExpressionItem();
		try {
			si.setExpression(CCJSqlParserUtil.parseExpression("count(*)"));
			
			if(countname != null)
				si.setAlias(new Alias(countname));

		} catch (JSQLParserException e) {
			throw new SqlParseException("SqlParse001", e);
		}
		SelectBody selectBody = selectStatement1.getSelectBody();
		List<SelectItem> ll = null;
		if (selectBody instanceof PlainSelect) {  
			ll = ((PlainSelect)selectBody).getSelectItems();
			ll.clear();
			((PlainSelect)selectBody).addSelectItems(si);
		}
		else
		{  
	        List<SelectBody> operationList = ((SetOperationList) selectBody).getSelects();  
	        for(SelectBody sb : operationList)
	        {
	        	ll = ((PlainSelect)sb).getSelectItems();
	        	ll.clear();
	        	((PlainSelect)sb).addSelectItems(si);
	        } 
	    } 
	}
	
	public static void removeSelectItem(SQLStatement stmt, List<String> remainitem)
	{
		Select selectStatement = (Select) stmt.getStatement();
		List<SelectItem> lsi = ((PlainSelect)selectStatement.getSelectBody()).getSelectItems();
		lsi.clear();
		if(remainitem==null || remainitem.size()==0)
		{
			return;
		}
		
		for(String s : remainitem)
		{
			SelectItem si = null;
			try {
				si = new SelectExpressionItem(CCJSqlParserUtil.parseExpression(s));
			} catch (JSQLParserException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			lsi.add(si);
		}
	}
	public static void remainLastSelectItem(SQLStatement stmt)
	{
		Select selectStatement = (Select) stmt.getStatement();
		List<SelectItem> lsi = ((PlainSelect)selectStatement.getSelectBody()).getSelectItems();
		
		List<SelectItem> dellsi = new ArrayList<SelectItem>();
		dellsi.add(lsi.get(lsi.size()-1));
		lsi.clear();
		lsi.add(dellsi.get(0));
	}
	public static void removeGroupBy(SQLStatement stmt, List<String> remainitem)
	{
		Select selectStatement = (Select) stmt.getStatement();
		List<Expression> lep = ((PlainSelect)selectStatement.getSelectBody()).getGroupByColumnReferences();
		
		if(remainitem==null || remainitem.size()==0)
		{
			if(lep != null)
				lep.clear();
			return;
		}
		
		List<Expression> dellep = new ArrayList<Expression>();
		
		for(Expression ep : lep)
		{
			boolean remain = false;
			for(String s : remainitem)
			{
				if(s.toLowerCase().equals(ep.toString().toLowerCase()))
				{
					remain = true;
					break;
				}
			}
			if(!remain)
			{
				dellep.add(ep);
			}
		}
		for(Expression ep : dellep)
		{
			lep.remove(ep);
		}
	}
	
	public static void removeWhereWithConstantExpress(SQLStatement stmt, final SQLExpression removetargetexpress, final SQLExpression constantexpress)
	{
		replaceExpressWithAnother(stmt, removetargetexpress, constantexpress);
	}
	
	public static void replaceConstantExpressWithTargetExpress(SQLStatement stmt, final SQLExpression targetexpress, final SQLExpression constantexpress)
	{
		replaceExpressWithAnother(stmt, constantexpress, targetexpress);
	}
	
	private static void replaceExpressWithAnother(SQLStatement stmt, final SQLExpression express, final SQLExpression another)
	{
		Select selectStatement = (Select) stmt.getStatement();
		Expression exp = ((PlainSelect)selectStatement.getSelectBody()).getWhere();
		final boolean []checkExist=new boolean[1];
		if(exp == null)
		{
			Parenthesis p = new Parenthesis();
			p.setExpression(another.getExpress());
			Builder.addWhere(stmt, Builder.OP_AND, p);
			return;
		}
			
		exp.accept(new ExpressionVisitorAdapter() {

	        @Override
			public void visit(Parenthesis parenthesis) {
	        	//System.out.println(parenthesis.getExpression().toString());
	        	Expression e = express.getExpress();
	        	Expression anothere = another.getExpress();

	        	if(e.toString().equals(parenthesis.getExpression().toString()))
	        	{
	        		parenthesis.setExpression(anothere);
	        		checkExist[0]=true;
	        	}
	            super.visit(parenthesis); 
	        }
	    });
		if(!checkExist[0])
		{
			Parenthesis p = new Parenthesis();
			p.setExpression(another.getExpress());
			Builder.addWhere(stmt, Builder.OP_AND, p);
		}
	}
	
	public static void setFirst(SQLStatement stmt, int firstcnt)
	{
		First f = new First();
		f.setRowCount((long)firstcnt);
		f.setKeyword(Keyword.FIRST);
		
		Select selectStatement = (Select) stmt.getStatement();
		((PlainSelect)selectStatement.getSelectBody()).setFirst(f);
		
	}
	
	public static List<SQLExpression> getBinaryWhereExpression(SQLStatement stmt, int firstlast, int cnt)//1 for first 2 for last
	{
		List<SQLExpression> ret = new ArrayList<SQLExpression>();
		Select selectStatement = (Select) stmt.getStatement();
		Expression exp = ((PlainSelect)selectStatement.getSelectBody()).getWhere();
		final List<Expression> lexp = new ArrayList<Expression>();
		if(exp != null)
		{
			exp.accept(new ExpressionVisitorAdapter() {
	
		        @Override
		        protected void visitBinaryExpression(BinaryExpression expr) {
		            if (expr instanceof ComparisonOperator) {
		                lexp.add(expr);
		            }
	
		            super.visitBinaryExpression(expr); 
		        }
		    });
		}
		if(firstlast==1)
		{
			for(int i=0;i<cnt;i++)
			{
				ret.add(new SQLExpression(lexp.get(i)));
			}
		}
		else
		{
			for(int i=0;i<cnt && i<lexp.size();i++)
			{
				ret.add(new SQLExpression(lexp.get(lexp.size()-1-i)));
			}
		}
		
		
		//List順序first依照stmt原始排列順序  last由最後開始往前排
		return ret;
	}
	
	public static List<SQLExpression> getBinaryWhereExpression(SQLStatement stmt, final String statement)//1 for first 2 for last
	{
		List<SQLExpression> ret = new ArrayList<SQLExpression>();
		Select selectStatement = (Select) stmt.getStatement();
		Expression exp = ((PlainSelect)selectStatement.getSelectBody()).getWhere();
		final List<Expression> lexp = new ArrayList<Expression>();
		exp.accept(new ExpressionVisitorAdapter() {

	        @Override
	        protected void visitBinaryExpression(BinaryExpression expr) {
	            if (expr instanceof ComparisonOperator) {
	            	ComparisonOperator co = (ComparisonOperator)expr;
	            	co.getLeftExpression().toString().equals(statement);
	                lexp.add(expr);
	            }

	            super.visitBinaryExpression(expr); 
	        }
	    });
		
		{
			for(int i=0;i<lexp.size();i++)
			{
				ret.add(new SQLExpression(lexp.get(i)));
			}
		}

		
		
		//List順序first依照stmt原始排列順序  last由最後開始往前排
		return ret;
	}
	
	/*
	public static String fixInformixTableForCompatible(String s)
	{
		return s.replace(":", ".");
	}
	
	public static String fixTableForInformix(SQLStatement stmt)
	{
		StringBuffer sb = new StringBuffer(stmt.toString());
		
		TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
		List<String> tableList = tablesNamesFinder.getTableList(stmt.getStatement());
		for(String s : tableList)
		{
			int i = sb.indexOf(s);
			s=s.replace('.', ':');
			sb.replace(i, i+s.length(), s);
		}
		return sb.toString();
	}*/
	
	public static SQLExpression BuildExpression(SQLExpression left, SQLExpression right, String op)
	{
		return BuildExpression(left.getExpress(), right.getExpress(), op);
	}
	
	private static SQLExpression BuildExpression(Expression left, Expression right, String op)
	{
		Expression condexp = null;
		try {
			if(op.equals(Builder.OP_EQUAL))
			{
				condexp = CCJSqlParserUtil.parseCondExpression("?=?");
				BinaryExpression be = ( BinaryExpression)condexp;
				be.setLeftExpression(left);
				be.setRightExpression(right);
			}
			if(op.equals(Builder.OP_AND))
			{
				condexp = new AndExpression(left, right);
			}
			if(op.equals(Builder.OP_OR))
				condexp = new OrExpression(left, right);

			 
		} catch (JSQLParserException e) {
			e.printStackTrace();
			throw new SqlParseException("SqlParse001", e);
		}
		return new SQLExpression(condexp);
	}
	
	public static SQLExpression BuildExpression(String left, String right, String op)
	{
			if(op != Builder.OP_EQUAL || (!left.equals("?") && !right.equals("?")))
				try {
					return BuildExpression(CCJSqlParserUtil.parseCondExpression(left), CCJSqlParserUtil.parseCondExpression(right), op);
				} catch (JSQLParserException e1) {
					// TODO Auto-generated catch block
					throw new SqlParseException("SqlParse001", e1);
				}
			else
			{
				Expression condexp = null;
				try {
						condexp = CCJSqlParserUtil.parseCondExpression(left+"="+right);
						BinaryExpression be = ( BinaryExpression)condexp;
						return new SQLExpression(condexp);
					}
				 catch (JSQLParserException e) {
					// TODO Auto-generated catch block
					throw new SqlParseException("SqlParse001", e);
				}
			}
	}
	
	public static SQLStatement getFirstUnionSelect(SQLStatement stmt)
	{
		try {
			Select selectStatement = (Select) stmt.getStatement();
			SelectBody sb = selectStatement.getSelectBody();
			if(sb instanceof PlainSelect)
			{
				return stmt;
			}
			else if(sb instanceof SetOperationList)
			{
				SetOperationList operationList = (SetOperationList) sb;
				if (operationList.getSelects() != null && operationList.getSelects().size() > 0) {  
		            List<SelectBody> plainSelects = operationList.getSelects(); 
		            SelectBody plainSelect = plainSelects.get(0);
		            Select select = new Select();
		            select.setSelectBody(plainSelect);
		            return new SQLStatement(select);
				}
			}
			else
			{
				
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static String checkalias(SQLStatement stmt, String columnname)
	{
		try {
			Select selectStatement = (Select) stmt.getStatement();
			SelectBody sb = selectStatement.getSelectBody();
			if(sb instanceof PlainSelect)
			{
				List<SelectItem> lsi = ((PlainSelect)sb).getSelectItems();
				for(SelectItem si : lsi)
				{
					if(si instanceof SelectExpressionItem)
					{
						if(((SelectExpressionItem)si).getAlias()!=null)
						{
							if(columnname.equalsIgnoreCase(((SelectExpressionItem)si).getAlias().getName()))
							{
								return ((SelectExpressionItem) si).getExpression().toString();
							}
						}
					}
				}
				return columnname;
			}
			else if(sb instanceof SetOperationList)
			{
				SetOperationList operationList = (SetOperationList) sb;
				
				if (operationList.getSelects() != null && operationList.getSelects().size() > 0) {  
		            List<SelectBody> plainSelects = operationList.getSelects(); 
		            SelectBody plainSelect = plainSelects.get(0);
		            
	            	List<SelectItem> lsi = ((PlainSelect)plainSelect).getSelectItems();  
	            	for(SelectItem si : lsi)
					{
						if(si instanceof SelectExpressionItem)
						{
							if(((SelectExpressionItem)si).getAlias()!=null)
							{
								if(columnname.equals(((SelectExpressionItem)si).getAlias().getName()))
								{
									return ((SelectExpressionItem) si).getExpression().toString();
								}
							}
						}
					}
					return columnname;
  
		        }  
			}
			else
			{
				
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static SQLExpression addParenthese(SQLExpression express)
	{
		Parenthesis p = new Parenthesis();
		p.setExpression(express.getExpress());
		return new SQLExpression(p);
	}
	
	public static void main(String[] args) throws Exception {
		String sleft="insert into abc (q,w,e) value('a','b','c')";
		SQLStatement sleftstmt = (SQLStatement)Parser.parseStatement(sleft);
		//sleftstmt.getStatement()
		//String abc = Builder.checkalias(sleftstmt, "transseqno");
		//System.out.println(abc);
		
		/*
		SelectItem si = new SelectExpressionItem(CCJSqlParserUtil.parseExpression("abc"));
		String a="select * from service s where s.ssno <> 255";
		SQLStatement sstmt1 = (SQLStatement)Parser.parseInformixTableSupport(a.toString());
		Select selectStatement = (Select) sstmt1.getStatement();
		List<SelectItem> lsi = ((PlainSelect)selectStatement.getSelectBody()).getSelectItems();
		lsi.clear();
		lsi.add(si);
		System.out.println(sstmt1);
		
		
		String sleft="select s.servno,s.EQUIPNO,s.ssno,NVL(s.SERVEFFDATE,TO_DATE('2100-01-01','YYYY-MM-DD')) SERVEFFDATE,NVL(s.SERVDUEDATE,TO_DATE('2100-01-01','YYYY-MM-DD')) SERVDUEDATE from SERVICE s,TELEQUIP t where s.EQUIPNO = t.EQUIPNO and (EQUIPBUSSID in ('A','B','D','E','K','T','X','Y','Z') or EQUIPNUM like 'CL%') and EQUIPNUM not like 'Z0%' and EQUIPEFFDATE is not NULL and BILLUSEMODE is not NULL and SERVEFFDATE is not null and SSNO <> 2  and t.EQUIPNO<110000 and t.EQUIPNO>100000";
		SQLStatement sleftstmt = (SQLStatement)Parser.parse(sleft);
		List<SQLExpression> sleftexpr = Builder.getBinaryWhereExpression(sleftstmt, 2, 2);
		
		//抓最後兩個，並比較最後兩個的left是否一致
		boolean checktwo=false;
		SQLExpression expr1 = sleftexpr.get(1);
		SQLExpression expr2 = sleftexpr.get(0);
		System.out.println("("+expr1.getLeftExpression()+")  ("+expr2.getLeftExpression()+")");;
		if(expr1.getLeftExpression().equals(expr2.getLeftExpression()))
		{
			System.out.println("bingo");
			checktwo=true;
		}
		
		String s="select s.servno,s.userid,s.ssno,NVL(s.servstartdate,TO_DATE('2100-01-01','%Y-%m-%d')) servstartdate,NVL(s.servenddate,TO_DATE('2100-01-01','%Y-%m-%d')) servenddate from service s where s.ssno <> 255";
		SQLStatement sstmt = (SQLStatement)Parser.parseInformixTableSupport(s.toString());
		List<String> sc = new ArrayList<String>();sc.add("s.servno");
		Builder.removeSelectItem(sstmt, sc);
		Builder.addSelectExpression(sstmt, new SqlObject("Column","0"));
		Builder.addSelectExpression(sstmt, new SqlObject("Column","count(*)"));
		//Builder.buildCount(sstmt, null);
		Builder.addGroupby(sstmt,"s.servno");
		
		String inputrange = "s.userid";
		Builder.addWhere(sstmt, "And",inputrange+expr2.getStringExpression()+expr2.getRightExpression());
		if(checktwo)
			Builder.addWhere(sstmt, "And",inputrange+expr1.getStringExpression()+expr1.getRightExpression());
		
		System.out.println(InformixBuilder.fixTableForInformix(sstmt));
		
		/*
		SQLStatement stmt = Parser.parseStatement("select f.equipno as equipno,f.offcode as offcode,f.equipnum as equipnum from FBMS_TESTEQUIPLIST_ F, TOPSCBM2.telequip t  where f.equipno=t.equipno and  F.ITEMFLAG=$ITEMFLAG and F.BILLYM=$BILLYM and F.BILLCYC=$BILLCYC");
		Builder.addSelectExpression(stmt, new SqlObject("Column","test"));
		Builder.addWhere(stmt, "And", "MOD(prc.accountbookid,10)=$testidtail");
		Builder.addFrom(stmt, new SqlObject("Table", "testtable","a1"));
		Builder.addFrom(stmt, new SqlObject("Subselect", "select * from testsubselect","a2"));
		Builder.addFrom(stmt, new SqlObject("Subselect", Parser.parseStatement("select * from testsubselectobj"),"a3"));

	    System.out.println(stmt.toString());;
	    
	    SQLStatement stmtc = Parser.parseStatement("select c.accountbookid,o.ratecode from (select distinct a.productid,a.ratecode, a.fttbspeed, b.feecode, b.feeamt, b.description from blmutildb.pddadaptermapping a,blmutildb.productitems b where 1=1 and a.productid = 204 and b.productid = a.productid and b.ratecode = a.ratecode and ppstartdate < sysdate and nvl(ppenddate,to_date('20991231','%Y%m%d')) >= sysdate) o ,bldbodd.rcsum c where 1=1 and c.productid  = o.productid and c.ratecode = o.ratecode and c.source in (103,104) and o.fttbspeed=$$informix_ratecode($ratecode) group by o.ratecode,c.accountbookid");
	    System.out.println(InformixBuilder.fixTableForInformix(stmtc));
	    
	    SQLExpression exp= new SQLExpression();
	    Builder.addWhere(exp, "And","test='a'");
	    Builder.addWhere(exp, "And","test1='a1'");
	    System.out.println(exp);
	    
	    SQLStatement stmt1 = Parser.parseStatement("select f.equipno as equipno,f.offcode as offcode,f.equipnum as equipnum from FBMS_TESTEQUIPLIST_ F, TOPSCBM2.telequip t  where f.equipno=t.equipno group by t.equipno");
	    Builder.addOrderby(stmt1, false, "testtt");
	    Builder.addGroupby(stmt1, "fsdf");
	    System.out.println(stmt1.toString());
	    
	    SQLStatement bstmt = Builder.buildBlankSQLStatement();
	    Builder.addSelectExpression(bstmt, new SqlObject("Column","mainaa"));
	    Builder.addSelectExpression(bstmt, new SqlObject("AllTableColumn","main"));
	    Builder.addFrom(bstmt, new SqlObject("Subselect","select * from abc","main"));
	    System.out.println(bstmt);
	    
	    Expression exps = CCJSqlParserUtil.parseCondExpression("a=b and c=d and d=e");
	    Expression exps2 = CCJSqlParserUtil.parseExpression("ff=dd");
	    exps = new AndExpression(exps, exps2);
	    System.out.println(exps.toString());;
	    
	    Expression expp = CCJSqlParserUtil.parseCondExpression("MOD(prc.accountbookid,10)=$testidtail");
	    System.out.println(expp.toString());;

	    Statement stmtp = CCJSqlParserUtil.parse("select prc.equipoffcode as offcode, prc.equipnumber as equipnum, prc.accountbookid as equipno,qc.itemtitle as name from blmqcdb.INF_QCLIST_CURRENTYM qc, (select * from bldbeven.billdataprc union select * from bldbodd.billdataprc) prc where qc.accountbookid=prc.accountbookid and qc.itemtitle=$itemtitle and qc.itemtype <> 0");
	    System.out.println(stmtp.toString());;
	    
	    Statement stmtp1 = CCJSqlParserUtil.parse("select c.accountbookid,o.ratecode from (select distinct a.productid,a.ratecode, a.fttbspeed, b.feecode, b.feeamt, b.description from blmutildb.pddadaptermapping a,blmutildb.productitems b where 1=1 and a.productid = 204 and b.productid = a.productid and b.ratecode = a.ratecode and ppstartdate < sysdate and nvl(ppenddate,to_date('20991231','%Y%m%d')) >= sysdate) o ,bldbodd.rcsum c where 1=1 and c.productid  = o.productid and c.ratecode = o.ratecode and c.source in (103,104) and o.fttbspeed=$ratecode group by o.ratecode,c.accountbookid");
	    System.out.println(stmtp1.toString());;
	    
	    SQLStatement sbbasesql = Parser.parseStatement("select c from abc");
	    Select selectStatement = (Select) sbbasesql.getStatement();
		PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();
		//Top f = new First();
		//f.setRowCount((long) 2);
		//plainSelect.setTop(f);
		
		SQLStatement stmtcmpdata = Parser.parseStatement("select a.* from bcmdb_qcdb.servattr a");
		
		//Builder.addSelectExpression(stmtcmpdata, new SqlObject("Column","test"));
		Select selectStatement1 = (Select) stmtcmpdata.getStatement();
		
		{
			SelectExpressionItem si = new SelectExpressionItem();
			try {
				si.setExpression(CCJSqlParserUtil.parseExpression("count(*)"));

			} catch (JSQLParserException e) {
				throw new SqlParseException("SqlParse001", e);
			}
			List<SelectItem> ll = ((PlainSelect)selectStatement1.getSelectBody()).getSelectItems();
			ll.clear();

			((PlainSelect)selectStatement1.getSelectBody()).addSelectItems(si);
		}
		System.out.println(stmtcmpdata.toString());*/
	}
}
