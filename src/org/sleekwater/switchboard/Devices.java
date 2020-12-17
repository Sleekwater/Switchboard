package org.sleekwater.switchboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.websocket.Session;

import org.sleekwater.switchboard.websocket.ClientWebsocketServlet;

import com.plivo.helper.api.client.RestAPI;
import com.plivo.helper.api.response.call.LiveCall;
import com.plivo.helper.api.response.call.LiveCallFactory;
import com.plivo.helper.exception.PlivoException;

/**
 * Keep an in-memory list of registered devices and what we think their current state is<br/>
 * NOTE that this is reentrant, hence synchronized for all access to the internal list
 * @author sleekwater
 *
 */
public final class Devices {

	public static Devices d = new Devices();
	private static HashMap<String, Device> devices = new HashMap<String, Device>();

	public List<String> getall()
	{
		List<String> messages = new ArrayList<String>();
		synchronized(devices)
		{
			for (Device d : devices.values())
			{
				messages.add(d.toString());
			}
		}
		return messages;
	}

	public Boolean exists(String number)
	{
		synchronized(devices)
		{
			return devices.containsKey(number);
		}
	}

	public Boolean add(String number, String registerChannel)
	{
		Device d = new Device();

		// Testing setup
		/*		Recording r = new Recording();
				r.url="woo";
				r.duration = "100";
				d.recordings.add(r);
		 */
		// 
		//d.cueText = Texts.t.get("a");
		//d.cueAudio = Audios.a.get("Ringing_Phone.mp3");

		d.state = DeviceState.IDLE;
		if (registerChannel.equals("phone"))
			d.state = DeviceState.BUSY;
		d.number = number;
		d.name = number;	// By default the name is the same as the number
		d.addAudit("Registered by " + registerChannel);
		synchronized(devices)
		{
			if (!devices.containsKey(number))
			{
				devices.put(number, d);
				d.broadcastChange("add");
				return true;
			}
		}


		return false;
	}	


	public Boolean remove(String number) {
		synchronized(devices)
		{
			if (devices.containsKey(number))
			{
				Device d = devices.get(number);
				d.state = DeviceState.REMOVED;
				d.broadcastChange("remove");
				devices.remove(number);
				return true;
			}
		}
		return false;
	}

	public void removeAll() {
		synchronized(devices)
		{
			for (Device d : devices.values())
			{
				d.state = DeviceState.REMOVED;
				d.broadcastChange("remove");
			}			
			devices.clear();
		}
	}


	public Boolean ring(String number, String CallUUID)
	{
		synchronized(devices)
		{
			Device d = devices.get(number);
			if (null == d)
				return false;			
			d.state = DeviceState.RING;
			d.call_uuid = CallUUID;
			d.broadcastChange("ring");
		}
		return true;
	}

	public Boolean ivr(String number, String CallUUID)
	{
		synchronized(devices)
		{
			Device d = devices.get(number);
			if (null == d)
				return false;			
			d.state = DeviceState.IVR;
			d.call_uuid = CallUUID;
			d.broadcastChange("ivr");
		}
		return true;
	}

	public static Boolean hangup(String number, String callStatus, String hangupCause)
	{
		synchronized(devices)
		{
			Device d = devices.get(number);
			if (null == d)
				return false;
			d.hangup();
			d.state = DeviceState.IDLE;

			d.call_uuid = "";
			if (d.currentTransfer != null)
			{
				d.currentTransfer.hangup();
				d.currentTransfer.state = DeviceState.IDLE;
				d.currentTransfer.call_uuid = "";
				d.currentTransfer.broadcastChange("hangup");
				d.currentTransfer = null;				
			}
			if (callStatus.contains("failed"))
			{
				d.addAudit("CALL FAILED because " + hangupCause);
			}

			d.broadcastChange("hangup");
		}
		return true;
	}

	public static Boolean updateName(String number, String name)
	{
		synchronized(devices)
		{
			Device d = devices.get(number);
			if (null == d)
				return false;
			d.addAudit("Rename from " + d.name + " to " + name);
			d.name = name ;
			d.broadcastChange("edit");
		}
		return true;
	}

	public static Boolean updateProgress(String number, String newProgress)
	{
		synchronized(devices)
		{
			Device d = devices.get(number);
			if (null == d)
				return false;
			d.addAudit("Edited progress from " + d.progress + " to " + newProgress);
			d.progress = newProgress;
			d.broadcastChange("edit");
		}
		return true;
	}

