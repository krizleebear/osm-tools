package net.leberfinger.osm.nominatim;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.io.ParseException;

import com.google.common.base.Stopwatch;

class PolygonCacheTest {

	private static PolygonCache polys = new PolygonCache();

	@BeforeAll
	public static void importDump() throws IOException, ParseException
	{
		Path dumpFile = Paths.get("postgisdump.txt");
		try (Reader r = Files.newBufferedReader(dumpFile, StandardCharsets.UTF_8)) {
			polys.importCache(r);
		}
		
		System.out.println(Runtime.getRuntime().totalMemory());
	}
	
	@Test
	void test() throws IOException, ParseException {
		
		// Stadtbergen, Bauernstraße, Augsburg
		System.out.println(polys.resolve(48.366231478923446, 10.844616293907167));
		
		Stopwatch stopwatch = Stopwatch.createStarted();
//		for(int i=0; i<1_000_000; i++)
//		{
//			polys.resolve(48.366231478923446, 10.844616293907167);
//		}
		long elapsedSeconds = stopwatch.elapsed(TimeUnit.SECONDS);
		System.out.println("Resolving took " + elapsedSeconds);
	}

	@Test
	void resolve()
	{
		Optional<AdminPlace> place = polys.resolve(48, 11);
		System.out.println(place);

		// Stadtbergen, Bauernstraße, Augsburg
		System.out.println(polys.resolve(48.366231478923446, 10.844616293907167));

		// Augsburg Hauptbahnhof
		System.out.println(polys.resolve(48.36541265686059, 10.885391235351564));

		// Berlin
		System.out.println(polys.resolve(52.518611, 13.408333));

		System.out.println(polys.resolve(50, 12));
	}
	
}
