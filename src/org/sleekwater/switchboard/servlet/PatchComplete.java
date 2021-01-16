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
import org.sleekwater.switchboard.DeviceState;
import org.sleekwater.switchboard.Devices;

/*
 * See https://www.plivo.com/docs/xml/dial/#callbackurl-request-parameters
 * 
 * 
 * PatchComplete/GET
KeyDialBLegUUID   :   [0df857fe-2ffb-4f8f-818f-c2161eaf4933]
KeyDialBLegBillRate   :   [0.01781]
KeyDialAction   :   [answer]
KeyEvent   :   [DialAnswer]
KeyDialBLegFrom   :   [+447866555273]
KeyDialALegUUID   :   [a39290d0-eacb-4f45-87f4-93256d1ac6f0]
KeyDialBLegPosition   :   [1]
KeyCallUUID   :   [a39290d0-eacb-4f45-87f4-93256d1ac6f0]
KeyDialBLegStatus   :   [answer]
KeyDialBLegTo   :   [447496026874]
 */
@WebServlet(description = "Servlet handler for when a Plivo patch (i.e. dial) completes, either successfully or unsuccessfully", urlPatterns = { "/Patch/PatchComplete" }, loadOnStartup=1)
public class PatchComplete extends HttpServlet {
	private static final long serialVersionUID = 1L;
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		System.out.println("PatchComplete/GET");
		Map<String, String[]> map = request.getParameterMap();
		for (Object key: map.keySet())
		{
			String keyStr = (String)key;
			String[] value = (String[])map.get(keyStr);
			System.out.println("Key=" + (String)key + "   :   " + Arrays.toString(value));
		};

		String from = request.getParameter("DialBLegFrom");
		String to = request.getParameter("DialBLegTo");		
		String dialAction = request.getParameter("DialAction");
		Device dFrom = Devices.d.get(from);
		Device dTo = Devices.d.get(to);
		// OK, we should now know what leg B (i.e. the target device) is doing, based on DialAction
		if (null != dFrom)
		{
			if (dialAction.equals("hangup"))
			{
				dFrom.state = DeviceState.IDLE;
				dFrom.addAudit("Patch ended");
				dFrom.broadcastChange("hangup");
			}
			if (dialAction.equals("answer"))
			{
				dFrom.state = DeviceState.BUSY;
				if (dTo != null)
					dFrom.addAudit("Patch started to: " + dTo.name);				
				//d.call_uuid = request.getParameter("DialBLegUUID"); // Should already have this...
				dFrom.broadcastChange("busy");
			}
		}

		if (null != dTo)
		{
			if (dialAction.equals("hangup"))
			{
				dTo.state = DeviceState.IDLE;
				dTo.addAudit("Patch ended");
				dTo.broadcastChange("hangup");
			}
			if (dialAction.equals("answer"))
			{
				dTo.state = DeviceState.BUSY;
				if (dFrom != null)
					dTo.addAudit("Patch started from: " + dFrom.name);				
				dTo.call_uuid = request.getParameter("DialBLegUUID"); // In case we want to do something about it...
				dTo.broadcastChange("busy");
			}
		}

	}
}