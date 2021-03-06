package org.sleekwater.switchboard.servlet.answer;

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
import org.sleekwater.switchboard.IvrStep;
import org.sleekwater.switchboard.IvrSteps;
import org.sleekwater.switchboard.Switchboard;

import com.plivo.helper.xml.elements.PlivoResponse;

/**
 * We start off the Ivr system in the /Answer servlet, but handle all subsequent callbacks in this servlet
 */
@WebServlet("/Answer/Ivr")
public class Ivr extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Ivr() {
        super();
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		System.out.println("Answer/Ivr/POST");
		Map<String, String[]> map = request.getParameterMap();
		for (Object key: map.keySet())
	    {
	            String keyStr = (String)key;
	            String[] value = (String[])map.get(keyStr);
	            System.out.println("Key=" + (String)key + "   :   " + Arrays.toString(value));
	    };
		
		String xml = "<Response>"
				+ Switchboard.s.getMessageGenericError()
				+ "</Response>";
		String from = request.getParameter("From");
		String digits = request.getParameter("Digits");
		String url = request.getRequestURL().toString(); // We carry on with the /answer/ivr servlet

		try
		{
			// OK, we've had the audio message played for this step of the IVR. We've (maybe) had a key pressed - find out what was pressed
			Device d = Devices.d.get(from);
			System.out.println("Ivr - do the next step...");
			IvrStep currentStep = IvrSteps.i.getStep(d);
			System.out.println("currentStep is " + currentStep);
			IvrStep nextStep = null;
			if (null != currentStep)
			{
				nextStep = currentStep.getNextStep(digits, d);
				System.out.println("nextStep is " + nextStep);
			}
			
			if (null != nextStep)
			{
				// Ask what to play next
				PlivoResponse resp = new PlivoResponse();
				nextStep = nextStep.buildPlivoIvrResponse(resp, d, 0);
				xml = resp.toXML();
				// Remember where we are, so that the next callback will go to the right place in the menu system
				if (null != d)
				{
					d.updateIvrProgress(nextStep);					
				}
			}
			else
			{
				PlivoResponse resp = new PlivoResponse();
				currentStep.buildPlivoIvrResponse(resp, d, 0);
				// Replay the last message, which should always be the resume step message here...
				xml = "<Response>"
						+ "<GetDigits action=\"" + url + "\" method=\"POST\" numDigits=\"1\" retries=\"1\" timeout=\"30\">"
						+ Switchboard.s.getMessageInvalidKey()
						+ (d.currentAudio == null ? "" : "<Play>" + d.currentAudio.getUrl() + "</Play>")
						+ "</GetDigits></Response>";
				
				// Make sure we don't loop forever
				if (null != d)
				{
					d.loopCount--;
					if (d.loopCount <=0)
					{
						xml = "<Response>"							
								+ Switchboard.s.getMessageGoodbye()
								+ "</Response>";
					}
				}
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to parse ivrstep " + e);
		}
		
		System.out.println("Plivo XML (answer/ivr) is : " + xml);
		
		response.getWriter().write(xml);
		response.addHeader("content-type", "application/xml");
		
	}

}
