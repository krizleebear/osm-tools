package net.leberfinger.osm.nominatim;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.Geometries;
import com.github.davidmoten.rtree2.geometry.Point;
import com.github.davidmoten.rtree2.geometry.Rectangle;
import com.google.gson.JsonObject;

/**
 * A cache for a local instance of nominatim. Stores all received polygons in an
 * R-Tree and uses this polygons to resolve further requests without querying
 * Nominatim.
 */
public class NominatimCache {

	private final NominatimConnection nominatim;

	private RTree<AdminPlace, Rectangle> index = RTree.star().create();
	private final GeometryFactory geoFactory = new GeometryFactory();
	private final MutableLongSet containedPlaceIDs = LongSets.mutable.empty();

	private int cacheMisses;
	private int cacheHits;

	public NominatimCache() {
		this(new NominatimConnection());
	}

	public NominatimCache(NominatimConnection nominatim) {
		this.nominatim = nominatim;
	}

	public Optional<AdminPlace> resolve(double lat, double lon) throws IOException {

		Optional<AdminPlace> placeFromIndex = searchInIndex(lat, lon);
		if (placeFromIndex.isPresent()) {
			cacheHits++;
			return placeFromIndex;
		}

		cacheMisses++;
		Optional<AdminPlace> placeFromNominatim = nominatim.askNominatim(lat, lon);
		placeFromNominatim.ifPresent((place) -> {
			
			cachePlaceIfUnknown(place);
			
		});

		return placeFromNominatim;
	}

	private void cachePlaceIfUnknown(AdminPlace place) {
		long placeID = place.getPlaceID();
		if(!containedPlaceIDs.contains(placeID))
		{
			this.index = this.index.add(place, place.getBoundingBox());
			containedPlaceIDs.add(placeID);
		}
	}

	public Optional<AdminPlace> searchInIndex(double lat, double lon) {
		Point point = Geometries.pointGeographic(lon, lat);
		Iterable<Entry<AdminPlace, Rectangle>> nearest = index.nearest(point, 0.9, 10);

		Coordinate coordinate = new Coordinate(lon, lat);
		org.locationtech.jts.geom.Point jtsPoint = geoFactory.createPoint(coordinate);

		for (Entry<AdminPlace, Rectangle> result : nearest) {
			Rectangle boundingBox = result.geometry();
			if (boundingBox.contains(lon, lat)) {
				AdminPlace place = result.value();
				if (place.covers(jtsPoint)) {
					return Optional.of(place);
				}
			}
		}

		return Optional.empty();
	}

	public String getStatistic() {
		JsonObject stats = new JsonObject();
		stats.addProperty("cacheMisses", cacheMisses);
		stats.addProperty("cacheHits", cacheHits);
		stats.addProperty("RTreeSize", index.size());

		return stats.toString();
	}
}
