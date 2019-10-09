package net.leberfinger.osm.nominatim;

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
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.postgis.PGbox2d;
import org.postgis.PGbox3d;
import org.postgis.PGgeometry;
import org.postgresql.PGConnection;
import org.postgresql.util.HStoreConverter;

import com.google.gson.JsonObject;

/**
 * Import all admin boundaries from the given Nominatim PostGIS DB to RAM. This
 * class is assuming the setup provided by nominatim-docker.
 * 
 * @see https://github.com/mediagis/nominatim-docker
 */
public class PostGISPolygons {

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

			String query = "select osm_id, name, ST_AsText(geometry), osm_type, class, type, osm_type, admin_level "
					+ "from place where name IS NOT NULL and class='boundary' and type='administrative';";

			try (Statement s = conn.createStatement(); //
					ResultSet r = s.executeQuery(query);) {
				while (r.next()) {

					JsonObject jsonPlace = new JsonObject();
					// PGgeometry geomContainer = r.getObject(3, PGgeometry.class);

					final int adminLevel = r.getInt(8);
					jsonPlace.addProperty("admin_level", adminLevel);
					jsonPlace.addProperty("place_id", r.getLong(1));

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
					Envelope envelope = geometry.getGeometry().getEnvelopeInternal();

					AdminPlace adminPlace = new AdminPlace(geometry, jsonPlace);
					index.insert(envelope, adminPlace);
				}
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}

			System.out.println("Index size: " + index.size());
		}
	}

	private PreparedGeometry createGeoFromText(String geotext) throws ParseException {
		Geometry geometry = wktReader.read(geotext);

		// create an optimized version of the read geometry
		PreparedGeometry optimizedGeometry = preparedGeoFactory.create(geometry);
		return optimizedGeometry;
	}

	public Optional<AdminPlace> searchInIndex(double lat, double lon) {
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
}
