(function ( $ ) {
    $.fn.subqueryui = function(options) {
		var subselid;
		var subdivid;
		var selname;
		var selid;
			
    	hideall = function()
		{
			$.each($("span[divtype='sub']"), function(index){
				$(this).hide();
			});

		} 
		hidegroup = function(groupname)
		{
			$.each($("span[group='"+groupname+"']"), function(index){
				$(this).hide();
			});
		}
		select1_select = function(divid, objname)
		{ 
			hidegroup(objname);
			var objectid = "#"+divid+"_"+$("#"+objname).val();
			console.log("show:"+objectid+"|objname:"+objname);
			$(objectid).show();
		}
		render_sub_div = function(subjueryjson,subindex)
		{
			var sub="";
			$.each(subjueryjson, function(i, e) {
				//$.each(subqueryelement, function(i,e) {
				if(e != undefined)
					sub+='<span id='+subdivid+'_'+e[1]+' divtype=sub group='+subselid+'>'+e[2].replace(/\$sub/gi,subindex)+'</span>';
				//});
			});
			return sub;
		}

		render_sub_select = function(subjueryjson,subindex)
		{
			var sub="";
			$.each(subjueryjson, function(i, e) {
				//$.each(subqueryelement, function(i,e) {
				if(e != undefined)	
					sub+="<option value="+e[1]+">"+e[0]+"</option>";
				//});
			});
			return sub;
		}
		this.testalert=function()
		{
			alert('teewt');
		}
		this.getNode = function(idtmplt)
		{console.log("subselid:"+subselid+"|subdivid:"+subdivid+"|selname:"+selname);
			var nodeid = idtmplt.replace(/\$sub/gi,selid);
			return $("#"+nodeid);
		}
		this.getSelectedValue = function()
		{
			return $("#"+subselid).val();
		}
		
		this.initialize = function() {
			subselid = 'subsel'+this.attr('id');
			subdivid = 'subdiv'+this.attr('id');
			selname = this.attr('id');
			selid = this.attr('id');
			this.append("<select id='"+subselid+"' name='"+selname+"'></select><span id='"+subdivid+"'></span>");
			$("#"+subselid).append(render_sub_select(options));
			$("#"+subselid).off('change').on('change',function(){
				select1_select(subdivid, this.id);
			});
			$("#"+subdivid).append(render_sub_div(options,selid));
			hideall();
			return this;
		};

		
		return this.initialize();
    };
}( jQuery ));