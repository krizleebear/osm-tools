package net.leberfinger.osm;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.leberfinger.geo.GeoJSONUtils;

/**
 * Combines place polygons and place nodes.
 * </p>
 * Can e.g. be used to merge the place nodes inside of land-use polygons. Writes
 * a new file of the original polygons containing the additional attributes of
 * their respective place node.
 */
public class LandusePolygonResolver {

	private Path placesJSON;
	private Path polygonJSON;

	private final STRtree index = new STRtree();
	private final GeometryFactory geoFactory = new GeometryFactory();
	private final PreparedGeometryFactory preparedGeoFactory = new PreparedGeometryFactory();

	public LandusePolygonResolver(Path placesJSON, Path polygonJSON) {
		this.placesJSON = placesJSON;
		this.polygonJSON = polygonJSON;
	}

	public void resolve() throws IOException {
		System.out.println("1/3 importing polygons");
		importPolygons();

		System.out.println("2/3 importing and resolving places");
		importPlaces();

		System.out.println("3/3 writing resolved polygons");
		writeResolvedPolygons();
	}

	private void importPlaces() throws IOException {
		JsonParser parser = new JsonParser();

		// e.g.
		// {"type":"Feature","geometry":{"type":"Point","coordinates":[12.6611721,47.9993422]},"properties":{"name":"Gengham","place":"hamlet"}}
		try (Stream<String> lines = Files.lines(placesJSON)) {
			lines.forEach(line -> {
				JsonObject placeJson = parser.parse(line).getAsJsonObject();
				resolvePlace(placeJson);
			});
		}
	}

	private void resolvePlace(JsonObject placeJson) {
		// {"name":"Nordhausen","place":"village"}
		JsonObject placeProperties = GeoJSONUtils.getProperties(placeJson);

		if (hasNoName(placeProperties)) {
			return;
		}

		JsonArray placeCoordinates = GeoJSONUtils.getCoordinate(placeJson);
		double lon = placeCoordinates.get(0).getAsDouble();
		double lat = placeCoordinates.get(1).getAsDouble();

		MutableList<OSMPolygon> coveringPolygons = findCoveringPolygons(lat, lon);

		OSMPolygon polygon = findSmallestPolygon(coveringPolygons);
		if (polygon == null) {
			return;
		}

		JsonObject polygonJson = polygon.getJSON();
		JsonObject feature = polygonJson;
		if (GeoJSONUtils.isFeatureCollection(polygonJson)) {
			feature = GeoJSONUtils.getFirstFeature(polygonJson);
		}

		JsonObject polygonProperties = feature.get("properties").getAsJsonObject();

		// add centre coordinate
		polygonProperties.add("center", placeCoordinates);

		// add admin_level
		JsonElement defaultAdminLevel = new JsonPrimitive(9);
		GeoJSONUtils.addAttributeIfMissing(polygonProperties, "admin_level", defaultAdminLevel);

		// merge all other attributes (e.g. name)
		addMissingAttributes(polygonProperties, placeProperties);
	}

	private boolean hasNoName(JsonObject placeProperties) {
		return !placeProperties.has("name");
	}

	private void addMissingAttributes(JsonObject polygonProperties, JsonObject placeProperties) {

		for (Entry<String, JsonElement> entry : placeProperties.entrySet()) {
			GeoJSONUtils.addAttributeIfMissing(polygonProperties, entry.getKey(), entry.getValue());
		}
	}

	public static OSMPolygon findSmallestPolygon(MutableList<OSMPolygon> coveringPolygons) {
		MutableList<OSMPolygon> sorted = coveringPolygons
				.sortThisByDouble(polygon -> polygon.getJSON().get("area").getAsDouble());
		return sorted.getFirst();
	}

	public static Optional<OSMPolygon> findPolygonMatchingName(String name, MutableList<OSMPolygon> coveringPolygons) {
		return coveringPolygons.select(polygon -> Optional.of(name).equals(polygon.getName())).getFirstOptional();
	}

	private MutableList<OSMPolygon> findCoveringPolygons(double lat, double lon) {
		Coordinate coordinate = new Coordinate(lon, lat);
		Point jtsPoint = geoFactory.createPoint(coordinate);
		final Envelope pointEnvelope = jtsPoint.getEnvelopeInternal();

		@SuppressWarnings("unchecked")
		List<OSMPolygon> result = index.query(pointEnvelope);

		MutableList<OSMPolygon> coveringPlaces = Lists.mutable.empty();

		for (OSMPolygon place : result) {
			if (place.covers(jtsPoint)) {
				coveringPlaces.add(place);
			}
		}

		return coveringPlaces;
	}

