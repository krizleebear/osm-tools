package net.leberfinger.osm.nominatim;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.io.ParseException;

import com.google.common.base.Stopwatch;
import com.google.gson.JsonObject;

class GeoJSONResolverTest {

	private static final String NOMINATIM_BASE_URL = "http://192.168.43.201:7070/";
	public static final String TEST_RESOURCES_DIR = "src/test/resources"; 
	
	@Test
	void addAddress() throws IOException {
		String jsonString = "{\"type\":\"Feature\",\"properties\":{\"name\":\"Rathaus\",\"amenity\":\"townhall\",\"building\":\"yes\",\"wheelchair\":\"yes\",\"osmType\":\"Way\",\"wayID\":\"4345036\"},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[11.831806400000001,48.4586248],[11.8314803,48.458621300000004],[11.831486400000001,48.458368400000005],[11.831715500000001,48.458370900000006],[11.831715200000001,48.458384300000006],[11.832006900000001,48.45838740000001],[11.832004300000001,48.4584988],[11.8318095,48.458496800000006],[11.831806400000001,48.4586248]]}}";
		assertFalse(jsonString.contains("addr:"));
		
		GeoJSONResolver resolver = new GeoJSONResolver(NOMINATIM_BASE_URL);
		JsonObject resolved = resolver.addAddress(jsonString);
		
		JsonObject properties = resolved.get("properties").getAsJsonObject();
		assertTrue(properties.has("addr:city"));
		assertEquals("Haag an der Amper", properties.get("addr:city").getAsString());
	}

	@Test
	void resolveFile() throws IOException, ParseException
	{
		Stopwatch w = Stopwatch.createStarted();
		
		Path input = Paths.get(TEST_RESOURCES_DIR, "testpois.linedelimited.geojson");
//		input = Paths.get("oberbayern-latest.osm.pois.geojson"); //TODO:comment
//		input = Paths.get("germany-latest.osm.pois.geojson"); //TODO:comment
		
		Path destFile = Paths.get("testpois.linedelimited.resolved.geojson"); 
		Files.deleteIfExists(destFile);
		
//		GeoJSONResolver resolver = new GeoJSONResolver(NOMINATIM_BASE_URL);
		GeoJSONResolver resolver = new GeoJSONResolver(getPolygonResolver());
		resolver.resolveLinesInFile(input);
		
		w.stop();
		System.out.println("finished after " + w.elapsed(TimeUnit.MINUTES) + " minutes.");
		System.out.println(resolver.getStatistics());
		
		assertTrue(Files.exists(destFile));
		assertTrue(Files.size(destFile) > 0);
	}
	
	public IAdminResolver getPolygonResolver() throws IOException, ParseException
	{
		PolygonCache polys = new PolygonCache();
		
		Path dumpFile = Paths.get("postgisdump.txt");
		try (Reader r = Files.newBufferedReader(dumpFile, StandardCharsets.UTF_8)) {
			polys.importCache(r);
		}

		return polys;
	}
}
