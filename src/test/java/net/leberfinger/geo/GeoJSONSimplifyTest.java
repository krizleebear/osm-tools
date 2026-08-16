package net.leberfinger.geo;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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

    /**
     * Verifies that the hierarchical processing pipeline combines coverage simplification,
     * coastal buffering, and hierarchy grouping without throwing exceptions on multi-level datasets.
     */
    @Test
    void processWithBufferAndCoverage() throws IOException {
        Path inFile = Paths.get(TEST_RESOURCES_DIR, "frenchTestHierarchy.geojsonseq");
        GeoJSONSimplify s = new GeoJSONSimplify();
        Path destFile = s.process(inFile, 0.001, 0.005, true);
        assertTrue(Files.exists(destFile));
        assertTrue(Files.size(destFile) > 0);
        Files.deleteIfExists(destFile);
    }

    /**
     * Tests the extraction of hierarchy grouping keys from feature properties
     * (checking precedence for 'subtype', 'admin_level', 'class', and fallback defaults).
     */
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

    /**
     * Verifies the adaptive per-admin-level tolerance mapping (spec §4.1).
     */
    @Test
    void getAdminLevelTolerance() {
        JsonObject props = new JsonObject();
        props.addProperty("admin_level", "2");
        assertEquals(0.001, GeoJSONSimplify.getAdminLevelTolerance(props), 1e-9);
        props.addProperty("admin_level", "4");
        assertEquals(0.001, GeoJSONSimplify.getAdminLevelTolerance(props), 1e-9);
        props.addProperty("admin_level", "5");
        assertEquals(0.0005, GeoJSONSimplify.getAdminLevelTolerance(props), 1e-9);
        props.addProperty("admin_level", "8");
        assertEquals(0.0005, GeoJSONSimplify.getAdminLevelTolerance(props), 1e-9);
        props.addProperty("admin_level", "9");
        assertEquals(0.0001, GeoJSONSimplify.getAdminLevelTolerance(props), 1e-9);
        props.addProperty("admin_level", "11");
        assertEquals(0.0001, GeoJSONSimplify.getAdminLevelTolerance(props), 1e-9);

        assertEquals(-1, GeoJSONSimplify.getAdminLevelTolerance(null));
        assertEquals(-1, GeoJSONSimplify.getAdminLevelTolerance(new JsonObject()));

        JsonObject nonNumeric = new JsonObject();
        nonNumeric.addProperty("admin_level", "abc");
        assertEquals(-1, GeoJSONSimplify.getAdminLevelTolerance(nonNumeric));

        JsonObject outOfRange = new JsonObject();
        outOfRange.addProperty("admin_level", "1");
        assertEquals(-1, GeoJSONSimplify.getAdminLevelTolerance(outOfRange));
        outOfRange.addProperty("admin_level", "12");
        assertEquals(-1, GeoJSONSimplify.getAdminLevelTolerance(outOfRange));

        // GeoJSON.properties stores the whole feature minus geometry, so admin_level
        // is nested inside a "properties" member. This must be unwrapped.
        JsonObject nested = new JsonObject();
        nested.addProperty("type", "Feature");
        JsonObject nestedProps = new JsonObject();
        nestedProps.addProperty("name", "Ortsteil");
        nestedProps.addProperty("admin_level", "10");
        nested.add("properties", nestedProps);
        assertEquals(0.0001, GeoJSONSimplify.getAdminLevelTolerance(nested), 1e-9);
    }

    /**
     * Verifies that resolveTolerance prefers the adaptive per-level tolerance for features
     * with a known admin_level and falls back to the explicit/envelope tolerance otherwise.
     */
    @Test
    void resolveTolerance() {
        GeoJSONSimplify s = new GeoJSONSimplify();
        GeometryFactory gf = new GeometryFactory();
        Geometry rect = gf.createPolygon(new Coordinate[]{
                new Coordinate(11.0, 48.0), new Coordinate(11.01, 48.0),
                new Coordinate(11.01, 48.01), new Coordinate(11.0, 48.0)});

        JsonObject level10Props = new JsonObject();
        level10Props.addProperty("admin_level", "10");
        assertEquals(0.0001, s.resolveTolerance(new GeoJSON(rect, level10Props), 0.001), 1e-9);

        JsonObject nested = new JsonObject();
        nested.addProperty("type", "Feature");
        JsonObject nestedProps = new JsonObject();
        nestedProps.addProperty("admin_level", "8");
        nested.add("properties", nestedProps);
        assertEquals(0.0005, s.resolveTolerance(new GeoJSON(rect, nested), 0.001), 1e-9);

        GeoJSON plain = new GeoJSON(rect, new JsonObject());
        assertEquals(0.001, s.resolveTolerance(plain, 0.001), 1e-9);
        assertTrue(s.resolveTolerance(plain, 0.0) > 0);
    }

    /**
     * E2E: Verifies that the adaptive tolerance preserves small-scale border detail of
     * sub-municipal districts (admin_level 9-11) that a global 100 m tolerance would collapse.
     */
    @Test
    void adaptiveTolerancePreservesSmallDistrictDetail() {
        GeometryFactory gf = new GeometryFactory();

        int zig = 40;
        List<Coordinate> ring = new ArrayList<>();
        double baseX = 12.5, baseY = 47.9, step = 0.00005, bump = 0.0002;
        for (int i = 0; i <= zig; i++) {
            ring.add(new Coordinate(baseX + i * step, baseY + (i % 2 == 0 ? 0.0 : bump)));
        }
        for (int i = 0; i <= zig; i++) {
            ring.add(new Coordinate(baseX + (zig - i) * step, baseY + 0.0004));
        }
        ring.add(new Coordinate(baseX, baseY));
        Polygon district = gf.createPolygon(ring.toArray(new Coordinate[0]));

        JsonObject level9Props = new JsonObject();
        level9Props.addProperty("admin_level", "9");
        double adaptiveTol = GeoJSONSimplify.getAdminLevelTolerance(level9Props);

        Geometry simplifiedGlobal = TopologyPreservingSimplifier.simplify(district, 0.001);
        Geometry simplifiedAdaptive = TopologyPreservingSimplifier.simplify(district, adaptiveTol);

        int globalVerts = simplifiedGlobal.getNumPoints();
        int adaptiveVerts = simplifiedAdaptive.getNumPoints();

        assertTrue(adaptiveVerts > globalVerts, "adaptive tolerance should retain more border detail");
        assertTrue(adaptiveVerts >= zig, "adaptive tolerance should preserve the zigzag detail");
        assertTrue(globalVerts < zig, "global 100 m tolerance should collapse the zigzag detail");
    }

    /**
     * Verifies that the adaptive tolerance is applied end-to-end through process() so that
     * small-level features inside a mixed-level file are not over-simplified.
     */
    @Test
    void processAppliesAdaptiveTolerance() throws IOException {
        Path tempDir = Files.createTempDirectory("osm_tools_adaptive");
        Path input = tempDir.resolve("adaptive_test.geojsonseq");

        // Two features: a municipality (level 8) and a small district (level 10).
        String municipality = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[12.5,47.9],[12.7,47.9],[12.7,48.0],[12.5,48.0],[12.5,47.9]]]},\"properties\":{\"name\":\"Stadt\",\"admin_level\":\"8\"}}";
        // Small ~700 m x 330 m district with ~22 m zigzag detail on its southern border.
        // A global 100 m tolerance would collapse the zigzag; the adaptive 10 m tolerance keeps it.
        String district = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[12.505,47.901],[12.506,47.901],[12.506,47.9012],[12.507,47.901],[12.507,47.9012],[12.508,47.901],[12.508,47.9012],[12.509,47.901],[12.509,47.9012],[12.510,47.901],[12.510,47.9012],[12.511,47.901],[12.511,47.9012],[12.512,47.901],[12.512,47.9012],[12.513,47.901],[12.513,47.9012],[12.514,47.901],[12.514,47.904],[12.505,47.904],[12.505,47.901]]]},\"properties\":{\"name\":\"Ortsteil\",\"admin_level\":\"10\"}}";

        Files.write(input, (municipality + "\n" + district + "\n").getBytes());

        GeoJSONSimplify s = new GeoJSONSimplify();
        Path dest = s.process(input, 0.001, 0.0, true);

        List<GeoJSON> simplified = GeoJSON.streamParsedGeoJSONLines(dest).collect(java.util.stream.Collectors.toList());
        assertEquals(2, simplified.size());

        // The level-10 district (21 input points) must keep its zigzag border under the
        // adaptive 10 m tolerance; a global 100 m tolerance would collapse it to a few points.
        GeoJSON districtOut = simplified.stream()
                .filter(f -> "admin_level_10".equals(s.getHierarchyGroupKey(f.properties)))
                .findFirst()
                .get();
        assertTrue(districtOut.geometry.getNumPoints() >= 18, "level-10 zigzag detail should survive adaptive simplification");

        Files.deleteIfExists(dest);
        Files.deleteIfExists(input);
        Files.deleteIfExists(tempDir);
    }

    /**
     * Verifies CLI main execution with explicit option arguments (--input, --buffer, --tolerance).
     */
    @Test
    void mainCLI() throws Exception {
        Path inFile = Paths.get(TEST_RESOURCES_DIR, "polygon-palling.geojsonseq");
        GeoJSONSimplify.main(new String[]{"--input", inFile.toString(), "--buffer", "0.002", "--tolerance", "0.001"});
        Path destFile = Paths.get(TEST_RESOURCES_DIR, "polygon-palling.simplified.geojsonseq");
        assertTrue(Files.exists(destFile));
        Files.deleteIfExists(destFile);
    }

    /**
     * E2E Test: Verifies that coastal points of interest (POIs) located in coastal bays/water areas
     * are covered by the buffered coastal municipality polygon, while ensuring that the coastal buffer
     * does NOT intrude into the original geometry of adjacent inland municipalities.
     */
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
        Files.deleteIfExists(tempDir.resolve("coastal_test.simplified.geojsonseq.html"));
        Files.deleteIfExists(simplifiedFile);
        Files.deleteIfExists(inputGeojson);
        Files.deleteIfExists(tempDir);
    }

    /**
     * E2E Test: Verifies that CoverageSimplifier handles files containing mixed geometry types
     * (e.g., admin boundary Polygons mixed with Point nodes or LineStrings) cleanly without throwing
     * ClassCastExceptions, preserving non-polygonal features intact while simplifying polygonal coverages.
     */
    @Test
    void e2eMixedGeometryCoverageTest() throws Exception {
        Path tempDir = Files.createTempDirectory("osm_tools_mixed_e2e");
        Path inputGeojson = tempDir.resolve("mixed_test.geojsonseq");

        // Polygon A
        String polyA = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[10.0,50.0],[10.1,50.0],[10.1,50.1],[10.0,50.1],[10.0,50.0]]]},\"properties\":{\"name\":\"CityA\",\"subtype\":\"locality\"}}";
        // Polygon B
        String polyB = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[10.1,50.0],[10.2,50.0],[10.2,50.1],[10.1,50.1],[10.1,50.0]]]},\"properties\":{\"name\":\"CityB\",\"subtype\":\"locality\"}}";
        // Point feature (admin center node) mixed in the same hierarchy group
        String pointNode = "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[10.05,50.05]},\"properties\":{\"name\":\"CityA Center\",\"subtype\":\"locality\"}}";

        Files.write(inputGeojson, (polyA + "\n" + polyB + "\n" + pointNode + "\n").getBytes());

        // Run default pipeline (coverage=true) on file containing mixed Point and Polygon features
        GeoJSONSimplify.main(new String[]{inputGeojson.toString()});

        Path simplifiedFile = tempDir.resolve("mixed_test.simplified.geojsonseq");
        assertTrue(Files.exists(simplifiedFile));

        List<GeoJSON> simplifiedFeatures = GeoJSON.streamParsedGeoJSONLines(simplifiedFile).collect(java.util.stream.Collectors.toList());
        assertEquals(3, simplifiedFeatures.size());

        // Verify the Point feature was preserved without crashing CoverageSimplifier
        assertTrue(simplifiedFeatures.get(2).geometry instanceof org.locationtech.jts.geom.Point);

        Files.deleteIfExists(tempDir.resolve("mixed_test.simplified.geojsonseq.html"));
        Files.deleteIfExists(simplifiedFile);
        Files.deleteIfExists(inputGeojson);
        Files.deleteIfExists(tempDir);
    }

    /**
     * Verifies that GeoJSONSimplifyVerifier correctly validates spatial consistency,
     * feature count, geometry validity, and tag continuity on simplified outputs.
     */
    @Test
    void verifierConsistencyTest() throws Exception {
        Path inFile = Paths.get(TEST_RESOURCES_DIR, "frenchTestHierarchy.geojsonseq");
        GeoJSONSimplify s = new GeoJSONSimplify();
        Path destFile = s.process(inFile, 0.001, 0.005, true);

        GeoJSONSimplifyVerifier.VerificationResult result = GeoJSONSimplifyVerifier.verify(inFile, destFile);
        result.printSummary();

        assertTrue(result.isSuccess(), "Simplification verification should pass all spatial consistency checks");
        assertEquals(7, result.totalInputFeatures);
        assertEquals(7, result.totalOutputFeatures);
        assertEquals(0, result.invalidGeometriesCount);
        assertEquals(0, result.tagContinuityMismatches);
        assertEquals(0, result.coverageFailures);
        assertEquals(0, result.inlandOverlapViolations);

        Files.deleteIfExists(destFile);
    }

    /**
     * Verifies that the generic HTML map viewer doc/map_viewer.html exists and is valid.
     */
    @Test
    void mapViewerGeneratorTest() throws Exception {
        Path htmlFile = Paths.get("doc", "map_viewer.html");
        assertTrue(Files.exists(htmlFile));
        assertTrue(Files.size(htmlFile) > 0);

        String content = new String(Files.readAllBytes(htmlFile), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(content.contains("GeoJSON & GeoJSONSeq Map Viewer"));
        assertTrue(content.contains("leaflet.js"));
    }
}