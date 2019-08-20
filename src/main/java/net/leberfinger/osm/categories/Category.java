package net.leberfinger.osm.categories;

import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Multimaps;

public class Category {

	private final int categoryID;
	private final String osmCategory;
	private final MutableSetMultimap<String, String> namesForLang = Multimaps.mutable.set.empty();

	public Category(int categoryID, String osmCategory) {
		this.osmCategory = osmCategory;
		this.categoryID = categoryID;
	}

	public void addNameForLang(String lang, String name) {
		namesForLang.put(lang, name);
	}
	
	public ImmutableSet<String> getNames(String lang)
	{
		return namesForLang.get(lang).toImmutable();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Category [categoryID=");
		builder.append(categoryID);
		builder.append(", osmCategory=");
		builder.append(osmCategory);
		builder.append(", namesForLang=");
		builder.append(namesForLang);
		builder.append("]");
		return builder.toString();
	}

}
