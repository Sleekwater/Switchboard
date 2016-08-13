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
import org.sleekwater.switchboard.DeviceState;
import org.sleekwater.switchboard.Devices;
import org.sleekwater.switchboard.websocket.ClientWebsocketServlet;

import com.plivo.helper.exception.PlivoException;
import com.plivo.helper.xml.elements.Dial;
import com.plivo.helper.xml.elements.Number;
import com.plivo.helper.xml.elements.Play;
import com.plivo.helper.xml.elements.PlivoResponse;

@WebServlet(description = "Servlet handler for Plivo patch callbacks", urlPatterns = { "/Patch" }, loadOnStartup=1)
public class Patch extends HttpServlet {
	private static final long serialVersionUID = 1L;
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO - need to work out what's passed here for a call transfer...
		
		System.out.println("Patch/GET");
		Map map = request.getParameterMap();
		for (Object key: map.keySet())
	    {
	            String keyStr = (String)key;
	            String[] value = (String[])map.get(keyStr);
	            System.out.println("Key=" + (String)key + "   :   " + Arrays.toString(value));
	    };

		String from = request.getParameter("From");
		Device d = Devices.d.get(from);
		if (null != d)
			d.call_uuid = request.getParameter("CallUUID");

		Device t = d.currentTransfer;
		
		System.out.println("Patch device " + d.number + " to " + t.number);
		
		PlivoResponse resp = new PlivoResponse();
		Dial dial = new Dial();
		//dial.getAttributes().put("dialMusic", "real");	// This doesn't seem to do anything...		
		dial.setDialMusic("real");	// .. so trying this...
		// TODO - consider callbackUrl so that we get notified when the transfer is complete, ends, etc...
		try {
			dial.append(new Number(t.number));
			dial.setCallbackUrl(request.getRequestURL().toString()+ "/PatchComplete");
			dial.setCallbackMethod("GET");

			resp.append(dial);
			System.out.println(resp.toXML());
			response.addHeader("Content-Type", "text/xml");
			response.getWriter().print(resp.toXML());
			// Make sure that this device isn't used while it's being called
			t.state = DeviceState.CALL;
            t.broadcastChange("call");
            
		} catch (Exception e) {
			ClientWebsocketServlet.sessionHandler.BroadcastError(e.getLocalizedMessage());
		}
	}
}