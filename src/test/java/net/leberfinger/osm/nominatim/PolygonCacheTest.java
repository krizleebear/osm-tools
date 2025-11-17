package net.leberfinger.osm.nominatim;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.io.ParseException;

import com.google.gson.JsonObject;

class PolygonCacheTest {

	public static final String TEST_RESOURCES_DIR = "src/test/resources";

	@Test 
	void useCityDistrictAsCity() throws IOException, ParseException
	{
		PolygonCache polys = new PolygonCache();
		Path input = Paths.get(TEST_RESOURCES_DIR, "wkt-poly-schwabach.json");
		
		try(Reader r = Files.newBufferedReader(input))
		{
			polys.importCache(r);
		}
		
		String json = "{\"id\":33127072,\"type\":\"node\",\"lat\":49.361308,\"lon\":11.0231662,\"tags\":{\"amenity\":\"restaurant\",\"cuisine\":\"greek\",\"name\":\"Bierstüberl\",\"nominatim:place_id\":62720,\"addr:state\":\"Bayern\",\"addr:country\":\"Deutschland\"}}";
		GeoJSONResolver resolver = new GeoJSONResolver(polys);
		JsonObject resolved = resolver.addAddress(json);
		
		JsonObject tags = resolved.getAsJsonObject("tags");
		
		assertEquals("Schwabach", tags.get("addr:city").getAsString());
		assertEquals("Schwabach", tags.get("addr:county").getAsString());
	}
	
	@Test
	void importGeoJSON() throws IOException, ParseException
	{
		Path input = Paths.get(TEST_RESOURCES_DIR, "polygon-palling.geojsonseq");
		PolygonCache cache = new PolygonCache();
		try(Reader r = Files.newBufferedReader(input))
		{
			cache.importGeoJSONStream(r);
		}
		
		assertEquals(1, cache.size());
        
		Optional<AdminPlace> resolved = cache.resolve(48.001021364084664,12.638118267059326);
		Optional<String> city = resolved.get().getCity();
		assertEquals("Palling", city.get());
	}
}
