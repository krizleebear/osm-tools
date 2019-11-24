package net.leberfinger.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;

import org.assertj.core.api.MapAssert;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class GeoJSONMergeTest {

	JsonParser parser = new JsonParser();

	private static final String adminCentreExample = "{\"type\":\"Feature\",\"geometry\":null,\"properties\":{\"@id\":27026,\"@type\":\"relation\",\"admincentre_type\":\"node\",\"admincentre_id\":240100817}}";;

	private JsonObject featureWithProperties(String... keyValuePairs)
	{
		JsonObject feature = jsonWith("type", "Feature", "geometry", null);
		JsonObject properties = jsonWith(keyValuePairs);
		feature.add("properties", properties);
		
		return feature;
	}
	
	private static JsonObject jsonWith(String... keyValuePairs) {
		JsonObject o = new JsonObject();

		for (int i = 0; i < keyValuePairs.length; i += 2) {
			String key = keyValuePairs[i];
			String value = keyValuePairs[i + 1];

			o.addProperty(key, value);
		}

		return o;
	}
	
	@Test
	void testJsonWithHelper()
	{
		JsonObject json = jsonWith("a","1", "b", "2");
		assertEquals("1", json.get("a").getAsString());
		assertEquals("2", json.get("b").getAsString());
	}
	
	@Test
	void mergeEmpty()
	{
		JsonObject src = new JsonObject();
		JsonObject dest = new JsonObject();
		
		assertThrows(NullPointerException.class, () -> {
			GeoJSONMerge.mergeProperties(src, dest);	
		});
	}
	
	@Test
	void existingValuesMustBeKept()
	{
		JsonObject src = featureWithProperties("@id", "0");
		
		JsonObject dest = parser.parse(adminCentreExample).getAsJsonObject();
		
		GeoJSONMerge.mergeProperties(src, dest);
		
		assertThatPropertiesOf(dest).containsEntry("@id", new JsonPrimitive(27026));
	}
	
	@Test
	void propertiesMustBeMerged() {
		JsonObject src = featureWithProperties("a", "a");

		JsonObject dest = parser.parse(adminCentreExample).getAsJsonObject();
		GeoJSONMerge.mergeProperties(src, dest);

		assertThatPropertiesOf(dest).containsEntry("a", new JsonPrimitive("a"));
	}
	
	private static MapAssert<String, JsonElement> assertThatPropertiesOf(JsonObject geoJson)
	{
		Map<String, JsonElement> m = GeoJSONUtils.getPropertiesAsMap(geoJson);
		return assertThat(m);
		
	}
}
