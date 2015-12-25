


function postXml(url, xmlSrc, callback, failcallback)
{
	var xmlString = xmlSrc;
	$.ajax(
	{
		url: url,
    contentType: "application/json; charset=utf-8",
    data: xmlString,
    dataType: "xml",
    success: function(x, textStatus, xhr)
    { 
    	callback(x, textStatus, xhr); 
    },
    error: function(x, textStatus, xhr)
    { 
        failcallback(x, textStatus, xhr);
    },
    processData: false,
    type: "POST"
  });
}

//行政區下拉式選單
function GetAddressRegion(optionarr, arrid, level, parent)
{
	var xmlSend = "<search><key>level</key><value>"+level+"</value><key>parent</key><value>"+parent+"</value></search>";              
  postXml("/HouseUCO/GetAddressRegion",
  xmlSend, function(data1, textStatus, jqXHR)
  {
  	$(data1).find("AddressRegion").each(function(i)
  	{
  		var $option = $(this);
      optionarr[i]=$option.children("ShowName").text();
      arrid[i]=$option.children("Dataid").text();
    });
    
    $('#govarea'+level).combobox(optionarr);
  });        
}
     


function checkoption(inputid, optionarr, arrid, value)
{
	for(var i=0;i<optionarr.length;i++)
  {
  	if(optionarr[i] == value)
    {
    	$('#'+inputid).val(arrid[i]);
      return arrid[i];
    }
  }
  $('#'+inputid).val(-1);
  return -1;
}

//type:  1 for simple format yy/mm/dd   2 for full format '民國yy年mm月dd日'
function Common2ROCDate(commondate, type)
{
    var retdata="";
    Datedata = commondate.split("/");
    
   	if(type ==1)
   	{
   		for(var i in Datedata)
	    {
	        if(i==0)
	        {
	            var rocyear=parseInt(Datedata[i])-1911;
	            retdata+=rocyear+"/";
	        }
	        if(i==1)
	        {
	            retdata+=Datedata[i]+"/";
	        }
	        if(i==2)
	        {
	            retdata+=Datedata[i];
	        }
	    }
   	}
   	else if(type ==2)
   	{
	    for(var i in Datedata)
	    {
	        if(i==0)
	        {
	            var rocyear=parseInt(Datedata[i])-1911;
	            if(rocyear>=1)
	                retdata+="民國"+rocyear+"年";
	            else
	                retdata+="民前"+(rocyear*-1)+"年";
	        }
	        if(i==1)
	        {
	            retdata+=Datedata[i]+"月";
	        }
	        if(i==2)
	        {
	            retdata+=Datedata[i]+"日";
	        }
	    }
  	}
    return retdata;
}

function ROCDate2CommonDate(rocdate)//rocdate format is yy/mm/dd
{
    var retdata="";
    Datedata = rocdate.split("/");
    for(var i in Datedata)
    {
        if(i==0)
        {
            retdata+=(parseInt(Datedata[i])+1911)+"/";
        }
        if(i==1)
        {
            retdata+=Datedata[i]+"/";
        }
        if(i==2)
        {
            retdata+=Datedata[i];
        }
    }
    return retdata;
}

//address UI
function addressclick(addresselm)
{
	if($('#'+addresselm).css('display')=='none')
  {
  	$('#'+addresselm).show();
  	$('#'+addresselm).focus();
    $('#'+addresselm+'label').css('color','black');
    $('#'+addresselm+'label').css('text-decoration','none');
  }
  else
  {
  	$('#'+addresselm).hide();
    $('#'+addresselm+'label').css('text-decoration','line-through');
    $('#'+addresselm+'label').css('color','grey');
  }
}

//檢查輸入的資料
function checkdate(checkst, msg)//if forced is 1, the blank data is not allowed
{
	if($(checkst).val().match(/^\d{2,3}[\/]\d{1,2}[\/]\d{1,2}$/))
	{
		$(checkst+"_state").html("");
		return true;
	}
  else
  {
  	//alert("輸入正確格式 yy/mm/dd");
  	if(msg=="")
    	$(checkst+"_state").html("輸入正確格式 yy/mm/dd");
    else
    	$(checkst+"_state").html(msg);
    return false;
  }
}
function checknum(checkst, msg)
{
	if($(checkst).val().match(/(\d{1,})/))
	{
		$(checkst+"_state").html("");
		return true;
	}
  else
  {
  	if(msg=="")
    	$(checkst+"_state").html("輸入數字");
    else
    	$(checkst+"_state").html(msg);
    return false;
  }
}
function checknull(checkst, msg)
{
	if($(checkst).val()!="")
  {
		$(checkst+"_state").html("");
		return true;
	}
  else
  {
  	if(msg=="")
    	$(checkst+"_state").html("輸入資料");
    else
    	$(checkst+"_state").html(msg);
    return false;
  }
}

