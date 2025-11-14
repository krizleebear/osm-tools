package net.leberfinger.overture;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DivisionIndexTest {

	public static final String TEST_RESOURCES_DIR = "src/test/resources"; 
	
	@Test
	void repairMissingLinks() throws IOException {
		DivisionIndex index = new DivisionIndex();

		/**
		 * 
COPY (
        SELECT
            id,
            names.primary as name,
            subtype,
            "class",
            CAST(hierarchies as JSON) as hierarchies
        FROM
            divisions
        WHERE
            id in (
            	select division_id from division_areas where ST_Contains(geometry, ST_Point(11.4285,48.764))  and is_territorial=true
            )
            OR id in (
            	select id from divisions where names.primary='Ingolstadt' and subtype='locality' AND class ='city' AND country='DE' AND region='DE-BY'
            )
        ORDER BY
            country, region
    ) TO 'ingol-divisions.geojsonseq' WITH (FORMAT GDAL, DRIVER 'GeoJSONSeq', SRS 'EPSG:4326');

		 */
		Path divisionFile = Paths.get(TEST_RESOURCES_DIR, "ingol-divisions.geojsonseq");

		try (final BufferedReader reader = Files.newBufferedReader(divisionFile)) {
			index.importGeoJSONStream(reader);
		}
		
		Division ingolstadtCity = index.get("585bedf2-7cbb-41fe-ad8d-0f5562eb63f6");
		Division ingolstadtCounty = index.get("11dc7bb4-4fa2-4e3c-a661-2ba2f51a66ca");
		
		assertNotSame(ingolstadtCity, ingolstadtCounty);
		
		index.repairMissingLinks();

		ingolstadtCounty = index.get("11dc7bb4-4fa2-4e3c-a661-2ba2f51a66ca");
		assertSame(ingolstadtCity, ingolstadtCounty);
	}
	
	@Test
	void findMissingLinks() throws IOException {
		final DivisionIndex index = new DivisionIndex();
		Path divisionFile = Paths.get(TEST_RESOURCES_DIR, "ingol-divisions.geojsonseq");
		try (final BufferedReader reader = Files.newBufferedReader(divisionFile)) {
			index.importGeoJSONStream(reader);
		}
		
        //Division ingolstadtCity   = "585bedf2-7cbb-41fe-ad8d-0f5562eb63f6"
        //Division ingolstadtCounty = "11dc7bb4-4fa2-4e3c-a661-2ba2f51a66ca"
        
		// wrong ID is the key of this map
		// correct division is the mapped value
        Map<UUID, Division> missingLinks = index.findMissingLinks();
        
        UUID wrongID = UUID.fromString("11dc7bb4-4fa2-4e3c-a661-2ba2f51a66ca");
        assertTrue(missingLinks.containsKey(wrongID));
        
        Division correctDivision = missingLinks.get(wrongID);
        assertEquals(UUID.fromString("585bedf2-7cbb-41fe-ad8d-0f5562eb63f6"), correctDivision.getID());
        assertEquals("city", correctDivision.getDivisionClass().get());
	}

}
