package org.sleekwater.switchboard;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.sleekwater.switchboard.websocket.ClientWebsocketServlet;

/**
 * Keep an in-memory list of registered audio recordings<br/>
 * NOTE that this is reentrant, hence synchronized for all access to the internal list
 * @author sleekwater
 *
 */
public final class Audios {
	
	public static Audios a = new Audios();
	private static HashMap<String, Audio> audios = new HashMap<String, Audio>();

	/**
	 * Init reads any existing files from disk - called from the FileUpload servlet
	 */
	public void init()
	{
    	System.out.println("Init audio upload directory as " + Settings.s.uploadDiskPath);
		File folder = new File(Settings.s.uploadDiskPath);
		if (!folder.exists()) {
			folder.mkdirs();
		}
		File[] listOfFiles = folder.listFiles();

		if (null != listOfFiles)
		{
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile() && (listOfFiles[i].getName().endsWith("mp3"))) {
					this.add(listOfFiles[i].getPath(), listOfFiles[i].getName());
				} 
			}
		}
	}
	
	public List<String> getall()
	{
		List<String> messages = new ArrayList<String>();
		synchronized(audios)
		{
			for (Audio a : audios.values())
			{
				messages.add(a.toString());
			}
		}
		return messages;
	}
	
	// Note that this is no longer syncronized...
	public Audio get(String name)
	{
		return audios.get(name);
	}
	
	public Boolean exists(String path)
	{
		synchronized(audios)
		{
			return audios.containsKey(path);
		}
	}
	
	public Boolean add(String path, String name)
	{
		Audio a = new Audio();
		a.state = AudioState.IDLE;
		a.name = name;
		a.path = path;
		synchronized(audios)
		{
			if (!audios.containsKey(name))
			{
				audios.put(name, a);
				a.broadcastChange("add");
				return true;
			}
		}
		return false;
	}	
	

	public Boolean remove(String name) {
		synchronized(audios)
		{
			if (audios.containsKey(name))
			{
				Audio a = audios.get(name);
				File f = new File(Settings.s.uploadDiskPath + "/" + name);
				if (null != f)
					f.delete();
				a.state = AudioState.REMOVED;
				a.broadcastChange("remove");
				audios.remove(name);
				return true;
			}
		}
		return false;
	}
	
	public void removeAll() {
		synchronized(audios)
		{
			for (Audio a : audios.values())
			{
				a.state = AudioState.REMOVED;
				a.broadcastChange("remove");
			}			
			audios.clear();
		}
	}	
}
