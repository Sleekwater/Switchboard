package org.sleekwater.switchboard.websocket;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.json.JsonObject;
import javax.websocket.Session;

import org.sleekwater.switchboard.Audios;
import org.sleekwater.switchboard.Devices;
import org.sleekwater.switchboard.Goals;
import org.sleekwater.switchboard.IvrSteps;
import org.sleekwater.switchboard.Switchboard;
import org.sleekwater.switchboard.Texts;

/**
 * Manager class to keep hold of the various websocket sessions that I might have connected
 * This will have one entry for each browser connected, so should have at least 2 at all times.
 * I use this to broadcast changes to all browser webapps
 * @author sleekwater
 *
 */
public class SocketSessionHandler {

	// The list of actual connections
    private final HashSet<Session> sessions = new HashSet<Session>();
    // And we attach some info to each connection, as consoleInfo
    private final HashMap<Session, ConsoleInfo> consoles = new HashMap<Session, ConsoleInfo>();
    public List<String> validAccounts = new ArrayList<String>();

    public SocketSessionHandler(){
    	validAccounts.add(sha256("yourusernameyourpassword"));   
         
    }
    
    public static String sha256(String base) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer();

            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch(Exception ex){
           throw new RuntimeException(ex);
        }
    }
    
    public void addSession(Session session) {
        sessions.add(session);
        //System.out.println("After add there are now " + sessions.size() + " sessions");
    }
    
    /**
     * Once a console connects it sends some additional information - which we stash here...
     * @param ci
     * @throws IOException 
     */
    public void addConsoleInfo(Session session, ConsoleInfo ci) throws IOException
    {
    	synchronized (session) {
    		if (consoles.containsKey(session))
    		{
    			consoles.remove(session);
    		}
    		consoles.put(session, ci);    		
    		Broadcast(ci.toJson());
    	}
    	
    	// Now broadcast all current state to this new console...
        List<String> deviceState = Devices.d.getall();
        for (String s : deviceState)
        {
        	session.getBasicRemote().sendText(s);
        }   
        
        // And the same for console sessions
        synchronized (session) {
        	for (ConsoleInfo cInfo : consoles.values())
	    	{
        		Broadcast(cInfo.toJson());
	    	}
        }
        
        // And audios
        List<String> audioState = Audios.a.getall();
        for (String s : audioState)
        {
        	session.getBasicRemote().sendText(s);
        } 
        
        // And texts
        List<String> textState = Texts.t.getall();
        for (String s : textState)
        {
        	session.getBasicRemote().sendText(s);
        }
        // And goals
        List<String> goalState = Goals.g.getall();
        for (String s : goalState)
        {
        	session.getBasicRemote().sendText(s);
        }
        
        // And ivrsteps
        List<String> ivrStep = IvrSteps.i.getall();
        for (String s : ivrStep)
        {
        	session.getBasicRemote().sendText(s);
        }
        
        // And system settings
        session.getBasicRemote().sendText(Switchboard.s.toString());
        
        // The new session should now have an up to date list of all the data that the server has.
        // As states changes then the latest will be sent to each connected console.
    }
    public void removeSession(Session session) {
    	synchronized (session) {
    		sessions.remove(session);
    		
	    	if (consoles.containsKey(session))
			{
	    		ConsoleInfo ci = consoles.get(session);
	    		ci.state = ConsoleState.REMOVED;
	    		Broadcast(ci.toJson());
				consoles.remove(session);
			}
    	}
    	//System.out.println("After remove there are now " + sessions.size() + " sessions");
    }
    
    
    /**
     * Send this message to all consoles who have authorised themselves.
     * @param message
     */
    public void Broadcast(JsonObject message) {
    	System.out.println("Broadcasting" + message.toString() + " to " + sessions.size());
    	    	
    	synchronized (sessions) {			
	    	for (Session s : sessions)
	    	{
	    		// Only broadcast to authorised consoles...
	    		if (consoles.containsKey(s))
	    		{
		    		try {
						s.getBasicRemote().sendText(message.toString());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    		}
	    	}
    	}
    }

    /**
     * If there has been an error, send that to all the consoles
     * @param error
     */
	public void BroadcastError(String error) {
		System.out.println("Broadcasting error: '" + error + "' to " + sessions.size());
    	
    	synchronized (sessions) {			
	    	for (Session s : sessions)
	    	{
	    		// Only broadcast to authorised consoles...
	    		if (consoles.containsKey(s))
	    		{
		    		try {
						s.getBasicRemote().sendText("{\"error\":\""+error+"\"}");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    		}
	    	}
    	}
	}
    
	/**
     * If the server is reloading data from disk, tell all the consoles to clear their in-memory sets
     * - the server should be about to tell them everything it knows again.
     * @param error
     */
	public void BroadcastReset() {
		System.out.println("Broadcasting reset to " + sessions.size());
    	
    	synchronized (sessions) {			
	    	for (Session s : sessions)
	    	{
	    		// Only broadcast to authorised consoles...
	    		if (consoles.containsKey(s))
	    		{
		    		try {
						s.getBasicRemote().sendText("{\"reset\":\"true\"}");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    		}
	    	}
    	}
	}
}