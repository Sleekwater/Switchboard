package org.sleekwater.switchboard.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.sleekwater.switchboard.Audios;
import org.sleekwater.switchboard.Goals;
import org.sleekwater.switchboard.IvrSteps;
import org.sleekwater.switchboard.Settings;
import org.sleekwater.switchboard.Switchboard;
import org.sleekwater.switchboard.Texts;
import org.sleekwater.switchboard.websocket.ClientWebsocketServlet;

/**
 * Cope with audio files being uploaded to the server - we load on startup to parse all existing files
 */
@WebServlet(value="/FileUpload", loadOnStartup=1)
@MultipartConfig
public class FileUpload extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FileUpload() {
        super();
        
    }
    
    
    public void init(ServletConfig config) throws ServletException
    {
    	
    	System.out.println("Init FileUpload servlet");
    	super.init(config);
    	
    	// Read our web.xml stuff
    	
    	Settings.s.callbackUrl = getServletContext().getInitParameter("callback.url");
    	Settings.s.originalCallbackUrl = getServletContext().getInitParameter("callback.url"); // As the callback can be edited in the UI, keep the original
    	Settings.s.uploadDirectory = getServletContext().getInitParameter("upload.location");
    	// By default, these are all relative to our servlet webapp folder - so I need to start with a "/"
    	Settings.s.uploadDiskPath = getServletContext().getRealPath("/" + Settings.s.uploadDirectory);
    	
    	Settings.s.plivo_auth_id= getServletContext().getInitParameter("plivo.auth_id");
    	Settings.s.plivo_auth_token= getServletContext().getInitParameter("plivo.auth_token");
    	Settings.s.plivo_registerednumber= getServletContext().getInitParameter("plivo.registerednumber");
    	
    	
    	scanUploadDirectory();
    }
    
    /**
     * Find out what resources we have saved on disk, load them into memory and tell the connected consoles about them
     */
    public void scanUploadDirectory()
    {
    	// Tell all the clients to remove their data in case they were previously loaded
    	ClientWebsocketServlet.sessionHandler.BroadcastReset();
    	
    	try{
    		Audios.a.init();
    	} 
    	catch (Exception e){
    		System.out.println(e);
    	}
    	try{
    		Texts.t.init();
    	} 
    	catch (Exception e){
    		System.out.println(e);
    	}
    	try{
    		Goals.g.init();
    	} 
    	catch (Exception e){
    		System.out.println(e);
    	}
    	
    	try{
    		IvrSteps.i.init();
    	} 
    	catch (Exception e){
    		System.out.println(e);
    	}
    	
    	ClientWebsocketServlet.sessionHandler.Broadcast(Switchboard.s.toJsonObject());
    }
    
	/**
	 * Any files being uploaded appear in this servlet<br/>
	 * At time of writing this is either an individual audio file or a zipfile of the entire performance (all files)
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		System.out.println("FileUpload/POST " + request.getParameterMap().toString());
		try
		{			
			String auth = Backup.getCookie(request, "auth").getValue();
			if (!ClientWebsocketServlet.sessionHandler.validAccounts.contains(auth))
			{
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You must be a known user to use this servlet");
				return;
			}
		}	
		catch (Exception e)
		{
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.toString());
			return;
		}
		
        Part filePart = request.getPart("file"); // Retrieves <input type="file" name="file">
        String fileName = filePart.getSubmittedFileName();
        InputStream fileContent = filePart.getInputStream();
        
        if (fileName.endsWith("zip"))
        {
        	// This is a restore. Special behaviour...
        	File uploads = new File(Settings.s.uploadDiskPath);
        	// Delete? This is the default 
        	String reset = request.getParameter("reset");
        	if (null != reset)
        	{
        		if (reset.equalsIgnoreCase("yes") || reset.equalsIgnoreCase("true"))
        		{
        			// Use some java8 syntax sugar to recursively delete all files below this path 
        			Files.walk(uploads.toPath())
        		      .sorted(Comparator.reverseOrder())
        		      .map(Path::toFile)
        		      .forEach(File::delete);

        		}
        	}
        	// And now unpack the zipfile into the uploads directory, maintaining any structure we have in the zipfile relative to the uplaod dir
        	try {
        		try (ZipInputStream zipIn  = new ZipInputStream(fileContent))
        		{
	        		for (ZipEntry ze; (ze = zipIn.getNextEntry()) != null; ) {
	                    Path resolvedPath = uploads.toPath().resolve(ze.getName());
	                    try
	                    {
		                    if (ze.isDirectory()) {
		                        Files.createDirectories(resolvedPath);
		                    } else {
		                        Files.createDirectories(resolvedPath.getParent());
		                        // remove any copy we've already got
		                        if (Files.exists(resolvedPath))
		                        {
		                        	Files.delete(resolvedPath);
		                        }
		                        Files.copy(zipIn, resolvedPath);
		                    }
	                    }
	                    catch (Exception e)
	                    {
	                    	// Log and carry on
	                    	System.out.println("Failed to restore " + resolvedPath + " : " + e);
	                    }
	                }
        		}
        	} catch (ZipException e) {
        	    e.printStackTrace();
        	}
        	
        	// And now rescan the project from scratch, like we do when we first init the server
        	scanUploadDirectory();        	
        }
        else
        {
	        // Save the file here
	        // Plivo doesn't like spaces in the name, so replace
	        fileName = fileName.replace(' ', '_');
	        // Have we got a folder specified?
	        String folder = request.getParameter("folder");
	        if (null == folder || folder.equalsIgnoreCase("undefined"))
	        	folder = "";
	        File uploads = new File(Settings.s.uploadDiskPath + "/" + (folder.length()==0?"" : folder + "/") + fileName);
	        // Prevent traversals
	        if (!uploads.getParentFile().toPath().startsWith(new File(Settings.s.uploadDiskPath).toPath()))
	        {
	        	// Naughty!
	        	throw new ServletException("Cannot save to " + uploads.getParentFile().toPath());        	
	        }
	        // Create the folder if necessary
	        if (!uploads.getParentFile().exists())
	        {
	        	uploads.mkdirs();
	        }
	        
	        Files.copy(fileContent, uploads.toPath(), StandardCopyOption.REPLACE_EXISTING);
	        Audios.a.add(uploads.toString(), fileName, folder, false);
        }
    }
}
