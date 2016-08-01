package org.sleekwater.switchboard;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.sleekwater.switchboard.websocket.DeviceWebSocketServer;

/**
 * Contains details about one text - this can be an SMS or a synthesized voice message
 * @author sleekwater
 *
 */
public class Text {
	// This is the name that appears on the UI
	public String label = "";
	// This is the message that is sent
	public String name = "";
	public TextState state = TextState.IDLE;
		
	@Override
	public String toString()
	{
		JsonObjectBuilder message = Json.createObjectBuilder();
		this.toJson(message);		
		return message.build().toString();
	}
	
	public void toJson(JsonObjectBuilder message)
	{
		if (name == null)
			name = "";
		if (label == null)
			label = name.substring(Math.min(20, name.length()));
		message.add("text", Json.createObjectBuilder()
		         .add("state", state.toString())
		         .add("name", name)
		         .add("label", label));
	}
	
	public void broadcastChange(String event)
	{
		// Build our JSON message to send to the browser control console		
		JsonObjectBuilder message = Json.createObjectBuilder();
		this.toJson(message);		
		DeviceWebSocketServer.sessionHandler.Broadcast(message.build());
	}
	
	public void saveToFile(String fileName) {
		// And save to file
		PrintWriter out;
		try {
			out = new PrintWriter(Settings.s.uploadDiskPath + "/" + fileName + ".txt");
			out.println(this.toString());				
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
