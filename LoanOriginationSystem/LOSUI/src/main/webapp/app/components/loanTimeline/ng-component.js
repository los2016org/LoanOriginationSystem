(function(angular)
{
	"use strict";

	function loanTimeline($scope, $element, $attrs,HTTPInterfaceProvider,loggedUser)
	{
		var $ctrl = this;
		
		this.$routerOnActivate = function()
		{
			if(loggedUser.isValid)
			{
				//TODO Code here
				$ctrl.loadTimeLineData();
			}
			else
			{
				//this.$router.navigate(['Login']);
			}
		}

		$ctrl.loadTimeLineData = function () {
			
		}	
	}

	angular.module("mainModule").component("appLoanTimeline",{
		templateUrl:"/app/components/loanTimeline/ng-template.html",
		controller:loanTimeline,
		$routeConfig:[
		{
			path:"/loanSummary/timeline",
			name:"Timeline",
			useAsDefault:true,
			component:"appLoanTimeline"
		}],
		bindings:{$router: '<'}
	});
})(window.angular);