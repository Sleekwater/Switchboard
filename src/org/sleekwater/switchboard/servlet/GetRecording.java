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

import com.plivo.helper.exception.PlivoException;
import com.plivo.helper.xml.elements.Play;
import com.plivo.helper.xml.elements.PlivoResponse;
import com.plivo.helper.xml.elements.Redirect;

@WebServlet(description = "Servlet handler that Plivo calls when a recording is finished", urlPatterns = { "/PlayAudio/GetRecording/*" }, loadOnStartup=1)
public class GetRecording extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {

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
		
		String recordUrl = request.getParameter("RecordUrl");
		String recordDuration = request.getParameter("RecordingDuration");
		Devices.d.recording(deviceNumber, recordUrl, recordDuration);

		super.doPost(request, resp);
	}
}