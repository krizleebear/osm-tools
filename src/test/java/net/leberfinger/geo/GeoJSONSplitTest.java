package net.leberfinger.geo;

import org.eclipse.collections.api.list.ImmutableList;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class GeoJSONSplitTest {

    @Test
    public void run() throws Exception {
        Path inFile = Paths.get("testplaces.geojsonseq");
        ImmutableList<Path> outFiles = GeoJSONSplit.split(inFile, "place", 3);
        assertEquals(3, outFiles.size());

        outFiles.forEach(path -> {
            assertTrue(Files.exists(path));
            try {
                Files.delete(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}