package org.sleekwater.switchboard;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.sleekwater.switchboard.websocket.ClientWebsocketServlet;

/**
 * Keep an in-memory list of registered text messages<br/>
 * NOTE that this is reentrant, hence synchronized for all access to the internal list
 * @author sleekwater
 *
 */
public final class Texts {
	
	public static Texts t = new Texts();
	private static HashMap<String, Text> texts = new HashMap<String, Text>();

	/**
	 * Init reads any existing files from disk - called from the FileUpload servlet
	 * @throws IOException 
	 */
	public void init() throws IOException
	{
    	System.out.println("Init text upload directory as " + Settings.s.uploadDiskPath);
		File folder = new File(Settings.s.uploadDiskPath);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		
		File[] listOfFiles = folder.listFiles();

		if (null != listOfFiles)
		{
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile() && (listOfFiles[i].getName().endsWith("txt"))) {
					try{
						this.add(listOfFiles[i]);
					}
					catch (Exception e)
					{
			    		System.out.println("Cannot read text: " +  listOfFiles[i] + " : " + e);
					}
				} 
			}
		}
		
		// Testing TODO
		//this.add("Welcome", "Welcome to the switchboard");
		//this.add("Thank you for joining");
	}
	
	public static String readStream(InputStream is) {
	    StringBuilder sb = new StringBuilder(512);
	    try {
	        Reader r = new InputStreamReader(is, "UTF-8");
	        int c = 0;
	        while ((c = r.read()) != -1) {
	            sb.append((char) c);
	        }
	    } catch (IOException e) {
	        throw new RuntimeException(e);
	    }
	    return sb.toString();
	}
	
	public List<String> getall()
	{
		List<String> messages = new ArrayList<String>();
		synchronized(texts)
		{
			for (Text t : texts.values())
			{
				messages.add(t.toString());
			}
		}
		return messages;
	}
	
	// Note that this is no longer syncronized...
	public Text get(String name)
	{
		return texts.get(name);
	}
	
	public Boolean exists(String path)
	{
		synchronized(texts)
		{
			return texts.containsKey(path);
		}
	}
	
	/**
	 * Given a file, read the contents of the file (saved as JSON) and stash it into the texts array
	 * @param f
	 * @return
	 */
	public void add(File f)
	{
		

		// Read the file as the body of this text.
		try {
			FileInputStream fis = new FileInputStream(f);
			String json = readStream(fis);
			JsonReader jsonReader = Json.createReader(new StringReader(json));
			JsonObject object = jsonReader.readObject();
			jsonReader.close();
			JsonObject text = object.getJsonObject("text");
			add(new Text(text));
		} catch (IOException e) {}
		
	}

	/**
	 * Add this text to the collection, and save to file
	 * @param label
	 * @param name
	 * @return
	 */
	public Boolean add(String label, String name)
	{
		Text t = new Text();
		t.label = label;
		t.name = name;
		t.saveToFile(t.label);				
		return add(t);
	}
	
	public Boolean add(Text t)
	{
		synchronized(texts)
		{
			if (texts.containsKey(t.label))
			{
				texts.remove(t.label);
			}
			texts.put(t.label, t);
			t.broadcastChange("add");				
			return true;		
		}
	}	


	public Boolean remove(String name) {
		synchronized(texts)
		{
			if (texts.containsKey(name))
			{
				Text t = texts.get(name);
				File f = new File(Settings.s.uploadDiskPath + "/" + t.label + ".txt");
				if (null != f)
					f.delete();
				t.state = TextState.REMOVED;
				t.broadcastChange("remove");
				texts.remove(name);
				return true;
			}
		}	
		return false;
	}
	
	public void removeAll() {
		synchronized(texts)
		{
			for (Text t : texts.values())
			{
				t.state = TextState.REMOVED;
				t.broadcastChange("remove");
			}			
			texts.clear();
		}
	}	
}
