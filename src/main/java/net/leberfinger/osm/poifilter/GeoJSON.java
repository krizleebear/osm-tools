package net.leberfinger.osm.poifilter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import com.github.filosganga.geogson.gson.GeometryAdapterFactory;
import com.github.filosganga.geogson.model.Feature;
import com.github.filosganga.geogson.model.LineString;
import com.github.filosganga.geogson.model.Point;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class GeoJSON {

	private final Gson gson;

	public GeoJSON() {
		GsonBuilder gsonBuilder = new GsonBuilder()//
				.registerTypeAdapterFactory(new GeometryAdapterFactory());

		gson = gsonBuilder.create();
	}

	public String toGeoJSON(Node poi) {

		Point point = Point.from(poi.getLongitude(), poi.getLatitude());

		final Collection<Tag> tags = poi.getTags();
		tags.add(new Tag("osmType", "Node"));
		tags.add(new Tag("nodeID", Long.toString(poi.getId())));
		ImmutableMap<String, JsonElement> properties = tagsAsMap(tags);

		Feature feature = Feature.of(point) //
				// do not use ID, as OSM IDs are only unique within object category
				.withProperties(properties);

		return gson.toJson(feature);
	}

	private static ImmutableMap<String, JsonElement> tagsAsMap(Collection<Tag> tags) {
		Builder<String, JsonElement> map = ImmutableMap.<String, JsonElement>builder();
		for (Tag tag : tags) {
			map.put(tag.getKey(), new JsonPrimitive(tag.getValue()));
		}

		return map.build();
	}

	public String toGeoJSON(Way poi) {

		List<Point> points = toPoints(poi.getWayNodes());
		LineString geometry = LineString.of(points);

		final Collection<Tag> tags = poi.getTags();
		tags.add(new Tag("osmType", "Way"));
		tags.add(new Tag("wayID", Long.toString(poi.getId())));

		Feature feature = Feature.of(geometry) //
				.withProperties(tagsAsMap(tags));

		return gson.toJson(feature);
	}

	private static Point wayNodeToPoint(WayNode node) {
		//note: GeoJSON has lon/lat order of coordinates
		return Point.from(node.getLongitude(), node.getLatitude());
	}

	private static List<Point> toPoints(List<WayNode> wayNodes) {

		Stream<Point> points = wayNodes.stream().map(GeoJSON::wayNodeToPoint);

		return points.collect(Collectors.toList());
	}

	public void writeToLineDelimitedGeoJSON(BufferedWriter bw, Way way) {
		try {
			bw.write(toGeoJSON(way));
			bw.newLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void writeToLineDelimitedGeoJSON(BufferedWriter bw, Node node) {
		try {
			bw.write(toGeoJSON(node));
			bw.newLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
