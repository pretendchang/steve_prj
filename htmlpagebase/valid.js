!function(t,e)
{
	"object"==typeof exports&&"undefined"!=typeof module?
		module.exports=e():
		"function"==typeof define&&define.amd?
			define(e):
			(t||self).reportValidity=e()
}
(
	this,function()
	{
		return function(t)
		{
			if(!t)
				throw Error("Target form element missing for function `reportValidity`.");
			return HTMLFormElement.prototype.reportValidity?
				t.reportValidity():
				!!t.checkValidity()||
				(
					t.reportValidityFakeSubmit||
					(t.reportValidityFakeSubmit=document.createElement("button"),
												t.reportValidityFakeSubmit.setAttribute("type","submit"),
												t.reportValidityFakeSubmit.setAttribute("hidden","hidden"),
												t.reportValidityFakeSubmit.setAttribute("style","display:none"),
												t.reportValidityFakeSubmit.setAttribute("class","reportValidityFakeSubmit"),
												t.reportValidityFakeSubmit.addEventListener
												(
													"click",
													function(e)
													{
														t.checkValidity()&&e.preventDefault()
													}
												),
												t.appendChild(t.reportValidityFakeSubmit)
					),
					t.reportValidityFakeSubmit.click(),
					!1
				)
		}
	}
);
