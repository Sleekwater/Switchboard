package org.sleekwater.switchboard.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sleekwater.switchboard.Devices;

/**
 * Servlet implementation class Answer
 */
@WebServlet(description = "Servlet handler for Plivo Sms status callbacks", urlPatterns = { "/Smsstatus" }, loadOnStartup=1)
public class Smsstatus extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Smsstatus() {
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
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		
		String from = request.getParameter("From");
		//String CallUUID = request.getParameter("CallUUID");
		
		if (Devices.d.exists(from))
		{
			// Log on the device that this SMS has been delivered - TODO
		}
		
	}

}
