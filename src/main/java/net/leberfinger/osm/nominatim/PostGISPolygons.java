package net.leberfinger.osm.nominatim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.postgis.PGbox2d;
import org.postgis.PGbox3d;
import org.postgis.PGgeometry;
import org.postgresql.PGConnection;
import org.postgresql.util.HStoreConverter;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Import all admin boundaries from the given Nominatim PostGIS DB to RAM. This
 * class is assuming the setup provided by nominatim-docker.
 * 
 * @see https://github.com/mediagis/nominatim-docker
 */
public class PostGISPolygons implements IAdminResolver {

	private final Quadtree index = new Quadtree();
	private final GeometryFactory geoFactory = new GeometryFactory();

	private final WKTReader wktReader = new WKTReader(geoFactory);
	private final PreparedGeometryFactory preparedGeoFactory = new PreparedGeometryFactory();

	public void importFromDB() throws SQLException {
		String url = "jdbc:postgresql://192.168.43.201:6432/nominatim";
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		try (Connection conn = DriverManager.getConnection(url, "nominatim", "password1234");) {

			/*
			 * Add the geometry types to the connection. Note that you must cast the
			 * connection to the pgsql-specific connection implementation before calling the
			 * addDataType() method.
			 */
			PGConnection pgCon = (PGConnection) conn;
			pgCon.addDataType("geometry", PGgeometry.class);
			pgCon.addDataType("box2d", PGbox2d.class);
			pgCon.addDataType("box3d", PGbox3d.class);

			// ST_AsText(geometry)

			String query = "select osm_id, name, ST_AsText(geometry), osm_type, admin_level "
					+ "from place where name IS NOT NULL and class='boundary' and type='administrative';";

			try (Statement s = conn.createStatement(); //
					ResultSet r = s.executeQuery(query);) {
				while (r.next()) {

					JsonObject jsonPlace = new JsonObject();
					// PGgeometry geomContainer = r.getObject(3, PGgeometry.class);

					final int adminLevel = r.getInt(5);
					jsonPlace.addProperty("admin_level", adminLevel);
					jsonPlace.addProperty("place_id", r.getLong(1));
					jsonPlace.addProperty("osm_type", r.getString(4));

					JsonObject addressProperties = new JsonObject();
					jsonPlace.add("address", addressProperties);

					String nameHStore = r.getString(2);
					if (nameHStore == null) {
						// skip polygons without names
						continue;
					} else {
						Map<String, String> names = HStoreConverter.fromString(nameHStore);

						String addressKey = AdminPlace.getAddressElementForAdminLevel(adminLevel);
						addressProperties.addProperty(addressKey, names.get("name"));

						names.forEach((lang, translation) -> {
							jsonPlace.addProperty(lang, translation);
						});
					}

					String geotext = r.getString(3);
					
					PreparedGeometry geometry = createGeoFromText(geotext);
					AdminPlace adminPlace = new AdminPlace(geometry, jsonPlace);

					if (isArea(geometry.getGeometry())) {
						addToIndex(adminPlace);
					}
				}
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private boolean isArea(Geometry geometry) {
		Class<? extends Geometry> geoClass = geometry.getClass();
		if (geoClass == LineString.class) {
			return false;
		}
		return true;
	}

	private void addToIndex(AdminPlace place) {
		final Geometry geometry = place.getGeometry();
		Envelope envelope = geometry.getEnvelopeInternal();

		index.insert(envelope, place);
	}
	
	/**
	 * Export all objects in the current cache to the given writer. A new JSON line
	 * is written for each object.
	 * 
	 * @param w
	 */
	public void exportCache(Writer w) {
		try (PrintWriter pw = new PrintWriter(w)) {
			WKTWriter wktWriter = new WKTWriter(2);

			@SuppressWarnings("unchecked")
			List<AdminPlace> places = index.queryAll();
			for (AdminPlace place : places) {
				JsonObject copy = copyJSON(place.getJSON());
				String geometryString = wktWriter.write(place.getGeometry());
				copy.addProperty("wktGeometry", geometryString);

				pw.println(copy.toString());
			}
		}
	}

	/**
	 * Fill the cache with the objects provided by the given Reader. This is the
	 * reverse operation of {@link #exportCache(Writer)}.
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

				AdminPlace adminPlace = new AdminPlace(geometry, json);
				
				if (isArea(geometry.getGeometry())) {
					addToIndex(adminPlace);
				}
			}
		}
	}

	private static JsonObject copyJSON(JsonObject original) {
		JsonObject copy = new JsonObject();
		original.entrySet().forEach(entry -> {
			copy.add(entry.getKey(), entry.getValue());
		});

		return copy;
	}

	private PreparedGeometry createGeoFromText(String geotext) throws ParseException {
		Geometry geometry = wktReader.read(geotext);

		// create an optimized version of the read geometry
		PreparedGeometry optimizedGeometry = preparedGeoFactory.create(geometry);
		return optimizedGeometry;
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

		// order places by admin_level to return most detailled information and not
		// only "Germany" or such
		coveringPlaces.sortThisByInt(place -> place.getAdminLevel());

		// TODO: resolve hierarchy to top
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
				place.getAddress().getAsJsonObject().add(entry.getKey(), entry.getValue());
			});
		}
	}

	@Override
	public String getStatistics() {
		return "Index size: " + index.size();
	}
	
	
}
