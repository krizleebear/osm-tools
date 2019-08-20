package net.leberfinger.osm.poifilter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import crosby.binary.osmosis.OsmosisReader;

public class Extract {

	public static void main(String[] args) throws IOException {

//		File inputFile = new File("germany-latest.osm.pbf");
//		File inputFile = new File("liechtenstein-latest.osm.pbf");
		File inputFile = new File("/Users/krizleebear/Downloads", "oberbayern-latest.osm.pbf");

		long fileSize = Files.size(inputFile.toPath());
		String msg = String.format("First pass of %s with size of %d", inputFile.getName(), fileSize);
		System.out.println(msg);
		WayNodeFinder wayNodes = firstPass(inputFile);

		System.out.println("Second pass of file");
		WayToNodeConverter converter = secondPass(inputFile, wayNodes);
		Stream<Way> waysWithLocation = converter.waysWithLocation();
		writePOIs(waysWithLocation);
	}

	/**
	 * Identify all nodes that are used in POI ways.
	 * 
	 * @param inputFile
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private static WayNodeFinder firstPass(File inputFile) throws IOException, FileNotFoundException {
		
		try(InputStream inputStream = new FileInputStream(inputFile);)
		{
			POIFilter poiFilter = new POIFilter();

			OsmosisReader reader = new OsmosisReader(inputStream);
			reader.setSink(poiFilter);
			reader.run();
			
			return poiFilter.wayNodes;
		}
	}

	private static WayToNodeConverter secondPass(File inputFile, WayNodeFinder wayNodes) throws FileNotFoundException {
		InputStream inputStream = new FileInputStream(inputFile);
		OsmosisReader reader = new OsmosisReader(inputStream);

		WayToNodeConverter converter = new WayToNodeConverter(wayNodes);
		reader.setSink(converter);
		reader.run();

		return converter;
	}

	private static void writePOIs(Stream<Way> wayPOIs) throws IOException {

		GeoJSON geoJSON = new GeoJSON();
		Path path = Paths.get("waypois.geojson.txt");

		try (BufferedWriter bw = Files.newBufferedWriter(path)) {
			wayPOIs.forEach(way -> geoJSON.writeToLineDelimitedGeoJSON(bw, way));
		}
	}

}
