package net.leberfinger.osm.nominatim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
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
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Import all admin boundaries from the given text file to RAM. <br/>
 * The text file can be created using {@link PostGISPlaceExport}
 */
public class PolygonCache implements IAdminResolver {

	private final STRtree index = new STRtree();
	private final GeometryFactory geoFactory = new GeometryFactory();

	private final WKTReader wktReader = new WKTReader(geoFactory);
	private final PreparedGeometryFactory preparedGeoFactory = new PreparedGeometryFactory();

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
