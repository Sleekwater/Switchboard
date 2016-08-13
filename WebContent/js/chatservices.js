phonecatApp.factory(['$http', function ($http) {
	var Service = {};
	return Service;
}]);


phonecatApp.factory('NotifyService', ['$rootScope','$cookies', '$timeout', function($rootScope,$cookies,$timeout) {

	var Service = {};

	Service.connected = false;
	Service.errormessage = [];
	
	//console.log(authCookie);
	// Find out where my server is located (relative to this file), so I can call back to it using websockets
	var pathArray = window.location.pathname.split( '/' );
	var newPathname = "";
	for (i = 0; i < pathArray.length - 1; i++) {
		newPathname += pathArray[i];
		newPathname += "/";
	}
	var serverURL =  "ws://" + window.location.host + newPathname + "chat";
	console.log("Connecting to " + serverURL);

	// Let us open a web socket
	Service.ws = new WebSocket(serverURL);
	//var ws = new WebSocket("ws://localhost:8080/Switchboard/actions");
	
	
	Service.registerws = function(ws)
	{
	
		ws.onopen = function()
		{
			// Web Socket is connected, try to register ourselves automatically
			$rootScope.$apply(function() {
				Service.connected = true;
			})
			Service.ws.send('{"device":"' +  $cookies.get('device') + '"}');
			console.log("connection established ...");
		};
	

		ws.onmessage = function(event) {
	
			console.log("message received : " + event.data);
			// Push add to my array - it's wrapped in an apply in case any Angular controllers are watching this array...
			$rootScope.$apply(function() {
				// Is this a device update?
				var data = angular.fromJson(event.data);
				if (data.text)
				{
					// Add in the message to my array
					Service.messages.push(data);
					$timeout(Service.sticktobottom,100);		
				}
				
				if (data.error)
				{
					console.log("Got error: " + data.error);
					Service.errormessage.push(data.error);					
				}
			});
		}   
	
		ws.onclose = function(){
			$rootScope.$apply(function() {
				Service.connected = false;
			})
			console.log('Connection closed; reconnecting');
			// Try to reconnect...
			$timeout(function(){
				console.log('restart connection');
				Service.ws = new WebSocket(serverURL);
				Service.registerws(Service.ws);
			},1000)
		}

	}
	
	Service.registerws(Service.ws);
	// The messages sent and received
	Service.messages = [];
	//Service.messages.push({'text':'hello sent', 'sent':true})
	//Service.messages.push({'text':'message received', 'sent':false})

	/* Talk to the server, always add the auth cookie value */
	Service.ping = function(message){
		console.log("Message " + message)
		Service.messages.push({"text": message, "sent":true})
		$timeout(Service.sticktobottom,100);		
		Service.ws.send('{"device":"' +  $cookies.get('device') + '", "text":' + JSON.stringify(message) + '}');
	};

	// If the auth isn't good then we can be called here to reauth
	Service.connect = function()
	{
		console.log("Connecting this device...");
		if (Service.ws.readyState != 0){
			Service.ws.send('{"device":"' +  $cookies.get('device') + '"}');
			Service.noAuth = false;
		}
		else {
			console.log('Socket not ready.')
		} 
		//ws.send('{"console":{"name":"' + getParameterByName("name") + '"}, "auth":"' + $cookies.get('auth') + '"}');
	}
	
	Service.isScrolledToBottom = true;
		
	Service.sticktobottom = function(){
		if(Service.isScrolledToBottom)
		{
			var out = document.getElementById("container");
		    out.scrollTop = out.scrollHeight - out.clientHeight;
		    //console.log("Scrolltop : " + out.scrollTop + "Scrollheight : " + out.scrollHeight + " clientHeight:" + out.clientHeight)
		}
	}


	return Service;
}]);

