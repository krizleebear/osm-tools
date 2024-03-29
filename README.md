# osm-tools [![Build Status](https://travis-ci.org/krizleebear/osm-tools.svg?branch=master)](https://travis-ci.org/krizleebear/osm-tools)
Java tools to handle OpenStreetMap data. 

## [Categories](../../tree/master/src/main/java/net/leberfinger/osm/categories)
Category names for OSM POI categories (e.g. amenity/restaurant) in several languages.

## [POI filter](../../tree/master/src/main/java/net/leberfinger/osm/poifilter)
A Java tool reading OSM PBF files and writing POIs to geojson format. Comparable to [pbf2json](https://github.com/pelias/pbf2json) but less features.

## [Nominatim reverse geo coding](../../tree/master/src/main/java/net/leberfinger/osm/nominatim)
Classes to connect to Nominatim and perform reverse geo coding queries. Offers cached resolvers with a throughput of millions of requests per minute. 

## [Geofabrik downloads](../../tree/master/src/main/java/net/leberfinger/osm/geofabrik)
List of download URLs for data extracts offered by Geofabrik. 
