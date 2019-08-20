# OpenStreetMap category translations
# The tsv files from this folder are a modified form of
# https://github.com/openvenues/libpostal/blob/master/resources/categories/

# HTML tables of this content can be found here (sporadically updated):
# https://wiki.openstreetmap.org/wiki/Nominatim/Special_Phrases/DE

# libpostal has a script with TSV export, but unfortunately tolower()ed the terms:
# https://github.com/openvenues/libpostal/blob/master/scripts/geodata/categories/scrape_nominatim_special_phrases.py
# that's why I changed the script to skip tolower()

# OSM category translations are originally provided by translatewiki.net
# https://translatewiki.net/wiki/Translating:OpenStreetMap
# e.g. https://translatewiki.net/wiki/Osm:Geocoder.search_osm_nominatim.prefix.building.terrace/de

# an export function for offline translation is offered:
# https://translatewiki.net/w/i.php?title=Special:ExportTranslations&language=de&group=out-osm-0-all
# but only a few translations are contained. almost all nominatim expressions are missing.

# all locales from Translatewiki are pushed to OSM:
# https://git.openstreetmap.org/rails.git/tree/HEAD:/config/locales
# example key: geocoder.search_osm_nominatim.prefix.amenity.kindergarten

# e.g. https://git.openstreetmap.org/rails.git/blob_plain/HEAD:/config/locales/de.yml
# unfortunately plural forms are not contained there



