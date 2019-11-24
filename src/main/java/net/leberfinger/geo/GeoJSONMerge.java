package net.leberfinger.geo;

import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class GeoJSONMerge {

	public static void mergeProperties(JsonObject src, JsonObject destination) {

		JsonObject props = src.get("properties").getAsJsonObject();
		JsonObject destProps = destination.get("properties").getAsJsonObject();

		for (Entry<String, JsonElement> property : props.entrySet()) {

			String key = property.getKey();
			JsonElement value = property.getValue();

			// keep existing values
			if (!destProps.has(key)) {
				destProps.add(key, value);
			}
		}
	}

}
