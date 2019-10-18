package net.leberfinger.osm.geofabrik;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Class to retrieve download URLs of Geofabrik's download server.
 */
public class GeofabrikIndexer {

	public static final String START_URL = "https://download.geofabrik.de/";

	public static class DownloadIndex {
		private MutableList<URL> children = Lists.mutable.empty();
		private MutableList<URL> pbfFiles = Lists.mutable.empty();

		public void addURL(URL url) {
			if (isHTML(url)) {
				children.add(url);
			} else if (isPBF(url)) {
				pbfFiles.add(url);
			}
		}

		public ImmutableList<URL> getChildren() {
			return children.toImmutable();
		}

		public ImmutableList<URL> getPBFFiles() {
			return pbfFiles.toImmutable();
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("DownloadIndex [children=");
			builder.append(children);
			builder.append(", \n" + "pbfFiles=");
			builder.append(pbfFiles);
			builder.append("]");
			return builder.toString();
		}
		
		public void printDownloadURLs()
		{
			for(URL url : getPBFFiles())
			{
				System.out.println(url);
			}
		}

		public DownloadIndex nextLevel() throws IOException {
			DownloadIndex subIndex = new DownloadIndex();
			for (URL child : children) {
				DownloadIndex indexOfChild = parse(child);
				subIndex.children.addAll(indexOfChild.children);
				subIndex.pbfFiles.addAll(indexOfChild.pbfFiles);
			}

			return subIndex;
		}
	}

	public static DownloadIndex getIndex() throws IOException {
		URL start = new URL(START_URL);
		return parse(start);
	}

	public static DownloadIndex parse(URL url) throws IOException {

		DownloadIndex index = new DownloadIndex();
		Document doc = Jsoup.connect(url.toString()).get();

		removeDetailsDiv(doc);

		Element subRegions = doc.getElementById("subregions");
		List<URL> subRegionURLs = extractAbsoluteURLs(subRegions);

		subRegionURLs.forEach(index::addURL);

		return index;
	}

	private static List<URL> extractAbsoluteURLs(Element element) throws MalformedURLException {
		List<URL> subRegionURLs = Lists.mutable.empty();
		if (element != null) {
			Elements links = element.getElementsByTag("a");
			for (Element link : links) {
				String absoluteURL = link.absUrl("href");
				subRegionURLs.add(new URL(absoluteURL));
			}
		}
		return subRegionURLs;
	}

	private static void removeDetailsDiv(Document doc) {
		Element detailsDiv = doc.getElementById("details");
		if (detailsDiv != null) {
			detailsDiv.remove();
		}
	}

	public static boolean isHTML(URL url) {
		return url.getPath().toLowerCase().endsWith("html");
	}

	public static boolean isPBF(URL url) {
		return url.getPath().toLowerCase().endsWith("pbf");
	}

	public static Path buildAbsoluteDestination(Path destinationFolder, URL downloadURL) {
		String urlPath = removeLeadingSlash(downloadURL.getPath());
		Path resolved = destinationFolder.resolve(urlPath);
		return resolved.toAbsolutePath().normalize();
	}

	public static String removeLeadingSlash(String s) {
		if (s.startsWith("/")) {
			return s.substring(1);
		}
		return s;
	}
}
