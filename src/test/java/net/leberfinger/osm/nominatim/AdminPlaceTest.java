package net.leberfinger.osm.nominatim;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AdminPlaceTest {

    @Test
    void preferOfficialName() {

        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse("{\n" +
                "  \"properties\": {\n" +
                "    \"place_id\": 2528142,\n" +
                "    \"@type\": \"relation\",\n" +
                "    \"ISO3166-1\": \"BA\",\n" +
                "    \"ISO3166-1:alpha2\": \"BA\",\n" +
                "    \"ISO3166-1:alpha3\": \"BIH\",\n" +
                "    \"ISO3166-1:numeric\": \"070\",\n" +
                "    \"admin_level\": \"2\",\n" +
                "    \"boundary\": \"administrative\",\n" +
                "    \"default_language\": \"bs\",\n" +
                "    \"int_name\": \"Bosnia and Herzegovina\",\n" +
                "    \"name\": \"Bosna i Hercegovina / Босна и Херцеговина\",\n" +
                "    \"official_name\": \"Bosna i Hercegovina\",\n" +
                "    \"official_name:ar\": \"البوسنة والهرسك\",\n" +
                "    \"short_name\": \"BiH / БиХ\",\n" +
                "    \"timezone\": \"Europe/Sarajevo\",\n" +
                "    \"wikidata\": \"Q225\",\n" +
                "    \"wikipedia\": \"en:Bosnia and Herzegovina\"\n" +
                "  },\n" +
                "  \"type\": \"Feature\"\n" +
                "}").getAsJsonObject();
        AdminPlace adminPlace = new AdminPlace(null, json.getAsJsonObject("properties"), AdminLevel.COUNTRY);

        assertEquals("Bosna i Hercegovina", adminPlace.getName().get());
        assertEquals("2", adminPlace.getFirstExistingProperty("bla", "blu", "admin_level").get().getAsString());
    }
}