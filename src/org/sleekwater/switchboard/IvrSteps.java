package org.sleekwater.switchboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

/**
 * Keep an in-memory list of the various IVR steps. This is set up beforehand, and is then referenced whenever someone is in the IVR system
 * @author KevinScott
 *
 */
public class IvrSteps {

	public static IvrSteps i = new IvrSteps();
	private static HashMap<String, IvrStep> ivrs= new HashMap<String, IvrStep>();


	/**
	 * Init reads any existing files from disk - called from the FileUpload servlet 
	 * - so we know what files are already present when the server restarts
	 */
	public void init()
	{
		System.out.println("Init ivr upload directory as " + Settings.s.uploadDiskPath);
		File uploadDiskPath = new File(Settings.s.uploadDiskPath);
		if (!uploadDiskPath.exists()) {
			uploadDiskPath.mkdirs();
		}
		File[] listOfFiles = uploadDiskPath.listFiles();

		if (null != listOfFiles)
		{
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile() && (listOfFiles[i].getName().endsWith("ivr"))) {
					this.add(listOfFiles[i]);
				}
			}
		}
	}

	public List<String> getall()
	{
		List<String> messages = new ArrayList<String>();
		synchronized(ivrs)
		{
			for (IvrStep a : ivrs.values())
			{
				messages.add(a.toString());
			}
		}
		return messages;
	}

	// Note that this is no longer syncronized...
	public IvrStep get(String name)
	{
		return ivrs.get(name);
	}

	public Boolean exists(String path)
	{
		synchronized(ivrs)
		{
			return ivrs.containsKey(path);
		}
	}

	public void add(File file)
	{
		// Read the contents of the file
		StringBuilder sb = new StringBuilder();

        try (BufferedReader br = Files.newBufferedReader(file.toPath())) {

            // read line by line
            /*String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }*/
            
            
            JsonReader jsonReader = Json.createReader(br);
            JsonObject object = jsonReader.readObject();
            jsonReader.close();
    		// We always have a wrapper so we know the object type - this is useful in the UI
    		JsonObject o = object.getJsonObject("ivrstep");
            add(o);
            
        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
        }
        
	}

	/**
	 * Add this new IvrStep from JSON, optionally saving to disk
	 * @param json
	 * @param saveToDisk
	 * @return
	 */
	public IvrStep add(JsonObject json)
	{
		IvrStep a = new IvrStep(json);
		synchronized(ivrs)
		{
			ivrs.put(a.getName(), a);
			a.broadcastChange("add");			
		}
		
		return a;
	}	

	/**
	 * Remove this ivrstep from the system
	 * @param name
	 * @return
	 */
	public Boolean remove(String name) {
		synchronized(ivrs)
		{
			if (ivrs.containsKey(name))
			{
				IvrStep ivrStep = ivrs.get(name);
				File f = new File(Settings.s.uploadDiskPath + "/" + ivrStep.getName() + ".ivr");
				if (null != f)
					f.delete();		
				ivrStep.state = IvrState.REMOVED;
				ivrStep.broadcastChange("remove");
				ivrs.remove(name);
				return true;
			}
		}
		return false;
	}

	public void removeAll() {
		synchronized(ivrs)
		{
			for (IvrStep a : ivrs.values())
			{
				a.broadcastChange("remove");
			}			
			ivrs.clear();
		}
	}	


}
