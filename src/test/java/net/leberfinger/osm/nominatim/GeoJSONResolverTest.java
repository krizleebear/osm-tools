package net.leberfinger.osm.nominatim;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.io.ParseException;

import com.google.common.base.Stopwatch;
import com.google.gson.JsonObject;

import static org.junit.jupiter.api.Assertions.*;

class GeoJSONResolverTest {

	public static final String NOMINATIM_BASE_URL = "http://192.168.43.201:7070/";
	public static final String TEST_RESOURCES_DIR = "src/test/resources"; 
	
	@Test
	void addAddress() throws IOException, ParseException {
//		String jsonString = "{\"type\":\"Feature\",\"properties\":{\"name\":\"Rathaus\",\"amenity\":\"townhall\",\"building\":\"yes\",\"wheelchair\":\"yes\",\"osmType\":\"Way\",\"wayID\":\"4345036\"},\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[11.831806400000001,48.4586248],[11.8314803,48.458621300000004],[11.831486400000001,48.458368400000005],[11.831715500000001,48.458370900000006],[11.831715200000001,48.458384300000006],[11.832006900000001,48.45838740000001],[11.832004300000001,48.4584988],[11.8318095,48.458496800000006],[11.831806400000001,48.4586248]]}}";
		String jsonString = "{\"id\":120915427,\"type\":\"way\",\"properties\":{\"amenity\":\"place_of_worship\",\"building\":\"church\",\"denomination\":\"catholic\",\"name\":\"Mariä Geburt\",\"religion\":\"christian\",\"wheelchair\":\"no\"},\"centroid\":{\"lat\":\"48.0010465\",\"lon\":\"12.6383187\"},\"bounds\":{\"e\":\"12.6386123\",\"n\":\"48.0011875\",\"s\":\"48.0009058\",\"w\":\"12.6378228\"}}";
		assertFalse(jsonString.contains("addr:"));
		
		GeoJSONResolver resolver = new GeoJSONResolver(getExampleResolver());
		JsonObject resolved = resolver.addAddress(jsonString);
		
		JsonObject properties = resolved.get("properties").getAsJsonObject();
		assertTrue(properties.has("addr:city"));
		assertEquals("Palling", properties.get("addr:city").getAsString());
	}
	
	@Test
	void fillEmptyAttribute() throws IOException, ParseException
	{
		// this json has a city attribute, but its empty and should be filled
		String json = "{\"type\": \"Feature\", \"properties\": {\"id\": \"31d5e1eb-d10c-4616-acf0-47e34187e1c6\", \"type\": \"node\", \"lat\": 41.31325531, \"lon\": 19.44623566, \"addr:city\": \"\", \"addr:postcode\": \"1011\", \"addr:country\": \"AL\", \"addr:state\": \"\", \"addr:street\": \"2005 Plazh Lagjia 13, rruga Prometeu, Plepa, Durres\", \"name\": \"Sol Tropikal Durres\", \"confidence\": 0.77, \"website\": \"\", \"categories\": {\"primary\": \"resort\", \"alternate\": [\"hotel\", \"lodge\"]}, \"brand\": null, \"sources\": [{\"property\": \"\", \"dataset\": \"Microsoft\", \"license\": \"CDLA-Permissive-2.0\", \"record_id\": \"2251799821412020\", \"update_time\": \"2025-07-22T09:39:24.200Z\", \"confidence\": 0.77, \"between\": null}, {\"property\": \"/properties/confidence\", \"dataset\": \"Overture\", \"license\": \"CDLA-Permissive-2.0\", \"record_id\": null, \"update_time\": \"2025-10-15T20:07:19Z\", \"confidence\": null, \"between\": null}], \"names\": {\"primary\": \"Sol Tropikal Durres\", \"common\": null, \"rules\": null}, \"adminxml:POI_TYPE\": 43}, \"geometry\": {\"type\": \"Point\", \"coordinates\": [12.6378228, 48.0009058]}}";
		
		GeoJSONResolver resolver = new GeoJSONResolver(getExampleResolver());
		
		JsonObject resolved = resolver.addAddress(json);
		JsonObject properties = resolved.get("properties").getAsJsonObject();
		assertTrue(properties.has("addr:city"));
		assertEquals("Palling", properties.get("addr:city").getAsString());
	}

