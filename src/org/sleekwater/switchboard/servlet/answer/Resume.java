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
import org.sleekwater.switchboard.Settings;
import org.sleekwater.switchboard.Switchboard;

import com.plivo.helper.xml.elements.PlivoResponse;

/**
 * If we've played the resume message then we will get called back to here - decide if we go to a specific step, or if we resume from the last step<br/>
 * Note that this is a different servlet to /Ivr because I normally keep state in "progress" - but need to maintain that value if we want to resume...
 */
@WebServlet("/Answer/Resume")
public class Resume extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Resume() {
        super();
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		System.out.println("Answer/Resume/POST");
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
		String url = Settings.s.callbackUrl + "Answer/Ivr"; // We switch over to the /answer/ivr servlet after parsing this response

		try
		{
			// OK, we've had the resume message. What do we do next?
			Device d = Devices.d.get(from);
			IvrStep currentStep = IvrSteps.i.getStep("resume");	// Can't use the current step of the device, as that's what we need to resume to	
			IvrStep nextStep = null;
			if (null != currentStep)
			{
				nextStep = currentStep.getNextStep(digits, d);
				System.out.println("nextStep is " + nextStep);
			}
			
			if (null != nextStep)
			{
				// Tell plivo what to play next						
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
				xml = "<Response>"
						+ "<GetDigits action=\"" + url + "\" method=\"POST\" numDigits=\"1\" retries=\"1\" timeout=\"30\">"
						+ Switchboard.s.getMessageInvalidKey() 
						+ (d.currentAudio == null ? "" : "<Play>" + d.currentAudio.getUrl() + "</Play>")
						+ "</GetDigits></Response>";
				
			}
		}
		catch (Exception e)
		{
			System.out.println("Failed to parse ivrstep " + e);
		}
		
		System.out.println("Plivo XML (answer/resume) is : " + xml);		
		response.getWriter().write(xml);
		response.addHeader("content-type", "application/xml");

	}

}
