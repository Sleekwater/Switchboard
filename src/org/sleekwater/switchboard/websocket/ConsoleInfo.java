package org.sleekwater.switchboard.websocket;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.sleekwater.switchboard.DeviceState;

/**
 * Information about each control console attached to the server.
 * @author sleekwater
 *
 */
public class ConsoleInfo {

	public String name;
	public ConsoleState state = ConsoleState.IDLE;
	
	public JsonObject toJson()
	{
		JsonObjectBuilder message = Json.createObjectBuilder();
		message.add("console", Json.createObjectBuilder()
		         .add("name", name)
		         .add("state", state.toString()));
		return message.build();
	}
}
