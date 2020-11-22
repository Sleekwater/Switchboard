package org.sleekwater.switchboard;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.websocket.Session;

import org.sleekwater.switchboard.websocket.ClientWebsocketServlet;

import com.plivo.helper.api.client.RestAPI;
import com.plivo.helper.api.response.call.Call;
import com.plivo.helper.api.response.message.MessageResponse;
import com.plivo.helper.api.response.response.GenericResponse;
import com.plivo.helper.exception.PlivoException;

/**
 * Contains details about one device
 * @author sleekwater
 *
 */
public class Device {

	public String name;	// Can be edited by console
	public String number;	// Unique key
	public List<String> audit = new ArrayList<String>();	// Our audit trail
	public Integer unreadMessages = 0;
	public DeviceState state = DeviceState.IDLE;
	// If there's an active call, stash the Plivo ID for it here
	public String call_uuid="";
	// If there's an audio file planning to be played, attach it here
	public Audio currentAudio;
	// If there's a device that we're going to connect to, reference it here
	public Device currentTransfer;

	// What our current progress tracker is...
	public String progress = "";
	// Keep a history of progress so we can reliably go back to the previous step
	private List<String> history = new ArrayList<String>();
	// Do we have any recordings? (voicemails)
	public List<Recording> recordings = new ArrayList<Recording>();

	// Are we cueing up the next thing? Store it here (this is set by a goal)
	public Audio cueAudio = null;
	// Have we played this audio to this device?
	public List<Audio> playedAudios = new ArrayList<Audio>();
	public Text cueText = null;
	// Is this device directly connected to a chat window via a websocket session?
	public Session session = null;	
	
	@Override
	public String toString()
	{
		JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
		this.toJson(jsonBuilder);		
		return jsonBuilder.build().toString();
	}

	private void toJson(JsonObjectBuilder jsonBuilder)
	{
		JsonArrayBuilder auditBuilder = Json.createArrayBuilder();
		for(String a : audit) {
			auditBuilder.add(a);
		}

		JsonArrayBuilder recordingBuilder = Json.createArrayBuilder();
		for(Recording rec : recordings) {
			JsonObjectBuilder recjob = Json.createObjectBuilder();
			rec.toJson(recjob);
			recordingBuilder.add(recjob);
		}
		
		JsonObjectBuilder cueBuilder = Json.createObjectBuilder();
		if (cueText!= null)
		{
			cueText.toJson(cueBuilder);
		}
		else if (cueAudio != null)
		{
			cueAudio.toJson(cueBuilder);
		}
		jsonBuilder.add("device", Json.createObjectBuilder()
				.add("number", number)
				.add("name", name)
				.add("audit", auditBuilder.build())
				.add("unreadmessages", unreadMessages)
				.add("state", state.toString())
				.add("progress", progress)
				.add("recordings", recordingBuilder.build())
				.add("cue", cueBuilder)
				.add("direct", session != null));
			
	}
	
	/**
	 * Called whenever we change the step in an IVR menu, so that we can remember where this device has got to (for resume and record) and 
	 * keep a history of steps (so we can go back)
	 * @param newStep
	 */
	public void updateIvrProgress(IvrStep newStep)
	{
		if (!this.progress.equals(newStep.name))
		{
			this.progress = newStep.name;
			this.history.add(newStep.name);
			addAudit("Ivr step reached: " + newStep.name);
			broadcastChange("audit");
		}
	}

	public void broadcastChange(String event)
	{
		// Build our JSON message to send to the browser control console		
		JsonObjectBuilder message = Json.createObjectBuilder();
		this.toJson(message);		
		ClientWebsocketServlet.sessionHandler.Broadcast(message.build());
	}


	/**
	 * Sync my status with what Plivo thinks it should be, in case it's not what I think it is...
	 */
	public void updateState(DeviceState newState){
		if (newState != this.state)
		{
			this.state = newState;
			broadcastChange(newState.toString().toLowerCase());
		}
	}

