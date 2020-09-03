package com.cmpdata.rst;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cmpdata.CmpContext;
import com.cmpdata.TreeMapUtil;
import com.qa.log.errpk;
import com.security.PathManipulation;

import com.cmpdata.genericCmpDataException;

import com.cmpdata.logic.CmpPair;
import com.cmpdata.logic.ScriptAPI;

public class ResultExecutor {
	//${left.colname}, ${right.colname}
	//${tablename}
	//#caseconstant(case1,case2,case3,case4)
	//#columnname(mode)
	//#pk('splitter')
	//string constant 'xxx'
	//left treemap, right treemap, tablename, casetype, error column, pk
	public static String executeResultFormat(CmpContext context, String input, String inputsplitter, String outputsplitter)
	{
		String []formatelm = input.split(inputsplitter);
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<formatelm.length; i++)
		{
			formatelm[i]=formatelm[i].trim();
			if(formatelm[i].startsWith("$"))
			{//${obj.field}
				Pattern pattern = Pattern.compile("^\\$\\{([\\w]+\\.?[\\w]+)\\}");
				Matcher matcher = pattern.matcher(formatelm[i]);
				while (matcher.find())
				{
					String sv = matcher.group(1);
					if(sv != null)
					{
						sb.append(parseVariable(context, sv)).append(outputsplitter);
					}
				}
			}
			else if(formatelm[i].startsWith("#"))
			{//#function_name(para1,para2,...)
				String pattern = "^#([a-zA-Z0-9_]+)\\(([a-zA-Z0-9\\,\\'\\s]+)\\)";
				Pattern r = Pattern.compile(pattern);
				Matcher m = r.matcher(formatelm[i]);
				if(m.find())
				{
					String functionname=m.group(1);
					String functionvalue=m.group(2);
					String []funval; 
					if(functionvalue.equals("','"))
					{
						funval=new String[1];
						funval[0]="','";
					}
					else
					{
						funval = functionvalue.split(",");
					}
					sb.append(parseFunction(context, functionname, funval)).append(outputsplitter);
				}
			}
			else if(formatelm[i].startsWith("'"))
			{//string constant 'xxx'
				// ^\'([\w]+)\'
				sb.append(parseConstant(formatelm[i])).append(outputsplitter);
			}
		}
		return sb.toString();
	}
	
	private static String parseVariable(CmpContext context, String sv)
	{
		if(sv.contains("."))
		{//${left.colname}, ${right.colname}
			String []s = sv.split("\\.");
			if(s[0].equals("left"))
			{
				if(context.getLeft()==null)
					return "";
				else
					return TreeMapUtil.getString(context.getLeft().get(s[1]));
			}
			else if(s[0].equals("right"))
			{
				if(context.getRight()==null)
					return "";
				else
					return TreeMapUtil.getString(context.getRight().get(s[1]));
			}
			else
			{
				//unimplemented
			}
		}
		else
		{//${tablename}
			if(sv.equals("tablename"))
			{
				return context.getTableName();
			}
			else if(sv.equals("leftvalue"))
			{
				if(context.getLeftValue() != null)
					return context.getLeftValue();
				if(context.getErrColumn()!=null && context.getLeft()!=null)
					return TreeMapUtil.getString(context.getLeft().get(context.getErrColumn().getLeftcm().toString()));
				return "";
			}
			else if(sv.equals("rightvalue"))
			{
				if(context.getRightValue() != null)
					return context.getRightValue();
				if(context.getErrColumn()!=null && context.getRight()!=null)
					return TreeMapUtil.getString(context.getRight().get(context.getErrColumn().getRightcm().toString()));
				return "";
			}
		}
		return "";
	}
	
	private static String  parseFunction(CmpContext context, String functionname, String []param)
	{
		if(functionname.equals("caseconstant"))
		{//#caseconstant(case1,case2,case3,case4)
			return parseConstant(param[context.getErrType()-1]);
		}
		else if(functionname.equals("columnname"))
		{//#columnname(mode)
			if(context.getErrColumn() == null)
				return "";
			if(param[0].equals("1"))
				return (context.getErrColumn().getRightcm().toString().startsWith("_"))?"":context.getErrColumn().getRightcm().toString();
			else
				return context.getErrColumn().toString();
		}
		else if(functionname.equals("pk"))
		{//#pk('splitter')
			String splitter = parseConstant(param[0]);
			if(context.getLeftPK()!=null)
				return context.getPKDisplay().Display(context.getLeftPK(), context.getLeft(), splitter).toString();
			else
				return context.getPKDisplay().Display(context.getRightPK(), context.getRight(), splitter).toString();
		}
		else
		{
			throw new genericCmpDataException("cmpdata006","col function unimplemented ");
		}
	}
	
	private static String parseConstant(String input)
	{//string constant 'xxx'
		// ^\'([\w]+)\'
		input=input.trim();
		Pattern pattern = Pattern.compile("^\\'([\\w|,]+)\\'");
		Matcher matcher = pattern.matcher(input);
		while (matcher.find())
		{
			return matcher.group(1);
		}
		return "";
		
	}
	
	public static void main(String []args)
	{
		String setting="${left.offcode}|${left.equipnum}|${right.equipoffcode}|${right.equipnumber}|#pk(',')|#caseconstant('m3', 'm1', 'm2', 'm4')|${tablename}|#columnname(1)|${leftvalue}| ${rightvalue}| #pk(',')";
		
		CmpContext context = new CmpContext();
		TreeMap<String, Object> left = new TreeMap<String, Object>();
		left.put("offcode", "23");left.put("equipnum", "M2345678");left.put("errcol", "test");
		TreeMap<String, Object> right = new TreeMap<String, Object>();
		right.put("equipoffcode", "23");right.put("equipnumber", "M2345678");right.put("errcol", "test");
		List<String> leftpk = new ArrayList<String>();
		leftpk.add("offcode");leftpk.add("equipnum");
		List<String> rightpk = new ArrayList<String>();
		rightpk.add("equipoffcode");rightpk.add("equipnumber");
		left.put("_cnt","3");TreeMap<String, Object> right1 = new TreeMap<String, Object>();right1.put("_cnt","4");
		context.setLeft(left);
		context.setRight(right1);
		context.setLeftPK(leftpk);
		context.setRightPK(null);
		context.setTableName("testtable");
		context.setErrType(4);
		context.setErrColumn(new CmpPair("_cnt","_cnt"));
		//CmpPair cp = new CmpPair();
		
		System.out.println(executeResultFormat(context, setting, "\\|", ","));
	}
}
	
