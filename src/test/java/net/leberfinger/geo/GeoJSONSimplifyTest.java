package net.leberfinger.geo;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.io.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GeoJSONSimplifyTest {

    private static final String TEST_RESOURCES_DIR = "src/test/resources";

    @Test
    void simplify() throws ParseException, IOException {
        GeoJSONSimplify s = new GeoJSONSimplify();

        assertThrows(NullPointerException.class, () -> s.simplify(null));
        assertThrows(NullPointerException.class, () -> s.simplify(new JsonObject()));

        Path inFile = Paths.get(TEST_RESOURCES_DIR, "polygon-palling.geojsonseq");
        List<String> lines = Files.readAllLines(inFile);
        String firstLine = lines.get(0);
        JsonObject simplified = s.simplifyLine(firstLine);
        String simplifiedString = simplified.toString();

        int originalLength = firstLine.length();
        int simplifiedLength = simplifiedString.length();
        assertTrue(originalLength > simplifiedLength);
    }

    @Test
    void simplifyFile() throws IOException {
        Path inFile = Paths.get(TEST_RESOURCES_DIR, "polygon-palling.geojsonseq");
        GeoJSONSimplify s = new GeoJSONSimplify();
        Path destFile = s.simplifyLines(inFile);
        assertTrue(Files.exists(destFile));
        Files.deleteIfExists(destFile);
    }

    @Test
    void simplifyCoverage() throws IOException {
        Path inFile = Paths.get(TEST_RESOURCES_DIR, "frenchTestHierarchy.geojsonseq");
        GeoJSONSimplify s = new GeoJSONSimplify();
        s.simplifyCoverage(inFile, 0.01);
        Path destFile = Paths.get(TEST_RESOURCES_DIR, "frenchTestHierarchy.simplified.geojsonseq");
        assertTrue(Files.exists(destFile));
        Files.deleteIfExists(destFile);
    }

    @Test
    void processWithBufferAndCoverage() throws IOException {
        Path inFile = Paths.get(TEST_RESOURCES_DIR, "frenchTestHierarchy.geojsonseq");
        GeoJSONSimplify s = new GeoJSONSimplify();
        Path destFile = s.process(inFile, 0.001, 0.005, true);
        assertTrue(Files.exists(destFile));
        assertTrue(Files.size(destFile) > 0);
        Files.deleteIfExists(destFile);
    }

    @Test
    void getHierarchyGroupKey() {
        GeoJSONSimplify s = new GeoJSONSimplify();

        JsonObject subtypeProps = new JsonObject();
        subtypeProps.addProperty("subtype", "locality");
        assertEquals("locality", s.getHierarchyGroupKey(subtypeProps));

        JsonObject adminLevelProps = new JsonObject();
        adminLevelProps.addProperty("admin_level", "8");
        assertEquals("admin_level_8", s.getHierarchyGroupKey(adminLevelProps));

        JsonObject classProps = new JsonObject();
        classProps.addProperty("class", "city");
        assertEquals("city", s.getHierarchyGroupKey(classProps));

        assertEquals("default", s.getHierarchyGroupKey(new JsonObject()));
        assertEquals("default", s.getHierarchyGroupKey(null));
    }

    @Test
    void mainCLI() throws Exception {
        Path inFile = Paths.get(TEST_RESOURCES_DIR, "polygon-palling.geojsonseq");
        GeoJSONSimplify.main(new String[]{"--input", inFile.toString(), "--buffer", "0.002", "--tolerance", "0.001"});
        Path destFile = Paths.get(TEST_RESOURCES_DIR, "polygon-palling.simplified.geojsonseq");
        assertTrue(Files.exists(destFile));
        Files.deleteIfExists(destFile);
    }

    @Test
    void e2eCoastalPointResolutionTest() throws Exception {
        Path tempDir = Files.createTempDirectory("osm_tools_e2e");
        Path inputGeojson = tempDir.resolve("coastal_test.geojsonseq");

        // Polygon A: Coastal municipality with a bay inlet at (10.05, 50.05)
        String coastalPolygonJSON = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[10.0,50.0],[10.1,50.0],[10.1,50.1],[10.05,50.05],[10.0,50.1],[10.0,50.0]]]},\"properties\":{\"name\":\"CoastalCity\",\"subtype\":\"locality\"}}";

        // Polygon B: Inland municipality sharing border with A at x=10.1
        String inlandPolygonJSON = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[10.1,50.0],[10.2,50.0],[10.2,50.1],[10.1,50.1],[10.1,50.0]]]},\"properties\":{\"name\":\"InlandCity\",\"subtype\":\"locality\"}}";

        Files.write(inputGeojson, (coastalPolygonJSON + "\n" + inlandPolygonJSON + "\n").getBytes());

        // Run GeoJSONSimplify with default pipeline settings (coverage=true, buffer=0.01, tolerance=0.001)
        GeoJSONSimplify.main(new String[]{inputGeojson.toString()});

        Path simplifiedFile = tempDir.resolve("coastal_test.simplified.geojsonseq");
        assertTrue(Files.exists(simplifiedFile));

        List<GeoJSON> simplifiedFeatures = GeoJSON.streamParsedGeoJSONLines(simplifiedFile).collect(java.util.stream.Collectors.toList());
        assertEquals(2, simplifiedFeatures.size());

        GeoJSON coastalFeature = simplifiedFeatures.get(0);
        GeoJSON inlandFeature = simplifiedFeatures.get(1);

        // A coastal POI inside the bay/water area (10.04, 50.06) that falls outside unbuffered polygon
        org.locationtech.jts.geom.GeometryFactory gf = new org.locationtech.jts.geom.GeometryFactory();
        org.locationtech.jts.geom.Point coastalPOI = gf.createPoint(new org.locationtech.jts.geom.Coordinate(10.04, 50.06));

        // Verify coastal POI is covered by the buffered coastal polygon
        assertTrue(coastalFeature.geometry.covers(coastalPOI), "Coastal POI should be covered by the buffered coastal municipality polygon");

        // Read original geometries for non-intrusion verification
        List<GeoJSON> originalFeatures = GeoJSON.streamParsedGeoJSONLines(inputGeojson).collect(java.util.stream.Collectors.toList());
        org.locationtech.jts.geom.Geometry origInlandGeom = originalFeatures.get(1).geometry;

        // Verify coastal polygon A does not intrude into original inland polygon B's area
        org.locationtech.jts.geom.Geometry inlandIntrusion = coastalFeature.geometry.intersection(origInlandGeom);
        assertTrue(inlandIntrusion.getArea() < 1e-6, "Buffered coastal polygon should not intrude into neighboring inland municipality");

        // Cleanup
        Files.deleteIfExists(simplifiedFile);
        Files.deleteIfExists(inputGeojson);
        Files.deleteIfExists(tempDir);
    }
}