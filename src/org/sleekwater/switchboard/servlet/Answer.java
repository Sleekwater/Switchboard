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
import org.sleekwater.switchboard.IvrStep;
import org.sleekwater.switchboard.IvrSteps;
import org.sleekwater.switchboard.Settings;
import org.sleekwater.switchboard.Switchboard;

import com.plivo.helper.exception.PlivoException;
import com.plivo.helper.xml.elements.PlivoResponse;

/**
 * Servlet implementation class Answer
 * This is the initial URL that Plivo will call for any inbound call
 */
@WebServlet(description = "Servlet handler for Plivo Answer callbacks", urlPatterns = { "/Answer" }, loadOnStartup=1)
public class Answer extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Answer() {
        super();
        // Nothing to do here
    }

	/**
	 * We will get posted something like this:
	 * ?Digits=12345
	 * &Direction=inbound
	 * &From=447866555273
	 * &CallerName=447866555273
	 * &BillRate=0.00500
	 * &To=441172050456
	 * &CallUUID=2ff4863a-f899-44c3-b576-76deae37b0c3
	 * &CallStatus=in-progress
	 * &Event=Redirect
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		System.out.println("Answer/POST");
		Map<String, String[]> map = request.getParameterMap();
		for (Object key: map.keySet())
	    {
	            String keyStr = (String)key;
	            String[] value = (String[])map.get(keyStr);
	            System.out.println("Key=" + (String)key + "   :   " + Arrays.toString(value));
	    };
		
		
		String from = request.getParameter("From");
		String CallUUID = request.getParameter("CallUUID");
		String xml = "<Response>" + Switchboard.s.getMessageGenericError() + "</Response>";
		String url = request.getRequestURL().toString();
		
		// Are we skipping the manual registration message step?
		if (Switchboard.s.isIVR && Switchboard.s.skipRegistration && Switchboard.s.isAutoregister)
		{
			if (!Devices.d.exists(from))
			{
				System.out.println("Registering silently");			
				// Add it in
				Devices.d.add(from, "phone");
			}
		}
		
		// Are we registering?
		if (!Devices.d.exists(from))
		{
			System.out.println("Not a known number - registering");			
			if (!Switchboard.s.isAutoregister)
			{
				xml = "<Response>"+
						Switchboard.s.getMessageCannotRegister() + 
						"</Response>";
			}
			else
			{
				String plivoXML = Goals.checkGoal("registeraudiostart", null, null);
				if (null == plivoXML)
					plivoXML = Switchboard.s.getMessageWelcome();
				// Return the registration XML, and point Plivo to the next servlet
				
				// Do the "thank you for registering" message
				url +="/Register";
				xml = "<Response>"
						+ "<GetDigits action=\"" + url + "\" method=\"POST\" numDigits=\"1\" retries=\"1\" timeout=\"10\">"
						+ plivoXML
						+ "</GetDigits></Response>";
			}
		}
		else
		{
			// This is a known (registered) device
			// Are we running an IVR menu?
			if (Switchboard.s.isIVR)
			{
				// We are. Since this is the start of a new call, we can do one of two things 
				// - start an IVR sequence (at the start step) or resume from where we left off
				System.out.println("Known number - (re)start the IVR menu");
				// Tell the console that this device is in the IVR menu
				Devices.d.ivr(from, CallUUID);
				// Get the current step in the IVR from the device
				Device d = Devices.d.get(from);
				IvrStep currentStep = IvrSteps.i.getStep(d);
				// OK, work out which of the two steps relates to our case
				if (null == currentStep || currentStep.endsCall() || "start".equalsIgnoreCase(currentStep.name))
				{
					// We've either finished (and are restarting) or have never started the IVR menu. So, start at the beginning
					currentStep = IvrSteps.i.getStep("start");
					if (null != d)
						d.updateIvrProgress(currentStep);					
				}
				else
				{
					// We're half-way through an IVR menu, so play the resume option which allows the user to decide to restart or resume
					currentStep = IvrSteps.i.getStep("resume");		
					if (null != d)
						d.addAudit("Resuming interrupted call...");
				}
				// And point plivo to what I want to play
				PlivoResponse resp = new PlivoResponse();
				try {
					currentStep.buildPlivoIvrResponse(resp, d, 0);
				}
				catch (PlivoException e) {					
					e.printStackTrace();
				}
				xml = resp.toXML();				
			}
			else
			{
				// Normal switchboard operation - tell the console that someone is phoning in
				System.out.println("Known number - broadcast a call");
				// Tell the console that this device is calling
				Devices.d.ring(from, CallUUID);
	
				String audioPath = Settings.s.callbackUrl + "resources/Ringing_Phone.mp3"; 	
				// And point plivo to the ringing callback - I have the getdigits on, as I'm listening for a 9 to unregister
				// Note loop=0 to repeat indefinitely
				url +="/Ring";
				xml = "<Response>"
						+ "<GetDigits action=\"" + url + "\" method=\"POST\" numDigits=\"1\" retries=\"1\" timeout=\"30\">"
						+ "<Play loop=\"0\">" + audioPath +"</Play>"
						+ "</GetDigits></Response>";
			}
		}
		System.out.println("Plivo XML (answer) is : " + xml);
		
		response.getWriter().write(xml);
		response.addHeader("content-type", "application/xml");
		
		
	}

}
