package org.sleekwater.switchboard.servlet;

import java.io.IOException;

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

@WebServlet(description = "Servlet handler for Plivo outbound calls", urlPatterns = { "/PlayAudio/PlayComplete" }, loadOnStartup=1)
public class PlayComplete extends HttpServlet {
	private static final long serialVersionUID = 1L;
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		
		String to = request.getParameter("To");
		System.out.println("PlayComplete " + to );
		
		Device d = Devices.d.get(to);
		if (null != d)
			d.completeAudioPlay();
		
		// Plivo is expecting some XML, so return it here
		String emptyXML = "<Response></Response>";
		response.addHeader("Content-Type", "text/xml");
		response.getWriter().print(emptyXML);
		return;	
		
	}
	
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {

		String to = request.getParameter("To");
		System.out.println("PlayComplete doPost" + to );
		
		Device d = Devices.d.get(to);
		if (null != d)
			d.completeAudioPlay();

		// Plivo is expecting some XML, so return it here
		String emptyXML = "<Response></Response>";
		resp.addHeader("Content-Type", "text/xml");
		resp.getWriter().print(emptyXML);
		
		super.doPost(request, resp);
	}
}