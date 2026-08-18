package net.leberfinger.geo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scale and memory stress test that verifies GeoJSONSimplify can process
 * massive, multi-level hierarchy datasets (10,000+ features) under restricted
 * memory without throwing OutOfMemoryError or dropping features.
 */
class ScaleStressTest {

    @Test
    void processLargeHierarchyUnderHeapPressure(@TempDir Path tempDir) throws IOException {
        Path testFile = tempDir.resolve("scale_stress_test.geojsonseq");
        int totalFeatures = 10_000;

        // Generate 10,000 synthetic multi-level features:
        // - Levels 2, 4 (macro boundaries)
        // - Levels 6, 8 (counties and municipalities)
        // - Levels 9, 10, 11 (dense sub-municipalities, quarters, and local neighborhoods)
        try (BufferedWriter bw = Files.newBufferedWriter(testFile)) {
            for (int i = 1; i <= totalFeatures; i++) {
                int level;
                if (i <= 10) {
                    level = (i % 2 == 0) ? 2 : 4;
                } else if (i <= 200) {
                    level = 6;
                } else if (i <= 5_000) {
                    level = 8;
                } else if (i <= 8_000) {
                    level = 9;
                } else if (i <= 9_500) {
                    level = 10;
                } else {
                    level = 11;
                }

                double baseLon = 2.0 + (i % 100) * 0.05;
                double baseLat = 46.0 + (i / 100) * 0.05;
                double delta = 0.01;

                String line = String.format(Locale.ROOT,
                        "{\"type\":\"Feature\",\"properties\":{\"@id\":%d,\"name\":\"Feature_%d\",\"admin_level\":\"%d\",\"wikidata\":\"Q%d\"}," +
                                "\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[%.5f,%.5f],[%.5f,%.5f],[%.5f,%.5f],[%.5f,%.5f],[%.5f,%.5f]]]}}",
                        i, i, level, 100000 + i,
                        baseLon, baseLat,
                        baseLon + delta, baseLat,
                        baseLon + delta, baseLat + delta,
                        baseLon, baseLat + delta,
                        baseLon, baseLat);

                bw.write(line);
                bw.write('\n');
            }
        }

        assertTrue(Files.exists(testFile));
        assertEquals(totalFeatures, Files.lines(testFile).count());

        GeoJSONSimplify simplifier = new GeoJSONSimplify();
        long startTime = System.currentTimeMillis();

        // Process with coverage simplification enabled
        Path destFile = simplifier.process(testFile, 0.001, 0.0, true);

        long durationMs = System.currentTimeMillis() - startTime;
        assertTrue(Files.exists(destFile));
        assertEquals(totalFeatures, Files.lines(destFile).count(), "Feature count invariant must be strictly preserved.");
        assertTrue(durationMs < 15_000, "10,000 features should process within 15 seconds (took " + durationMs + " ms).");

        Files.deleteIfExists(destFile);
        Files.deleteIfExists(testFile);
    }
}
