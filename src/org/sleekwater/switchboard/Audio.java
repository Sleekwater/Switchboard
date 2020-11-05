package org.sleekwater.switchboard;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.sleekwater.switchboard.websocket.ClientWebsocketServlet;

/**
 * Contains details about one audio recording
 * @author sleekwater
 *
 */
public class Audio {
	public String path;
	public String name = "not known";
	public String folder = "";
	public Boolean isFolder = false;
	public AudioState state = AudioState.IDLE;
	
	
	/**
	 * Create a blank audio object so it can be populated manually
	 */
	public Audio()
	{		
	}
	
	/**
	 * Create an audio object from Json - this is roughly the opposite of toJson
	 */
	public Audio(JsonObject o)
	{
		 this.state = AudioState.IDLE;
		 this.name = o.getString("name");
		 this.folder = o.getString("folder");
         this.isFolder = o.getBoolean("isFolder");
	}
	
	// return a publicly-accessible URL to this audio, so that Plivo can read it...
	public String getUrl()
	{
		return Settings.s.callbackUrl + Settings.s.uploadDirectory + "/" + (folder.length() == 0 ? "": folder + "/") + name;
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
		if (name == null)
			name = "";
		message.add("audio", Json.createObjectBuilder()
		         .add("state", state.toString())
		         .add("name", name)
		         .add("folder", folder)
		         .add("isFolder", isFolder));
	}
	
	public void broadcastChange(String event)
	{
		// Build our JSON message to send to the browser control console		
		JsonObjectBuilder message = Json.createObjectBuilder();
		this.toJson(message);		
		ClientWebsocketServlet.sessionHandler.Broadcast(message.build());
	}
}
