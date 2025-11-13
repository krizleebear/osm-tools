# GeoJSON downloader

Usage:

```bash
python3 scripts/download_geojsons.py --repo krizleebear/osm-polygons --dest data/geojsons
```

Set `GITHUB_TOKEN` environment variable if you hit API limits or need access to private releases.

The downloader looks for assets ending with `.geojson` or `.geojsonseq` by default.

Filtering by country/filename
- To download only assets whose filenames contain a substring (case-insensitive), pass a match string as the third argument to the wrapper. Example:


```bash
python3 scripts/download_geojsons.py --repo krizleebear/osm-polygons --dest data/geojsons --match germany
```
