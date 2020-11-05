package org.sleekwater.switchboard;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
	 * - so we know what files are already present when the server restarts
	 */
	public void init()
	{
    	System.out.println("Init audio upload directory as " + Settings.s.uploadDiskPath);
		File uploadDiskPath = new File(Settings.s.uploadDiskPath);
		if (!uploadDiskPath.exists()) {
			uploadDiskPath.mkdirs();
		}
		parseFolderR(uploadDiskPath, "");
	}

	private void parseFolderR(File folderToParse, String folderName)
	{
		File[] listOfFiles = folderToParse.listFiles();

		if (null != listOfFiles)
		{
			for (int i = 0; i < listOfFiles.length; i++) {
				if (listOfFiles[i].isFile() && (listOfFiles[i].getName().endsWith("mp3"))) {
					this.add(listOfFiles[i].getPath(), listOfFiles[i].getName(), folderName, false);
				}
				else if (!listOfFiles[i].isFile())
				{
					// Create an entry for this folder itself
					this.add(listOfFiles[i].getPath(), listOfFiles[i].getName(), folderName, true);
					// Recurse into this folder
					parseFolderR(listOfFiles[i], listOfFiles[i].getName());
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
	
	public Boolean add(String path, String name, String folder, Boolean isFolder)
	{
		Audio a = new Audio();
		a.state = AudioState.IDLE;
		a.name = name;	// The name of the file (or the folder if this is a folder)
		a.path = path;	// The full path to the file
		a.folder = folder;	// If this is a file then this is the name of the folder it lives in - otherwise blank
		a.isFolder = isFolder;
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
				// TODO - cope with deleting folders with contents...
				File f = new File(a.path);
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
	
	/**
	 * Pick one of the audios within this folder, randomly - preferring ones that have not been heard by this device before
	 * @param folder
	 * @param d
	 * @return
	 */
	public Audio getRandomChild(Audio folder, Device d)
	{
		System.out.println("Getting random child of : " + folder + " for device " + d);

		List<Audio> children = new ArrayList<Audio>();
		synchronized(audios)
		{
			for (Audio a : audios.values())
			{
				if (a.folder.equals(folder.name)) {
					children.add(a);
					System.out.println("Child is: " + a.name);
				}
			}
		}
		
		// OK, we have all the possibles. Eliminate the ones that have already been played to this device
		List<Audio> unplayed = new ArrayList<Audio>();
		for (Audio a : children)
		{
			if (!d.playedAudios.contains(a)) {
				unplayed.add(a);
				System.out.println("Unplayed is: " + a.name);
			}
		}
		
		// Have we got any eligable results?
		Audio r = null;
		if (unplayed.size() > 0)
		{
			// Pick a random one
			int randomNum = ThreadLocalRandom.current().nextInt(0, unplayed.size());
			 r = unplayed.get(randomNum);
		}
		else
		{
			// Pick one we've heard before
			int randomNum = ThreadLocalRandom.current().nextInt(0, children.size());
			r = children.get(randomNum);
		}
		System.out.println("Returning audio: " + r);
		return r;
	}
}
