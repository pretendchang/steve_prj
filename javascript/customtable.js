(function ( $ ) {
    $.fn.customtableui = function(options) {
		var rowcnt=1;
		addtitle = function(id, titlelm)
		{
			var rowhtm='<tr>';
			$.each(titlelm, function(index, element) {
				rowhtm+="<th>"+ element+"</th>";
			});
			rowhtm+='</th>';
			$('#'+id+' thead').after(rowhtm);
		}
		this.addrow = function(rowelm)
		{
			var rowhtm='<tr>';
			$.each(rowelm, function(index, element) {
				rowhtm+="<td>"+ element.replace(/\$sub/gi,"sub"+rowcnt)+"</td>";
			});
			rowhtm+='</tr>';
			$('#'+this.attr('id')+' tr:last').after(rowhtm);
			rowcnt++;
		}
		this.testalert=function()
		{
			alert('teewt');
		}
		
		this.initialize = function() {
			addtitle(this.attr('id'), options.colname);
			return this;
		};

		return this.initialize();
    };
}( jQuery ));