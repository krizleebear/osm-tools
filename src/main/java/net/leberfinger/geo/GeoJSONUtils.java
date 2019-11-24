package net.leberfinger.geo;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.collections.impl.factory.Maps;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class GeoJSONUtils {

	/**
	 * note: in x/y order (means: lon/lat)
	 * @param json
	 * @return
	 */
	public static JsonArray getCoordinate(JsonObject json) {
	
		JsonArray coordinates = new JsonArray();
		
		if(json.has("lat") && json.has("lon"))
		{
			coordinates.add(json.get("lon"));
			coordinates.add(json.get("lat"));
		}
		else if(json.has("centroid"))
		{
			//{"id":4408507,"type":"way","tags":{"amenity":"parking"},"centroid":{"lat":"49.0393827","lon":"8.3355651"},"bounds":{"e":"8.3358719","n":"49.0395493","s":"49.0391033","w":"8.3351585"}}
			JsonObject centroid = json.get("centroid").getAsJsonObject();
			coordinates.add(centroid.get("lon"));
			coordinates.add(centroid.get("lat"));
		}
		else if(json.has("geometry"))
		{
			JsonObject geometry = json.get("geometry").getAsJsonObject();
			JsonArray geometryCoordinates = geometry.get("coordinates").getAsJsonArray();
			
			if ("Point".equals(geometry.get("type").getAsString())) {
				coordinates = geometryCoordinates;
			} else if ("MultiPolygon".equals(geometry.get("type").getAsString())) {
				coordinates = geometryCoordinates.get(0).getAsJsonArray();
				coordinates = coordinates.get(0).getAsJsonArray();
				coordinates = coordinates.get(0).getAsJsonArray();
			} else {
				coordinates = geometryCoordinates.get(0).getAsJsonArray();
			}
		}
		
		return coordinates;
	}

	/**
	 * GeoJSON properties might be stored as property "properties" or "tags".
	 * Normalize to the official standard "properties".
	 */
	public static JsonObject getProperties(JsonObject json) {
	
		JsonElement props = json.get("properties");
	
		if (props == null) {
			props = json.get("tags");
		}
	
		if (props == null) {
			props = new JsonObject();
		}
	
		return props.getAsJsonObject();
	}

	public static String getType(JsonObject json)
	{
		return json.get("type").getAsString();
	}
	
	public static boolean isFeature(JsonObject json) {
		return "Feature".equals(getType(json));
	}

	public static boolean isFeatureCollection(JsonObject json) {
		return "FeatureCollection".equals(getType(json));
	}

	public static JsonObject getFirstFeature(JsonObject json) {
		JsonArray features = json.get("features").getAsJsonArray();
		return features.get(0).getAsJsonObject();
	}
	
	public static void addAttributeIfMissing(JsonObject properties, String key, JsonElement value) {
		if (!properties.has(key) && value != null) {
			if (!value.isJsonNull() && !value.getAsString().isEmpty()) {
				properties.add(key, value);
			}
		}
	}

	public static Map<String, JsonElement> getPropertiesAsMap(JsonObject geoJson) {
		JsonObject properties = getProperties(geoJson);
		
		Map<String, JsonElement> map = Maps.mutable.empty();
		for(Entry<String, JsonElement> entry : properties.entrySet())
		{
			map.put(entry.getKey(), entry.getValue());
		}
		
		return map;
	}

	public static void addProperty(JsonObject dest, String key, String value) {
		JsonObject properties = getProperties(dest);
		properties.addProperty(key, value);
	}
}
