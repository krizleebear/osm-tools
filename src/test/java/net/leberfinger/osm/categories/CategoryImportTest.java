package net.leberfinger.osm.categories;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Paths;

import org.eclipse.collections.api.set.ImmutableSet;
import org.junit.jupiter.api.Test;

class CategoryImportTest {

	@Test
	void getLanguageFromFilename() {
		assertThat(CategoryImport.getLang(Paths.get("de-at.tsv"))).isEqualTo("de");
		assertThat(CategoryImport.getLang(Paths.get("DE.TSV"))).isEqualTo("de");
	}

	@Test
	void recognizeTSVFile() {
		assertThat(CategoryImport.isTSVFile(Paths.get("DE.TSV"))).isTrue();
		assertThat(CategoryImport.isTSVFile(Paths.get("DETSV"))).isFalse();
	}

	@Test
	void importBundledCategoryFiles() throws IOException {
		CategoryImport c = new CategoryImport();
		c.importBundledFiles();

		Categories categories = c.getCategories();

		Category restaurantCategory = categories.get("amenity", "restaurant");
		assertThat(restaurantCategory).isNotNull();

		ImmutableSet<String> englishNames = restaurantCategory.getNames("en");
		assertThat(englishNames).contains("Restaurant");
		System.out.println(englishNames);
	}

}
