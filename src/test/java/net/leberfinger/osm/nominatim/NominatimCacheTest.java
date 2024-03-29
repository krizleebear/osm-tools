package net.leberfinger.osm.nominatim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisabledOnOs(OS.LINUX)
@Disabled
class NominatimCacheTest {

	@Test
	void testEmptyCache() throws IOException {
		NominatimCache cache = new NominatimCache();
		Optional<AdminPlace> place = cache.resolve(48, 11);
		
		assertEquals(1, cache.getCacheSize());
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
	
	@Test
	void resolveCountryNotCity() throws IOException {
		
		//http://192.168.43.201:7070/reverse.php?format=jsonv2&lat=50.6319056&lon=6.2725377&zoom=18
		
		NominatimCache cache = new NominatimCache();
		Optional<AdminPlace> place = cache.resolve(50.6319056, 6.2725377);
		assertEquals(4, place.get().getPlaceRank());

		assertEquals(0, cache.getCacheSize());
	}
}
