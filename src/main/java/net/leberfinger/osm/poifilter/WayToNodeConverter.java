package net.leberfinger.osm.poifilter;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.collections.api.map.primitive.MutableLongDoubleMap;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.LongDoubleMaps;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.CommonEntityData;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public class WayToNodeConverter implements Sink {

	private MutableLongSet wayNodeIDs;
	private MutableLongDoubleMap latitudes = LongDoubleMaps.mutable.empty();
	private MutableLongDoubleMap longitudes = LongDoubleMaps.mutable.empty();
	private List<Way> wayPOIs;
	
	private long entityCount;

	public WayToNodeConverter(WayNodeFinder wayNodes) {
		wayNodeIDs = wayNodes.wayNodeIDs;
		wayPOIs = wayNodes.wayPOIs;
	}

	public Stream<Way> waysWithLocation()
	{
		return wayPOIs.stream().map(way -> withLocation(way));
	}
	
	private Way withLocation(Way way) {

		List<WayNode> withLocation = Lists.mutable.empty();
		
		//TODO: calculate center or something fancy
		List<WayNode> wayNodes = way.getWayNodes();
		for(WayNode node : wayNodes)
		{
			double lat = latitudes.get(node.getNodeId());
			double lon = longitudes.get(node.getNodeId());
			
			withLocation.add(new WayNode(node.getNodeId(), lat, lon));
		}
		
		CommonEntityData commonData = new CommonEntityData(way.getId(), way.getVersion(), way.getTimestamp(), way.getUser(), way.getChangesetId(), way.getTags());
		return new Way(commonData, withLocation);
	}

	@Override
	public void initialize(Map<String, Object> metaData) {
	}

	@Override
	public void complete() {
		System.out.println();
	}

	@Override
	public void close() {
	}

	@Override
	public void process(EntityContainer entityContainer) {
		
		entityCount++;
		if (entityCount % 1_000_000 == 0) {
			System.out.print(".");
		}
		
		if (entityContainer instanceof NodeContainer) {
			Node node = (Node) entityContainer.getEntity();
			long id = node.getId();
			if (wayNodeIDs.contains(id)) {
				latitudes.put(id, node.getLatitude());
				longitudes.put(id, node.getLongitude());
			}
		}
	}

}
