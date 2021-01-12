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
import org.sleekwater.switchboard.IvrStep;
import org.sleekwater.switchboard.IvrSteps;
import org.sleekwater.switchboard.Settings;
import org.sleekwater.switchboard.Switchboard;

import com.plivo.helper.exception.PlivoException;
import com.plivo.helper.xml.elements.Play;
import com.plivo.helper.xml.elements.PlivoResponse;
import com.plivo.helper.xml.elements.Redirect;

@WebServlet(description = "Servlet handler that Plivo calls when a recording is finished", urlPatterns = { "/GetRecording/*" }, loadOnStartup=1)
public class GetRecording extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		System.out.println("GetRecording/POST");
		Map map = request.getParameterMap();
		for (Object key: map.keySet())
	    {
	            String keyStr = (String)key;
	            String[] value = (String[])map.get(keyStr);
	            System.out.println("Key=" + (String)key + "   :   " + Arrays.toString(value));
	    };

		
		// Irritatingly, plivo doesn't tell us in the callback what the number that did the recording was.
		// So I have to fold it into the callback URL, REST-style		
		String deviceNumber = request.getPathInfo();
		System.out.println("GetRecording doPost" + deviceNumber );
		if (deviceNumber.startsWith("/"))
			deviceNumber = deviceNumber.substring(1, deviceNumber.length());
		
		Device d = Devices.d.get(deviceNumber);
		String xml = "<Response>" +
				Switchboard.s.getMessageGenericError()
				+ "</Response>";;
		
		String recordUrl = request.getParameter("RecordUrl");
		String recordDuration = request.getParameter("RecordingDuration");
		Devices.d.recording(deviceNumber, recordUrl, recordDuration);

		// OK, was this recording as part of an IVR menu? Then we'll need to chain to the next step for this device in the IVR menu
		if (map.containsKey("ivr"))
		{
			if (null != d)
			{
				PlivoResponse resp = new PlivoResponse();
				IvrStep thisStep = IvrSteps.i.get(d.progress); // Should be a record step, otherwise why are we in the recording servlet?
				IvrStep nextStep = IvrSteps.i.get(thisStep.defaultKey);
				if (null == nextStep)
				{
					System.out.println("defaultKey not mapped? - thisStep is " + thisStep);						
				}
				else
				{
					try {
						nextStep.buildPlivoIvrResponse(resp, d, 0);
						// Remember where we are, so that the next callback will go to the right place in the menu system
						d.updateIvrProgress(nextStep);					
						xml = resp.toXML();
					} catch (Exception e) {	
						System.out.println("Failed while parsing recording for IVR: " + deviceNumber);	
						e.printStackTrace();
					}
				}
			}
			else
			{
				System.out.println("Device not found: " + deviceNumber);						
			}
			System.out.println("Plivo XML (record) is : " + xml);
			response.getWriter().write(xml);
			response.addHeader("content-type", "application/xml");
		}		
	}
}