package net.leberfinger.geo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FilenameUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class GeoJSONSimplify {

    private final JsonParser parser = new JsonParser();
    private final GeoJsonReader geoReader = new GeoJsonReader(new GeometryFactory());
    private final GeoJsonWriter geoWriter = new GeoJsonWriter(7);

    public GeoJSONSimplify() {
        geoWriter.setEncodeCRS(false);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new RuntimeException("Missing args: \n" +
                    "[1] path to input file (geojsonseq)");
        }

        Path inFile = Paths.get(args[0]);

        GeoJSONSimplify simplifier = new GeoJSONSimplify();
        simplifier.simplifyLines(inFile);
    }

    public void simplifyLines(Path origFile) throws IOException {
        Path destFile = getDestFile(origFile);
        try (Writer w = Files.newBufferedWriter(destFile);
             Stream<String> lines = Files.lines(origFile)) {
            lines.forEach(line -> {
                try {
                    JsonObject simplified = simplifyLine(line);
                    w.write(simplified.toString());
                    w.write("\n");
                } catch (IOException | ParseException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    protected JsonObject simplifyLine(String line) throws ParseException {
        JsonObject geoJSON = parser.parse(line).getAsJsonObject();
        simplify(geoJSON);
        return geoJSON;
    }

    protected void simplify(JsonObject json) throws ParseException {
        String geometryJSON = json.remove("geometry").toString();
        Geometry geometry = geoReader.read(geometryJSON);

        // todo: calculate distance tolerance according to geometry size
        Geometry simplified = TopologyPreservingSimplifier.simplify(geometry, 0.001);

        // it's a pity we have to first write and then parse it,
        // but the JTS IO API offers no other way.
        String simplifiedJSON = geoWriter.write(simplified);
        JsonElement simplifiedObject = parser.parse(simplifiedJSON);
        json.add("geometry", simplifiedObject);

        JsonObject properties = GeoJSONUtils.getProperties(json);
        properties.addProperty("numPoints", geometry.getNumPoints());
    }

    private static Path getDestFile(Path origFile) {
        String origFilename = origFile.getFileName().toString();
        origFilename = FilenameUtils.removeExtension(origFilename);
        String destFilename = origFilename + ".simplified.geojsonseq";
        return origFile.getParent().resolve(destFilename);
    }

}
