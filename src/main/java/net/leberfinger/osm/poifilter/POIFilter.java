package net.leberfinger.osm.poifilter;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public class POIFilter implements Sink {

	private long entityCount = 0;
	private long wayPOICount = 0;
	private long nodePOICount = 0;
	private long nodeCount = 0;

	Instant startTime = Instant.now();
	
	WayNodeFinder wayNodes = new WayNodeFinder();

	@Override
	public void initialize(Map<String, Object> metaData) {
	}

	@Override
	public void complete() {

	}

	@Override
	public void close() {

		System.out.println();

		Instant stopTime = Instant.now();
		Duration duration = Duration.between(startTime, stopTime);
		System.out.println("Duration [s]: " + duration.getSeconds());

		long totalMemory = Runtime.getRuntime().totalMemory();
		System.out.println("Mem Use: " + totalMemory);

		System.out.println("Entities: " + entityCount);
		System.out.println("Nodes: " + nodeCount);
		System.out.println("Way POIs: " + wayPOICount);
		System.out.println("Node POIs: " + nodePOICount);
	}

	@Override
	public void process(EntityContainer entityContainer) {

		entityCount++;
		if (entityCount % 1_000_000 == 0) {
			System.out.print(".");
		}

		if (entityContainer instanceof NodeContainer) {
			Node node = (Node) entityContainer.getEntity();
			nodeCount++;

			if (isAmenity(node)) {
				nodePOI(node);
			}
		} else if (entityContainer instanceof WayContainer) {

			wayNodes.addWay(entityContainer);
//			Way way = ((WayContainer) entityContainer).getEntity();
//			if (isAmenity(way)) {
//				wayPOI(way);
//			}

		} else if (entityContainer instanceof RelationContainer) {
			// Nothing to do here
		} else {
//			System.out.println("Unknown Entity!");
		}

	}

	private void nodePOI(Node node) {

		nodePOICount++;

	}

	public static boolean isAmenity(Entity entity) {

		Collection<Tag> tags = entity.getTags();
		for (Tag myTag : tags) {
			if ("amenity".equals(myTag.getKey())) {
				return true;
			}
		}
		return false;
	}
}
