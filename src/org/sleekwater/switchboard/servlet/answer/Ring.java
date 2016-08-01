package org.sleekwater.switchboard.servlet.answer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sleekwater.switchboard.Devices;

/**
 * Servlet implementation class Register
 * Called when the user has completed registration in Plivo
 */
@WebServlet("/Answer/Ring")
public class Ring extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Ring() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		System.out.println("Answer/Ring/POST");
		Map map = request.getParameterMap();
		for (Object key: map.keySet())
	    {
	            String keyStr = (String)key;
	            String[] value = (String[])map.get(keyStr);
	            System.out.println("Key=" + (String)key + "   :   " + Arrays.toString(value));
	    };
		
		
		String from = request.getParameter("From");
		String digits = request.getParameter("Digits");
		String xml = "<Response><Speak voice=\"WOMAN\">Sorry, there has been an error.</Speak></Response>";
		String url = request.getRequestURL().toString() + "/Unregister";

		// OK, we've had the ringing. We've had a key pressed while ringing.
		if (Devices.d.exists(from))
		{
			if (digits.contains("9"))
			{
				Devices.d.remove(from);
				
				// Tell plivo that we're done
				xml = "<Response>"
						+ "<Speak voice=\"WOMAN\">Thank you. You are now unregistered, and will not take any further part in the performance.</Speak>"
						+ "</Response>";
			}
			else
			{
				// Todo - work out what happens with a timeout...
				xml = "<Response>"
						+ "<GetDigits action=\"" + url + "\" method=\"POST\" numDigits=\"1\" retries=\"1\" timeout=\"10\">"
						+ "<Speak voice=\"WOMAN\">Sorry it's taking a while. Ring ring.</Speak>"
						+ "</GetDigits></Response>";				
			}
			
		}
		
		response.getWriter().write(xml);
		response.addHeader("content-type", "application/xml");

	}

}
