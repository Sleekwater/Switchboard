var phonecatApp = angular.module('switchboardApp', ['ngCookies']);

// Make the container a fixed size, less the "banner" of the input window - so that the sticky scrolling works
phonecatApp.directive('banner', function ($window) {

    return {
        restrict: 'A',

        link: function (scope, elem, attrs) {

            var winHeight = $window.innerHeight;

            var headerHeight = attrs.banner ? attrs.banner : 0;

            elem.css('height', winHeight - headerHeight + 'px');
        }
    };
});

phonecatApp.controller('ChatCtrl', function ($scope, NotifyService, $cookies, $timeout) {
	$scope.hasError = true;

	$scope.date = new Date();
	
	// Map the observables from the service into my scope so I can bind directly against them
	$scope.messages = NotifyService.messages;
	$scope.errormessage = NotifyService.errormessage;
	$scope.notifyService = NotifyService;
	
	$scope.connected = function(){
		return $scope.notifyService.connected;
	}

	$scope.noAuth = function(){
		return $scope.notifyService.noAuth;
	}

	$scope.device=location.search.split('device=')[1]; //"447866555273";
	console.log($scope.device)
	$scope.login = function(){
		// Consider allowing join this way
		console.log("Login");
		$cookies.put('device', $scope.device)
		// This relies on the WS connection being ready first. We will retry if it's not ready yet
		NotifyService.connect();
	}
	
	$scope.logoff = function(){
		$cookies.put('device');
		$scope.notifyService.noAuth = true;
	}

	$scope.send = function()
	{
		if ($scope.message.length > 0)
		{
			$scope.notifyService.ping($scope.message)
			$scope.message = ""
		}
	}

	$scope.removeErrorMessage = function(index)
	{
		$scope.errormessage.splice(index, 1)
	}
	
	$scope.login();
	
});