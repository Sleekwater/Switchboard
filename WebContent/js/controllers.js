var phonecatApp = angular.module('switchboardApp', ['ngCookies']);

phonecatApp.controller('DeviceCtrl', function ($scope, NotifyService, fileUpload, $cookies) {

	$scope.hasError = true;
	$scope.devices = NotifyService.devices;
	$scope.fileUpload = fileUpload;
	// WATCH OUT. Angular will break if you bind inputs directly to a value when in a child scope (e.g. a modal dialog), so you MUST bind to an object instead
	$scope.edit = {};

	$scope.addDevice = function(){
		$scope.notifyService.ping('{"register":{"number":"' + $scope.edit.newdevice +'"}}')	
		$scope.toggleModal();
	}
	
	/* Modal stuff */
	$scope.modalShown = false;
	  $scope.toggleModal = function() {
	    $scope.modalShown = !$scope.modalShown;
	  };
	
	// Map the observables from the service into my scope so I can bind directly against them
	$scope.messages = NotifyService.messages;
	$scope.consoles = NotifyService.consoles;
	$scope.audios = NotifyService.audios;
	$scope.texts= NotifyService.texts;
	$scope.goals = NotifyService.goals;
	$scope.errormessage = NotifyService.errormessage;

	$scope.notifyService = NotifyService;

	$scope.connected = function(){
		return $scope.notifyService.connected;
	}

	$scope.noAuth = function(){
		return $scope.notifyService.noAuth;
	}
	$scope.name="";
	$scope.password = "";
	$scope.login = function(){
		console.log("Login");
		$cookies.put('auth',forge_sha256($scope.name + $scope.password));
		$cookies.put('name', $scope.name)
		// This relies on the WS connection being ready first. We will retry if it's not ready yet
		NotifyService.connect();
	}
	
	$scope.logoff = function(){
		$cookies.put('auth');
		$cookies.put('name');
		$scope.notifyService.noAuth = true;
	}

	$scope.resetDevices = function() {
		// At the moment, just reset everything...
		$scope.notifyService.ping('{"reset":{"type":"full"}}')
	}

	$scope.resetSelectedDevice = function() {
		$scope.notifyService.ping('{"reset":{"type":"part", "number":"' + $scope.getSelectedDevice().number + '"}}')
	}

	// Setup mode shows additional UI components, which are confusing when you're running a performance.
	$scope.issetup = false;	
	$scope.setupMode = function() {
		$scope.issetup = !$scope.issetup;
	}


	$scope.activePanel = 'overview'; 
	$scope.toggleActivePanel = function(panel) {
		if ($scope.isActivePanel(panel))
			$scope.activePanel = 'overview';
		else
			$scope.activePanel = panel;	  
	};
	$scope.isActivePanel = function(panel) {
		return panel === $scope.activePanel;
	};

	// Replacement for toggleSelect, as that doesn't fit with the way the system is used in real life
	$scope.deviceSelect = function(selected)
	{
		//console.log("In deviceSelect, activeAuto=" + $scope.activeAuto)
		// If we have autoselection by state on and the user clicks on something that's not in that state, turn autoselection off
		if ($scope.activeAuto == "all")
		{
			$scope.activeAuto = ""
			$scope.notifyService.activeAuto = ""
		}
		if ($scope.activeAuto == "idle" || $scope.activeAuto == "ring")
		{
			if (selected.state != $scope.activeAuto){
				$scope.activeAuto = ""
				$scope.notifyService.activeAuto = ""
			}
		}
		// Select just this device...
		for(var i = 0; i < $scope.devices.length; i++){
			var device = $scope.devices[i];
			if (device != selected)
				device.selected = false;
			else
			{
				device.selected = true;
				$scope.notifyService.ping('{"setmessagesread":{"number":"' + device.number + '"}}')
			}
		}

		// Also clear out the top selection
		$scope.activePanel = 'overview';
	}

	$scope.togglegroup1 = function()
	{
		if (!$scope.getSelectedDevice().group1)
			$scope.getSelectedDevice().group1 = true;
		else			
			$scope.getSelectedDevice().group1 = false;
	}
	
	$scope.togglegroup2 = function()
	{
		if (!$scope.getSelectedDevice().group2)
			$scope.getSelectedDevice().group2 = true;
		else			
			$scope.getSelectedDevice().group2 = false;
	}
	
	$scope.toggleSelect = function(device)
	{


		device.selected = !device.selected;
		//console.log("selected " + device.name)
		// If we've got an auto group selected, remember it by decorating the object
		if ($scope.issetup)
		{
			if ($scope.activeAuto == 'group1')
			{
				device.group1 = device.selected

			}
			if ($scope.activeAuto == 'group2')
			{
				device.group2 = device.selected
			}
		}

		if ($scope.activePanel == 'overview'){
			console.log("activepanel:" + $scope.activePanel)
			var d = $scope.getSelectedDevice()
			if (d)
				$scope.notifyService.ping('{"setmessagesread":{"number":"' + d.number + '"}}')
		}	  
	}
	$scope.activeAuto = "";
	$scope.selectAuto = function(auto)
	{
		var test = auto
		if (auto === $scope.activeAuto){
			auto = "";	// Deselect
		}
		$scope.activeAuto = auto;
		$scope.notifyService.activeAuto = auto;
		//console.log("selected " + auto)
		//if ($scope.activeAuto != "")
			$scope.doAutoSelect();
	}

	$scope.doAutoSelect = function()
	{
		// based on the current autoselect, make sure the relevant devices are selected.
		if ($scope.activeAuto == 'group1')
		{
			for(var i = 0; i < $scope.devices.length; i++){
				var device = $scope.devices[i];
				if (device.group1 != "undefined")
					device.selected = device.group1;
				if (!$scope.issetup)
					device.hidden = !device.selected;
			}	
		}
		else if ($scope.activeAuto == 'group2'){
			for(var i = 0; i < $scope.devices.length; i++){
				var device = $scope.devices[i];
				if (device.group2 != "undefined")
					device.selected = device.group2;
				if (!$scope.issetup)
					device.hidden = !device.selected;
			}	
		}
		else{	
			for(var i = 0; i < $scope.devices.length; i++){
				var device = $scope.devices[i];
				device.hidden = false;
				$scope.notifyService.autoSelectDevice(device);
			}	  
		}	  
	}


	/* Do the audio UI panel */
	$scope.activeAudio = null;
	$scope.selectAudio = function(audio)
	{
		$scope.activeAudio = audio;
		//console.log("selected " + audio.name)
	}
	$scope.deleteAudio = function(audio)
	{
		$scope.notifyService.ping('{"deleteaudio":{"name":"' + audio.name + '"}}')
	}

	$scope.broadcastAudio = function(){

		if (!$scope.activeAudio)
			return
			var selectedDevices = [];
		for(var i = 0; i < $scope.devices.length; i++){
			var device = $scope.devices[i];
			if (device.selected && (device.state == "IDLE" || device.state == "RING"))
			{
				selectedDevices.push(device.number);
			}
		}

		$scope.notifyService.ping('{"playaudio":{"devices":' + JSON.stringify(selectedDevices) + ',"audio":"' + $scope.activeAudio.name +'"}}')
	}

	/* Do the text (SMS) UI panel */
	$scope.activeText = null;
	$scope.currentText = "";
	$scope.currentLabel = "";
	
	$scope.selectText = function(text)
	{
		if ($scope.activeText == text)
		{
			$scope.activeText = null;	// Deselect
			$scope.currentText = "";
			$scope.currentLabel = "";
		}
		else{
			$scope.activeText = text;
			$scope.currentText = text.name; 
			$scope.currentLabel = text.label;
		} 
		//console.log("selected " + text.name)
	}
	$scope.deleteText = function(text)
	{
		$scope.notifyService.ping('{"deletetext":{"name":"' + text.label + '"}}')
	}
	$scope.addText = function(text)
	{
		var added = {"name":"new text", "label":"label"};
		$scope.texts.push(added);
		$scope.selectText(added);
	}

	// We always save the current text - don't need a text selected
	$scope.saveText = function()
	{
		if (null != $scope.activeText)
		{
			$scope.notifyService.ping('{"deletetext":{"name":"' + $scope.activeText.label + '"}}')
		}
		
		$scope.notifyService.ping('{"savetext":{"name":' + angular.toJson($scope.currentText) + ', "label":"' + $scope.currentLabel + '"}}')
	}


	$scope.broadcastText = function(){

		var selectedDevices = [];
		for(var i = 0; i < $scope.devices.length; i++){
			var device = $scope.devices[i];
			if (device.selected){
				selectedDevices.push(device.number);
			}
		}

		$scope.notifyService.ping('{"playtext":{"devices":' + JSON.stringify(selectedDevices) + ',"text":' + JSON.stringify($scope.currentText) +', "activetext":"' + ($scope.activeText == null ? "" : $scope.activeText.label) + '"}}')
	}

	$scope.updatestatus = function(){

		$scope.notifyService.ping('{"updatestatus":{}}')

	}

	$scope.ignore = function(){
		var devices = [];
		for(var i = 0; i < $scope.devices.length; i++){
			var device = $scope.devices[i];
			if (device.selected && device.state != "IDLE"){
				devices.push(device.number);
			}
		}

		$scope.notifyService.ping('{"ignore":{"devices":' + JSON.stringify(devices) + '}}')   	  
	}

	$scope.patch = function(){
		if (!$scope.getCanPatch())
			return
			var devices = [];
		for(var i = 0; i < $scope.devices.length; i++){
			var device = $scope.devices[i];
			if (device.selected)
				devices.push(device.number);
		}

		$scope.notifyService.ping('{"patch":{"devices":' + JSON.stringify(devices) + '}}')   	  
	}


	// You can edit the name of a device, so this allows you to save it...
	$scope.saveDevice = function(){
		$scope.notifyService.ping('{"savedevice":' + JSON.stringify($scope.getSelectedDevice()) + '}')  
	}


	/* Handlers for the left-hand side device list */  
	$scope.getSelectedDevice = function(){
		for(var i = 0; i < $scope.devices.length; i++){
			var device = $scope.devices[i];
			if (device.selected)
			{
				//console.log("selected " + device.number)
				return device;
			}
		}
		return null;
	}

	$scope.getNumDevicesSelected = function(){
		var total = 0;
		for(var i = 0; i < $scope.devices.length; i++){
			if ($scope.devices[i].selected)
			{
				total += 1;
			}
		}
		return total;
	}

	$scope.getNumNonIdleDevicesSelected = function(){
		var count = 0;
		for(var i = 0; i < $scope.devices.length; i++){
			var device = $scope.devices[i];
			if (device.selected)
			{
				if (device.state != "IDLE"){
					count +=1;
				}
			}
		}
		return count;
	}

	$scope.getNumIdleOrRingDevicesSelected = function(){
		var count = 0;
		for(var i = 0; i < $scope.devices.length; i++){
			var device = $scope.devices[i];
			if (device.selected)
			{
				if (device.state == "RING" || device.state=="IDLE"){
					count +=1;
				}
			}
		}
		return count;
	}

	$scope.getNumDevicesInState = function(state){
		var count = 0;
		for(var i = 0; i < $scope.devices.length; i++){
			var device = $scope.devices[i];
			if (device.state == state){
				count +=1;
			}
		}
		return count;
	}


	$scope.getCanPatch = function(){
		var idle = 0;
		var ring = 0;
		var total = 0;

		$scope.selectedDevice = {};
		for(var i = 0; i < $scope.devices.length; i++){
			var device = $scope.devices[i];
			if (device.selected)
			{
				total += 1;
				if (device.state == "IDLE"){
					idle +=1;
				}
				if (device.state == "RING"){
					ring +=1;
				}
			}
		}
		//console.log("canPatch: " + idle + " / " + ring);
		if (total == 2 && ((idle == 1 && ring == 1) || (idle == 0 && ring == 2)))
		{
			return true;
		}
		return false;
	}

	$scope.getCanReply = function(){
		var total = 0;
		var ring = 0;

		$scope.selectedDevice = {};
		for(var i = 0; i < $scope.devices.length; i++){
			var device = $scope.devices[i];
			if (device.selected)
			{
				total += 1;
				if (device.state == "RING"){
					ring +=1;
				}
			}
		}
		//console.log("canReply: " + total + " / " + ring);
		if (total == ring && total > 0)
		{
			return true;
		}
		return false;
	}


	$scope.uploadFile = function(){
		var file = $scope.myFile;

		//console.log('file is ' );
		//console.dir(file);

		var uploadUrl = "./FileUpload";
		$scope.fileUpload.uploadFileToUrl(file, uploadUrl);
		$scope.myFile=null;
	};



	/* Do the Goal UI panel */
	$scope.activeGoal = null;

	$scope.addGoal = function(){
		var newGoal = {}
		// Walk the list of goals and get the next number in sequence
		var key = 0
		for(var i = 0; i < $scope.goals.length; i++){
			var g = $scope.goals[i];
			if (g.name > key)
				key = g.name
		}
		key = key + 1
		console.log("Key is " + key)
		newGoal.name = key
		newGoal.srctype="audiostart";
		newGoal.desttype="sendtext";
		$scope.goals.push(newGoal)
		$scope.activeGoal = newGoal
	}

	$scope.selectGoal = function(goal)
	{
		$scope.activeGoal=goal    	  
		//console.log("selected " + goal.name)
	}
	$scope.deleteGoal = function(goal)
	{
		$scope.notifyService.ping('{"deletegoal":{"name":"' + goal.name + '"}}')
	}

	// We always save the active goal
	$scope.saveGoal = function()
	{	
		$scope.notifyService.ping('{"savegoal":' + JSON.stringify($scope.activeGoal) + '}')
	}

	$scope.describeGoal = function(goal)
	{
		var src = "<not set>";
		try{
			if (goal.srctype == "audiostart") src = "audio '" + goal.activeAudioGoalSrc.name + "' starts playing"
			if (goal.srctype == "audiofinish") src = "audio '" + goal.activeAudioGoalSrc.name + "' finishes playing"
			if (goal.srctype == "textsent") src = "text '" + goal.activeTextGoalSrc.label + "' is sent"
			if (goal.srctype == "textreceived") src = "a text containing '" + goal.textsrc + "' is received"
			if (goal.srctype == "registeraudiostart") src = "starting registration by phone "
			if (goal.srctype == "registeraudiofinish") src = "registering by phone"
			if (goal.srctype == "registertext") src = "registering by text"
		}
		catch (err){}

		var dest = "<not set>"    	  
			try{
				if (goal.desttype == "playaudio") dest = "play audio '" + goal.activeAudioGoalDest.name + "'"
				if (goal.desttype == "sendtext") dest = "send text '" + goal.activeTextGoalDest.label + "'"
				if (goal.desttype == "setprogress") dest = "change device progress to '" + goal.setprogress + "'"
				if (goal.desttype == "record") dest = "record for up to " + goal.recordtime + " seconds"
				if (goal.desttype == "cuetext") dest = "cue up text '" + goal.activeTextGoalDest.label + "'"
				if (goal.desttype == "cueaudio") dest = "cue up audio '" + goal.activeAudioGoalDest.name+ "'"
			} catch (err){}

			var delay = "";
			if (goal.delaytime > 0)
				delay = " after " + goal.delaytime + " secs ";

			return "When " + src + " then " + delay + dest 
	}

	$scope.hasCue = function()
	{
		var d = $scope.getSelectedDevice();
		if (!d)
			return false;
		if (d.cue.text || d.cue.audio )
			return true;
		return false;
	}
	
	$scope.playCue = function()
	{
		// Switch to the right screen, and make the correct item selected
		var d = $scope.getSelectedDevice()
		if (d)
		{
			if (d.cue.text)
			{
				$scope.activePanel = "sms"
				for(var i = 0; i < $scope.texts.length; i++){
					var t = $scope.texts[i];
					if (t.label == d.cue.text.label)
					{
						$scope.activeText = t;
						$scope.currentText = t.name;
						break;
					}
				}				  
			} else if (d.cue.audio)
			{
				$scope.activePanel = "audio"
					for(var i = 0; i < $scope.audios.length; i++){
						var a = $scope.audios[i];
						if (a.name == d.cue.audio.name)
						{
							$scope.activeAudio = a;
							break;
						}
					}				  
			}

			// And clear the cue
			$scope.notifyService.ping('{"uncue":{"number":"' + d.number + '"}}')
			
		}
	}

	$scope.removeErrorMessage = function(index)
	{
		$scope.errormessage.splice(index, 1)
	}
});