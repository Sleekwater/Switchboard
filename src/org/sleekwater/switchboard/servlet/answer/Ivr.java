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
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		System.out.println("Answer/Ivr/POST");
		Map map = request.getParameterMap();
		for (Object key: map.keySet())
	    {
	            String keyStr = (String)key;
	            String[] value = (String[])map.get(keyStr);
	            System.out.println("Key=" + (String)key + "   :   " + Arrays.toString(value));
	    };
		
		String xml = "<Response>"
				+ "<Speak voice=\"WOMAN\">I'm sorry, there has been a problem. Please try again."
				+ "</Speak>"
				+ "</Response>";
		String from = request.getParameter("From");
		String digits = request.getParameter("Digits");
		String url = request.getRequestURL().toString(); // We carry on with the /answer/ivr servlet

		try
		{
			// OK, we've had the audio message played for this step of the IVR. We've (hopefully) had a key pressed - find out what was pressed
			Device d = Devices.d.get(from);
			System.out.println("Ivr - do the next step for device " + d);
			IvrStep currentStep = IvrSteps.i.getStep(d);
			System.out.println("currentStep is " + currentStep);
			IvrStep nextStep = null;
			if (null != currentStep)
			{
				nextStep = currentStep.parseDigits(digits);
				System.out.println("nextStep is " + nextStep);
			}
			
			if (null != nextStep)
			{
				xml = "<Response>"
					+ "<GetDigits action=\"" + url + "\" method=\"POST\" numDigits=\"1\" retries=\"1\" timeout=\"30\">"
					+ "<Play loop=\"0\">" + nextStep.getAudioPath(d) +"</Play>"
					+ "</GetDigits></Response>";
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
						+ "<Speak voice=\"WOMAN\">Sorry, " + digits + " is not a valid key. Please try again."
						+ "</Speak>"
						+ "</Response>";
				
			}
			// TODO - put handlers for "common" keys like * or # here

		}
		catch (Exception e)
		{
			System.out.println("Failed to parse ivrstep " + e);
		}
		
		response.getWriter().write(xml);
		response.addHeader("content-type", "application/xml");

	}

}