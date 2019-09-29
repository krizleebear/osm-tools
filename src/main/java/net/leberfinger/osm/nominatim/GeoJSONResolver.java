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
		JsonObject geometry = json.get("geometry").getAsJsonObject();
		JsonArray coordinates = geometry.get("coordinates").getAsJsonArray();
		JsonArray firstCoordinate = coordinates.get(0).getAsJsonArray();
		double lon = firstCoordinate.get(0).getAsDouble();
		double lat = firstCoordinate.get(1).getAsDouble();

		Optional<AdminPlace> resolvedPlace = resolver.resolve(lat, lon);
		JsonObject properties = json.get("properties").getAsJsonObject();

		if (resolvedPlace.isPresent()) {
			resolvedPlace.get().addMissingAddressProperties(properties);
		}

		return json;
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
