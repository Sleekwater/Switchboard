package org.sleekwater.switchboard.servlet;

import javax.servlet.annotation.WebServlet;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sleekwater.switchboard.AuditEntry;
import org.sleekwater.switchboard.Devices;
import org.sleekwater.switchboard.websocket.ClientWebsocketServlet;
/**
 * Servlet implementation class FullAudit
 */
@WebServlet(description = "Servlet handler downloading all device audit trails in one big, ordered file", urlPatterns = { "/FullAudit" }, loadOnStartup=1)

public class FullAudit extends HttpServlet {
	private static final long serialVersionUID = 1L;
	static final int BUFFER = 1024;
	List<File> fileList = new ArrayList<File>();
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

	/** Check the auth and then download a csv with the generated file in it*/

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// Auth'd? We cookie the page...
		try
		{			
			String auth = getCookie(req, "auth").getValue();
			if (!ClientWebsocketServlet.sessionHandler.validAccounts.contains(auth))
			{
				resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You must be a known user to use this servlet");
				return;
			}
		}	
		catch (Exception e)
		{
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.toString());
			return;
		}
		
		Boolean groupByDevice = true;
		if (null != req.getParameter("group"))
		{
			groupByDevice = "true".equalsIgnoreCase(req.getParameter("group"));
		}

		// get the full audit, already sorted
		List<AuditEntry> fullAudit = Devices.d.getFullAudit(groupByDevice);
		
		// And send the data to the caller
		resp.setContentType("application/csv");
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());        
		resp.setHeader("Content-Disposition", "attachment; filename=" + "audit_" + sdf.format(timestamp) + ".csv");

		ServletOutputStream sos = resp.getOutputStream();
		sos.println("Timestamp,Device name,Message");
		for (AuditEntry a : fullAudit)
		{
			sos.println(a.toString());
		}
		sos.flush();
	}


	public static Cookie getCookie(HttpServletRequest request, String name) {
	    Cookie[] cookies = request.getCookies();
	    if (cookies != null) {
	        for (Cookie cookie : cookies) {
	            if (cookie.getName().equals(name)) {
	                return cookie;
	            }
	        }
	    }
	    return null;
	}
}