	// Initiate a call with Plivo - when connected it will callback to the supplied URL to get the XML that determines what to do.
	// That XML will point to the URL of the currentAudio
	public void MakeCall(Audio audio) throws IOException{

		System.out.println("** MakeCall: device " + this.name + " with audio " + audio.name);	
		this.currentAudio = audio;

		RestAPI api = new RestAPI(Settings.s.plivo_auth_id, Settings.s.plivo_auth_token, "v1");

		if (this.state == DeviceState.IDLE){
			// Prevent a race hazard that causes the bizarre "to parameter must be present" error from Plivo
			this.state = DeviceState.CALL;
			LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
			parameters.put("to",this.number); // The phone number to which the all has to be placed
			parameters.put("from",Settings.s.plivo_registerednumber); // The phone number to be used as the caller id
			addAudit("Making call to " + this.number + " to play " + audio.name);
			playedAudios.add(audio);

			// answer_url is the URL invoked by Plivo when the outbound call is answered
			// and contains instructions telling Plivo what to do with the call
			// TODO - consider putting the audio name on the callback URL here so that PlayAudio can pull it out, instead of attaching to the device in currentAudio
			parameters.put("answer_url",Settings.s.callbackUrl + "PlayAudio");
			parameters.put("answer_method","GET"); // method to invoke the answer_url
			parameters.put("hangup_url",Settings.s.callbackUrl + "Hangup"); // Notified when the called device hangs up - default POST


			try {
				// Make an outbound call and print the response
				Call resp = api.makeCall(parameters);
				if (null != resp.error)
				{
					this.state = DeviceState.IDLE;
					ClientWebsocketServlet.sessionHandler.BroadcastError(resp.error);
				}
				System.out.println("Make call returned: " + resp.message);	// Normally "call fired"
				
				broadcastChange("call");

			} catch (Exception e) {
				this.state = DeviceState.IDLE;
				ClientWebsocketServlet.sessionHandler.BroadcastError(e.getLocalizedMessage());
			}
		}
		else if (this.state == DeviceState.RING){
			// This is almost certainly why we get the "to parameter must be present" error. We start the call, passing correct parameters
			// We then get routed into MakeCall again (because of a second goal matching) and were dropping into this code as an "else" clause
			// That then trampled the parameters that Plivo was using for makeCall, giving us the "to parameter..." error
			
			// OK, we're trying to answer an inbound ring with a prerecorded audio. Transfer to the PlayAudio
			LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
			// aleg_url is the URL invoked by Plivo when the call is transferred
			// and contains instructions telling Plivo what to do with the call
			parameters.put("aleg_url",Settings.s.callbackUrl + "PlayAudio");
			parameters.put("aleg_method","GET"); // method to invoke the aleg_url
			parameters.put("call_uuid", this.call_uuid);
			parameters.put("hangup_url",Settings.s.callbackUrl + "Hangup"); // (Probably) notified when the called device hangs up - default POST
			try {
				// Transfer this call and print the response
				GenericResponse resp = api.transferCall(parameters);
				if (null != resp.error)
				{
					this.state = DeviceState.IDLE;
					ClientWebsocketServlet.sessionHandler.BroadcastError(resp.error);
				}
				System.out.println(resp.message);	// Normally "transfer executed"
				addAudit("Playing: " + audio.name);
				playedAudios.add(audio);
				broadcastChange("call");				
			} catch (Exception e) {
				this.state = DeviceState.IDLE;
				ClientWebsocketServlet.sessionHandler.BroadcastError(e.getLocalizedMessage());
			}		

		}
		else
		{
			addAudit("Unable to play : " + audio.name + " because device is in state '" + this.state + "'");
			broadcastChange("warn");	
		}
	}

	/**
	 * Link two devices together - one might be already be ringing, which would be this device if so
	 * @param target
	 */
	public void Patch(Device target) {

		System.out.println("Patching " + this.number + " to " + target.number);		

		RestAPI api = new RestAPI(Settings.s.plivo_auth_id, Settings.s.plivo_auth_token, "v1");

		// Is the current target idle?
		if (this.state == DeviceState.IDLE){
			LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
			parameters.put("to",this.number); // The phone number to which the all has to be placed
			parameters.put("from",Settings.s.plivo_registerednumber); // The phone number to be used as the caller id

			// answer_url is the URL invoked by Plivo when the outbound call is answered
			// and contains instructions telling Plivo what to do with the call
			parameters.put("answer_url",Settings.s.callbackUrl + "Patch");
			parameters.put("answer_method","GET"); // method to invoke the answer_url
			parameters.put("hangup_url",Settings.s.callbackUrl + "Hangup"); // Notified when the called device hangs up - default POST

			try {
				// Make an outbound call and print the response
				Call resp = api.makeCall(parameters);
				if (null != resp.error)
				{
					ClientWebsocketServlet.sessionHandler.BroadcastError(resp.error);
				}
				System.out.println(resp.message);	// Normally "call fired"
				this.state = DeviceState.CALL;
				broadcastChange("call");

			} catch (Exception e) {
				ClientWebsocketServlet.sessionHandler.BroadcastError(e.getLocalizedMessage());
			}
		}
		else
		{			
			// Not idle, so need to transfer - this is the most likely scenario
			LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
			// aleg_url is the URL invoked by Plivo when the call is transferred
			// and contains instructions telling Plivo what to do with the call
			this.currentTransfer = target;
			parameters.put("aleg_url",Settings.s.callbackUrl + "Patch");
			parameters.put("aleg_method","GET"); // method to invoke the aleg_url
			parameters.put("call_uuid", this.call_uuid);

			try {
				// Transfer this call and print the response
				GenericResponse resp = api.transferCall(parameters);
				if (null != resp.error)
				{
					ClientWebsocketServlet.sessionHandler.BroadcastError(resp.error);
				}
				System.out.println(resp.message);	// Normally "transfer executed"
				this.state = DeviceState.CALL;
				broadcastChange("call");
			} catch (PlivoException e) {
				ClientWebsocketServlet.sessionHandler.BroadcastError(e.getLocalizedMessage());
			}		

		}

	}

