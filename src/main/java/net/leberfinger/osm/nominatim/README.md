# Nominatim
Nominatim is an OSM based tool for geocoding and reverse geocoding. This package offers a connector for parts of Nominatim's API. Its focus is reverse geocoding.  

This package expects Nominatim to be setup via https://github.com/mediagis/nominatim-docker/

## Reverse GeoCoding 
Reverse geocoding is a process that takes lat/lon coordinates and resolves address information.

The common interface for reverse geocoders is IAdminResolver.
Multiple providers are implementing the AdminResolver interface:

### NominatimConnection
This is the basic resolver implementation that has no internal cache but redirects all requests directly to the connected Nominatim instance.

### NominatimCache
RAM cache for nominatim requests. Uses a given NominatimConnection to resolve given coordinates. Then stores the resolved address data and the localities polygonal boundaries in a cache. Requests are always first searched in the local cache and Nominatim is only contacted in case of cache misses.

Internally JTS structures and algorithms are used which are quite optimized and mature. Tests resolving all 2m POIs of Germany showed a performance increase of about 100%.   