package net.leberfinger.osm.categories;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.collections.api.set.ImmutableSet;
import org.junit.jupiter.api.Test;

class CategoryTest {

	@Test
	void getNamesForLang() {
		Category cat = new Category(123, "amenity:restaurant");
		assertThat(cat.getCategoryID()).isEqualTo(123);
		assertThat(cat.getOsmCategory()).isEqualTo("amenity:restaurant");

		cat.addNameForLang("de", "Restaurant");

		ImmutableSet<String> deNames = cat.getNames("de");
		assertThat(deNames).contains("Restaurant");

		ImmutableSet<String> enNames = cat.getNames("en");
		assertThat(enNames).hasSize(0);
	}

	@Test
	void testToString() {
		Category cat = new Category(1, "amenity:restaurant");
		assertThat(cat.toString()).contains("restaurant");
	}

	@Test
	void testNames() {
		Category cat = new Category(123, "amenity:restaurant");
		cat.addNameForLang("de", "Restaurant");
		cat.addNameForLang("en", "restaurant");

		cat.names((lang, name) -> {
			assertThat(lang).isNotBlank();
			assertThat(name).isNotBlank();
		});
	}

	@Test
	void testNamesPerLang() {
		Category cat = new Category(123, "amenity:restaurant");
		cat.addNameForLang("de", "Restaurant");
		cat.addNameForLang("en", "restaurant");

		cat.namesPerLanguage((lang, names) -> {
			assertThat(lang).isNotBlank();
			assertThat(names).hasSize(1);
		});
	}
}
