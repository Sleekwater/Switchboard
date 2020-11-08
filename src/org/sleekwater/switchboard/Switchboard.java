package org.sleekwater.switchboard;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.sleekwater.switchboard.websocket.ClientWebsocketServlet;

/**
 * A singleton to hold settings common across the entire switchboard system
 * @author sleekwater
 *
 */
public final class Switchboard {
	
	public static Switchboard s = new Switchboard();
	
	public boolean isIVR = true;
	public boolean isAutoregister = false;
	
	public String toString()
	{
		return toJsonObject().toString();
	}
	/**
	 * Turn the settings into something the client can understand
	 * @return
	 */
	public void toJson(JsonObjectBuilder message)
	{
		JsonObjectBuilder settings = Json.createObjectBuilder()
		         .add("isivrmode", isIVR)
		         .add("autoregister", isAutoregister);

		message.add("setting", settings);
	}
	
	public JsonObject toJsonObject()
	{
		JsonObjectBuilder message = Json.createObjectBuilder();
		this.toJson(message);	
		return message.build();
	}

}
