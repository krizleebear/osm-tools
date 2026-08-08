package net.leberfinger.geo;

import com.google.gson.JsonObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Verification utility that compares input GeoJSON files against simplified/buffered output files
 * to ensure topological invariants, tag continuity, geometry validity, and coverage preservation.
 */
public class GeoJSONSimplifyVerifier {

    public static class VerificationResult {
        public int totalInputFeatures;
        public int totalOutputFeatures;
        public int validGeometriesCount;
        public int invalidGeometriesCount;
        public int tagContinuityMatches;
        public int tagContinuityMismatches;
        public int coverageFailures;
        public double averageAreaOverlapPercent;
        public int inlandOverlapViolations;

        public boolean isSuccess() {
            return totalInputFeatures == totalOutputFeatures
                    && invalidGeometriesCount == 0
                    && tagContinuityMismatches == 0
                    && coverageFailures == 0
                    && inlandOverlapViolations == 0;
        }

        public void printSummary() {
            System.out.println("============================================================");
            System.out.println(" GeoJSON Simplification Consistency Verification Report");
            System.out.println(" Feature Count Check:  " + (totalInputFeatures == totalOutputFeatures ? "PASSED" : "FAILED") +
                    String.format(Locale.ROOT, " (%,d input, %,d output)", totalInputFeatures, totalOutputFeatures));
            System.out.println(" Geometry Validity:    " + (invalidGeometriesCount == 0 ? "PASSED" : "FAILED") +
                    String.format(Locale.ROOT, " (%,d valid, %,d invalid)", validGeometriesCount, invalidGeometriesCount));
            System.out.println(" Tag Continuity:       " + (tagContinuityMismatches == 0 ? "PASSED" : "FAILED") +
                    String.format(Locale.ROOT, " (%,d matches, %,d missing)", tagContinuityMatches, tagContinuityMismatches));
            System.out.println(" Coverage Preservation:" + (coverageFailures == 0 ? "PASSED" : "FAILED") +
                    String.format(Locale.ROOT, " (Avg Overlap: %.2f%%, Failures: %,d)", averageAreaOverlapPercent, coverageFailures));
            System.out.println(" Inland Non-Overlap:   " + (inlandOverlapViolations == 0 ? "PASSED" : "FAILED") +
                    String.format(Locale.ROOT, " (%,d violations)", inlandOverlapViolations));
            System.out.println(" Overall Verdict:      " + (isSuccess() ? "ALL CHECKS PASSED" : "VERIFICATION FAILED"));
            System.out.println("============================================================");
        }
    }

    public static VerificationResult verify(Path origFile, Path simplifiedFile) throws IOException {
        VerificationResult result = new VerificationResult();

        List<GeoJSON> inputFeatures = new ArrayList<>();
        try (Stream<GeoJSON> stream = GeoJSON.streamParsedGeoJSONLines(origFile)) {
            stream.forEach(inputFeatures::add);
        }
        result.totalInputFeatures = inputFeatures.size();

        List<GeoJSON> outputFeatures = new ArrayList<>();
        try (Stream<GeoJSON> stream = GeoJSON.streamParsedGeoJSONLines(simplifiedFile)) {
            stream.forEach(outputFeatures::add);
        }
        result.totalOutputFeatures = outputFeatures.size();

        if (inputFeatures.size() != outputFeatures.size()) {
            return result;
        }

        Map<String, GeoJSON> outputMap = new HashMap<>();
        for (int i = 0; i < outputFeatures.size(); i++) {
            GeoJSON out = outputFeatures.get(i);
            String key = getFeatureKey(out, i);
            outputMap.putIfAbsent(key, out);
        }

        double totalOverlapPercentSum = 0.0;
        int validOverlapCount = 0;

        for (int i = 0; i < inputFeatures.size(); i++) {
            GeoJSON in = inputFeatures.get(i);
            String key = getFeatureKey(in, i);
            GeoJSON out = outputMap.get(key);
            if (out == null && i < outputFeatures.size()) {
                out = outputFeatures.get(i);
            }
            if (out == null) continue;

            // 1. Geometry validity
            if (out.geometry != null && out.geometry.isValid()) {
                result.validGeometriesCount++;
            } else {
                result.invalidGeometriesCount++;
            }

            // 2. Tag continuity check
            if (verifyTags(in.properties, out.properties)) {
                result.tagContinuityMatches++;
            } else {
                result.tagContinuityMismatches++;
            }

            // 3. Spatial coverage check (input geometry must be covered by simplified/buffered output)
            if (in.geometry != null && out.geometry != null && in.geometry instanceof org.locationtech.jts.geom.Polygonal) {
                double inArea = in.geometry.getArea();
                if (inArea > 0) {
                    try {
                        Geometry intersection = in.geometry.intersection(out.geometry);
                        double overlapPercent = (intersection.getArea() / inArea) * 100.0;
                        totalOverlapPercentSum += Math.min(100.0, overlapPercent);
                        validOverlapCount++;

                        if (overlapPercent < 95.0) {
                            result.coverageFailures++;
                        }
                    } catch (Exception e) {
                        result.coverageFailures++;
                    }
                }
            }
        }

        result.averageAreaOverlapPercent = validOverlapCount > 0 ? totalOverlapPercentSum / validOverlapCount : 100.0;

        // 4. Inland non-overlap verification per hierarchy group
        result.inlandOverlapViolations = checkInlandOverlaps(inputFeatures, outputFeatures);

        return result;
    }

