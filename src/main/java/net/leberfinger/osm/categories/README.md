# OSM Categories
```Java

CategoryImport c = new CategoryImport();
c.importBundledFiles();

Categories categories = c.getCategories();

Category restaurantCategory = categories.get("amenity", "restaurant");
ImmutableSet<String> englishNames = restaurantCategory.getNames("en");

System.out.println(englishNames);
// will print [Restaurant, Restaurants, Food]
```
