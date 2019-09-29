package net.leberfinger.osm.nominatim;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.prep.PreparedGeometry;

import com.github.davidmoten.rtree2.geometry.internal.RectangleDouble;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AdminPlace {

	private final PreparedGeometry geometry;
	private final JsonObject json;
	private final RectangleDouble boundingBox;

	AdminPlace(PreparedGeometry geometry, JsonObject json, RectangleDouble bbox) {
		this.geometry = geometry;
		this.json = json;
		this.boundingBox = bbox;
	}

	public Geometry getGeometry() {
		return geometry.getGeometry();
	}

	/**
	 * e.g.:
	 * 
	 * <pre>
	 * {
	 * "city": "Hofstetten",
	 * "county": "Pürgen",
	 * "state_district": "Oberbayern",
	 * "state": "Bayern",
	 * "country": "Deutschland",
	 * "country_code": "de"
	 * }
	 * </pre>
	 * 
	 * @return
	 */
	public JsonElement getAddress() {
		return json.get("address");
	}

	/**
	 * Add the missing address properties to the given GeoJSON properties, e.g.
	 * 
	 * <pre>
	 * {
	 *   "addr:city": "München",
	 *   "addr:country": "DE",
	 *   "addr:housenumber": "1",
	 *   "addr:postcode": "80336",
	 *   "addr:street": "Nußbaumstraße"
	 * }
	 * </pre>
	 */
	public void addMissingAddressProperties(JsonObject properties) {
		JsonObject nominatimAddress = getAddress().getAsJsonObject();

		addIfMissing(properties, "nominatim:place_id", this.json.get("place_id"));
		addIfMissing(properties, "addr:city", nominatimAddress.get("city"));
		addIfMissing(properties, "addr:state", nominatimAddress.get("state"));
		addIfMissing(properties, "addr:country", nominatimAddress.get("country"));
		addIfMissing(properties, "country_code", nominatimAddress.get("country_code"));
	}

	private void addIfMissing(JsonObject properties, String key, JsonElement value) {
		if (!properties.has(key) && value != null) {
			if (!value.getAsString().isEmpty()) {
				properties.add(key, value);
			}
		}
	}

	public boolean contains(double lat, double lon) {
		Coordinate coordinate = new Coordinate(lon, lat);
		org.locationtech.jts.geom.Point point = GeometryFactory.createPointFromInternalCoord(coordinate, geometry.getGeometry());
		return geometry.contains(point);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AdminPlace [");
		builder.append(getAddress());
		builder.append("]");
		return builder.toString();
	}

	public RectangleDouble getBoundingBox() {
		return boundingBox;
	}
}