package org.sleekwater.switchboard.websocket;

import java.io.File;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.servlet.ServletException;
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
import org.sleekwater.switchboard.IvrSteps;
import org.sleekwater.switchboard.Settings;
import org.sleekwater.switchboard.Switchboard;
import org.sleekwater.switchboard.Text;
import org.sleekwater.switchboard.Texts;

@ServerEndpoint("/client")
public class ClientWebsocketServlet {

	public static SocketSessionHandler sessionHandler = new SocketSessionHandler();
	
	@OnOpen
	public void open(Session session) {
		//System.out.println("Open from " + session + " sessionhandler=" + sessionHandler);		
		sessionHandler.addSession(session);		
	}

	@OnClose
	public void close(Session session) {
		//System.out.println("Close from " + session+ " sessionhandler=" + sessionHandler);
		sessionHandler.removeSession(session);
	}

	@OnError
	public void onError(Throwable error) {
	}

	@OnMessage
	public void handleMessage(String message, Session session) {
		
		//message = message.replace('\'', '"');
		System.out.println("Received: " + message);

		try{
			// We always expect a single json object - all our commands are routed in via this mechanism, with each command having a different root json object name
			JsonReader jsonReader = Json.createReader(new StringReader(message));
			JsonObject o = jsonReader.readObject();
			
			// Every command should have an auth value on it...
			String auth = o.getString("auth");
			//System.out.println("Auth is " + auth);
			if (!sessionHandler.validAccounts.contains(auth))
			{
				session.getBasicRemote().sendText("{\"noauth\":true}");
				throw new Exception("Invalid auth");
			}
			
			
			// See what command the client has sent me - the name of the object denotes the command
			JsonObject console = o.getJsonObject("console");
			if (null != console)
			{
				ConsoleInfo ci = new ConsoleInfo();
				ci.name = console.getString("name");
				sessionHandler.addConsoleInfo(session, ci);
			}			
			JsonObject reset = o.getJsonObject("reset");
			if (null != reset)
			{
				if (reset.getString("type").equals("full"))
				{
					// Clear out the registered devices
					Devices.d.removeAll();
				}
				else
				{
					Devices.d.remove(reset.getString("number"));
				}
			}
			JsonObject deleteaudio = o.getJsonObject("deleteaudio");
			if (null != deleteaudio)
			{
				// remove this audio
				Audios.a.remove(deleteaudio.getString("name"));
			}
			JsonObject addaudiofolder = o.getJsonObject("addfolder");
			if (null != addaudiofolder)
			{
				// Create a folder on disk
		        String folder = addaudiofolder.getString("folder");
		        String newName = addaudiofolder.getString("name");
		        File uploads = new File(Settings.s.uploadDiskPath + "/" + (folder.length()==0?"" : folder + "/") + newName);
		        // Prevent traversals
		        if (!uploads.getParentFile().toPath().startsWith(new File(Settings.s.uploadDiskPath).toPath()))
		        {
		        	// Naughty!
		        	throw new ServletException("Cannot save to " + uploads.getParentFile().toPath());        	
		        }
		        // Create the folder if necessary
		        if (!uploads.exists())
		        {
		        	uploads.mkdirs();
		        }
		        // And tell everyone about it...
				Audios.a.add(uploads.toString(), newName, folder, true);
			}
			JsonObject playaudio = o.getJsonObject("playaudio");
			if (null != playaudio)
			{
				// Play this audio to the listed devices
				String audio = playaudio.getString("audio");
				JsonArray devices = playaudio.getJsonArray("devices");
				Audio a = Audios.a.get(audio);
				if (null != a)
				{					
					// TODO - pass the array to Plivo instead of looping here, as I think the play API can take an array...
					for (JsonValue device : devices)
					{
						Device d = Devices.d.get(device.toString());
						// Is this a folder? If so, pick a random child audio, preferring one that this device has not had
						if (a.isFolder)
						{
							a = Audios.a.getRandomChild(a, d);
						}
						// We should be able to handle ringing calls here
						if (null != d)
							d.MakeCall(a);
					}
				}
			}
			JsonObject patch = o.getJsonObject("patch");
			if (null != patch)
			{
				// Connect this active device to an idle device, or connect 2 idle devices together
				JsonArray devices = patch.getJsonArray("devices");
				Devices.patch(devices.get(0).toString(), devices.get(1).toString());
			}
			
			JsonObject playtext = o.getJsonObject("playtext");
			if (null != playtext)
			{
				// As playAudio, but for sms - and the text can be edited in the console and not saved...
				String text = playtext.getString("text");
				String activetext = playtext.getString("activetext");
				Text atext = Texts.t.get(activetext);
				JsonArray devices = playtext.getJsonArray("devices");
				if (!text.isEmpty())
				{
					for (JsonValue device : devices)
					{
						Device d = Devices.d.get(device.toString());
						if (null != d)
							d.Sms(text, atext);
					}
				}
			}
			JsonObject ignore = o.getJsonObject("ignore");
			if (null != ignore)
			{
				JsonArray devices = ignore.getJsonArray("devices");
				for (JsonValue device : devices)
				{
					Devices.hangup(device.toString(),"failed","Request from console");
				}
			}
			
			JsonObject deletetext = o.getJsonObject("deletetext");
			if (null != deletetext)
			{
				Texts.t.remove(deletetext.getString("name"));
			}
			
			JsonObject savetext = o.getJsonObject("savetext");
			if (null != savetext)
			{
				Texts.t.add(savetext.getString("label"),savetext.getString("name"));
			}
			
			JsonObject updatestatus = o.getJsonObject("updatestatus");
			if (null != updatestatus)
			{
				Devices.updateStatus();
			}
			JsonObject savedevice = o.getJsonObject("savedevice");
			if (null != savedevice)
			{
				Devices.updateName(savedevice.getString("number"), savedevice.getString("name"));
			}
			JsonObject saveprogress = o.getJsonObject("saveprogress");
			if (null != saveprogress)
			{
				Devices.updateProgress(saveprogress.getString("number"), saveprogress.getString("progress"));
			}

			
			JsonObject setmessagesread = o.getJsonObject("setmessagesread");
			if (null != setmessagesread)
			{
				Devices.setMessagesRead(setmessagesread.getString("number"));
			}
			JsonObject savegoal = o.getJsonObject("savegoal");
			if (null != savegoal)
			{
				Goals.add(savegoal);
			}
			JsonObject deletegoal = o.getJsonObject("deletegoal");
			if (null != deletegoal)
			{
				Goals.remove(deletegoal.getString("name"));
			}
			JsonObject uncue = o.getJsonObject("uncue");
			if (null != uncue)
			{
				Devices.uncue(uncue.getString("number"));
			}
			JsonObject register = o.getJsonObject("register");
			if (null != register)
			{
				Devices.d.add(register.getString("number"),"console");
			}
			
			// IVR commands from the console			
			JsonObject saveivrstep = o.getJsonObject("saveivrstep");
			if (null != saveivrstep)
			{
				IvrSteps.i.add(saveivrstep).saveToDisk();
			}
			JsonObject deleteivrstep = o.getJsonObject("deleteivrstep");
			if (null != deleteivrstep)
			{
				IvrSteps.i.remove(deleteivrstep.getString("name"));
			}
			// System-wide settings
			JsonObject setting = o.getJsonObject("setting");
			if (null != setting)
			{
				Switchboard.s.parseJson(setting);
				Switchboard.s.persist();
				
				// Tell everyone about this...
				sessionHandler.Broadcast(o);
				
				// And do any heartbeat changes that I need
				Switchboard.s.setupHeartbeat(true);
			}
	
		}
		catch (Exception e)
		{
			System.out.println("Failed to parse " + message + " with error " + e.getMessage());
		}
	}

}