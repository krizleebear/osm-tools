package net.leberfinger.geo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.leberfinger.FileSplittingWriter;
import org.eclipse.collections.api.list.ImmutableList;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class GeoJSONSplit {

    public static ImmutableList<Path> split(Path origFile, String propertyKey, int splitFactor) throws Exception {
        try (FileSplittingWriter splittingWriter = new FileSplittingWriter(origFile, splitFactor);
             Stream<String> lines = Files.lines(origFile)) {
            lines.forEach(line -> {
                try {
                    JsonObject geoJSON = GeoJSONUtils.fromString(line);
                    JsonObject properties = GeoJSONUtils.getProperties(geoJSON);

                    JsonElement property = properties.get(propertyKey);
                    int hashCode = 0;
                    if (property != null) {
                        hashCode = property.hashCode();
                    }

                    Writer writer = splittingWriter.getWriter(hashCode);
                    writer.write(line);
                    writer.write("\n");
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            return splittingWriter.getOutFiles();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new RuntimeException("Missing args: \n" +
                    "[1] path to input file (geojson) \n" +
                    "[2] geojson property to split on, e.g. 'addr:state' \n" +
                    "[3] file count, e.g. 2");
        }

        Path inFile = Paths.get(args[0]);
        int splitFactor = Integer.parseInt(args[2]);

        GeoJSONSplit.split(inFile, args[1], splitFactor);
    }

}
