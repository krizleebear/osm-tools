package net.leberfinger.osm.nominatim;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.locationtech.jts.io.ParseException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.leberfinger.geo.GeoJSONUtils;

public class GeoJSONResolver {

	// e.g. {"type":"Feature",
	// "properties":{"access":"permissive","amenity":"parking","ele":"622","fee":"yes","name":"Kampenwandbahn","note:fee":"5€
	// im Winter bei Skibetrieb, 2€ im Sommer bis
	// mittags","osmType":"Way","wayID":"4439608"},
	// "geometry":{"type":"LineString","coordinates":[[12.3246216,47.764686700000006],[12.3250828,47.7644864],[12.325169200000001,47.764477500000005],[12.325607000000002,47.7648224],[12.325173900000001,47.7650616],[12.3254827,47.7653313],[12.326057100000002,47.765092200000005],[12.3262151,47.7655946],[12.325594500000001,47.7657013],[12.3252178,47.765698500000006],[12.325133200000002,47.765490400000004],[12.325050000000001,47.7653011],[12.3248721,47.765119500000004],[12.324669,47.7648832],[12.3246216,47.764686700000006]]}}

	JsonParser parser = new JsonParser();
	private final IAdminResolver resolver;

	public GeoJSONResolver(IAdminResolver resolver)
	{
		this.resolver = resolver;
	}

	public JsonObject addAddress(String geoJSON) throws IOException {
		JsonObject json = parser.parse(geoJSON).getAsJsonObject();
		
		JsonArray coordinate = GeoJSONUtils.getCoordinate(json);
		
		double lon = coordinate.get(0).getAsDouble();
		double lat = coordinate.get(1).getAsDouble();

		Optional<AdminPlace> resolvedPlace = resolver.resolve(lat, lon);
		
		if (resolvedPlace.isPresent()) {
			JsonObject properties = GeoJSONUtils.getProperties(json);
			resolvedPlace.get().addMissingAddressProperties(properties);
		}
		else
		{
			json.addProperty("error", "unresolved");
		}

		return json;
	}
	
	/**
	 * Resolve all geo json entries in the given file, assuming that a full geo json
	 * object is contained in a single line.
	 * 
	 * Will write a new file with extension ".resolved.geojson".
	 * 
	 * @param inputFile
	 * @throws IOException
	 */
	public void resolveLinesInFile(Path inputFile) throws IOException {
		try (Writer resolvedWriter = Files.newBufferedWriter(getDestFile(inputFile));
				Stream<String> lines = Files.lines(inputFile);) {
			AtomicInteger ai = new AtomicInteger(0);
			lines.forEach(line -> {
				try {
					JsonObject resolved = addAddress(line);
					resolvedWriter.write(resolved.toString());
					resolvedWriter.write("\n");

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

	private Path getDestFile(Path inputFile) {
		String origFilename = inputFile.getFileName().toString();
		origFilename = FilenameUtils.removeExtension(origFilename);
		String destFilename = origFilename + ".resolved.geojson";
		return Paths.get(destFilename);
	}

	public String getStatistics() {
		return resolver.getStatistics();
	}
	
	public static void main(String[] args) throws IOException, ParseException {
		
		OptionParser parser = new OptionParser(); 
		
		parser.accepts("poly-file") //
				.withRequiredArg().ofType(String.class) //
				.describedAs("input geojsonseq with admin polygons");

		parser.accepts("input-file") //
				.withRequiredArg().ofType(String.class) //
				.describedAs("input geojsonseq with POIs");

		OptionSet options = parser.parse(args);
		
		System.out.println(options.asMap());
		
		if (args.length < 2) {
			parser.printHelpOn(System.err);
			throw new RuntimeException("Missing args.");
		}
        
		Path polygonFile = Paths.get(options.valueOf("poly-file").toString());
		Path inputFile = Paths.get(options.valueOf("input-file").toString());

		final PolygonCache polygons = new PolygonCache();
		if(Files.isDirectory(polygonFile))
		{
			Files.list(polygonFile).forEach(singlePolygonFile -> {
				System.out.println(singlePolygonFile);
				cachePolygons(polygons, singlePolygonFile);
			});
		}
		else
		{
			cachePolygons(polygons, polygonFile);
		}
		
		GeoJSONResolver resolver = new GeoJSONResolver(polygons);
		
		if(Files.isDirectory(inputFile))
		{
			Files.list(inputFile).forEach(geojsonFile -> {
				try {
					System.out.println(geojsonFile);
					resolver.resolveLinesInFile(geojsonFile);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
		else
		{
			resolver.resolveLinesInFile(inputFile);
		}
	}

	private static void cachePolygons(final PolygonCache polygons, Path singlePolygonFile) {
		try (Reader r = Files.newBufferedReader(singlePolygonFile, StandardCharsets.UTF_8)) {
			polygons.importGeoJSONStream(r);
		} catch (IOException | ParseException e) {
			throw new RuntimeException(e);
		}
	}
}
