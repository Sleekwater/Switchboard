package org.sleekwater.switchboard;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.sleekwater.switchboard.Goal.GoalRunner;
import org.sleekwater.switchboard.websocket.ClientWebsocketServlet;

import com.plivo.helper.exception.PlivoException;
import com.plivo.helper.xml.elements.GetDigits;
import com.plivo.helper.xml.elements.Play;
import com.plivo.helper.xml.elements.PlivoResponse;
import com.plivo.helper.xml.elements.Record;
import com.plivo.helper.xml.elements.Redirect;
import com.plivo.helper.xml.elements.Speak;


public class IvrStep {

	public String name;
	JsonObject audio = Json.createObjectBuilder().build();	// By default, an empty object
	HashMap<String, String> keys = new HashMap<String, String>();
	public IvrState state = IvrState.IDLE;	
	public String steptype = "playaudio";// sendtext, record
	public String defaultKey = "";	// If no key pressed (or it can't be pressed)
	public String specialKey = "";	// Some steps have a "special" option - e.g. resume - and this is the key that maps to the special option
	JsonObject text =  Json.createObjectBuilder().build();	// By default, an empty object
	public String recordTime = "";	// if steptype=record then how long should it record for?

	// Parse the JSON into our object for ease of use; this is the reverse of toJson, except we've already stripped off the outer ivrstep level
	public IvrStep(JsonObject o)
	{

		if (o.containsKey("name"))
			this.setName(o.getString("name"));

		try {
			if (o.containsKey("audio"))
				this.audio = o.getJsonObject("audio");
		}
		catch (Exception e) {}	// Not an audio JSON object, leave it empty
		try {
			if (o.containsKey("text"))
				this.text = o.getJsonObject("text");
		}
		catch (Exception e) {}	// Not a text JSON object, leave it empty
		this.state = IvrState.IDLE;
		if (o.containsKey("keys"))
		{
			JsonArray arr = o.getJsonArray("keys");
			for (int i=0; i< arr.size(); i++)
			{
				JsonObject entry = arr.getJsonObject(i);
				String key = entry.getString("key");
				String target = entry.getString("target");
				keys.put(key, target);
			}
		}
		if (o.containsKey("steptype")) {
			this.steptype = o.getString("steptype");
		}
		if (o.containsKey("defaultkey")) {
			this.defaultKey = o.getString("defaultkey");
		}
		if (o.containsKey("recordtime")) {
			this.recordTime = o.getString("recordtime");
		}

		if (o.containsKey("specialkey"))
			this.specialKey = o.getString("specialkey");
	}

	/**
	 * Should we end the call after speaking the audio for this step?
	 * @return
	 */
	public boolean endsCall()
	{
		// Resume never ends the call.
		if ("resume".equalsIgnoreCase(name))
			return false;
		
		if ("playaudio".equalsIgnoreCase(steptype))
		{
			if ((null == keys || keys.size() == 0))
			{
				if ((null==defaultKey || defaultKey.length()==0))
				{
					System.out.println("Step " + this.name + " ends the call because there are no mapped keys and no default key!");
					return true;
				}
			}
		}
		// There's at least one key or a default set up - so carry on
		return false;
	}

	@Override
	public String toString()
	{
		JsonObjectBuilder message = Json.createObjectBuilder();
		this.toJson(message);		
		return message.build().toString();
	}

	void toJson(JsonObjectBuilder message)
	{
		if (getName() == null)
			setName("");
		JsonArrayBuilder keyBuilder = Json.createArrayBuilder();
		for(String key : keys.keySet()) {
			keyBuilder.add(Json.createObjectBuilder()
					.add("key", key)
					.add("target", keys.get(key)));
		}

		JsonObjectBuilder ivrstep = Json.createObjectBuilder()
				.add("name", getName())
				.add("audio", audio)
				.add("text", text)
				.add("keys", keyBuilder)
				.add("defaultkey", defaultKey)
				.add("specialkey", specialKey)
				.add("recordtime", recordTime)				
				.add("steptype", steptype)
				.add("state", state.toString());

		String errorMsg = validate();
		if (errorMsg.length() > 0)
			ivrstep.add("hasError", true).add("error", errorMsg);


		message.add("ivrstep", ivrstep);
	}

	/**
	 * Return a string saying why this step is not valid, or blank if it's all OK
	 * @return
	 */
	private String validate()
	{
		String result = "";
		if (null == this.name || this.name.length() == 0) {
			result += "Name not set";
		}
		
		if ("resume".equalsIgnoreCase(name))
		{
			if (specialKey.length() == 0)
			{
				result += " Resume key not set";
			}
		}
		if ("playaudio".equalsIgnoreCase(steptype))
		{

			if (audio.size()==0)
			{
				result += " Audio not set";
			}
			for (String key : keys.keySet())
			{
				if (key.length() == 0)
				{
					result += " key not set";
				}
				else
				{
					String target = keys.get(key);
					if (null == IvrSteps.i.get(target))
					{
						result += " step '" + target + "' does not exist for key " + key;
					}
				}
			}
		}
		else if ("sendtext".equalsIgnoreCase(steptype))
		{
			if (text.size()==0)
			{
				result += " Text not set";
			}
			if (defaultKey.length() > 0 && null == IvrSteps.i.get(defaultKey))
			{
				result += " step '" + defaultKey + "' does not exist";
			}
		}
		else if ("record".equalsIgnoreCase(steptype))
		{
			try{
				Integer rec = Integer.parseInt(recordTime);
				if (rec <=0 || rec > 120)
				{
					throw new Exception ("out of range");
				}
			}
			catch (Exception e)
			{
				result += " record time must be between 1 and 120 seconds";
			}
			if (defaultKey.length() > 0 && null == IvrSteps.i.get(defaultKey))
			{
				result += " step '" + defaultKey + "' does not exist";
			}
		}
		else
		{
			result += " not a known type of step: " + steptype;
		}
		return result;
	}

