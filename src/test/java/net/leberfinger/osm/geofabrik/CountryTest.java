package net.leberfinger.osm.geofabrik;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.jupiter.api.Test;

public class CountryTest {

	@Test
	void testImport() throws IOException
	{
		Set<Country> countries = Country.readBundledFile();
		assertThat(countries).isNotEmpty();
	}
	
	@Test
	void resolveGeofabrikNames() throws IOException
	{
		String geofabrikNames = "algeria\n" + 
				"angola\n" + 
				"benin\n" + 
				"botswana\n" + 
				"burkina-faso\n" + 
				"wyoming\n" + 
				"argentina\n" + 
				"bolivia\n" + 
				"brazil\n" + 
				"chile";
		
		String[] lines = geofabrikNames.split("\n");
		MutableList<String> names = Lists.mutable.of(lines);
		List<Optional<Country>> resolvedCountries = Country.resolveCountries(names);
		
		assertThat(resolvedCountries).hasSize(10);
		assertThat(resolvedCountries).isNotEmpty();
		Country algeria = resolvedCountries.get(0).get();
		assertThat(algeria.getEnglishName()).isEqualTo("Algeria");
		assertThat(algeria.getAlpha2()).isEqualTo("DZ");
		assertThat(algeria.getAlpha3()).isEqualTo("DZA");
		assertThat(algeria.toString()).contains("Algeria");
	}
}
