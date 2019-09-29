package net.leberfinger.osm.nominatim;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class NominatimCacheTest {

	@Test
	void testEmptyCache() throws IOException {
		NominatimCache cache = new NominatimCache();
		Optional<AdminPlace> place = cache.resolve(48, 11);
		
		assertTrue(place.isPresent());
	}

	@Test
	void testFilledCache() throws IOException {

		NominatimCache cache = new NominatimCache();
		Optional<AdminPlace> place = cache.resolve(48, 11);
		Optional<AdminPlace> place2 = cache.resolve(48, 11);

		assertSame(place.get(), place2.get());
	}

	@Test 
	void resolveOutsideOfMap() throws IOException
	{
		NominatimCache cache = new NominatimCache();
		Optional<AdminPlace> place = cache.resolve(0, 0);

		assertEquals(Optional.empty(), place);
	}
}
