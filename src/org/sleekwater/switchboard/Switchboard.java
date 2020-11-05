package org.sleekwater.switchboard;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.sleekwater.switchboard.websocket.ClientWebsocketServlet;

/**
 * A singleton to hold settings common across the entire switchboard system
 * @author sleekwater
 *
 */
public final class Switchboard {
	
	public static Switchboard s = new Switchboard();
	
	public boolean isIVR = true;

}
