package net.leberfinger.osm.nominatim;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import static org.junit.jupiter.api.Assertions.*;

public class AdminPlaceAssert {

    private final JsonObject properties;

    private AdminPlaceAssert(JsonObject properties) {
        this.properties = properties;
    }

    public static AdminPlaceAssert assertThat(JsonObject resolved) {
        assertNotNull(resolved, "Resolved JsonObject must not be null");
        JsonObject props = resolved.has("properties") ? resolved.getAsJsonObject("properties") : resolved;
        return new AdminPlaceAssert(props);
    }

    public AdminPlaceAssert hasCountry(String expected) {
        return assertPropertyEquals("addr:country", expected);
    }

    public AdminPlaceAssert hasState(String expected) {
        return assertPropertyEquals("addr:state", expected);
    }

    public AdminPlaceAssert hasCounty(String expected) {
        return assertPropertyEquals("addr:county", expected);
    }

    public AdminPlaceAssert hasCity(String expected) {
        return assertPropertyEquals("addr:city", expected);
    }

    public AdminPlaceAssert hasCityDistrict(String expected) {
        return assertPropertyEquals("addr:city_district", expected);
    }

    public AdminPlaceAssert hasProperty(String key) {
        assertTrue(properties.has(key) && !properties.get(key).isJsonNull(),
                "Expected property '" + key + "' to exist, but was missing in " + properties);
        return this;
    }

    private AdminPlaceAssert assertPropertyEquals(String key, String expected) {
        assertTrue(properties.has(key), "Missing expected property '" + key + "' in " + properties);
        JsonElement val = properties.get(key);
        assertNotNull(val, "Property '" + key + "' is null");
        assertEquals(expected, val.getAsString(), "Mismatch for property '" + key + "'");
        return this;
    }
}
