package net.leberfinger.osm.nominatim;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AdminPlace {

	private final PreparedGeometry geometry;
	private final JsonObject json;
	private final long placeID;

	AdminPlace(PreparedGeometry geometry, JsonObject json) {
		this.geometry = geometry;
		this.json = json;
		placeID = this.json.get("place_id").getAsLong();
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
	
	public JsonObject getJSON()
	{
		return json;
	}

	/**
	 * <pre>
	 * Continent, sea  2
	 * Country 4
	 * State   8
	 * Region  10
	 * County  12
	 * City    16
	 * Island, town, moor, waterways   17
	 * Village, hamlet, municipality, district, borough, airport, national park    18
	 * Suburb, croft, subdivision, farm, locality, islet   20
	 * Hall of residence, neighbourhood, housing estate, landuse (polygon only)    22
	 * Airport, street, road   26
	 * Paths, cycleways, service roads, etc.   27
	 * House, building 28
	 * Postcode    11–25 (depends on country)
	 * Other   30
	 * </pre>
	 * 
	 * @return
	 */
	public int getPlaceRank()
	{
		return json.get("place_rank").getAsInt();
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
		addIfMissing(properties, "nominatim:place_rank", this.json.get("place_rank"));
		addIfMissing(properties, "addr:city", nominatimAddress.get("city"));
		addIfMissing(properties, "addr:city_district", nominatimAddress.get("city_district"));
		
		addIfMissing(properties, "addr:state", nominatimAddress.get("state"));
		addIfMissing(properties, "addr:country", nominatimAddress.get("country"));
		addIfMissing(properties, "country_code", nominatimAddress.get("country_code"));
		
		// if city was not set, try other place forms, e.g. town, village
		// @see https://wiki.openstreetmap.org/wiki/Key:place
		addIfMissing(properties, "addr:city", nominatimAddress.get("town"));
		addIfMissing(properties, "addr:city", nominatimAddress.get("village"));
		addIfMissing(properties, "addr:city", nominatimAddress.get("hamlet"));
		addIfMissing(properties, "addr:city", nominatimAddress.get("isolated_dwelling"));
		addIfMissing(properties, "addr:city", nominatimAddress.get("county"));
	}

	private void addIfMissing(JsonObject properties, String key, JsonElement value) {
		if (!properties.has(key) && value != null) {
			if (!value.getAsString().isEmpty()) {
				properties.add(key, value);
			}
		}
	}

	public boolean covers(Point point) {
		return geometry.covers(point);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AdminPlace [");
		builder.append(getAddress());
		builder.append("]");
		return builder.toString();
	}

	public long getPlaceID() {
		return placeID;
	}
}