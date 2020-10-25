package org.sleekwater.switchboard.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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
import org.sleekwater.switchboard.Settings;
import org.sleekwater.switchboard.Texts;

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
        // TODO Auto-generated constructor stub
        
    }
    
    
    public void init(ServletConfig config) throws ServletException
    {
    	
    	System.out.println("Init FileUpload servlet");
    	super.init(config);
    	
    	// Read our web.xml stuff
    	
    	Settings.s.callbackUrl = getServletContext().getInitParameter("callback.url");
    	Settings.s.uploadDirectory = getServletContext().getInitParameter("upload.location");
    	// By default, these are all relative to our servlet webapp folder - so I need to start with a "/"
    	Settings.s.uploadDiskPath = getServletContext().getRealPath("/" + Settings.s.uploadDirectory);
    	
    	Settings.s.plivo_auth_id= getServletContext().getInitParameter("plivo.auth_id");
    	Settings.s.plivo_auth_token= getServletContext().getInitParameter("plivo.auth_token");
    	Settings.s.plivo_registerednumber= getServletContext().getInitParameter("plivo.registerednumber");
    	
    	
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

    }
    
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		System.out.println("FileUpload/POST " + request.getParameterMap().toString());
		
        Part filePart = request.getPart("file"); // Retrieves <input type="file" name="file">
        String fileName = filePart.getSubmittedFileName();
        InputStream fileContent = filePart.getInputStream();
        // Save the file here
        // Plivo doesn't like spaces in the name, so replace
        fileName = fileName.replace(' ', '_');
        // Have we got a folder specified?
        String folder = request.getParameter("folder");
        if (folder.equalsIgnoreCase("undefined"))
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
