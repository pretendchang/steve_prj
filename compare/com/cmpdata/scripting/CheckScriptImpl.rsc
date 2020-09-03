<?xml version="1.0" encoding="utf-8" ?>
<ScriptImpls>
<Script>
<name>case2</name>
<functionname>Case2Check</functionname>
<code>
<![CDATA[
load('nashorn:mozilla_compat.js');
load('cmpdata:include.js');
importClass(Packages.com.cmpdata.logic.ScriptAPI);
importClass(Packages.com.cmpdata.logic.CmpLogic);
function Case2Check(ref){
${BODY}
}
]]>
</code>
</Script>
<Script>
<name>case3</name>
<functionname>Case3Check</functionname>
<code>
<![CDATA[
load('nashorn:mozilla_compat.js');
load('cmpdata:include.js');
importClass(Packages.com.cmpdata.logic.ScriptAPI);
importClass(Packages.com.cmpdata.logic.CmpLogic);
function Case3Check(ref){
${BODY}
}
]]>
</code>
</Script>
<Script>
<name>checkcolumnvalue</name>
<functionname>cmpvalue</functionname>
<code>
<![CDATA[
load('nashorn:mozilla_compat.js');
load('cmpdata:include.js');
importClass(Packages.com.cmpdata.logic.ScriptAPI);
importClass(Packages.com.cmpdata.logic.CmpLogic);
importClass(Packages.java.lang.Integer);


function cmpvalue(s1, s2, ref,runid){
	threadid = runid;
	_ref = ref;

${BODY}

}
]]>
</code>
</Script>
<Script>
<name>varscript</name>
<functionname>InitVar</functionname>
<code>
<![CDATA[
load('nashorn:mozilla_compat.js');
load('cmpdata:include.js');
importClass(Packages.com.cmpdata.logic.ScriptAPI);
importClass(Packages.com.cmpdata.logic.CmpLogic);
function InitVar(ref){
${BODY}
}
]]>
</code>
</Script>
<Script>
<name>SourceCountScript</name>
<functionname>count</functionname>
<code>
<![CDATA[
load('nashorn:mozilla_compat.js');
load('cmpdata:include.js');
importClass(Packages.com.cmpdata.logic.ScriptAPI);
importClass(Packages.java.lang.Integer);
function count(summary, allsummary, dbcnt){
var allrightcount = allsummary.rightcount;
var rightcount = summary.rightcount;
${BODY}
allsummary.rightcount=allrightcount;
summary.rightcount=rightcount;
}
]]>
</code>
</Script>
<Script>
<name>KeyTransformScript</name>
<functionname>KeyTransform</functionname>
<code>
<![CDATA[
load('nashorn:mozilla_compat.js');
load('cmpdata:include.js');
importClass(Packages.com.cmpdata.logic.ScriptAPI);
importClass(Packages.com.cmpdata.logic.CmpLogic);
function KeyTransform(keyvalue, ref){
${BODY}
}
]]>
</code>
</Script>
<Script>
<name>transvaluescript</name>
<functionname>transvaluescript</functionname>
<code>
<![CDATA[
load('nashorn:mozilla_compat.js');
load('cmpdata:include.js');
importClass(Packages.com.cmpdata.logic.ScriptAPI);
importClass(Packages.com.cmpdata.logic.TransLogic);
var threadid=0;
function printlog(s)
{
ScriptAPI.errpk_println(writer,s);
}
function printleftright(left)
{
result[threadid].left=left;
}
function transvalue(s1, ref,runid){
threadid = runid;
${BODY}
}
]]>
</code>
</Script>
</ScriptImpls>


