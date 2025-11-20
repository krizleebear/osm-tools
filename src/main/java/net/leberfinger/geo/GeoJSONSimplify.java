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

        long sizeBefore = Files.size(inFile);
        GeoJSONSimplify simplifier = new GeoJSONSimplify();
        Path destFile = simplifier.simplifyLines(inFile);
        long sizeSimplified = Files.size(destFile);
        double sizePercent = (double) sizeSimplified / sizeBefore * 100;

        String msg = String.format("Simplified %s saving %.0f%% space", inFile.getFileName(), 100-sizePercent);
        System.out.println(msg);
    }

    public Path simplifyLines(Path origFile) throws IOException {
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
        return destFile;
    }

    protected JsonObject simplifyLine(String line) throws ParseException {
        JsonObject geoJSON = parser.parse(line).getAsJsonObject();
        simplify(geoJSON);
        return geoJSON;
    }

    protected void simplify(JsonObject json) throws ParseException {
        String geometryJSON = json.remove("geometry").toString();
        Geometry geometry = geoReader.read(geometryJSON);


        double distanceTolerance = getDistanceTolerance(geometry);

        Geometry simplified = TopologyPreservingSimplifier.simplify(geometry, distanceTolerance);

        // it's a pity we have to first write and then parse it,
        // but the JTS IO API offers no other way.
        String simplifiedJSON = geoWriter.write(simplified);
        JsonElement simplifiedObject = parser.parse(simplifiedJSON);
        json.add("geometry", simplifiedObject);

        JsonObject properties = GeoJSONUtils.getProperties(json);
    }

    /**
     * calculate distance tolerance according to geometry size
     *
     * @param geometry
     * @return
     */
    private static double getDistanceTolerance(Geometry geometry) {
        Geometry envelope = geometry.getEnvelope();
        double envelopeArea = envelope.getArea();
        double distanceTolerance = 0.0005;
        if(envelopeArea > 0.3)
        {
            distanceTolerance = 0.001;
        }
        return distanceTolerance;
    }

    private static Path getDestFile(Path origFile) {
        String origFilename = origFile.getFileName().toString();
        origFilename = FilenameUtils.removeExtension(origFilename);
        String destFilename = origFilename + ".simplified.geojsonseq";
        return origFile.resolveSibling(destFilename);
    }

}
