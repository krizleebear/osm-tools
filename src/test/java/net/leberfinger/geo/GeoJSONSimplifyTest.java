package net.leberfinger.geo;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Disabled;
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
        s.simplifyLines(inFile);
    }

    @Test
    void simplifyCoverage() throws IOException {
        //Path inFile = Paths.get( "countries-full.geojsonseq");
        Path inFile = Paths.get( TEST_RESOURCES_DIR, "frenchTestHierarchy.geojsonseq");
        GeoJSONSimplify s = new GeoJSONSimplify();
        s.simplifyCoverage(inFile, 0.01);
    }

}