//multiline item UI
function addNewBtnClick()
{
  addbtnclick(this.id);
}   

//各小項
function rAdd(){
 var xidx=$(this).attr('subindex');
alert(xidx);
 var dzcnt=$('#idx').val();
 var NewIndex=parseInt(dzcnt,10)+1;
 $('#idx').val(NewIndex);
 //var tt=$('#tbdloc li[subid="EditZone"]').clone();
 var tt=$('li[subid2="DataZone' + xidx + '"]').clone();
 //alert($(tt).html());
 
 $(tt).attr('subid','DataZone');
 $(tt).attr('subid2','DataZone' + NewIndex);
 $(tt).css('display','');


 $('#btnDel'+xidx,tt)
  .attr('id','btnDel'+NewIndex)
  .attr('value','刪除')
  .attr('subindex',NewIndex)
  .css('display','');


 $('#btnAdd'+xidx,tt)
  .attr('id','btnAdd'+NewIndex)
  .attr('value','新增')
  .attr('subindex',NewIndex)
  .bind('click',rAdd);
  
 $('li[subid2="DataZone' + xidx + '"]').after(tt);
}

function rDel(btnid){
alert('a');
}

function calextbuilding(itemid)
{
	var total=0;
	
	$('#ulbtnAdd'+itemid+' li[subid="DataZone"]').each(function(i){
		var $lielm = $(this);
		total += parseFloat($lielm.children('input[calid=area'+itemid+']').val());
	});
	total = total.toFixed(2);
	$('label[calid=lblAdd'+itemid+']').html(total);
	var area1=parseFloat($('label[calid=lblAdd2]').html());
	if(isNaN(area1))
		area1=0;
	var area2=parseFloat($('label[calid=lblAdd3]').html());
	if(isNaN(area2))
		area2=0;
	var area3=parseFloat($('label[calid=lblAdd4]').html());
	if(isNaN(area3))
		area3=0;
	var allarea=area1+area2+area3;
	allarea=allarea.toFixed(2);
	var allarea_m=allarea*3.3058;
	allarea_m = allarea_m.toFixed(2);
	$('#allarea').html(allarea);
	$('#allarea1').html(allarea_m);
}

function calareaunit(itemobject, itemid, factor, unit)
{
	var total=parseFloat($(itemobject).val())*factor;
	total = total.toFixed(4);
	if(itemid==2)
	{
		if(unit==0)
		{
			$(itemobject).siblings('#float_LayerArea1').val(total);
		}
		else
		{
			$(itemobject).siblings('#float_LayerArea').val(total);
		}
	}
	else if(itemid==3)
	{
		if(unit==0)
		{
			$(itemobject).siblings('#float_ExtBuildingArea1').val(total);
		}
		else
		{
			$(itemobject).siblings('#float_ExtBuildingArea').val(total);
		}
	}
	else if(itemid==4)
	{
		if(unit==0)
		{
			$(itemobject).siblings('#float_ShareArea1').val(total);
		}
		else
		{
			$(itemobject).siblings('#float_ShareArea').val(total);
		}
	}
	calextbuilding(itemid);
}


 function addbtnclick(btnid,listelm) {
  var editzoneid = '#ul'+btnid+' '+listelm+'[subid="EditZone"]';
  
  var idxid = '#idx'+btnid;
  var dzcnt=$(idxid).val();
  var NewIndex=parseInt(dzcnt,10)+1;
  $(idxid).val(NewIndex);
  
  /*
  if(dzcnt==0)
  {
  	$(editzoneid).css('display','');
  }
  */
  //else
  {
	  var tt=$(editzoneid).clone();
	  $(tt).attr('subid','DataZone');
	  $(tt).attr('subid2','DataZone' + NewIndex);
	  $(tt).css('display','');
	
	   $('#btnDel',tt)
	   .attr('id','btnDel'+NewIndex)
	   .attr('value','刪')
	   .attr('subindex',NewIndex)
	   .css('display','')
	   .bind('click',function() {
	            $(this).parent().remove();
//				var xidx=$(this).attr('subindex');
//		 		$(listelm+'[subid2="DataZone' + xidx + '"]').remove();
		 		calextbuilding(btnid.charAt(btnid.length-1));
			});
	  
	  $('#btnAdd',tt)
	   .attr('id','btnAdd'+NewIndex)
	   .attr('value','新增')
	   .attr('subindex',NewIndex)
	   .bind('click',rAdd);  
	 
	  $('#ul'+btnid).append(tt);
	}
 }
 
 $('#btnDel').click(function(){
  alert($(this).attr('subindex'));
 });   