	public void broadcastChange(String event)
	{
		// Build our JSON message to send to the browser control console		
		JsonObjectBuilder message = Json.createObjectBuilder();
		this.toJson(message);		
		ClientWebsocketServlet.sessionHandler.Broadcast(message.build());
	}

	String getName() {
		return name;
	}

	void setName(String name) {
		this.name = name;
	}


	public void saveToDisk()
	{
		// Persist to disk
		PrintWriter out;
		try {
			out = new PrintWriter(Settings.s.uploadDiskPath + "/" + getName() + ".ivr");
			out.println(this.toString());				
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Return an audio, normally so that Plivo can read it
	 * @return
	 */
	public Audio pickAudio(Device d) {
		try
		{
			Audio a = new Audio(this.audio);
			if (a.isFolder)
			{
				a = Audios.a.getRandomChild(a, d);
			}
			System.out.println("Audio path is " + a.getUrl());
			return a;		
		}
		catch (Exception e)
		{
			System.out.println("Failed to get audio path for ivr step " + this);
			return null;
		}
	}

	/**
	 * Find out if the digits pressed match any of our mapped keys, and if so return the next step in the ivr menu
	 * @param digits
	 * @return
	 */
	public IvrStep parseDigits(String digits, Device d) {

		// Was a key pressed? Does it match something we're expecting?
		if (null == digits || digits.length() == 0)
		{
			// Is there a default "none" mapped?
			if (keys.containsKey("x"))
			{
				String target = keys.get(digits);
				return IvrSteps.i.get(target);
			}
		}

		// Is there a special step defined? 
		if (this.specialKey.equalsIgnoreCase(digits))
		{
			// Are we the "resume" step?
			if (this.name.equalsIgnoreCase("resume"))
			{
				// Yep, so let's grab the name of the last step this device reached, and use that instead
				IvrStep lastStepReached = IvrSteps.i.get(d.progress);
				if (null != lastStepReached)
					return lastStepReached;
			}
		}
		
		// Are we a step that has a default key (i.e. we're not expecting the user to press a key)
		if (!"playaudio".equalsIgnoreCase(this.steptype))
		{
			return (IvrSteps.i.get(defaultKey));
		}

		// Look at the key pressed to work out what the next step is
		if (keys.containsKey(digits))
		{
			String target = keys.get(digits);
			return IvrSteps.i.get(target);
		}
		return null;
	}

	/**
	 * Based on our type of step, populate the plivo response which will build the XML for us
	 * @return the step that actually handles it - so we can deal with things like SMS steps that just do something
	 * @throws PlivoException 
	 */
	public IvrStep buildPlivoIvrResponse(PlivoResponse resp, Device d, int depth) throws PlivoException {

		System.out.println("Building Plivo IVR xml for step '" + this.name + "' Device: " + d );

		try
		{
			if (depth > 4)
			{
				// OK, something's gone wrong. Bail with whatever we've got so far
				System.out.println("Depth limit exceeded  - bailing");
				return this;
			}
			if ("playaudio".equalsIgnoreCase(this.steptype))
			{
				Audio a = this.pickAudio(d);
				if (this.endsCall())
				{
					resp.append(new Play(a.getUrl()));
				}
				else
				{
					GetDigits digits = new GetDigits();
					digits.setAction(Settings.s.callbackUrl + "Answer/Ivr");
					// Only needed if we have multiple digits (it's the delay between them), but better same than sorry
					digits.setDigitTimeout(30);
					// This is how long we need to wait - TODO!
					//digits.setTimeout(a.lengthInSeconds);
					digits.setNumDigits(1);
					digits.setMethod("POST");
					digits.append(new Play(a.getUrl()));
					resp.append(digits);
					// And add in a default as well, so it will fire if no keys are pressed
					resp.append(new Redirect(Settings.s.callbackUrl + "Answer/Ivr"));
				}
			}
			else if ("sendtext".equalsIgnoreCase(this.steptype))
			{
				// This is a little unusual, in that I'm going to send the text but bounce directly to the *next* step in the chain
				// Thus, the "sendtext" step has no response to Plivo at all - it's the next step that responds.
				// Note also that if there are multiple send text steps then they'll chain. I add a depth counter to prevent mad loops...
				// Send text - I can safely do this synchronously as there's no delay involved
				Text sms = new Text(this.text);
				d.Sms(sms.name, sms);
				
				IvrStep nextStep = IvrSteps.i.get(this.defaultKey);
				if (null != nextStep)
				{
					return nextStep.buildPlivoIvrResponse(resp, d, depth+1);
				}
			}
			else if ("record".equalsIgnoreCase(this.steptype))
			{
				// OK, do the record step and tell the handler that we're part of an ivr, so we can chain onwards from that servlet
				Record rec = new Record();
				int recSecs = 60; // default
				try{
					recSecs = Integer.getInteger(recordTime);
				}
				catch (Exception e){}	// Ignore and carry on - use the default length
				rec.setMaxLength(recSecs);
				rec.setFinishOnKey("*");
				rec.setAction(Settings.s.callbackUrl + "GetRecording/" + d.number + "?ivr=true");
				resp.append(rec);
			}
		}
		catch (Exception e)
		{
			System.out.println("Exception while creating plivo XML " + e);
		}

		System.out.println("IVR XML is : " + resp.toXML());
		return this;
	}

}
