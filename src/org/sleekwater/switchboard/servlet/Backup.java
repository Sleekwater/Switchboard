package org.sleekwater.switchboard.servlet;

import javax.servlet.annotation.WebServlet;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sleekwater.switchboard.Settings;
import org.sleekwater.switchboard.websocket.ClientWebsocketServlet;
/**
 * Servlet implementation class Answer
 */
@WebServlet(description = "Servlet handler backing up the performance as a zipfile", urlPatterns = { "/Backup" }, loadOnStartup=1)

public class Backup extends HttpServlet {
	private static final long serialVersionUID = 1L;
	static final int BUFFER = 1024;
	List<File> fileList = new ArrayList<File>();
	File directoryToZip = null;
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

	/** Check the auth and then download a zipfile with the performance files in it */

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// Auth'd? We cookie the page...
		try
		{			
			String auth = getCookie(req, "auth").getValue();
			if (!ClientWebsocketServlet.sessionHandler.validAccounts.contains(auth))
			{
				resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You must be a known user to use this servlet");
				return;
			}
		}	
		catch (Exception e)
		{
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.toString());
			return;
		}
		directoryToZip = new File(Settings.s.uploadDiskPath);


		// get list of files
		fileList.clear();
		List<File> fileList = getFileList(directoryToZip);
		//go through the list of files and zip them into a bytearray
		byte[] zip = zipFiles(fileList);  

		// And send the bytearray to the caller
		resp.setContentType("application/zip");
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());        
		resp.setHeader("Content-Disposition", "attachment; filename=" + "backup_" + sdf.format(timestamp) + ".zip");

		ServletOutputStream sos = resp.getOutputStream();
		sos.write(zip);
		sos.flush();
	}



	private byte[] zipFiles(List<File> fileList){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try{
			ZipOutputStream zos = new ZipOutputStream(baos);
			// looping through all the files
			for(File file : fileList){
				// To handle empty directory
				if(file.isDirectory()){
					// ZipEntry --- Here file name can be created using the source file
					ZipEntry ze = new ZipEntry(getFileName(file.toString())+"/");
					// Putting zipentry in zipoutputstream
					zos.putNextEntry(ze);
					zos.closeEntry();
				}else{
					FileInputStream fis = new FileInputStream(file);
					BufferedInputStream bis = new BufferedInputStream(fis, BUFFER);
					// ZipEntry --- Here file name can be created using the source file
					ZipEntry ze = new ZipEntry(getFileName(file.toString()));
					// Putting zipentry in zipoutputstream
					zos.putNextEntry(ze);
					byte data[] = new byte[BUFFER];
					int count;
					while((count = bis.read(data, 0, BUFFER)) != -1) {
						zos.write(data, 0, count);
					}
					bis.close();
					zos.closeEntry();
				}               
			}                
			zos.close();    
		}catch(IOException ioExp){
			System.out.println("Error while zipping " + ioExp.getMessage());
			ioExp.printStackTrace();
		}
		try {
			baos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return baos.toByteArray();
	}

	/**
	 * This method will give the list of the files 
	 * in folder and subfolders
	 * @param source
	 * @return
	 */
	private List<File> getFileList(File source){      
		if(source.isFile()){
			fileList.add(source);
		}else if(source.isDirectory()){
			String[] subList = source.list();
			// This condition checks for empty directory
			if(subList.length == 0){
				//System.out.println("path -- " + source.getAbsolutePath());
				fileList.add(new File(source.getAbsolutePath()));
			}
			for(String child : subList){
				getFileList(new File(source, child));
			}
		}
		return fileList;
	}

	/**
	 * 
	 * @param filePath
	 * @return
	 */
	private String getFileName(String filePath){
		String name = filePath.substring(directoryToZip.toString().length() + 1, filePath.length());
		//System.out.println(" name " + name);
		return name;      
	}
	
	public static Cookie getCookie(HttpServletRequest request, String name) {
	    Cookie[] cookies = request.getCookies();
	    if (cookies != null) {
	        for (Cookie cookie : cookies) {
	            if (cookie.getName().equals(name)) {
	                return cookie;
	            }
	        }
	    }
	    return null;
	}
}
