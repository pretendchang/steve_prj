(function ( $ ) {
    $.fn.daterangepickerui = function(options) {
		var elmid;
		var init=0;
		this.getrange1=function()
		{
			alert($( "#"+elmid+"datepicker" ).val());
			return $( "#"+elmid+"datepicker" ).val();
		}
		this.getrange2=function()
		{
			alert($( "#"+elmid+"datepicker2" ).val());
			return $( "#"+elmid+"datepicker2" ).val();
		}
		
		this.getNode = function(idtmplt)
		{
			var nodeid = idtmplt.replace(/\$sub/gi,this.attr('id'));
			return $("#"+nodeid);
		}
		
		this.initialize = function() {
			if(init==1)
				return this;
			elmid=this.attr('id');
			if(this.children().length==0)
			{
				var nowdate = new Date();
				var nowday = nowdate.getDate();
				var cyc = Math.floor(nowday/5);
				var enddate=new Date(nowdate.getYear()+1900,nowdate.getMonth(),cyc*5);
				var startdate=new Date(nowdate.getYear()+1900,nowdate.getMonth()-1,cyc*5+1);
				this.append("<input type=text id="+elmid+"datepicker size=5>~<input type=text id="+elmid+"datepicker2 size=5>");
				$( "#"+elmid+"datepicker" ).datepicker({
					dateFormat: "yymmdd",
					monthNames:["1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月"],
					maxDate: enddate,
					minDate: startdate,
					onSelect: function(dateText, inst) {
						if($( "#"+elmid+"datepicker2" ).val()<dateText)
							$( "#"+elmid+"datepicker2" ).val(dateText);
						$( "#"+elmid+"datepicker2" ).datepicker("option", "minDate",dateText);
					}
				});
				$( "#"+elmid+"datepicker2" ).datepicker({
					dateFormat: "yymmdd",
					monthNames:["1月","2月","3月","4月","5月","6月","7月","8月","9月","10月","11月","12月"],
					maxDate: enddate,
					minDate: startdate,
					onSelect: function(dateText, inst) {
						if($( "#"+elmid+"datepicker" ).val()>dateText)
							$( "#"+elmid+"datepicker" ).val(dateText);
						$( "#"+elmid+"datepicker" ).datepicker("option", "maxDate",dateText);
					}
				});

			}
			init=1;
			return this;
		};

		
		return this.initialize();
    };
}( jQuery ));