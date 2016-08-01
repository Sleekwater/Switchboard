package org.sleekwater.switchboard;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

public class Recording {
	public String url;
	public String duration;
	
	public void toJson(JsonObjectBuilder jsonBuilder) {
		jsonBuilder.add("url", url).add("duration", duration);
	}

}
