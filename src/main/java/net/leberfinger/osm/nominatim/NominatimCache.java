package net.leberfinger.osm.nominatim;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.quadtree.Quadtree;

import com.google.gson.JsonObject;

/**
 * A cache for a local instance of nominatim. Stores all received polygons in a
 * Quad-Tree index and uses this polygons to resolve further requests without
 * querying Nominatim.
 */
public class NominatimCache implements IAdminResolver {

	private final NominatimConnection nominatim;
	private final Quadtree index = new Quadtree();

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

	@Override
	public Optional<AdminPlace> resolve(double lat, double lon) throws IOException {

		//TODO: find a way to skip coordinates outside of available DB
		
		Optional<AdminPlace> placeFromIndex = searchInIndex(lat, lon);
		if (placeFromIndex.isPresent()) {
			cacheHits++;
			return placeFromIndex;
		}

		cacheMisses++;
		Optional<AdminPlace> placeFromNominatim = nominatim.resolve(lat, lon);
		placeFromNominatim.ifPresent((place) -> {

			cachePlaceIfUnknown(place);

		});

		return placeFromNominatim;
	}

	private void cachePlaceIfUnknown(AdminPlace place) {
		
		// don't cache places that are overly huge, e.g. states or countries
		if (place.getPlaceRank() < 15) {
			return;
		}
		
		long placeID = place.getPlaceID();
		if (!containedPlaceIDs.contains(placeID)) {
			Envelope envelope = place.getGeometry().getEnvelopeInternal();
			index.insert(envelope, place);
			containedPlaceIDs.add(placeID);
		}
	}

	public Optional<AdminPlace> searchInIndex(double lat, double lon) {
		Coordinate coordinate = new Coordinate(lon, lat);
		Point jtsPoint = geoFactory.createPoint(coordinate);
		final Envelope pointEnvelope = jtsPoint.getEnvelopeInternal();

		@SuppressWarnings("unchecked")
		List<AdminPlace> result = index.query(pointEnvelope);

		for (AdminPlace place : result) {
			if (place.covers(jtsPoint)) {
				return Optional.of(place);
			}
		}

		return Optional.empty();
	}

	public String getStatistic() {
		JsonObject stats = new JsonObject();
		stats.addProperty("cacheMisses", cacheMisses);
		stats.addProperty("cacheHits", cacheHits);
		stats.addProperty("IndexSize", index.size());

		return stats.toString();
	}
	
	public int getCacheSize()
	{
		return index.size();
	}
}