	/**
	 * If we're directly connected then send the text across the websocket to the client chat window
	 * @param text
	 * @return True if delivered OK
	 */
	public boolean directMessage(String text)
	{
		if (this.session == null)
			return false;
	
		try {
			JsonObjectBuilder message = Json.createObjectBuilder();
			message.add("text", text);				
			session.getBasicRemote().sendText(message.build().toString());
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	/**
	 * Send this literal text as an SMS or a direct message (if available)
	 * @param text
	 */
	public void Sms(String text, Text activeText) {
		
		// Do any search and replace
		text = text.replace("{{direct}}", Settings.s.callbackUrl + "chat.html?device=" + this.number);
		
		try {
			// Do I have a direct connection? Prefer it if so
			if (!directMessage(text))
			{
				// No direct connection, so send the message via Plivo / SMS
				RestAPI api = new RestAPI(Settings.s.plivo_auth_id, Settings.s.plivo_auth_token, "v1");

				LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
				parameters.put("dst",this.number); // The phone number to which the all has to be placed
				parameters.put("src",Settings.s.plivo_registerednumber); // The phone number to be used as the caller id
				parameters.put("text", text); // Your SMS text message
				parameters.put("url",Settings.s.callbackUrl + "Smsstatus");
				parameters.put("method", "GET"); // The method used to call the url

				MessageResponse msgResponse = api.sendMessage(parameters);
				if (null != msgResponse.error)
				{
					ClientWebsocketServlet.sessionHandler.BroadcastError(msgResponse.error);
				}
				System.out.println(msgResponse.message);
			}			
			
			// We have an optional "active" text, which is the thing selected. However the literal characters sent may have been edited, which is the "text"
			// So I try to match against whatever I've got available for a goal, as trying to match against edited messages is problematic
			if (null == activeText)
			{
				addAudit("Sent text: " + text);
				broadcastChange("audit");
				Goals.checkGoal("textsent", text, this);	
			}
			else
			{
				addAudit("Sent text '" +activeText.label +"' : "+ text);
				broadcastChange("audit");
				Goals.checkGoal("activetext", activeText, this);
			}
		} catch (Exception e) {
			ClientWebsocketServlet.sessionHandler.BroadcastError(e.getLocalizedMessage());
		}
	}

	/**
	 * Whatever was going on has now finished, this is where I can now chain to other actions if there is a goal set
	 */
	public void completeAudioPlay()
	{
		System.out.println(this.number +" has now finished playing audio");
		addAudit("Played all of: " + this.currentAudio.name);
		this.currentAudio = null;
		this.call_uuid = null;
		this.state = DeviceState.IDLE;
		broadcastChange("idle");
		Goals.checkGoal("audiofinish", this.currentAudio, this);
	}

	public void hangup() {

		if (this.state == DeviceState.RING)
		{
			RestAPI api = new RestAPI(Settings.s.plivo_auth_id, Settings.s.plivo_auth_token, "v1");
			LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
			parameters.put("call_uuid",this.call_uuid); // The current call UUID, I think!
			try {
				api.hangupCall(parameters);
			} catch (Exception e) {
				ClientWebsocketServlet.sessionHandler.BroadcastError(e.getLocalizedMessage());
			}	
		}
		addAudit("Call ended");
	}

	public void addAudit(String string) {
		Calendar cal = Calendar.getInstance();
		// Always add to the head...
		this.audit.add(0, new SimpleDateFormat("HH:mm:ss").format(cal.getTime()) + ":" + string);
	}

	// Clear any existing cue - this may get reset by a goal, but it might not...
	public void uncue() {
		if (null != cueText)
			System.out.println("Clearing cue " + cueText.label);
		if (null != cueAudio)
			System.out.println("Clearing cue " + cueAudio.name);
		
		cueText = null;
		cueAudio = null;
	}

	
}
