package net.leberfinger.osm.categories;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

import org.eclipse.collections.impl.factory.SortedMaps;

public class Categories {

	private int categoryID = 0;
	private Map<String, Category> categories = SortedMaps.mutable.empty();

	protected String combinedKey(String osmKey, String osmValue) {
		return osmKey + "=" + osmValue;
	}

	public void add(String osmKey, String osmValue, String language, String name) {

		String key = combinedKey(osmKey, osmValue);
		Category category = categories.computeIfAbsent(key, k -> newCategory(k));

		category.addNameForLang(language, name);
	}

	public Category get(String osmKey, String osmValue) {
		return categories.get(combinedKey(osmKey, osmValue));
	}

	private Category newCategory(String osmKey) {
		categoryID++;
		return new Category(categoryID, osmKey);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Categories [");
		builder.append(categoriesToString());
		builder.append("]");
		return builder.toString();
	}

	public String categoriesToString() {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, Category> entry : categories.entrySet()) {
			sb.append(entry.getKey());
			sb.append("=");
			sb.append(entry.getValue());
			sb.append(System.lineSeparator());
		}
		return sb.toString();
	}

	public void foreach(BiConsumer<String, Category> action) {
		categories.forEach(action);
	}
}
