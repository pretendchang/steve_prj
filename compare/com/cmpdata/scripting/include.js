var threadid=0;
var _ref;
var _refobj;

function printlog(s)
{
	ScriptAPI.errpk_println(writer,s);
}

function printleftright(left, right)
{
	result[threadid].left=left;
	result[threadid].right=right;
}

function getLeftValue(colname)
{
	return ScriptAPI.left(_ref[0], colname);
}

function getRightValue(colname)
{
	return ScriptAPI.right(_ref[0], colname);
}

//idx begins from 0
function getRefValue(idx, colname)
{
	if(_refobj == null)
		_refobj = ScriptAPI.LazyRefInit(_ref);
		
	if(checkNULL(_refobj[idx]))
		return NULLValue();
	
	return _refobj[idx].get(colname);
}

function checkNULL(val)
{
	if(val.equals(NULLValue()))
		return true;
	
	return false;
}

function NULLValue()
{
	return "${NULL}";
}

function invokeMethod(methodname)
{
	ScriptAPI.invokeCustomMethod(methodname, arguments);
}