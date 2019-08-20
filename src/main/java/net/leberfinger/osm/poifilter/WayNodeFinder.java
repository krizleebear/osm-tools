package net.leberfinger.osm.poifilter;

import java.util.List;

import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

public class WayNodeFinder  {

	List<Way> wayPOIs = Lists.mutable.empty();
	MutableLongSet wayNodeIDs = LongSets.mutable.empty();

	public void addWay(EntityContainer entityContainer) {
		if (entityContainer instanceof WayContainer) {
			Way way = ((WayContainer) entityContainer).getEntity();
			if (POIFilter.isAmenity(way)) {
				wayPOI(way);
			}
		}
	}

	private void wayPOI(Way way) {
		wayPOIs.add(way);

		List<WayNode> wayNodes = way.getWayNodes();
		for (WayNode wayNode : wayNodes) {
			long nodeID = wayNode.getNodeId();
			wayNodeIDs.add(nodeID);
		}
	}

	public boolean nodeIsUsedInPOI(Node n)
	{
		return wayNodeIDs.contains(n.getId());
	}
}