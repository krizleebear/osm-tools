package net.leberfinger.osm.geofabrik;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;

/**
 * This class tries to map Geofabrik's country level maps to actual countries.
 * <p/>
 * This doesn't always work perfectly, because Geofabrik has its own strategy to
 * cut the world in parts that's not always mapping to political borders, but it
 * comes pretty close. It seems as if Geofabrik has a very 'spatial' approach
 * which is understandable from a technical point of view.
 * <p/>
 * Examples:
 * <ul>
 * <li>GCC (Gulf Cooperation Council) isn't a real state but a council of
 * several states</li>
 * <li>Ireland and Northern Ireland are geographically close and thus written to
 * the same file</li>
 * </ul>
 */
public class Country {

	private final String englishName;
	private final String alpha2;
	private final String alpha3;

	private Country(String englishName, String alpha2, String alpha3) {
		this.englishName = englishName;
		this.alpha2 = alpha2;
		this.alpha3 = alpha3;
	}

	public String getEnglishName() {
		return englishName;
	}

	public String getAlpha2() {
		return alpha2;
	}

	public String getAlpha3() {
		return alpha3;
	}

	public static List<Optional<Country>> resolveCountries(List<String> names) throws IOException {
		List<Optional<Country>> resolvedCountries = Lists.mutable.empty();

		Set<Country> knownCountries = readBundledFile();

		Map<String, Country> index = getCountryIndex(knownCountries);

		for (String name : names) {
			Optional<Country> resolved = resolveCountry(index, name);
			resolvedCountries.add(resolved);
		}

		return resolvedCountries;
	}

	private static Optional<Country> resolveCountry(Map<String, Country> index, String name) {

		name = name.toLowerCase();

		Country resolved = index.get(name);

		return Optional.ofNullable(resolved);
	}

	private static Map<String, Country> getCountryIndex(Set<Country> knownCountries) {

		Map<String, Country> countryIndex = Maps.mutable.empty();

		for (Country c : knownCountries) {
			countryIndex.put(c.englishName.toLowerCase(), c);
		}

		return countryIndex;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Country [englishName=");
		builder.append(englishName);
		builder.append(", alpha2=");
		builder.append(alpha2);
		builder.append(", alpha3=");
		builder.append(alpha3);
		builder.append("]");
		return builder.toString();
	}

	protected static Set<Country> readBundledFile() throws IOException {
		String f = "net/leberfinger/osm/geofabrik/countries.tsv";

		try (InputStream in = Country.class.getClassLoader().getResourceAsStream(f)) {
			return readTSV(in);
		}
	}

	public static Set<Country> readTSV(InputStream in) throws IOException {
		Set<Country> countries = Sets.mutable.empty();

		CSVFormat tsvFormat = CSVFormat.TDF.withFirstRecordAsHeader();
		try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			CSVParser records = tsvFormat.parse(r);
			for (CSVRecord record : records) {
				String englishName = record.get(0);
				String alpha2 = record.get(1);
				String alpha3 = record.get(2);
//				String numeric = record.get(3);
				String geofabrik = record.get(4);
				if (!geofabrik.isEmpty()) {
					englishName = geofabrik;
				}

				countries.add(new Country(englishName, alpha2, alpha3));
			}
		}

		return countries;
	}
}
