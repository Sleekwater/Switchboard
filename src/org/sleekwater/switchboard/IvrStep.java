package org.sleekwater.switchboard;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.sleekwater.switchboard.websocket.ClientWebsocketServlet;


public class IvrStep {
	
	public String name;
	JsonObject audio = Json.createObjectBuilder().build();	// By default, an empty object
	HashMap<String, String> keys = new HashMap<String, String>();
	public IvrState state = IvrState.IDLE;	
	
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
	}
	
	/**
	 * Should we end the call after speaking the audio for this step?
	 * @return
	 */
	public boolean endsCall()
	{
		if (keys.size() == 0)
			return true;
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
		         .add("keys", keyBuilder)
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
	 * Return an absolute url to the audio recording so that Plivo can read it
	 * @return
	 */
	public String getAudioPath(Device d) {
		try
		{
			Audio a = new Audio(this.audio);
			if (a.isFolder)
			{
				a = Audios.a.getRandomChild(a, d);
			}
			System.out.println("Audio path is " + a.getUrl());
			return a.getUrl();		
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
	public IvrStep parseDigits(String digits) {
		
		if (null == digits || digits.length() == 0 && "start".equalsIgnoreCase(this.name))
		{
			// Do the start step if we're jumping straight to it
			return this;
		}
		
		// Move to the next step
		if (keys.containsKey(digits))
		{
			String target = keys.get(digits);
			return IvrSteps.i.get(target);
		}
		return null;
	}
}
