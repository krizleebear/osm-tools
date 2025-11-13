package net.leberfinger.osm.nominatim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.MutableIntIntMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.IntIntMaps;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.geojson.GeoJsonReader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * In-memory Admin Resolver.
 * </p>
 * Imports administrative boundaries from a given text file to RAM. <br/>
 * The text file can be created using {@link PostGISPlaceExport}. <br/>
 * Alternatively {@link #importGeoJSONStream(Reader)} can be used to import line
 * delimited GeoJSON objects.
 * <p/>
 * Import can effectively only be called once.
 */
public class PolygonCache implements IAdminResolver {

	private final STRtree index = new STRtree();
	private final GeometryFactory geoFactory = new GeometryFactory();
	private final PreparedGeometryFactory preparedGeoFactory = new PreparedGeometryFactory();
	private final MutableIntIntMap adminLevelCounter = IntIntMaps.mutable.empty();

	private final WKTReader wktReader = new WKTReader(geoFactory);

	public static PolygonCache fromGeoJSONStream(Path polygonFile) throws IOException, ParseException
	{
		PolygonCache polys = new PolygonCache();
		
		try (Reader r = Files.newBufferedReader(polygonFile, StandardCharsets.UTF_8)) {
			polys.importGeoJSONStream(r);
		}

		return polys;
	}
	
	/**
	 * Import a file of line delimited GeoJSON objects.
	 * <p/>
	 * This kind of file can be directly exported using osmium-tool:
	 * 
	 * <pre>
	 * osmium tags-filter --output admins.osm.pbf --overwrite ${INPUT_PBF} boundary=administrative
	 * osmium export admins.osm.pbf -o polygons.geojsonseq --omit-rs --overwrite --geometry-types=polygon
	 * </pre>
	 * 
	 * @param r
	 * @throws IOException
	 * @throws ParseException
	 */
	public void importGeoJSONStream(Reader r) throws IOException, ParseException {
		GeoJsonReader geoReader = new GeoJsonReader(geoFactory);
        JsonParser parser = new JsonParser();

        try (BufferedReader br = new BufferedReader(r)) {
			String line = null;

			int i = 0;
			while ((line = br.readLine()) != null) {

                if(i % 10_000 == 0)
                {
                    System.out.print(".");
                }

				JsonObject json = parser.parse(line).getAsJsonObject();

				JsonObject properties = json.get("properties").getAsJsonObject();
				properties.addProperty("place_id", i++);
				
				JsonObject addressProperties = new JsonObject();
				properties.add("address", addressProperties);
				
				AdminLevel adminLevel = null;
				if(properties.has("admin_level")) // numeric AdminLevel, like OSM
				{
					try
					{
						int osmAdminLevel = properties.get("admin_level").getAsInt();
						adminLevel = AdminLevel.fromOsmAdminLevel(osmAdminLevel);
					}
					catch (Exception e)
					{
						continue;
					}
				}
				else if(properties.has("subtype")) // like in overture maps
				{
					String subType = properties.get("subtype").getAsString();
					//String class = properties.get
					adminLevel = AdminLevel.fromOvertureSubtype(subType);
				}
				else
				{
					//TODO: ignore the whole polygon for now
					continue;
				}
				
				addressProperties.add(adminLevel.getAddressElement(), properties.get("name"));	
				String geometryJSON = json.remove("geometry").toString();

				Geometry geometry = geoReader.read(geometryJSON);
				PreparedGeometry optimizedGeometry = preparedGeoFactory.create(geometry);

				AdminPlace adminPlace = new AdminPlace(optimizedGeometry, properties, adminLevel);

				addToIndex(adminPlace);
			}
		}
	}
	
	/**
	 * Fill the cache with the objects provided by the given Reader.
	 * 
	 * @param r
	 * @throws IOException
	 * @throws ParseException
	 */
	public void importCache(Reader r) throws IOException, ParseException {
		
		try (BufferedReader br = new BufferedReader(r)) {
			JsonParser parser = new JsonParser();
			String line = null;
			while ((line = br.readLine()) != null) {
				JsonObject json = parser.parse(line).getAsJsonObject();
				String wellKnownText = json.remove("wktGeometry").getAsString();

				PreparedGeometry geometry = createGeoFromText(wellKnownText);
				AdminPlace adminPlace = new AdminPlace(geometry, json, AdminLevel.UNKNOWN);
				
				addToIndex(adminPlace);
			}
		}
	}

	private PreparedGeometry createGeoFromText(String geotext) throws ParseException {
		Geometry geometry = wktReader.read(geotext);

		// create an optimized version of the read geometry
		PreparedGeometry optimizedGeometry = preparedGeoFactory.create(geometry);
		return optimizedGeometry;
	}
	
	private void addToIndex(AdminPlace place) {
		final Geometry geometry = place.getGeometry();
		Envelope envelope = geometry.getEnvelopeInternal();

		adminLevelCounter.addToValue(place.getAdminLevel().getOsmAdminLevel(), 1);
		
		index.insert(envelope, place);
	}

	@Override
	public Optional<AdminPlace> resolve(double lat, double lon) {
		Coordinate coordinate = new Coordinate(lon, lat);
		Point jtsPoint = geoFactory.createPoint(coordinate);
		final Envelope pointEnvelope = jtsPoint.getEnvelopeInternal();

		@SuppressWarnings("unchecked")
		List<AdminPlace> result = index.query(pointEnvelope);

		MutableList<AdminPlace> coveringPlaces = Lists.mutable.empty();

		for (AdminPlace place : result) {
			if (place.covers(jtsPoint)) {
				coveringPlaces.add(place);
			}
		}

		// order places by admin_level to return most detailed information and not
		// only "country=Germany" or such
		coveringPlaces.sortThisByInt(place -> place.getAdminLevel().getOsmAdminLevel());

        // at the borders of polygons, conflicting results could appear
        // e.g. a mountain top being assign to both Germany and Austria
        // potentially we should support this behavior

		// resolve hierarchy to top
		resolveHierarchy(coveringPlaces);

		return coveringPlaces.reverseThis().getFirstOptional();
	}

	private void resolveHierarchy(MutableList<AdminPlace> coveringPlaces) {
		JsonObject addressElements = new JsonObject();
		for (AdminPlace place : coveringPlaces) {
			// collect information from the current place
			place.getAddress().getAsJsonObject().entrySet().forEach(entry -> {
				addressElements.add(entry.getKey(), entry.getValue());
			});

			// add collected information to the current place
			addressElements.entrySet().forEach(entry -> {
				place.getAddress().add(entry.getKey(), entry.getValue());
			});
		}
	}

	@Override
	public String getStatistics() {
		return "Index size: " + index.size() + " Admin Levels: " + adminLevelCounter.toString();
	}

	public int size() {
		return index.size();
	}
}
