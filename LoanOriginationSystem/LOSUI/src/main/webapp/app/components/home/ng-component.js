(function(angular)
{
	"use strict";

	function homeController($scope, $element, $attrs,HTTPInterfaceProvider,loggedUser)
	{
		var $ctrl = this;

		this.$routerOnActivate = function()
		{
			if(loggedUser.isValid)
			{
				//TODO Code here
				$scope.userProfile = loggedUser.getUserInformation();
				console.log($scope.userProfile);
			}
			else
			{
				console.log("error - home");
				this.$router.navigate(['Login']);
			}
		}	

	}

	angular.module("mainModule").component("appHome",{
		templateUrl:"/app/components/home/ng-template.html",
		controller:homeController,
		bindings:{$router: '<'}
	});
})(window.angular);