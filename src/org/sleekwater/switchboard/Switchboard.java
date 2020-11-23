package org.sleekwater.switchboard;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.sleekwater.switchboard.websocket.ClientWebsocketServlet;

/**
 * A singleton to hold settings common across the entire switchboard system
 * @author sleekwater
 *
 */
public final class Switchboard {

	public static Switchboard s = new Switchboard();

	public boolean isIVR = true;
	public boolean isAutoregister = false;

	// Defaults for the server messages
	public String messageWelcome = "Welcome to the switchboard. Press 1 to register for the performance.";
	public String messageMustConfirmRegistration = "I'm sorry, you must press 1 on your keypad to register for the performance. Please try again.";
	public String messageCannotRegister = "I'm sorry, the switchboard is currently closed for new registrations. Thank you for your interest.";
	public String messageGenericError ="I'm sorry, there has been a problem. Please try again.";
	public String messageInvalidKey = "Sorry, that is not a valid key. Please try again.";
	public String messageRegistrationComplete = "Welcome, and thank you for registering. Press 9 to cancel at any time. "
			+ "You will be unregistered automatically at the end of the performance.";
	public String messageRegistrationIvr = "Welcome, and thank you for registering. The performance will start in just a few moments, please wait.";
	public String messageUnregistrationSuccessful = "Thank you. You are now unregistered, and will not take any further part in the performance.";
	public String messagePleaseWait = "Your call is in a queue. It will be answered as soon as possible. Please hold";


	// Default noargs ctor - this is a singleton so only should happen once
	public Switchboard()
	{
		load();
	}
	
	// Parse the JSON into our object for ease of use; this is the reverse of toJson
	public void parseJson(JsonObject o)
	{
		if (o.containsKey("isivrmode"))
			isIVR = o.getBoolean("isivrmode");
		if (o.containsKey("autoregister"))
			isAutoregister = o.getBoolean("autoregister");

		if (o.containsKey("messageCannotRegister"))
			messageCannotRegister = o.getString("messageCannotRegister");
		if (o.containsKey("messageGenericError"))
			messageGenericError = o.getString("messageGenericError");
		if (o.containsKey("messageInvalidKey"))
			messageInvalidKey = o.getString("messageInvalidKey");
		if (o.containsKey("messageMustConfirmRegistration"))
			messageMustConfirmRegistration = o.getString("messageMustConfirmRegistration");
		if (o.containsKey("messagePleaseWait"))
			messagePleaseWait = o.getString("messagePleaseWait");
		if (o.containsKey("messageRegistrationComplete"))
			messageRegistrationComplete = o.getString("messageRegistrationComplete");
		if (o.containsKey("messageRegistrationIvr"))
			messageRegistrationIvr = o.getString("messageRegistrationIvr");
		if (o.containsKey("messageUnregistrationSuccessful"))
			messageUnregistrationSuccessful = o.getString("messageUnregistrationSuccessful");
		if (o.containsKey("messageWelcome"))
			messageWelcome = o.getString("messageWelcome");
	}

	public String toString()
	{
		return toJsonObject().toString();
	}
	/**
	 * Turn the settings into something the client can understand
	 * @return
	 */
	public void toJson(JsonObjectBuilder message)
	{
		JsonObjectBuilder settings = Json.createObjectBuilder()
				.add("isivrmode", isIVR)
				.add("autoregister", isAutoregister)
				.add("messageCannotRegister", messageCannotRegister)
				.add("messageGenericError", messageGenericError)
				.add("messageInvalidKey", messageInvalidKey)
				.add("messageMustConfirmRegistration", messageMustConfirmRegistration)
				.add("messagePleaseWait", messagePleaseWait)
				.add("messageRegistrationComplete", messageRegistrationComplete)
				.add("messageRegistrationIvr", messageRegistrationIvr)
				.add("messageUnregistrationSuccessful", messageUnregistrationSuccessful)
				.add("messageWelcome", messageWelcome);

		message.add("setting", settings);
	}

	public JsonObject toJsonObject()
	{
		JsonObjectBuilder message = Json.createObjectBuilder();
		this.toJson(message);	
		return message.build();
	}

	/**
	 * Save current settings to file
	 */
	public void persist() {
		// Persist all to disk
		PrintWriter out;
		try {
			out = new PrintWriter(Settings.s.uploadDiskPath + "/settings.json");
			out.println(this.toString());				
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load current settings from file
	 */
	public void load()
	{
		File settings = new File(Settings.s.uploadDiskPath + "/settings.json");

		if (null != settings)
		{

			try (BufferedReader br = Files.newBufferedReader(settings.toPath())) {
				JsonReader jsonReader = Json.createReader(br);
				JsonObject object = jsonReader.readObject();
				jsonReader.close();
				parseJson(object.getJsonObject("setting"));

			} catch (Exception e) {
				System.out.println("Could not read settings - using default");
			}

		}		
	}

}
