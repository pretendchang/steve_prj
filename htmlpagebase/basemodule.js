var _page=(function basepage()
{
	return {
		reportValidity: reportValidityAPI,
		buildmasterjson: buildmasterjson,
		builddetailjson: builddetailjson,
		packetsendout: packetsendout,
		fillMasterInput:fillMasterInput,
		fillDetailInput:fillDetailInput
	};
	//formobject:"[attr='ch']"  check input type=text validity
	//formobject:"[attr='master']"  master input
	//formobject:"[attr='detail']"  detail input
	//master detail style" detail data fill in detail field of jsonobject
	function reportValidityAPI(formobject,check_attrname, placeholder_attrname,pls_input_string)
	{
		if((typeof HTMLFormElement)=='undefined' || (typeof $(formobject).get(0).checkValidity)=='undefined')
		{
			var ret=true;
			//check and notify
			var chkattr = (check_attrname==undefined || check_attrname=='')?'input[type=text]':check_attrname;
			var retvalidity = ($(formobject).find(chkattr).each(function()
			{
				var thisemt=$(this);
				var thisval = thisemt.val();
				if(thisemt.attr('required')!=undefined)
				{
					if(reportValidityFalseNotify
					(
						this,
						function()
						{
							return (thisval==null || thisval=='');
						}
					))
					{
						ret=false;
						return true;
					}
				}
				else
				{
					if(reportValidityFalseNotify
					(
						this,
						function()
						{
							return (thisval==null || thisval=='');
						}
					))
					{
						ret=false;
						return true;
					}
				}
				if(thisemt.attr('pattern')!=undefined && thisemt.attr('pattern')!="")
				{
					if(reportValidityFalseNotify
					(
						this,
						function()
						{
							return (!testregex(thisemt.attr('pattern'), thisval));
						}
					))
					{
						ret=false;
						return true;
					}
				}
			}));
			if(!ret)
				return false;
		}
		
		else
		{
			if(!reportValidity(formobject))
				return false;
		}

		return true;
	}
	var i18n_pls_input='please input ';		
	function reportValidityFalseNotify(formelement, checkfunction,placeholder_attrname,pls_input_string)
	{
		$(formelement).siblings('#errordiv').remove();
		$(formelement).removeClass("errorinput");
		if(checkfunction())
		{
			$(formelement).addClass("errorinput");
			var plsinput = (pls_input_string==undefined||pls_input_string=='')?i18n_pls_input:pls_input_string;
			var phattr = (placeholder_attrname==undefined||placeholder_attrname=='')?'placeholder':placeholder_attrname;
			$(formelement).after("<div class=errorlabel id=errordiv>"+plsinput+$(formelement).attr(phattr)+"</div>");
			return true;
		}
		return false;
	}
	function testregex(reg, input)
	{
		if(input.search(reg)!=0)
			return false;
		var ret = input.match(reg);
		if(ret[0].length!=input.length)
		return false;
		return true;
	}
	function buildmasterjson(masterattr)
	{
		if(masterattr==undefined||masterattr==''||$(masterattr)==undefined)
			return undefined;
		
		var masteritem={};
		$(masterattr).each(function()
		{
			masteritem[this.id]=this.value;
			//retjson.push(masteritem);
		});
		console.log(masteritem);
		return masteritem;
	}
	function builddetailjson(tablerow,detailattr)
	{
		if(tablerow==undefined||detailattr==undefined||detailattr==''||$(detailattr)==undefined)
			return undefined;
		
		var detailitems=[];
		tablerow.each(function()
		{
			var detailitem={};
			$(this).find(detailattr).each(function()
			{
				detailitem[this.name]=this.value;
			});
			detailitems.push(detailitem);
		});
		console.log(detailitems);
		return detailitems;
	}
	function packetsendout(url,packet,successfunction,errorfunction,contenttype)
	{
		if(url==undefined || url=='')
		{
			console.log('packetsendout.url is null');
			return;
		}
		var successfunc = (successfunction==undefined)?default_packetsendout_successfunction:successfunction;
		var errorfunc =   (errorfunction==undefined)?default_packetsendout_errorfunction:errorfunction;
		var cnttype = (contenttype==undefined||contenttype=='')?"application/json;charset=utf-8":contenttype;
		
		jQuery.ajax({
			async: true,
			type: "POST",
			url: url,
			dataType: "json",
			contentType: cnttype,
			error: function(jqXHR, textStatus, errorThrown){
				errorfunc(jqXHR, textStatus);
			},
			success: function(data, textStatus, jqXHR){
				successfunc(data);
			},
			"data":packet
		});
	}
	function default_packetsendout_successfunction(data)
	{
		alert(data);
	}
	function default_packetsendout_errorfunction(jqXHR, textStatus)
	{
		alert(textStatus+":"+jqXHR.responseText);
	}
	function fillMasterInput(jsonobjectmasterdata, masterattr)
	{
		if(masterattr==undefined||masterattr==''||$(masterattr)==undefined)
			return undefined;
		
		$(masterattr).each(function()
		{
			$('#'+this.id).val(eval("jsonobjectmasterdata."+this.id));
		});
		
	}
	function fillDetailInput(jsonobjectdetaildata, templateobject, tableobject)
	{
		$(templateobject).tmpl(jsonobjectdetaildata).appendTo(tableobject);
	}
})();