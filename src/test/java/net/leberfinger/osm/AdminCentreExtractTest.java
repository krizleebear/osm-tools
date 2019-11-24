package net.leberfinger.osm;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.leberfinger.geo.GeoJSONUtils;

public class AdminCentreExtractTest {

	@Test
	void test() throws IOException {
		Path pbfFile = Paths.get("src/test/resources/testadmins.pbf");

		try (AdminCentreExtract extractor = new AdminCentreExtract();) {
			extractor.extract(pbfFile);
		}
		
		Path adminCentreFile = Paths.get("admincentres.geojsonseq");
		assertThat(adminCentreFile.toFile()).exists();
		List<String> adminCentreLines = Files.readAllLines(adminCentreFile);
		assertThat(adminCentreLines).hasSize(2);
		
		JsonObject berlinCenter = GeoJSONUtils.fromString(adminCentreLines.get(0));
		Map<String, JsonElement> centerProps = GeoJSONUtils.getPropertiesAsMap(berlinCenter);
		assertThat(centerProps).containsEntry("@id", new JsonPrimitive(62422));
		assertThat(centerProps).containsEntry("admincentre_id", new JsonPrimitive(240109189));
	}

}
