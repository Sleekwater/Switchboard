package org.sleekwater.switchboard;

import java.io.IOException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.sleekwater.switchboard.websocket.ClientWebsocketServlet;

/**
 * Contains details about one audio recording
 * @author sleekwater
 *
 */
public class Goal {
	public JsonObject goaljson = null;	
	public GoalState state = GoalState.IDLE;	
	@Override
	public String toString()
	{
		return goaljson.toString();
	}

	
	public String getDescription()
	{
		String src = "<not set>";
		try{
			String srctype = goaljson.get("srctype").toString();
			if (srctype.equals("audiostart")) src = "audio '" + goaljson.getJsonObject("activeAudioGoalSrc").get("name").toString() + "' starts playing";
			if (srctype.equals("audiofinish")) src = "audio '" + goaljson.getJsonObject("activeAudioGoalSrc").get("name").toString() + "' finishes playing";
			if (srctype.equals("textsent")) src = "text '" + goaljson.getJsonObject("activeTextGoalSrc").get("label").toString() + "' is sent";
			if (srctype.equals("textreceived")) src = "a text containing '" + goaljson.get("textsrc").toString() + "' is received";
			if (srctype.equals("registeraudiostart")) src = "starting registration by phone ";
			if (srctype.equals("registeraudiofinish")) src = "registering by phone";
			if (srctype.equals("registertext")) src = "registering by text";
		}
		catch (Exception e){}

		String dest = "<not set>";    	  
			try{
				String desttype = goaljson.get("desttype").toString();
				if (desttype.equals("playaudio")) dest = "play audio '" + goaljson.getJsonObject("activeAudioGoalDest").get("name").toString() + "'";
				if (desttype.equals("sendtext")) dest = "send text '" + goaljson.getJsonObject("activeTextGoalDest").get("label").toString() + "'";
				if (desttype.equals("setprogress")) dest = "change device progress to '" + goaljson.get("setprogress").toString() + "'";
				if (desttype.equals("record")) dest = "record for up to " + goaljson.get("recordtime").toString() + " seconds";
				if (desttype.equals("cuetext")) dest = "cue up text '" + goaljson.getJsonObject("activeTextGoalDest").get("label").toString() + "'";
				if (desttype.equals("cueaudio")) dest = "cue up audio '" + goaljson.getJsonObject("activeAudioGoalDest").get("name").toString() + "'";
			} catch (Exception e){}

			String delay = "";
			try
			{
				delay = " after " + goaljson.get("delaytime").toString() + " secs ";
			}
			catch (Exception e){}
			return "When " + src + " then " + delay + dest; 
	}
	
	
	/**
	 * Goals are more complex json types, so I'll build the remove message manually here
	 */
	public void broadcastRemove(String name)
	{
		JsonObjectBuilder message = Json.createObjectBuilder();
		message.add("goal", Json.createObjectBuilder()
				.add("state", state.toString())
				.add("name", name));
		ClientWebsocketServlet.sessionHandler.Broadcast(message.build());
	}

	public void broadcastChange(String event)
	{
		JsonObjectBuilder message = Json.createObjectBuilder();
		message.add("goal", goaljson);		
		ClientWebsocketServlet.sessionHandler.Broadcast(message.build());
	}

