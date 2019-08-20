package net.leberfinger.osm.poifilter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Date;
import java.util.Collection;
import java.util.List;

import org.eclipse.collections.impl.factory.Lists;
import org.junit.jupiter.api.Test;
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import com.github.filosganga.geogson.gson.GeometryAdapterFactory;
import com.github.filosganga.geogson.model.Feature;
import com.github.filosganga.geogson.model.Geometry;
import com.github.filosganga.geogson.model.Geometry.Type;
import com.github.filosganga.geogson.model.positions.Positions;
import com.github.filosganga.geogson.model.positions.SinglePosition;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import net.leberfinger.osm.poifilter.GeoJSON;

class TestGeoJSONPOI {

	private Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GeometryAdapterFactory()).create();

	private GeoJSON geoJSON = new GeoJSON();

	@Test
	void testOSMNodeToGeoJSON() {

		final CommonEntityData commonData = getCommonData();

		final double latitude = 48;
		final double longitude = 11;

		Node node = new Node(commonData, latitude, longitude);
		Collection<Tag> tags = node.getTags();
		tags.add(new Tag("key", "value"));

		String json = geoJSON.toGeoJSON(node);

		Feature feature = gson.fromJson(json, Feature.class);

		ImmutableMap<String, JsonElement> properties = feature.properties();

		assertFalse(feature.id().isPresent());
		assertTrue(properties.containsKey("nodeID"));
		assertEquals("1234", properties.get("nodeID").getAsString());

		assertTrue(properties.containsKey("key"));
		assertEquals("value", properties.get("key").getAsString());

		Geometry<?> geometry = feature.geometry();
		assertNotNull(geometry);

		assertEquals(Type.POINT, geometry.type());
		Positions positions = geometry.positions();
		SinglePosition pos = (SinglePosition) positions;
		assertEquals(48, pos.coordinates().getLat(), 0.01);
		assertEquals(11, pos.coordinates().getLon(), 0.01);
	}

	@Test
	void testOSMWayToGeoJSON() {
		CommonEntityData commonData = getCommonData();

		List<WayNode> wayNodes = Lists.mutable.empty();
		wayNodes.add(new WayNode(1, 48, 11));
		wayNodes.add(new WayNode(2, 49, 12));
		wayNodes.add(new WayNode(3, 48, 13));

		Way way = new Way(commonData, wayNodes);

		String json = geoJSON.toGeoJSON(way);
		Feature feature = gson.fromJson(json, Feature.class);

		System.out.println(json);
		System.out.println(feature);
	}

	private CommonEntityData getCommonData() {
		final int id = 1234;
		final int version = 1;
		final Date timestamp = Date.valueOf("2010-01-01");

		final OsmUser user = new OsmUser(9876, "user");
		final long changesetId = 12;

		final CommonEntityData commonData = new CommonEntityData( //
				id, version, timestamp, user, changesetId);

		return commonData;
	}

}
