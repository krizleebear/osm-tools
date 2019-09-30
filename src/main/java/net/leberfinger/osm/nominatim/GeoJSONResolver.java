package net.leberfinger.osm.nominatim;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GeoJSONResolver {

	// e.g. {"type":"Feature",
	// "properties":{"access":"permissive","amenity":"parking","ele":"622","fee":"yes","name":"Kampenwandbahn","note:fee":"5€
	// im Winter bei Skibetrieb, 2€ im Sommer bis
	// mittags","osmType":"Way","wayID":"4439608"},
	// "geometry":{"type":"LineString","coordinates":[[12.3246216,47.764686700000006],[12.3250828,47.7644864],[12.325169200000001,47.764477500000005],[12.325607000000002,47.7648224],[12.325173900000001,47.7650616],[12.3254827,47.7653313],[12.326057100000002,47.765092200000005],[12.3262151,47.7655946],[12.325594500000001,47.7657013],[12.3252178,47.765698500000006],[12.325133200000002,47.765490400000004],[12.325050000000001,47.7653011],[12.3248721,47.765119500000004],[12.324669,47.7648832],[12.3246216,47.764686700000006]]}}

	JsonParser parser = new JsonParser();
	private final NominatimCache resolver;

	public GeoJSONResolver(String nominatimBaseURL) {
		NominatimConnection nominatim = new NominatimConnection(nominatimBaseURL);
		resolver = new NominatimCache(nominatim);
	}

	public JsonObject addAddress(String geoJSON) throws IOException {
		JsonObject json = parser.parse(geoJSON).getAsJsonObject();
		
		//{"id":722985747,"type":"way","tags":{"amenity":"parking"},"centroid":{"lat":"51.9065664","lon":"10.5317602"},"bounds":{"e":"10.5319003","n":"51.9066136","s":"51.9065176","w":"10.5316345"}}

		JsonArray coordinate = getCoordinate(json);
		
		double lon = coordinate.get(0).getAsDouble();
		double lat = coordinate.get(1).getAsDouble();

		Optional<AdminPlace> resolvedPlace = resolver.resolve(lat, lon);
		JsonObject properties = json.get("properties").getAsJsonObject();
		if(properties == null)
		{
			properties = json.get("tags").getAsJsonObject();
		}

		if (resolvedPlace.isPresent()) {
			resolvedPlace.get().addMissingAddressProperties(properties);
		}

		return json;
	}

	/**
	 * note: in x/y order (means: lon/lat)
	 * @param json
	 * @return
	 */
	private JsonArray getCoordinate(JsonObject json) {

		JsonArray coordinates = new JsonArray();
		
		//e.g. {"id":359829,"type":"node","lat":50.9049155,"lon":6.963953500000001,"tags":{"amenity":"car_rental","name":"Starcar Autovermietung"}}

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
			coordinates = geometryCoordinates.get(0).getAsJsonArray();
		}
		
		return coordinates;
	}

	/**
	 * Resolve all geo json entries in the given file, assuming that a full geo json
	 * object is contained in a single line.
	 * 
	 * Will write a new file with extension ".resolved.geojson".
	 * 
	 * @param file
	 * @throws IOException
	 */
	public void resolveLinesInFile(Path file) throws IOException {
		String origFilename = file.getFileName().toString();
		origFilename = FilenameUtils.removeExtension(origFilename);
		String destFilename = origFilename + ".resolved.geojson";

		//TODO: write file with entries that couldn't be resolved
		
		try (Writer writer = Files.newBufferedWriter(Paths.get(destFilename));
				Stream<String> lines = Files.lines(file);) {
			AtomicInteger ai = new AtomicInteger(0);
			lines.forEach(t -> {
				try {
					JsonObject resolved = addAddress(t);
					writer.write(resolved.toString());
					writer.write("\n");

					int i = ai.incrementAndGet();
					if (i % 1000 == 0) {
						System.out.println(i);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
	}

	public String getStatistics() {
		return resolver.getStatistic();
	}
}
