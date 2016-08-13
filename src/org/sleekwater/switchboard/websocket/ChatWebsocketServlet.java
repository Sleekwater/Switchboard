package org.sleekwater.switchboard.websocket;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParserFactory;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.sleekwater.switchboard.Audio;
import org.sleekwater.switchboard.Audios;
import org.sleekwater.switchboard.Device;
import org.sleekwater.switchboard.Devices;
import org.sleekwater.switchboard.Goals;
import org.sleekwater.switchboard.Text;
import org.sleekwater.switchboard.Texts;

@ApplicationScoped
@ServerEndpoint("/chat")
public class ChatWebsocketServlet {

	/**
	 * We keep hold of the session so that I can deregister devices when they go offline
	 */
	HashMap<Session, Device> sessionToDeviceMap = new HashMap<>();
	
	@OnOpen
	public void open(Session session) {
		System.out.println("Open chat from " + session );
	}

	@OnClose
	public void close(Session session) {
		System.out.println("Close chat from " + session );
		if (sessionToDeviceMap.containsKey(session))
		{
			Device d = sessionToDeviceMap.get(session);
			if (null != d)
			{
				d.session = null;
				d.broadcastChange("chat");
			}
			sessionToDeviceMap.remove(session);
		}
	}

	@OnError
	public void onError(Throwable error) {
		// TODO - remove here?
	}

	@OnMessage
	public void handleMessage(String message, Session session) {
		
		//message = message.replace('\'', '"');
		System.out.println("Received chat: " + message);

		try{
			// We always expect a single json object - all our commands are routed in via this mechanism, with each command having a different root json object name
			JsonReader jsonReader = Json.createReader(new StringReader(message));
			JsonObject o = jsonReader.readObject();
			String device = o.getString("device");

			// Make sure that we know that this device is marked as directly connected, by passing the console directly to it
			Device d = Devices.d.get(device);
			if (null == d)
			{
				// Device not registered, so do nothing
				System.out.println("Device " + device + " is not registered");
				// TODO - send an auth message to prompt logon?
			}
			else
			{
				d.session = session;			
				if (!sessionToDeviceMap.containsKey(session))
				{
					sessionToDeviceMap.put(session, d);
					d.broadcastChange("chat");
				}
	
				if (o.containsKey("text"))
				{
					String text = o.getString("text");
					// Say that this device has sent this text messages
					Devices.d.message(device, text);
					// Debugging - echo back to caller, using the attached session within the device
					//Devices.d.get(device).directMessage(text + " back to you");
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to parse " + message + " with error " + e.getMessage());
		}
	}

}