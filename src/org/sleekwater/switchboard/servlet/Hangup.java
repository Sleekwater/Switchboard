package org.sleekwater.switchboard.servlet;

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
 * Servlet implementation class Hangup
 * 
 * Hangup/POST
KeyTotalCost   :   [0.00500]
KeyDirection   :   [inbound]
KeyBillDuration   :   [60]
KeyFrom   :   [447866555273]
KeyCallerName   :   [447866555273]
KeyHangupCause   :   [NORMAL_CLEARING]
KeyBillRate   :   [0.00500]
KeyTo   :   [447441908765]
KeyAnswerTime   :   [2016-04-24 21:47:13]
KeyStartTime   :   [2016-04-24 21:47:12]
KeyDuration   :   [28]
KeyCallUUID   :   [fb07c1aa-e64a-4d30-ba6a-63707c3bceb2]
KeyEndTime   :   [2016-04-24 21:47:40]
KeyCallStatus   :   [completed]
KeyEvent   :   [Hangup]

 */
@WebServlet(description = "Handle Plivo hangup callbacks", urlPatterns = { "/Hangup" }, loadOnStartup=1)
public class Hangup extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Hangup() {
        super();
        // TODO Auto-generated constructor stub
    }

	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	
		System.out.println("Hangup/POST");
		Map map = request.getParameterMap();
		for (Object key: map.keySet())
	    {
	            String keyStr = (String)key;
	            String[] value = (String[])map.get(keyStr);
	            System.out.println("Key=" + (String)key + "   :   " + Arrays.toString(value));
	    };
		
	    String callStatus = "";
	    String hangupCause = "";
	    try
	    {	
			Arrays.toString((String[])map.get("HangupCause"));
		    Arrays.toString((String[])map.get("CallStatus"));
	    }
	    catch (Exception e){} // Ignore and carry on
	    
		String from = request.getParameter("From");		
		// Tell the console that this device has hung up
		Devices.hangup(from, callStatus, hangupCause);
		// I think this can go in either direction...
		String to = request.getParameter("To");		
		Devices.hangup(to, callStatus, hangupCause);			
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("Hangup/GET " + request.getParameterMap().toString());
		String from = request.getParameter("From");		
		// Tell the console that this device has hung up
	    String callStatus = "";
	    String hangupCause = "";
	    try
	    {	
	    	Map map = request.getParameterMap();
			Arrays.toString((String[])map.get("HangupCause"));
		    Arrays.toString((String[])map.get("CallStatus"));
	    }
	    catch (Exception e){} // Ignore and carry on
	    Devices.hangup(from, callStatus, hangupCause);
		// I think this can go in either direction...
		String to = request.getParameter("To");		
		Devices.hangup(to, callStatus, hangupCause);			
	}

}
