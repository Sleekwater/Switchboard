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
	
	private String name;
	JsonObject audio;
	HashMap<String, String> keys = new HashMap<String, String>();
	public IvrState state = IvrState.IDLE;	
	
	// Parse the JSON into our object for ease of use; this is the reverse of toJson, except we've already stripped off the outer ivrstep level
	public IvrStep(JsonObject o)
	{

		this.setName(o.getString("name"));
		this.audio = o.getJsonObject("audio");
		this.state = IvrState.IDLE;
		JsonArray arr = o.getJsonArray("keys");
		for (int i=0; i< arr.size(); i++)
		{
			JsonObject entry = arr.getJsonObject(i);
			String key = entry.getString("key");
			String target = entry.getString("target");
			keys.put(key, target);
		}		
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

		message.add("ivrstep", Json.createObjectBuilder()
		         .add("name", getName())
		         .add("audio", audio)
		         .add("keys", keyBuilder)
		         .add("state", state.toString()));
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
}