	/*
	 * {"type":"Feature","geometry":{"type":"MultiPolygon","coordinates":[[[[9.
	 * 6166975,52.6792848],[9.6170033,52.6793136],[9.6169842,52.6795319],[9.6167783,
	 * 52.6794781],[9.6167234,52.6793886],[9.6166975,52.6792848]]]]},"properties":{
	 * "leisure":"park","created_by":"JOSM"}}
	 */
	private void importPolygons() throws IOException {
		JsonParser parser = new JsonParser();
		GeoJsonReader geoReader = new GeoJsonReader(geoFactory);

		try (Stream<String> lines = Files.lines(polygonJSON)) {
			lines.forEach(line -> {

				if (line == null || line.isEmpty()) {
					return;
				}

				JsonObject json = parser.parse(line).getAsJsonObject();
				if (GeoJSONUtils.isFeatureCollection(json)) {
					json = GeoJSONUtils.getFirstFeature(json);
				}

				String geometryJSON = json.get("geometry").toString();
				Geometry geometry;
				try {
					geometry = geoReader.read(geometryJSON);
				} catch (ParseException e) {
					throw new RuntimeException(e);
				}
				PreparedGeometry optimizedGeometry = preparedGeoFactory.create(geometry);

				double area = geometry.getArea();
				json.addProperty("area", area);

				OSMPolygon osmPoly = new OSMPolygon(optimizedGeometry, json);
				addToIndex(osmPoly);
			});
		}
	}

	private void addToIndex(OSMPolygon place) {
		final Geometry geometry = place.getGeometry();
		Envelope envelope = geometry.getEnvelopeInternal();

		index.insert(envelope, place);
	}

	private void writeResolvedPolygons() throws IOException {
		@SuppressWarnings("unchecked")
		List<OSMPolygon> items = index.itemsTree();

		try (Writer resolvedWriter = Files.newBufferedWriter(getDestFile(polygonJSON))) {
			writePolygons(resolvedWriter, items);
		}
	}

	@SuppressWarnings("unchecked")
	private void writePolygons(Writer resolvedWriter, List<OSMPolygon> items) throws IOException {
		for (Object item : items) {
			// STRTree::itemsTree will omit two kinds of elements: The stored objects itself
			// or Lists of these object, so we have to distinguish
			if (item instanceof OSMPolygon) {
				writePolygons(resolvedWriter, (OSMPolygon) item);
			} else if (item instanceof List) {
				writePolygons(resolvedWriter, (List<OSMPolygon>) item);
			}
		}
	}

	private void writePolygons(Writer resolvedWriter, OSMPolygon polygon) throws IOException {
		JsonObject resolved = polygon.getJSON();
		resolvedWriter.write(resolved.toString());
		resolvedWriter.write("\n");
	}

	private Path getDestFile(Path inputFile) {
		String origFilename = inputFile.getFileName().toString();
		origFilename = FilenameUtils.removeExtension(origFilename);
		String destFilename = origFilename + ".resolved.geojsonseq";
		return Paths.get(destFilename);
	}

	private static class OSMPolygon {
		private final PreparedGeometry geometry;
		private final JsonObject json;

		public OSMPolygon(PreparedGeometry geometry, JsonObject json) {
			this.geometry = geometry;
			this.json = json;

			if (!json.has("properties")) {
				json.add("properties", new JsonObject());
			}
		}

		public Geometry getGeometry() {
			return geometry.getGeometry();
		}

		public JsonObject getJSON() {
			return json;
		}

		public boolean covers(Point point) {
			return geometry.covers(point);
		}

		public JsonObject getProperties() {
			return json.getAsJsonObject("properties");
		}

		public Optional<String> getName() {
			if (!getProperties().has("name")) {
				return Optional.empty();
			} else {
				return Optional.of(getProperties().get("name").getAsString());
			}
		}
	}

	public static void main(String[] args) throws IOException, ParseException {

		OptionParser parser = new OptionParser();

		parser.accepts("poly-file") //
				.withRequiredArg().ofType(String.class) //
				.describedAs("input geojsonseq with place polygons");

		parser.accepts("places-file") //
				.withRequiredArg().ofType(String.class) //
				.describedAs("input geojsonseq with place nodes");

		OptionSet options = parser.parse(args);

		System.out.println(options.asMap());

		if (args.length < 2) {
			parser.printHelpOn(System.err);
			throw new RuntimeException("Missing args.");
		}

		Path polygonFile = Paths.get(options.valueOf("poly-file").toString());
		Path placesFile = Paths.get(options.valueOf("places-file").toString());

		LandusePolygonResolver resolver = new LandusePolygonResolver(placesFile, polygonFile);
		resolver.resolve();
		resolver.writeResolvedPolygons();
	}
}
