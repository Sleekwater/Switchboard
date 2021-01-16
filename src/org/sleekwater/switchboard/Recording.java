package org.sleekwater.switchboard;

import javax.json.JsonObjectBuilder;

public class Recording {
	public String url;
	public String duration;
	
	public void toJson(JsonObjectBuilder jsonBuilder) {
		if (null != url)
			jsonBuilder.add("url", url);
		if (null != duration)
			jsonBuilder.add("duration", duration);
	}

}
