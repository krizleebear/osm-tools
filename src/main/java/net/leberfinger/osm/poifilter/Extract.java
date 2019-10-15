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

import org.apache.commons.io.FilenameUtils;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import crosby.binary.osmosis.OsmosisReader;

public class Extract {

	public static void main(String[] args) throws IOException {

		File inputFile = new File("/Users/krizleebear/Downloads", "germany-latest.osm.pbf");
//		File inputFile = new File("liechtenstein-latest.osm.pbf");
//		File inputFile = new File("/Users/krizleebear/Downloads", "oberbayern-latest.osm.pbf");

		long fileSize = Files.size(inputFile.toPath());
		String msg = String.format("First pass of %s with size of %d", inputFile.getName(), fileSize);
		System.out.println(msg);
		WayNodeFinder wayNodes = firstPass(inputFile);

		Path destFile = getOutputFilename(inputFile.toPath());
		//TODO: write node POIs to file and append way POIs afterwards

		System.out.println("Second pass of file");
		WayToNodeConverter converter = secondPass(inputFile, wayNodes);
		Stream<Way> waysWithLocation = converter.waysWithLocation();
		
		
		writePOIs(waysWithLocation, destFile);
		System.out.println("Wrote filtered POIs to " + destFile);
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

	private static void writePOIs(Stream<Way> wayPOIs, Path destFile) throws IOException {

		GeoJSON geoJSON = new GeoJSON();

		try (BufferedWriter bw = Files.newBufferedWriter(destFile)) {
			wayPOIs.forEach(way -> geoJSON.writeToLineDelimitedGeoJSON(bw, way));
		}
	}

	public static Path getOutputFilename(Path inputFile)
	{
		String originalName = inputFile.getFileName().toString();
		String baseName = FilenameUtils.removeExtension(originalName);
		
		return Paths.get(baseName + ".pois.geojson");
	}
}
