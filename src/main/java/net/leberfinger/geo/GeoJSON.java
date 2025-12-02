package net.leberfinger.geo;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;

public class GeoJSON {
    public final Geometry geometry;
    public final JsonObject properties;

    private final static JsonParser parser = new JsonParser();
    private final static GeoJsonReader geoReader = new GeoJsonReader(new GeometryFactory());
    private final static GeoJsonWriter geoWriter = new GeoJsonWriter(7);

    static {
        geoWriter.setEncodeCRS(false);
    }

    public GeoJSON(Geometry geometry, JsonObject properties) {
        this.geometry = geometry;
        this.properties = properties;
    }

    public static GeoJSON fromJSON(JsonObject json) {
        String geometryJSON = json.remove("geometry").toString();
        try {
            Geometry geometry = geoReader.read(geometryJSON);
            return new GeoJSON(geometry, json);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public GeoJSON withGeometry(Geometry geo) {
        return new GeoJSON(geo, properties);
    }

    public JsonObject toJSON() {
        // it's a pity we have to first write and then parse it,
        // but the JTS IO API offers no other way.
        String simplifiedJSON = geoWriter.write(geometry);
        JsonElement simplifiedObject = parser.parse(simplifiedJSON);

        properties.add("geometry", simplifiedObject);

        JsonObject copy = new JsonObject();
        copy.addProperty("type", "Feature");
        properties.entrySet().forEach(e -> {
            copy.add(e.getKey(), e.getValue());
        });

        return copy;
    }
}
