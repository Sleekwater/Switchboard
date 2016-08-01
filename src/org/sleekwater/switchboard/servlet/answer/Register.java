package org.sleekwater.switchboard.servlet.answer;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sleekwater.switchboard.Device;
import org.sleekwater.switchboard.Devices;
import org.sleekwater.switchboard.Goals;

/**
 * Servlet implementation class Register
 * Called when the user has completed registration in Plivo
 */
@WebServlet("/Answer/Register")
public class Register extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Register() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String from = request.getParameter("From");
		String xml = "<Response><Speak voice=\"WOMAN\">Sorry, there has been an error.</Speak></Response>";
		String url = request.getRequestURL().toString() + "/Unregister";
		String digits = request.getParameter("Digits");
		
		// Are we registering - we should be...
		if (!Devices.d.exists(from))
		{
			if (digits.contains("1"))
			{
				Devices.d.add(from, "phone");
				Device d = Devices.d.get(from);
				String plivoXML = Goals.checkGoal("registeraudiofinish", null, d);
				if (null == plivoXML)
					plivoXML = "<Speak voice=\"WOMAN\">Welcome, and thank you for registering. Press 9 to cancel at any time. "
						+ "You will be unregistered automatically at the end of the performance.</Speak>";
				
				// Tell plivo that we're done
				xml = "<Response>"
						+ plivoXML
						+ "</Response>";
			}
			else
			{
				xml = "<Response>"
						+ "<Speak voice=\"WOMAN\">Sorry, you must press 1 on your keypad to register for the performance. Please try again."
						+ "</Speak>"
						+ "</Response>";
			}
			
		}

		
		response.getWriter().write(xml);
		response.addHeader("content-type", "application/xml");

	}

}