    private static boolean verifyTags(JsonObject inProps, JsonObject outProps) {
        if (inProps == null || outProps == null) {
            return inProps == outProps;
        }
        JsonObject inP = inProps.has("properties") && inProps.get("properties").isJsonObject() ? inProps.getAsJsonObject("properties") : inProps;
        JsonObject outP = outProps.has("properties") && outProps.get("properties").isJsonObject() ? outProps.getAsJsonObject("properties") : outProps;

        String[] requiredTags = new String[]{"@id", "name", "subtype", "admin_level", "ISO3166-1", "ISO3166-2"};
        for (String tag : requiredTags) {
            if (inP.has(tag) && !inP.get(tag).isJsonNull()) {
                if (!outP.has(tag) || !outP.get(tag).equals(inP.get(tag))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static int checkInlandOverlaps(List<GeoJSON> inputFeatures, List<GeoJSON> outputFeatures) {
        Map<String, List<Integer>> groupedIndices = new LinkedHashMap<>();
        for (int i = 0; i < inputFeatures.size(); i++) {
            GeoJSON f = inputFeatures.get(i);
            if (f.geometry instanceof org.locationtech.jts.geom.Polygonal && !f.geometry.isEmpty()) {
                String key = getGroupKey(f.properties);
                groupedIndices.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
            }
        }

        int violations = 0;
        for (List<Integer> indices : groupedIndices.values()) {
            if (indices.size() <= 1) continue;

            STRtree inputIndex = new STRtree();
            for (int idx : indices) {
                Geometry g = inputFeatures.get(idx).geometry;
                inputIndex.insert(g.getEnvelopeInternal(), idx);
            }

            for (int idx1 : indices) {
                Geometry outGeom = outputFeatures.get(idx1).geometry;
                Geometry inGeom1 = inputFeatures.get(idx1).geometry;
                if (outGeom == null || inGeom1 == null) continue;

                @SuppressWarnings("unchecked")
                List<Integer> candidateIndices = inputIndex.query(outGeom.getEnvelopeInternal());
                for (int idx2 : candidateIndices) {
                    if (idx1 == idx2) continue;
                    Geometry inGeom2 = inputFeatures.get(idx2).geometry;
                    if (inGeom2 == null) continue;

                    // If original input neighbor 2 shares border with input 1 (no initial area overlap):
                    double origOverlap = inGeom1.intersects(inGeom2) ? inGeom1.intersection(inGeom2).getArea() : 0.0;
                    if (origOverlap < 1e-6) {
                        try {
                            Geometry intrusion = outGeom.intersection(inGeom2);
                            // Flag intrusion if buffered polygon significantly intrudes into inland neighbor (> 0.001 deg2 ~ 1 km2)
                            if (intrusion != null && !intrusion.isEmpty() && intrusion.getArea() > 1e-3) {
                                violations++;
                            }
                        } catch (Exception e) {
                            // ignore topology exception
                        }
                    }
                }
            }
        }
        return violations;
    }

    private static String getGroupKey(JsonObject json) {
        if (json == null) return "default";
        JsonObject props = json.has("properties") && json.get("properties").isJsonObject() ? json.getAsJsonObject("properties") : json;
        if (props.has("subtype") && !props.get("subtype").isJsonNull()) return props.get("subtype").getAsString();
        if (props.has("admin_level") && !props.get("admin_level").isJsonNull()) return "admin_level_" + props.get("admin_level").getAsString();
        return "default";
    }

    private static String getFeatureKey(GeoJSON f, int index) {
        if (f == null || f.properties == null) return "index_" + index;
        JsonObject props = f.properties.has("properties") && f.properties.get("properties").isJsonObject()
                ? f.properties.getAsJsonObject("properties") : f.properties;
        if (props.has("@id") && !props.get("@id").isJsonNull()) return props.get("@id").getAsString();
        if (props.has("id") && !props.get("id").isJsonNull()) return props.get("id").getAsString();
        String name = props.has("name") && !props.get("name").isJsonNull() ? props.get("name").getAsString() : "";
        String admin = props.has("admin_level") && !props.get("admin_level").isJsonNull() ? props.get("admin_level").getAsString() : "";
        String subtype = props.has("subtype") && !props.get("subtype").isJsonNull() ? props.get("subtype").getAsString() : "";
        return name + "_" + admin + "_" + subtype;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java net.leberfinger.geo.GeoJSONSimplifyVerifier <inputFile.geojsonseq> <outputFile.geojsonseq>");
            System.exit(1);
        }
        try {
            VerificationResult result = verify(Path.of(args[0]), Path.of(args[1]));
            result.printSummary();
            if (!result.isSuccess()) {
                System.exit(1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
