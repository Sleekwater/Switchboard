package org.sleekwater.switchboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import org.sleekwater.switchboard.websocket.ClientWebsocketServlet;

/**
 * Keep an in-memory list of registered goals<br/>
 * NOTE that this is reentrant, hence synchronized for all access to the internal list
 * @author sleekwater
 *
 */
public final class Goals {

	public static Goals g = new Goals();
	private static HashMap<String, Goal> goals = new HashMap<String, Goal>();

	/**
	 * Init reads any existing files from disk - called from the FileUpload servlet
	 */
	public void init()
	{
		System.out.println("Init goal upload directory as " + Settings.s.uploadDiskPath);
		File folder = new File(Settings.s.uploadDiskPath);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		File goals = new File(Settings.s.uploadDiskPath + "/goals.json");

		if (null != goals)
		{
			// Read the file as the body of this text.
			FileInputStream fis;
			try {
				fis = new FileInputStream(goals);

				BufferedReader br = new BufferedReader(new InputStreamReader(fis));

				String line = null;
				while ((line = br.readLine()) != null) {
					JsonReader jsonReader = Json.createReader(new StringReader(line));
					JsonObject object = jsonReader.readObject();
					jsonReader.close();
					add(object);
				}				 
				br.close();					
			} catch (IOException e) {			}
		}		
	}

	public List<String> getall()
	{
		List<String> messages = new ArrayList<String>();
		synchronized(goals)
		{
			for (Goal g : goals.values())
			{
				messages.add("{\"goal\":"+g.toString()+"}");
			}
		}
		return messages;
	}

	// Note that this is no longer syncronized...
	public Goal get(String name)
	{
		return goals.get(name);
	}

	public Boolean exists(String path)
	{
		synchronized(goals)
		{
			return goals.containsKey(path);
		}
	}

	/**
	 * As a goal is a complex object, pass json in here
	 * @param json
	 * @return
	 */
	public static Boolean add(JsonObject json)
	{		
		Goal g = new Goal();
		g.goaljson = json;

		String name = null;
		try{
			name = json.get("name").toString();
		}
		catch (Exception e){}	// Ignore and carry on - name is optional for new items

		synchronized(goals)
		{
			if (null == name)
			{
				// New items don't have a name - so create a new one
				int maxGoal = 0;
				for (String key : goals.keySet())
				{
					try{
						maxGoal = Math.max(maxGoal, Integer.parseInt(key));
					}
					catch (Exception e){}	// Ignore and carry on				
				}
				// Find the max name integer and add one to it...
				int key = maxGoal + 1;
				
				
				JsonObjectBuilder job = jsonObjectToBuilder(json);
				job.add("name", key);
				g.goaljson = job.build();
			}

			// If the name already exists then we're saving over the existing object
			if (goals.containsKey(name))
			{
				goals.remove(name);
			}
			goals.put(name, g);
			g.broadcastChange("add");
			saveToDisk();
		}
		return false;
	}	

	
	private static JsonObjectBuilder jsonObjectToBuilder(JsonObject jo) {
	    JsonObjectBuilder job = Json.createObjectBuilder();

	    for (Entry<String, JsonValue> entry : jo.entrySet()) {
	        job.add(entry.getKey(), entry.getValue());
	    }

	    return job;
	}

	public static Boolean remove(String name) {
		synchronized(goals)
		{
			if (goals.containsKey(name))
			{
				Goal g = goals.get(name);
				g.state = GoalState.REMOVED;
				g.broadcastRemove(name);
				goals.remove(name);
				saveToDisk();
				return true;
			}
		}
		return false;
	}

	private static void saveToDisk()
	{
		// Persist all to disk
		PrintWriter out;
		try {
			out = new PrintWriter(Settings.s.uploadDiskPath + "/goals.json");
			for (Goal goal : goals.values())
			{
				out.println(goal.toString());				
			}
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void removeAll() {
		synchronized(goals)
		{
			for (String name : goals.keySet())
			{
				Goal g = goals.get(name);
				g.state = GoalState.REMOVED;
				g.broadcastRemove(name);
			}			
			goals.clear();
		}
	}

	/**
	 * Walk the list of goals and see if any fire
	 * @param type
	 */
	public static String checkGoal(String eventtype, Object eventparam, Device d)
	{
		System.out.println("Checking goal of " + eventtype);
		try{
			synchronized(goals)
			{
				for (Goal g : goals.values())
				{
					String result = g.check(eventtype, eventparam, d);
					if (null != result)
						return result;
				}			
			}
		}
		catch (Exception e){}
		return null;
	}

}
