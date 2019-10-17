package net.leberfinger.osm.geofabrik;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.collections.api.list.ImmutableList;
import org.junit.jupiter.api.Test;

import net.leberfinger.osm.geofabrik.GeofabrikIndexer.DownloadIndex;

class GeofabrikIndexerTest {

	@Test
	void retrieveIndex() throws IOException {
		DownloadIndex index = GeofabrikIndexer.getIndex();
		assertTrue(index.getChildren().notEmpty());
		assertTrue(index.getPBFFiles().notEmpty());
		
		assertThat(index.toString()).contains(".pbf");
	}

	@Test
	void retrieveNextLevel() throws IOException {
		DownloadIndex index = GeofabrikIndexer.getIndex();
		ImmutableList<URL> parentPBFs = index.getPBFFiles();

		DownloadIndex subIndex = index.nextLevel();
		ImmutableList<URL> childrenPBFs = subIndex.getPBFFiles();

		assertTrue(childrenPBFs.size() > parentPBFs.size());
	}

	@Test
	void buildAbsoluteDestination() throws MalformedURLException {
		Path destFolder = Paths.get("/tmp").toAbsolutePath();
		String url = "https://download.geofabrik.de/south-america/venezuela-latest.osm.pbf";
		Path path = GeofabrikIndexer.buildAbsoluteDestination(destFolder, new URL(url));

		assertEquals("/tmp/south-america/venezuela-latest.osm.pbf", path.toString());
	}
	
	@Test
	void removeLeadingSlash()
	{
		String unmodified = GeofabrikIndexer.removeLeadingSlash("abc");
		assertThat(unmodified).isEqualTo("abc");
		
		String empty = GeofabrikIndexer.removeLeadingSlash("/");
		assertThat(empty).isEmpty();
		
		assertThat(GeofabrikIndexer.removeLeadingSlash("")).isEmpty();
	}
}
