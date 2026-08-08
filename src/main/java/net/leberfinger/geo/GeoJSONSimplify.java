package net.leberfinger.geo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.collections.api.list.primitive.MutableDoubleList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.DoubleLists;
import org.locationtech.jts.coverage.CoverageSimplifier;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class GeoJSONSimplify {

    private final JsonParser parser = new JsonParser();
    private final GeoJsonReader geoReader = new GeoJsonReader(new GeometryFactory());
    private final GeoJsonWriter geoWriter = new GeoJsonWriter(7);

    public GeoJSONSimplify() {
        geoWriter.setEncodeCRS(false);
    }

    public static void main(String[] args) throws Exception {
        OptionParser parser = new OptionParser();

        OptionSpec<String> inputOpt = parser.acceptsAll(Arrays.asList("i", "input"), "input geojsonseq file")
                .withRequiredArg().ofType(String.class);

        OptionSpec<Double> toleranceOpt = parser.acceptsAll(Arrays.asList("t", "tolerance"), "distance tolerance for simplification")
                .withOptionalArg().ofType(Double.class).defaultsTo(0.001);

        OptionSpec<Double> bufferOpt = parser.acceptsAll(Arrays.asList("b", "buffer"), "coastal outward buffer distance in degrees")
                .withOptionalArg().ofType(Double.class).defaultsTo(0.01);

        OptionSpec<Boolean> coverageOpt = parser.acceptsAll(Arrays.asList("c", "coverage"), "use coverage simplification")
                .withOptionalArg().ofType(Boolean.class).defaultsTo(true);

        parser.nonOptions("input geojsonseq file").ofType(String.class);

        OptionSet options;
        try {
            options = parser.parse(args);
        } catch (OptionException e) {
            System.err.println("Error parsing options: " + e.getMessage());
            parser.printHelpOn(System.err);
            System.exit(1);
            return;
        }

        String inputFilePath = null;
        if (options.has(inputOpt)) {
            inputFilePath = options.valueOf(inputOpt);
        } else if (!options.nonOptionArguments().isEmpty()) {
            inputFilePath = options.nonOptionArguments().get(0).toString();
        }

        if (inputFilePath == null) {
            System.err.println("Missing required input file.");
            parser.printHelpOn(System.err);
            System.exit(1);
            return;
        }

        Path inFile = Paths.get(inputFilePath);
        double tolerance = options.valueOf(toleranceOpt);
        double bufferDistance = options.valueOf(bufferOpt);
        boolean useCoverage = options.valueOf(coverageOpt);

        long sizeBefore = Files.size(inFile);
        GeoJSONSimplify simplifier = new GeoJSONSimplify();
        Path destFile = simplifier.process(inFile, tolerance, bufferDistance, useCoverage);
    }

    /**
     * Process an input GeoJSONSeq file with configurable simplification tolerance,
     * optional coverage simplification, and optional coastal buffering.
     */
    public Path process(Path inFile, double distanceTolerance, double bufferDistance, boolean useCoverage) throws IOException {
        long sizeBefore = Files.size(inFile);
        List<GeoJSON> allFeatures = Lists.mutable.empty();
        try (Stream<GeoJSON> stream = GeoJSON.streamParsedGeoJSONLines(inFile)) {
            stream.forEach(allFeatures::add);
        }

        int totalFeatures = allFeatures.size();
        int polygonalFeatures = 0;
        int nonPolygonalFeatures = 0;

        for (GeoJSON f : allFeatures) {
            if (f.geometry instanceof org.locationtech.jts.geom.Polygonal && !f.geometry.isEmpty()) {
                polygonalFeatures++;
            } else {
                nonPolygonalFeatures++;
            }
        }

        // Group features by hierarchy level to prevent CoverageSimplifier errors between overlapping levels
        Map<String, List<GeoJSON>> groups = new LinkedHashMap<>();
        for (GeoJSON feature : allFeatures) {
            String key = getHierarchyGroupKey(feature.properties);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(feature);
        }

        int coverageSuccessGroups = 0;
        int coverageFallbackGroups = 0;

        List<GeoJSON> processedFeatures = new ArrayList<>();

        for (List<GeoJSON> group : groups.values()) {
            boolean[] usedFallback = new boolean[]{false};
            List<Geometry> simplifiedGeometries = simplifyGroup(group, distanceTolerance, useCoverage, usedFallback);
            if (useCoverage && group.size() > 1) {
                if (usedFallback[0]) {
                    coverageFallbackGroups++;
                } else {
                    coverageSuccessGroups++;
                }
            }

            List<Geometry> bufferedGeometries = bufferGroup(simplifiedGeometries, bufferDistance);

            for (int i = 0; i < group.size(); i++) {
                GeoJSON original = group.get(i);
                GeoJSON processed = new GeoJSON(bufferedGeometries.get(i), original.properties);
                processedFeatures.add(processed);
            }
        }

        Path destFile = getDestFile(inFile);
        try (BufferedWriter bw = Files.newBufferedWriter(destFile)) {
            for (GeoJSON feature : processedFeatures) {
                bw.write(feature.toJSON().toString());
                bw.write('\n');
            }
        }

        long sizeAfter = Files.size(destFile);
        double spaceSavedPercent = sizeBefore > 0 ? (1.0 - (double) sizeAfter / sizeBefore) * 100 : 0;

        printExecutionSummary(inFile.getFileName().toString(), totalFeatures, polygonalFeatures, nonPolygonalFeatures,
                groups, coverageSuccessGroups, coverageFallbackGroups, bufferDistance, sizeBefore, sizeAfter, spaceSavedPercent);

        return destFile;
    }

    private void printExecutionSummary(String filename, int totalFeatures, int polygonalFeatures, int nonPolygonalFeatures,
                                       Map<String, List<GeoJSON>> groups, int coverageSuccess, int coverageFallback,
                                       double bufferDistance, long sizeBefore, long sizeAfter, double spaceSavedPercent) {
        StringBuilder groupStr = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, List<GeoJSON>> entry : groups.entrySet()) {
            if (count > 0) groupStr.append(", ");
            groupStr.append(entry.getKey()).append(": ").append(entry.getValue().size());
            count++;
        }

        System.out.println("============================================================");
        System.out.println(" GeoJSONSimplify Execution Summary");
        System.out.println(" File:               " + filename);
        System.out.println(" Total Features:     " + String.format(java.util.Locale.ROOT, "%,d", totalFeatures) +
                String.format(java.util.Locale.ROOT, " (%,d Polygonal, %,d Non-Polygonal)", polygonalFeatures, nonPolygonalFeatures));
        System.out.println(" Hierarchy Groups:   " + groups.size() + " (" + groupStr + ")");
        System.out.println(" Simplification:     Coverage Mode (" + coverageSuccess + " succeeded, " + coverageFallback + " fallbacks)");
        System.out.println(" Coastal Buffer:     " + (bufferDistance > 0 ? String.format(java.util.Locale.ROOT, "%.4f degrees (~%.1f km)", bufferDistance, bufferDistance * 111.0) : "Disabled"));
        System.out.println(" File Size:          " + String.format(java.util.Locale.ROOT, "%,d -> %,d bytes (%.1f%% saved)", sizeBefore, sizeAfter, spaceSavedPercent));
        System.out.println("============================================================");
    }

    private List<Geometry> simplifyGroup(List<GeoJSON> group, double distanceTolerance, boolean useCoverage, boolean[] usedFallback) {
        List<Geometry> geoms = new ArrayList<>();
        for (GeoJSON f : group) {
            geoms.add(f.geometry);
        }

        if (useCoverage && group.size() > 1) {
            List<Integer> polygonIndices = new ArrayList<>();
            List<Geometry> polygons = new ArrayList<>();
            for (int i = 0; i < geoms.size(); i++) {
                Geometry g = geoms.get(i);
                if (g instanceof org.locationtech.jts.geom.Polygonal && !g.isEmpty()) {
                    polygonIndices.add(i);
                    polygons.add(g);
                }
            }

            if (polygons.size() > 1) {
                try {
                    double[] tolerances = new double[polygons.size()];
                    Arrays.fill(tolerances, distanceTolerance);
                    CoverageSimplifier simplifier = new CoverageSimplifier(polygons.toArray(new Geometry[0]));
                    Geometry[] simplifiedPolygons = simplifier.simplify(tolerances);

                    List<Geometry> result = new ArrayList<>(geoms);
                    for (int i = 0; i < polygonIndices.size(); i++) {
                        result.set(polygonIndices.get(i), simplifiedPolygons[i]);
                    }
                    for (int i = 0; i < geoms.size(); i++) {
                        if (!(geoms.get(i) instanceof org.locationtech.jts.geom.Polygonal)) {
                            double tol = distanceTolerance > 0 ? distanceTolerance : getDistanceTolerance(geoms.get(i));
                            result.set(i, TopologyPreservingSimplifier.simplify(geoms.get(i), tol));
                        }
                    }
                    return result;
                } catch (Exception e) {
                    if (usedFallback != null && usedFallback.length > 0) {
                        usedFallback[0] = true;
                    }
                    System.err.println("Warning: CoverageSimplifier failed on group, falling back to individual simplification: " + e.getMessage());
                }
            }
        }

        // Individual topology preserving simplification
        List<Geometry> result = new ArrayList<>();
        for (Geometry g : geoms) {
            double tol = distanceTolerance > 0 ? distanceTolerance : getDistanceTolerance(g);
            result.add(TopologyPreservingSimplifier.simplify(g, tol));
        }
        return result;
    }

    private List<Geometry> bufferGroup(List<Geometry> geometries, double bufferDistance) {
        if (bufferDistance <= 0.0) {
            return geometries;
        }

        // Build spatial index for neighbor subtraction within the same hierarchy level
        STRtree index = new STRtree();
        for (int i = 0; i < geometries.size(); i++) {
            if (geometries.get(i) != null && !geometries.get(i).isEmpty()) {
                index.insert(geometries.get(i).getEnvelopeInternal(), geometries.get(i));
            }
        }

        List<Geometry> bufferedResult = new ArrayList<>();
        for (Geometry geom : geometries) {
            if (!(geom instanceof org.locationtech.jts.geom.Polygonal) || geom.isEmpty()) {
                bufferedResult.add(geom);
                continue;
            }

            Geometry buffered = geom.buffer(bufferDistance);

            @SuppressWarnings("unchecked")
            List<Geometry> candidates = index.query(buffered.getEnvelopeInternal());
            for (Geometry neighbor : candidates) {
                if (neighbor == geom || !(neighbor instanceof org.locationtech.jts.geom.Polygonal)) {
                    continue;
                }
                try {
                    buffered = buffered.difference(neighbor);
                } catch (Exception e) {
                    try {
                        buffered = buffered.buffer(0).difference(neighbor.buffer(0));
                    } catch (Exception ex) {
                        // ignore if subtraction fails
                    }
                }
            }
            bufferedResult.add(buffered);
        }
        return bufferedResult;
    }

    protected String getHierarchyGroupKey(JsonObject properties) {
        if (properties == null) {
            return "default";
        }
        if (properties.has("subtype") && !properties.get("subtype").isJsonNull()) {
            return properties.get("subtype").getAsString();
        }
        if (properties.has("admin_level") && !properties.get("admin_level").isJsonNull()) {
            return "admin_level_" + properties.get("admin_level").getAsString();
        }
        if (properties.has("class") && !properties.get("class").isJsonNull()) {
            return properties.get("class").getAsString();
        }
        return "default";
    }

    public Stream<GeoJSON> streamParsedGeoJSONLines(Path inFile) throws IOException {
        return GeoJSON.streamParsedGeoJSONLines(inFile);
    }

    public Path simplifyLines(Path origFile) throws IOException {
        return process(origFile, 0.0, 0.0, false);
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

        String simplifiedJSON = geoWriter.write(simplified);
        JsonElement simplifiedObject = parser.parse(simplifiedJSON);
        json.add("geometry", simplifiedObject);
    }

    private static double getDistanceTolerance(Geometry geometry) {
        Geometry envelope = geometry.getEnvelope();
        double envelopeArea = envelope.getArea();
        double distanceTolerance = 0.0005;
        if (envelopeArea > 0.3) {
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

    public void simplifyCoverage(Path inFile, double distanceTolerance) throws IOException {
        process(inFile, distanceTolerance, 0.0, true);
    }
}

