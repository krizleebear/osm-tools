package net.leberfinger.osm.nominatim;

import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.eclipse.collections.impl.factory.Lists;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.postgis.PGbox2d;
import org.postgis.PGbox3d;
import org.postgis.PGgeometry;
import org.postgresql.PGConnection;
import org.postgresql.util.HStoreConverter;

import com.google.gson.JsonObject;

/**
 * Export all place polygons from Nominatim's PostGIS DB to a text file. Each
 * line contains a JSON that describes a place and its geometry.</p>
 * 
 * This class is assuming the DB setup provided by nominatim-docker.
 * 
 * @see https://github.com/mediagis/nominatim-docker
 */
public class PostGISPlaceExport {

	private final GeometryFactory geoFactory = new GeometryFactory();

	private final WKTReader wktReader = new WKTReader(geoFactory);
	private final PreparedGeometryFactory preparedGeoFactory = new PreparedGeometryFactory();

	public List<AdminPlace> exportFromDB() throws SQLException {
		
		String url = "jdbc:postgresql://192.168.43.201:6432/nominatim";
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		List<AdminPlace> places = Lists.mutable.empty();
		
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

					if (!isArea(geometry.getGeometry())) {
						continue;
					}
					
					places.add(adminPlace);
				}
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
		
		return places;
	}
	
	private static boolean isArea(Geometry geometry) {
		Class<? extends Geometry> geoClass = geometry.getClass();
		if (geoClass == LineString.class) {
			return false;
		}
		return true;
	}

	private PreparedGeometry createGeoFromText(String geotext) throws ParseException {
		Geometry geometry = wktReader.read(geotext);

		// create an optimized version of the read geometry
		PreparedGeometry optimizedGeometry = preparedGeoFactory.create(geometry);
		return optimizedGeometry;
	}
	
	/**
	 * Export all objects in the current cache to the given writer. A new JSON line
	 * is written for each object.
	 * 
	 * @param w
	 */
	public void exportPlaces(List<AdminPlace> places, Writer w) {
		try (PrintWriter pw = new PrintWriter(w)) {
			WKTWriter wktWriter = new WKTWriter(2);

			for (AdminPlace place : places) {
				JsonObject copy = copyJSON(place.getJSON());
				String geometryString = wktWriter.write(place.getGeometry());
				copy.addProperty("wktGeometry", geometryString);

				pw.println(copy.toString());
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
}