	@Test
	void resolveFile() throws IOException, ParseException
	{
		Stopwatch w = Stopwatch.createStarted();
		
		Path input = Paths.get(TEST_RESOURCES_DIR, "testpois.linedelimited.geojson");
		
		Path destFile = Paths.get("testpois.linedelimited.resolved.geojson"); 
		Files.deleteIfExists(destFile);
		
		GeoJSONResolver resolver = new GeoJSONResolver(getExampleResolver());
		
		resolver.resolveLinesInFile(input);
		
		w.stop();
		System.out.println("finished after " + w.elapsed(TimeUnit.MINUTES) + " minutes.");
		System.out.println(resolver.getStatistics());
		
		assertTrue(Files.exists(destFile));
		assertTrue(Files.size(destFile) > 0);
	}
	
	public static IAdminResolver getExampleResolver() throws IOException, ParseException {

		Path dumpFile = Paths.get(TEST_RESOURCES_DIR, "polygon-palling.geojsonseq");
		return PolygonCache.fromGeoJSONStream(dumpFile);
	}
	
	public IAdminResolver getPostGISDumpResolver() throws IOException, ParseException
	{
		PolygonCache polys = new PolygonCache();
		
		Path dumpFile = Paths.get("postgisdump.txt");
		try (Reader r = Files.newBufferedReader(dumpFile, StandardCharsets.UTF_8)) {
			polys.importCache(r);
		}

		return polys;
	}
	
	public static IAdminResolver getOsmiumPolygonResolver() throws IOException, ParseException
	{
		return PolygonCache.fromGeoJSONStream(Paths.get("polygons.geojsonseq"));	
	}

	@Test
	void ignoreAlreadyProcessedFiles()
	{
		assertFalse(GeoJSONResolver.wasAlreadyProcessed(Paths.get("test.geojson")));
		assertTrue(GeoJSONResolver.wasAlreadyProcessed(Paths.get("test.resolved.geojson")));
	}

    /**
     * The following code can be used to export the full hierarchy of AdminPlaces for a given location to GeoJSON.
     *
     * @throws IOException
     * @throws ParseException
     */
    @Disabled
    @Test
    void exportResolvedHierarchy() throws IOException, ParseException {
        PolygonCache polygons = PolygonCache.fromGeoJSONStream(Paths.get(TEST_RESOURCES_DIR, "frenchTestHierarchy.geojsonseq"));
        double lat = 48.8528351;
        double lon = 2.3460262;
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get("resolvedHierarchy.geojsonseq"))) {
            polygons.exportGeoJSONHierarchy(lat, lon, bw);
        }
    }

   @Test
   void franceAdminLevel2Missing() throws IOException, ParseException {
        String parisPOI = "{ \"type\": \"Feature\", \"properties\": { \"id\": \"20fec114-a7ff-400e-94fd-29cec9251212\", \"version\": 4, \"names\": { \"primary\": \"Red Grill Steak House\", \"common\": null, \"rules\": null }, \"categories\": { \"primary\": \"restaurant\", \"alternate\": [ \"asian_restaurant\", \"diner\" ] }, \"confidence\": 0.98283596645211624, \"basic_category\": \"restaurant\", \"confidence_1\": 0.98283596645211624, \"websites\": [ \"https:\\/\\/redgrill.fr\\/\" ], \"addresses\": [ { \"freeform\": \"11 Rue de la Huchette\", \"locality\": \"Paris\", \"postcode\": \"75005\", \"region\": null, \"country\": \"FR\" } ], \"country\": \"FR\", \"region\": null }, \"geometry\": { \"type\": \"Point\", \"coordinates\": [ 2.3460262, 48.8528351 ] } }";

       PolygonCache polygons = PolygonCache.fromGeoJSONStream(Paths.get(TEST_RESOURCES_DIR, "frenchTestHierarchy.geojsonseq"));
       GeoJSONResolver resolver = new GeoJSONResolver(polygons);
       //Optional<AdminPlace> resolvedAndOrdered = polygons.resolve(48.8528351, 2.3460262);
       JsonObject fullyResolved = resolver.addAddress(parisPOI);
       JsonObject properties = fullyResolved.getAsJsonObject("properties");
       JsonElement countryProperty = properties.get("addr:country");
       assertNotNull(countryProperty);
       assertEquals("France métropolitaine", countryProperty.getAsString());
   }
}
