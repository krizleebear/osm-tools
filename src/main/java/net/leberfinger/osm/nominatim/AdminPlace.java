package net.leberfinger.osm.nominatim;

import java.util.Optional;

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
	
	public int getAdminLevel()
	{
		return json.get("admin_level").getAsInt();
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

		
		addIfMissing(properties, "addr:city", nominatimAddress.get("addr:city"));
		addIfMissing(properties, "addr:city_district", nominatimAddress.get("addr:city_district"));
		addIfMissing(properties, "addr:state", nominatimAddress.get("addr:state"));
		addIfMissing(properties, "addr:country", nominatimAddress.get("addr:country"));
		
		
		// if city was not set, try other place forms, e.g. town, village
		// @see https://wiki.openstreetmap.org/wiki/Key:place
		addIfMissing(properties, "addr:city", nominatimAddress.get("town"));
		addIfMissing(properties, "addr:city", nominatimAddress.get("village"));
		addIfMissing(properties, "addr:city", nominatimAddress.get("hamlet"));
		addIfMissing(properties, "addr:city", nominatimAddress.get("isolated_dwelling"));
		addIfMissing(properties, "addr:city", nominatimAddress.get("county"));
	}
	
	/**
	 * TODO: make country dependant, see
	 * https://wiki.openstreetmap.org/wiki/Tag:boundary%3Dadministrative#10_admin_level_values_for_specific_countries
	 * 
	 * @param adminLevel
	 * @return
	 */
	public static String getAddressElementForAdminLevel(int adminLevel) {
		switch (adminLevel) {
		case 2: {
			return "addr:country";
		}
		case 4: {
			return "addr:state";
		}
		case 5: {
			return "addr:state_district";
		}
		case 6: {
			return "addr:county";
		}
		case 7: {
			return "addr:amt";
		}
		case 8: {
			return "addr:city";
		}
		case 9: {
			return "addr:city_district";
		}
		case 10: {
			return "addr:city_district";
		}
		case 11: {
			return "addr:city_district_11";
		}
		case 12: {
			return "addr:city_district_12";
		}
		case 13: {
			return "addr:city_district_13";
		}
		case 14: {
			return "addr:city_district_14";
		}
		case 15: {
			return "addr:city_district_15";
		}
		case 16: {
			return "addr:city_district_16";
		}
		default: 
			throw new RuntimeException("Unknown admin level " + adminLevel);
		}
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
		builder.append("AdminPlace [address=");
		builder.append(getAddress());
		builder.append("]");
		return builder.toString();
	}

	public Optional<String> getName()
	{
		JsonElement nameElement = getJSON().get("name");
		if(nameElement == null)
		{
			return Optional.empty();
		}
		return Optional.of(nameElement.getAsString());
	}
	
	public long getPlaceID() {
		return placeID;
	}
}