package org.sleekwater.switchboard;

import java.util.LinkedHashMap;

import org.sleekwater.switchboard.websocket.ClientWebsocketServlet;

import com.plivo.helper.api.client.RestAPI;
import com.plivo.helper.api.response.response.GenericResponse;


/**
 * Create an instance of this class to interrupt the current call of the given device (if it's still going on)<br/>
 * Useful if you want to cut into a prerecorded message halfway and jump to something else
 * @author KevinScott
 *
 */
public class CallInterruptTimer implements Runnable {

	private Device d;
	private String timeInSeconds = "5";
	public Boolean cancelInterrupt = false;
	
	public CallInterruptTimer(Device d, String timeInSeconds)
	{
		this.d=d;
		this.timeInSeconds = timeInSeconds;
	}

	@Override
	public void run() {

		System.out.println("Interrupt timer thread (" + timeInSeconds + " seconds) starts for device " + d.name);

		// How long does this timer wait for? I can do this safely because we're in a worker thread		
		try {
			Thread.sleep(Long.parseLong(timeInSeconds) * 1000);
		} catch (InterruptedException e1) {
		}
		
		if (cancelInterrupt)
		{
			// interrupt has been cancelled, do nothing
			return;
		}

		d.addAudit("Timer is interrupting the current step (" + d.progress + ")");
		//https://api.plivo.com/v1/Account/{auth_id}/Call/{call_uuid}/

		// Interrupt this call, and push it to this new URL. 
		// At the moment, this is always the IVR system and this should process the next step to the timeout one
		RestAPI api = new RestAPI(Settings.s.plivo_auth_id, Settings.s.plivo_auth_token, "v1");
		if (d.state != DeviceState.IDLE){
			// Call is still live, so interrupt it and ask it to reprocess the step
			//  As we've timed out, the "next step" should be the timeout step...
			LinkedHashMap<String, String> parameters = new LinkedHashMap<String, String>();
			// aleg_url is the URL invoked by Plivo when the call is transferred
			// and contains instructions telling Plivo what to do with the call
			parameters.put("aleg_url",Settings.s.callbackUrl + "Answer/Ivr");
			parameters.put("aleg_method","POST"); // method to invoke the aleg_url
			parameters.put("call_uuid", d.call_uuid);
			parameters.put("hangup_url",Settings.s.callbackUrl + "Hangup"); // (Probably) notified when the called device hangs up - default POST
			try {
				// Transfer this call and print the response
				System.out.println("Transferring call: " + parameters);	// Normally "transfer executed"								
				GenericResponse resp = api.transferCall(parameters);
				System.out.println(resp.message);	// Normally "transfer executed"				
			} catch (Exception e) {
				ClientWebsocketServlet.sessionHandler.BroadcastError(e.getLocalizedMessage());
			}		
		}
		else
		{
			System.out.println("Timer for device has expired, but device state is " + d.state);	
		}
		
		System.out.println("Interrupt timer thread ends for device " + d.name);

	} // End the worker thread


}
