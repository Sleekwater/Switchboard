phonecatApp.factory('fileUpload', ['$http', function ($http) {

	var Service = {};
	Service.uploadFileToUrl = function(file, uploadUrl){
		var fd = new FormData();
		fd.append('file', file);

		$http.post(uploadUrl, fd, {
			transformRequest: angular.identity,
			headers: {'Content-Type': undefined}
		})

		.success(function(){
		})

		.error(function(){
		});
	}
	return Service;
}]);


phonecatApp.factory('NotifyService', ['$rootScope','$cookies', '$timeout', function($rootScope,$cookies,$timeout) {

	var Service = {};

	var noAuth = false;
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
	var serverURL =  "ws://" + window.location.host + newPathname + "client";
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
			Service.ws.send('{"console":{"name":"' +  $cookies.get('name') + '"}, "auth":"' + $cookies.get('auth') + '"}');
			console.log("connection established ...");
		};
	

		ws.onmessage = function(event) {
	
			console.log("message received : " + event.data);
			// Push add to my array - it's wrapped in an apply in case any Angular controllers are watching this array...
			$rootScope.$apply(function() {
				// Is this a device update?
				var data = angular.fromJson(event.data);
				if (data.noauth)
				{
					// Tell the UI to prompt for credentials
					console.log("Prompt for auth");
					Service.noAuth = true;
				}
				
				if (data.device)
				{
					// Do I already have this device? Need to search by phone number...
					var arrayLength = Service.devices.length;
					var found = -1;
					var deviceFound = null;
					for (var i = 0; i < arrayLength; i++) {
						if (Service.devices[i].number == data.device.number)
						{
							found = i;
							deviceFound = Service.devices[found]
							break;
						}
					}
					if (found == -1)
					{
						if (data.device.state!='REMOVED'){
							Service.devices.push(data.device);
							Service.autoSelectDevice(data.device);
						}
					}
					else {
						// Update existing. I do this so that I maintain array order
						if (data.device.state=='REMOVED')
							Service.devices.splice(found, 1);
						else{ 
							// Copy over any selection status
							//console.log("Original selected is " + deviceFound.selected + " : " + deviceFound.number)
							data.device.selected = false
							data.device.selected = deviceFound.selected
							if (data.device.selected == "undefined")
								data.device.selected = false;
							data.device.hidden = deviceFound.hidden
							data.device.group1 = deviceFound.group1
							data.device.group2 = deviceFound.group2
							data.device.name = deviceFound.name // In case we're editing it
							//console.log("Setting selected to " + data.device.selected + " : " + data.device.number)
							Service.devices[found] = data.device;
							Service.autoSelectDevice(data.device);
						}
					}
				}
				// Same again for consoles. Seems awkward, but..
				if (data.console)
				{				
					// Do I already have this console? Search by name...
					var arrayLength = Service.consoles.length;
					var found = -1;
					for (var i = 0; i < arrayLength; i++) {
						if (Service.consoles[i].name== data.console.name)
						{
							found = i;
							break;
						}
					}
					if (found == -1)
					{
						if (data.console.state!='REMOVED')
							Service.consoles.push(data.console);
					}
					else {
						// Update existing. I do this so that I maintain array order
						if (data.console.state=='REMOVED')
							Service.consoles.splice(found, 1);
						else 
							Service.consoles[found] = data.console;						
					}
				}
				
				// Are we resetting the performance? Wipe out all the data, the server will reinit and tell us what's going on shortly
				if (data.reset)
				{
					Service.audios.splice(0,Service.audios.length);
					Service.texts.splice(0,Service.texts.length);
					Service.goals.splice(0,Service.goals.length);
					Service.ivrsteps.splice(0,Service.ivrsteps.length);
				}
				
				if (data.audio)
				{
					//console.log("Got an audio event");
					// Do I already have this audio? Search by name...
					var arrayLength = Service.audios.length;
					var found = -1;
					for (var i = 0; i < arrayLength; i++) {
						if (Service.audios[i].name== data.audio.name)
						{
							found = i;
							break;
						}
					}
					if (found == -1)
					{
						if (data.audio.state!='REMOVED')
							Service.audios.push(data.audio);
					}
					else {
						// Update existing. I do this so that I maintain array order
						if (data.audio.state=='REMOVED')
							Service.audios.splice(found, 1);
						else 
							Service.audios[found] = data.audio;						
					}
				}
				if (data.text)
				{
					console.log("Got an text event, " + Service.texts.length);
					// Do I already have this text? Search by name...
					var arrayLength = Service.texts.length;
					var found = -1;
					for (var i = 0; i < arrayLength; i++) {
						if (Service.texts[i].label== data.text.label)
						{
							found = i;
							break;
						}
					}
					if (found == -1)
					{
						if (data.text.state!='REMOVED')
							Service.texts.push(data.text);
					}
					else {
						// Update existing. I do this so that I maintain array order
						if (data.text.state=='REMOVED')
							Service.texts.splice(found, 1);
						else 
							Service.texts[found] = data.text;						
					}
				}
				
				if (data.goal)
				{
					console.log("Got an goal event, " + Service.goals.length);
					// Do I already have this goal? Search by name...
					var arrayLength = Service.goals.length;
					var found = -1;
					for (var i = 0; i < arrayLength; i++) {
						if (Service.goals[i].name== data.goal.name)
						{
							found = i;
							break;
						}
					}
					if (found == -1)
					{
						if (data.goal.state!='REMOVED')
							Service.goals.push(data.goal);
					}
					else {
						// Update existing. I do this so that I maintain array order
						if (data.goal.state=='REMOVED')
							Service.goals.splice(found, 1);
						else 
							Service.goals[found] = data.goal;						
					}
				}
				
				if (data.ivrstep)
				{
					console.log("Got an ivrstep event, " + Service.ivrsteps.length);
					// Do I already have this? Search by name...
					var arrayLength = Service.ivrsteps.length;
					var found = -1;
					for (var i = 0; i < arrayLength; i++) {
						if (Service.ivrsteps[i].name== data.ivrstep.name)
						{
							found = i;
							break;
						}
					}
					if (found == -1)
					{
						if (data.ivrstep.state!='REMOVED')
							Service.ivrsteps.push(data.ivrstep);
					}
					else {
						// Update existing. I do this so that I maintain array order
						if (data.ivrstep.state=='REMOVED')
							Service.ivrsteps.splice(found, 1);
						else 
							Service.ivrsteps[found] = data.ivrstep;						
					}
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
	// Not currently used
	Service.messages = [];

	// The phones registered
	Service.devices = [];
	// The control consoles attached - I'm one of these, but there can be others and I'd like to know about them
	Service.consoles = [];
	// The audio files uploaded which can be selected
	Service.audios = [];
	// The text strings which can be selected
	Service.texts = [];
	// The goal objects that can be edited
	Service.goals = [];
	// The ivrstep objects that make up our IVR menu
	Service.ivrsteps = [];
	// What are we autoselecting?
	Service.activeAuto="";
	Service.activeText="";

	/* Talk to the server, always add the auth cookie value */
	Service.ping = function(message){
		console.log("Message " + message)
		var msg = JSON.parse(message)
		msg.auth=$cookies.get('auth')
		Service.ws.send(JSON.stringify(msg))
	};

	// If the auth isn't good then we can be called here to reauth
	Service.connect = function()
	{
		console.log("Connecting this console...");
		if (Service.ws.readyState != 0){
			Service.ws.send('{"console":{"name":"' +  $cookies.get('name') + '"}, "auth":"' + $cookies.get('auth') + '"}');
			Service.noAuth = false;
		}
		else {
			console.log('Socket not ready.')
		} 
		//ws.send('{"console":{"name":"' + getParameterByName("name") + '"}, "auth":"' + $cookies.get('auth') + '"}');
	}
	// Logic of whether a device should be autoselected or not. Here as we can do it from websocket messages
	Service.autoSelectDevice = function(device)
	{
		if (Service.activeAuto !=="" && Service.activeAuto !=="group1" && Service.activeAuto !=="group2"){
			if ((device.state == 'IDLE' && Service.activeAuto == 'idle') || 
					(device.state=='RING' && Service.activeAuto == 'ring') ||
					(Service.activeAuto == 'all'))
			{
				//console.log('Autoselecting true ' + device.number + ' because ActiveAuto=' + Service.activeAuto + ' and state=' + device.state)
				device.selected = true;
			}
			else
			{
				device.selected = false;
			}
		}
	}

	return Service;
}]);

/*
function getParameterByName(name, url) {
	if (!url) url = window.location.href;
	url = url.toLowerCase(); // This is just to avoid case sensitiveness  
	name = name.replace(/[\[\]]/g, "\\$&").toLowerCase();// This is just to avoid case sensitiveness for query parameter name
	var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
	results = regex.exec(url);
	if (!results) return null;
	if (!results[2]) return '';
	return decodeURIComponent(results[2].replace(/\+/g, " "));
}*/