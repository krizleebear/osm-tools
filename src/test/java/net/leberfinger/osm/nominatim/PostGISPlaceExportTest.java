package net.leberfinger.osm.nominatim;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Test;

class PostGISPlaceExportTest {

	PostGISPlaceExport exporter = new PostGISPlaceExport();
	
	@Test
	void importFromDB() throws SQLException, IOException {
		
		List<AdminPlace> places = exporter.exportFromDB();

		Path dumpFile = Paths.get("postgisdump.txt");
		try (Writer w = Files.newBufferedWriter(dumpFile, StandardCharsets.UTF_8)) {
			exporter.exportPlaces(places, w);
		}
	}

}
