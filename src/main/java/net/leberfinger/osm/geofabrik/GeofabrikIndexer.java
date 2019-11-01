package net.leberfinger.osm.geofabrik;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
	
	/**
	 * Retrieve all download URLs for the second level of Geofabrik's index. This is
	 * generally the 'country level', but for example US, its federal states are
	 * being listed instead.
	 * 
	 * @return
	 * @throws IOException
	 */
	public static DownloadIndex getCountryIndex() throws IOException
	{
		DownloadIndex index = GeofabrikIndexer.getIndex();
		DownloadIndex countryIndex = index.nextLevel();
		return countryIndex;
	}
	
	public static String getCountryNameFromURL(String url) throws MalformedURLException
	{
		return getCountryNameFromURL(new URL(url));
	}
	
	public static String getCountryNameFromURL(URL url)
	{
		String path = url.getPath();
		int directoryEndIndex = path.lastIndexOf("/");
		path = path.substring(directoryEndIndex + 1);
		
		int suffixIndex = path.lastIndexOf("-latest");
		path = path.substring(0, suffixIndex);
		
		return path;
	}

	public static void writeCountryIndex(Path p) throws IOException {
		DownloadIndex countryIndex = getCountryIndex();
		ImmutableList<URL> pbfFiles = countryIndex.getPBFFiles();

		List<String> countryMappings = Lists.mutable.empty();
		for (URL countryURL : pbfFiles) {
			String countryName = getCountryNameFromURL(countryURL);
			countryMappings.add(countryName + "=" + countryURL);
		}
		Files.write(p, countryMappings);
	}
	
	public static void main(String[] args) throws IOException
	{
		Path p = Paths.get("geofabrik-country-links.txt");
		GeofabrikIndexer.writeCountryIndex(p);
	}
}
