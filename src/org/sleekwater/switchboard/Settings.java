package org.sleekwater.switchboard;

// A convenient place to put web xml settings, etc
public class Settings {

	public static Settings s = new Settings();
	
    // Some public values read from the web.xml file
    public String uploadDiskPath="";
    public String uploadDirectory  = "";
    public String callbackUrl = ""; // Must end with a /
	public String plivo_auth_id;
	public String plivo_auth_token;
	public String plivo_registerednumber;    

}
