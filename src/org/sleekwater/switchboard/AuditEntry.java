package org.sleekwater.switchboard;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.json.JsonArrayBuilder;

/**
 * When a device does something then we add an audit entry
 * @author KevinScott
 *
 */
public class AuditEntry implements Comparable<AuditEntry>{

	private Device device;
	private Date time;
	private String message;

	public AuditEntry(Device device, Date time, String message) {
		this.device = device;
		this.time = time;
		this.message = message;
	}
	
	@Override
	public int compareTo(AuditEntry o) {
		// Sort purely by date
		return this.time.compareTo(o.time);
	}

	/**
	 * Output to JSON, used by the UI
	 * @param jsonBuilder
	 */
	public void toJson(JsonArrayBuilder jsonBuilder) {
		jsonBuilder.add(new SimpleDateFormat("HH:mm:ss").format(time.getTime()) + ":" + message);
	}

	/**
	 * Output to text, used by the fullaudit CSV download
	 */
	@Override
	public String toString()
	{
		// Excel will recognise this format as a datetime when importing the CSV, and auto-apply a sensible date formatting to it.
		SimpleDateFormat dfExcel = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return dfExcel.format(time) + ", " + this.device.name + ", " + this.message;		
	}
	
}
