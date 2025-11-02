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
	private final AdminLevel adminLevel;

	private static final boolean OVERWRITE_ADDRESS_ATTRIBUTES;
	
	static {
		OVERWRITE_ADDRESS_ATTRIBUTES = "true".equalsIgnoreCase(System.getProperty("OVERWRITE_ADDRESS_ATTRIBUTES", "false"));
	}
	
	public AdminPlace(PreparedGeometry geometry, JsonObject json, AdminLevel adminLevel) {
		this.geometry = geometry;
		this.json = json;
		this.adminLevel = adminLevel;
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
	public JsonObject getAddress() {
		return json.get("address").getAsJsonObject();
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
	
	public AdminLevel getAdminLevel()
	{
		return this.adminLevel;
		//return json.get("admin_level").getAsInt();
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
		JsonObject nominatimAddress = getAddress();

		addIfMissing(properties, "nominatim:place_id", this.json.get("place_id"));
		addIfMissing(properties, "nominatim:place_rank", this.json.get("place_rank"));
		
		addIfMissing(properties, "addr:city", nominatimAddress.get("city"));
		addIfMissing(properties, "addr:city_district", nominatimAddress.get("city_district"));
		addIfMissing(properties, "addr:state", nominatimAddress.get("state"));
		addIfMissing(properties, "addr:country", nominatimAddress.get("country"));
		addIfMissing(properties, "country_code", nominatimAddress.get("country_code"));

		
		addIfMissing(properties, "addr:city", nominatimAddress.get("addr:city"));
		addIfMissing(properties, "addr:city_district", nominatimAddress.get("addr:city_district"));
		addIfMissing(properties, "addr:county", nominatimAddress.get("addr:county"));
		addIfMissing(properties, "addr:state", nominatimAddress.get("addr:state"));
		addIfMissing(properties, "addr:country", nominatimAddress.get("addr:country"));
		
		
		// if city was not set, try other place forms, e.g. town, village
		// @see https://wiki.openstreetmap.org/wiki/Key:place
		addIfMissing(properties, "addr:city", nominatimAddress.get("town"));
		addIfMissing(properties, "addr:city", nominatimAddress.get("village"));
		addIfMissing(properties, "addr:city", nominatimAddress.get("hamlet"));
		addIfMissing(properties, "addr:city", nominatimAddress.get("isolated_dwelling"));
		addIfMissing(properties, "addr:city", nominatimAddress.get("county"));
		
		// if city is still not set, but county is set, assume a district-city
		// (e.g. Kreisfreie Stadt in Germany)
		addIfMissing(properties, "addr:city", nominatimAddress.get("addr:county"));

        // add division id with proper admin info
        getDivisionID().ifPresent(divisionID -> {
            addIfMissing(properties, "divisionID", divisionID);
        });
	}

    public Optional<JsonElement> getDivisionID()
    {
        JsonElement divisionId = json.get("division_id");
        return Optional.ofNullable(divisionId);
    }
	
	// properties are the values from the place to resolve
	// key is e.g. "addr:state"
	// value refers to the value of the AdminPlace, e.g. a state name
	public static void addIfMissing(JsonObject properties, String key, JsonElement value) {
		if(isMissing(properties, key) || OVERWRITE_ADDRESS_ATTRIBUTES)
		{
			if (value != null && !value.isJsonNull() && !value.getAsString().isEmpty()) {
				properties.add(key, value);
			}
		}
	}

	private static boolean isMissing(JsonObject properties, String key) {
		return !properties.has(key) || properties.get(key).isJsonNull() || properties.get(key).getAsString().isEmpty();
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

	public Optional<String> getCity() {
		return getAddressComponent("addr:city");
	}
	
	private Optional<String> getAddressComponent(String key) {
		if (getAddress().has(key)) {
			return Optional.of(getAddress().get(key).getAsString());
		}
		return Optional.empty();
	}
}