package org.sleekwater.switchboard.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sleekwater.switchboard.Device;
import org.sleekwater.switchboard.Devices;
import org.sleekwater.switchboard.Goals;
import org.sleekwater.switchboard.Settings;
import org.sleekwater.switchboard.Switchboard;
import org.sleekwater.switchboard.websocket.ClientWebsocketServlet;

import com.plivo.helper.exception.PlivoException;
import com.plivo.helper.xml.elements.Play;
import com.plivo.helper.xml.elements.PlivoResponse;
import com.plivo.helper.xml.elements.Record;
import com.plivo.helper.xml.elements.Redirect;

@WebServlet(description = "Servlet handler for Plivo PlayAudio calls", urlPatterns = { "/PlayAudio" }, loadOnStartup=1)
public class PlayAudio extends HttpServlet {
	private static final long serialVersionUID = 1L;
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		System.out.println("PlayAudio/GET");
		Map map = request.getParameterMap();
		for (Object key: map.keySet())
	    {
	            String keyStr = (String)key;
	            String[] value = (String[])map.get(keyStr);
	            System.out.println("Key=" + (String)key + "   :   " + Arrays.toString(value));
	    };

		
		String to = request.getParameter("To");
		String from = request.getParameter("From");
		Device d = Devices.d.get(to);
		if (null == d)
			d = Devices.d.get(from);
			
		if (d == null)
		{
			System.out.println("No device found to play");
			String errorXml = "<Response>" + Switchboard.s.getMessageGenericError() +"</Response>";
			response.addHeader("Content-Type", "text/xml");
			response.getWriter().print(errorXml);
			return;			
		}

		d.call_uuid = request.getParameter("CallUUID");
		
		
		System.out.println("PlayAudio " + d.currentAudio.getUrl() + " for device " + d.number);
		d.addAudit("Playing audio: " + d.currentAudio.name);		
		
		PlivoResponse resp = new PlivoResponse();
		Play play = new Play(d.currentAudio.getUrl());
		String url = request.getRequestURL().toString();		
		Redirect playComplete = new Redirect(url + "/PlayComplete");
		playComplete.setMethod("GET");
		// We can optionally record after an audio is played, so see if we're marked as something that should be recorded
		String recordLength = Goals.checkGoal("record", d.currentAudio, d);
		Record rec = null;
		if (null != recordLength)
		{
			rec = new Record();
			rec.setAction(Settings.s.callbackUrl + "GetRecording/" + d.number);
			int recSecs = 60; // default
			try{
				recSecs = Integer.getInteger(recordLength);
			}
			catch (Exception e){}	// Ignore and carry on - use the default
			rec.setMaxLength(recSecs);
			rec.setFinishOnKey("*");
		}
		
		try {
			resp.append(play);
			if (null != rec)
				resp.append(rec);
			resp.append(playComplete);
			System.out.println(resp.toXML());
			response.addHeader("Content-Type", "text/xml");
			response.getWriter().print(resp.toXML());
		} catch (PlivoException e) {
			ClientWebsocketServlet.sessionHandler.BroadcastError(e.getLocalizedMessage());
		}
		Goals.checkGoal("audiostart", d.currentAudio, d);		
	}
}