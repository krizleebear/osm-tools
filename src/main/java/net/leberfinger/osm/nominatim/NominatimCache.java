package net.leberfinger.osm.nominatim;

import java.io.IOException;
import java.util.Optional;

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
			this.index = this.index.add(place, place.getBoundingBox());
		});
		
		return placeFromNominatim;
	}

	public Optional<AdminPlace> searchInIndex(double lat, double lon) {
		Point point = Geometries.pointGeographic(lon, lat);
		Iterable<Entry<AdminPlace, Rectangle>> nearest = index.nearest(point, 0.9, 10);
		for (Entry<AdminPlace, Rectangle> result : nearest) {
			Rectangle boundingBox = result.geometry();
			if (boundingBox.contains(lon, lat)) {
				AdminPlace place = result.value();
				if (place.contains(lat, lon)) {
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