	// Note that this is no longer synchronized
	public Device get(String number) {
		System.out.println("About to get device " + number);
		Device d= devices.get(number);
		System.out.println("Got device " + d);
		return d;
	}


	/**
	 * TODO - do this on a timer?
	 */
	public static void updateStatus()
	{
		RestAPI api = new RestAPI(Settings.s.plivo_auth_id, Settings.s.plivo_auth_token, "v1");
		try {
			LiveCallFactory lc = api.getLiveCalls();
			List<Device> busyList = new ArrayList<Device>();
			for (String l : lc.liveCallList)
			{
				LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
				parameters.put("call_uuid",l); 		        
				LiveCall live = api.getLiveCall(parameters);
				Device dTo = d.get(live.to);
				if (null != dTo)
				{
					busyList.add(dTo);
				}
				Device dFrom = d.get(live.from);
				if (null != dFrom)
				{
					busyList.add(dFrom);
				}
			}
			// Our busyList should now contain all the devices that Plivo thinks are busy - so sync with our list...
			for (Device dev : devices.values()){
				if (busyList.contains(dev))
				{
					dev.updateState(DeviceState.BUSY);
				}
				else
				{
					dev.updateState(DeviceState.IDLE);
				}
			}


		} catch (Exception e) {
			ClientWebsocketServlet.sessionHandler.BroadcastError(e.getLocalizedMessage());
		}
	}

	public Boolean message(String number, String text) {
		// Log that we have received this message
		Boolean bRegister = false;
		synchronized(devices)
		{
			Device d = devices.get(number);
			if (null == d)
			{
				// Are we joining by text?
				if (text.toLowerCase().contains("join"))
				{
					Devices.d.add(number, "text");
					bRegister = true;
				}
			}
			else
			{
				d.addAudit("Text received: " + text);
				d.unreadMessages += 1;
				d.broadcastChange("message");
			}
		}

		// Check goals OUTSIDE the synchronized block...
		Device d = devices.get(number);
		if (bRegister)
		{
			Goals.checkGoal("registertext", text, d);
		}
		else
		{
			Goals.checkGoal("textreceived", text, d);
		}
		return true;
	}

	public Boolean recording(String number, String url, String duration) {

		synchronized(devices)
		{
			Device d = devices.get(number);
			if (null != d)
			{
				// We get called several times for the recording, so debounce on the URL
				for (Recording r : d.recordings)
				{
					if (r.url.equals(url))
					{
						// Already got this one, so ignore this
						return false;
					}
				}
				Recording rec = new Recording();
				rec.url = url;
				rec.duration = duration;
				d.recordings.add(rec);			
				d.addAudit("Message recorded");
				d.broadcastChange("recording");
			}
		}

		return true;
	}


	public static Boolean setMessagesRead(String number) {
		// Log that we have received this message
		synchronized(devices)
		{
			Device d = devices.get(number);
			if (null == d)
				return false;
			d.unreadMessages = 0;
			d.broadcastChange("message");
		}
		return true;
	}

	public static Boolean uncue(String number) {
		// Log that we have received this message
		synchronized(devices)
		{
			Device d = devices.get(number);
			if (null == d)
				return false;
			d.uncue();
			d.broadcastChange("uncue");
		}
		return true;
	}


	public static void patch(String numberA, String numberB) {
		synchronized(devices)
		{
			Device dA = devices.get(numberA);
			Device dB = devices.get(numberB);
			if (dA.state == DeviceState.RING){
				dA.Patch(dB);
			}else if (dB.state == DeviceState.RING){
				dB.Patch(dA);				
			}
			else if (dA.state == DeviceState.IDLE && dB.state == DeviceState.IDLE)
			{
				dA.Patch(dB);
			}

		}		
	}

	/** 
	 * If we have a direct connection from a chat window then mark this device as such
	 * @param connected
	 */
	public void directConnection(String number, Session session) {
		synchronized(devices)
		{
			Device d = devices.get(number);
			if (d.session != session)
			{
				d.session = session;
				d.broadcastChange("direct");
			}
		}
	}

	/**
	 * Get a date-sorted list of all audit messages from all devices
	 */
	public List<AuditEntry> getFullAudit()
	{
		List<AuditEntry> fullAudit = new ArrayList<AuditEntry>();

		synchronized(devices)
		{
			for (Device d : devices.values())
			{
				fullAudit.addAll(d.audit);
			}
		}
		Collections.sort(fullAudit);
		
		return fullAudit;
	}

}