	public String check(String eventtype, Object eventParam, Device d){

		// Special case for record
		if (eventtype.equals("record") && goaljson.get("srctype").toString().equals("audiofinish") && goaljson.get("desttype").toString().equals("record"))
		{
			System.out.println("Looks like a recording goal - is it the right audio?");			
			// Are we dealing with an audio that I'm going to record afterwards?
			Audio a = (Audio) eventParam;
			String s = goaljson.getJsonObject("activeAudioGoalSrc").getString("name");
			if (!s.equals(a.name))
				return null;
			// OK, so this is something that should be recorded - return the length of the recording...
			System.out.println("Met goal " + eventtype);
			return goaljson.getString("recordtime");
		}

		System.out.println("Checking event type " + eventtype + " against goal " + goaljson.get("srctype").toString());		
		if (eventtype.equals(goaljson.get("srctype").toString()) || (eventtype.equals("activetext") && goaljson.get("srctype").toString().equals("textsent")))
		{

			Boolean bReturnPlivoXml = false;
			if (eventtype.equals("registeraudiostart") || eventtype.equals("registeraudiofinish") || eventtype.equals("registertext")){
				bReturnPlivoXml = true;
			}

			// Now do a bunch of condition matching for the inputs - bail if they don't match
			if (eventtype.equals("audiostart") || eventtype.equals("audiofinish")){
				// Are we dealing with an audio that I'm watching for?
				Audio a = (Audio) eventParam;
				String s = goaljson.getJsonObject("activeAudioGoalSrc").getString("name");
				if (!s.equals(a.name))
					return null;				
			}


			if (eventtype.equals("textsent"))
			{
				// Does the text sent match what I'm watching for?
				String t = (String) eventParam;
				String s = goaljson.getJsonObject("activeTextGoalSrc").getString("name");
				if (!s.equals(t))
					return null;
			}

			if (eventtype.equals("activetext"))
			{
				// Does the text label match what I'm watching for?
				Text t = (Text) eventParam;
				String s = goaljson.getJsonObject("activeTextGoalSrc").getString("label");
				if (!s.equals(t.label))
					return null;				
			}			

			if (eventtype.equals("textreceived"))
			{
				// Does the text received match the string I've got?
				String message = ((String) eventParam).toLowerCase();
				String s = goaljson.getString("textsrc").toLowerCase();
				if (!message.contains(s))
					return null;				
			}




			// This goal fires. Are we returning some XML immediately?
			// Note that d can legally be null
			System.out.println("Met goal " + eventtype + ", doing " + goaljson.get("desttype").toString() + " with returnPlivoXml: " + bReturnPlivoXml);
			d.addAudit("Goal met: " +  getDescription());
			d.broadcastChange("goal");
			
			if (bReturnPlivoXml)
			{
				switch (goaljson.get("desttype").toString())
				{ 
				case "sendtext":
				{
					System.out.println("Goal is PlivoXML / sendText");
					return "<Speak voice=\"WOMAN\">" + goaljson.getJsonObject("activeTextGoalDest").getString("name")+ "</Speak>";
				}
				case "playaudio":
				{
					String audioname = goaljson.getJsonObject("activeAudioGoalDest").getString("name");
					System.out.println("Goal is PlivoXML / playAudio " + audioname);
					Audio a = Audios.a.get(audioname);
					if (null != a) {
						// Is this a folder? If so, pick a random child audio, preferring one that this device has not had
						if (a.isFolder)
						{
							a = Audios.a.getRandomChild(a, d);
						}
						return "<Play>" + a.getUrl() +"</Play>";		
					}
				}
				}
			}
			else
			{
				// We CANNOT do the goal code inline which might cause the original thread to wait here, 
				// as it's inside a synchronized block and so will cause other threads to block if they also
				// try to check goals - making everything stall.
				// So, start off a worker thread that does the goal instead, so that we can keep the application snappy.
				(new Thread(new GoalRunner(d, eventtype))).start();
			}

		}
		// returning non-null will prevent subsequent goals from being processed.
		return null;
	}

	public class GoalRunner implements Runnable {

		private Device d;
		private String eventtype;

		public GoalRunner(Device d, String eventtype)
		{
			this.d=d;
			this.eventtype = eventtype;
		}
		
		@Override
		public void run() {

			System.out.println("Worker thread starts for eventtype " + eventtype);

			// Do we wait for a bit first? I can do this safely because we're in a worker thread
			int delayms = 0;
			if (goaljson.get("desttype")!=null)
			{
				try{
					delayms = 1000*Integer.parseInt(goaljson.get("delaytime").toString());
				}
				catch (Exception e) {} // Ignore and carry on - doing nothing
			}
			if (delayms >0)
			{
				try {
					Thread.sleep(delayms);
					System.out.println("Worker thread sleep over after " + delayms);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			// Note that d can legally be null
			System.out.println("Met goal " + eventtype + ", doing " + goaljson.get("desttype").toString());

			switch (goaljson.get("desttype").toString())
			{
			case "playaudio":				
				String audioname = goaljson.getJsonObject("activeAudioGoalDest").getString("name");
				System.out.println("Goal is playAudio " + audioname);
				Audio a = Audios.a.get(audioname);	
				// Is this a folder? If so, pick a random child audio, preferring one that this device has not had
				if (a.isFolder)
				{
					a = Audios.a.getRandomChild(a, d);
				}
				try {
					if (null != d)
						d.MakeCall(a);
				} catch (IOException e) {

					e.printStackTrace();
				}				
				break;
			case "sendtext":
			{
				System.out.println("Goal is sendText");
				// Find the object as well, as that may chain goals...
				Text t = Texts.t.get(goaljson.getJsonObject("activeTextGoalDest").getString("label"));
				if (null != d)
					d.Sms(goaljson.getJsonObject("activeTextGoalDest").getString("name"), t);
				break;	
			}
			case "setprogress":
			{
				if (d != null){
					String newProgress = goaljson.get("setprogress").toString();
					System.out.println("Goal is setProgress " + newProgress);
					if (!d.progress.equals(newProgress))
					{
						d.progress = newProgress;
						d.broadcastChange("progress");				
					}
				}
				break;	
			}
			case "cuetext":
			{
				if (d != null)
				{
					String textLabel = goaljson.getJsonObject("activeTextGoalDest").getString("label");
					System.out.println("Goal is cuetext " + textLabel);					
					// Get hold of the relevant object...
					Text t = Texts.t.get(textLabel);
					if (null != t)
						d.cueText = t;
					d.broadcastChange("cue");
				}
				break;
			}
			case "cueaudio":
			{
				if (d != null)
				{
					String audioName = goaljson.getJsonObject("activeAudioGoalDest").getString("name");
					System.out.println("Goal is cueAudio " + audioName);

					// Get hold of the relevant object...
					Audio cueA = Audios.a.get(audioName);
					if (null != cueA)
						d.cueAudio = cueA;
					d.broadcastChange("cue");
				}
			}
			}
			System.out.println("Worker thread ends");
		} // End the worker thread


	}

}
