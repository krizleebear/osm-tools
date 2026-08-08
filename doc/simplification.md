# Boundary Polygon Simplification and Coastal Buffering

This document describes the administrative boundary simplification pipeline in `osm-tools` and how we address coordinate drops for coastal points of interest (POIs).

## Architecture Overview

`osm-tools` contains tools to simplify and resolve administrative boundaries. These boundaries are used downstream by the geocoder to perform reverse geocoding queries (resolving lat/lon coordinates to cities, states, and countries).

Simplification is crucial to keep the size of the geocoder index reasonable. However, standard simplification has a known side effect on coastal borders.

---

## The Coastline Coordinate Drop Problem

When simplifying boundary polygons, algorithms like Douglas-Peucker simplify the lines by removing vertices that fall within a given distance tolerance.

### Cause
On coastal shorelines, the boundary is the coastline. When Douglas-Peucker simplifies this line, it smooths out the intricate shapes of bays, inlets, ports, and protrusions. 

This smoothing process inevitably shifts the boundary polygon edges **inland** (cutting off the water areas of bays and inlets).

```
Actual Coastline:   ~~~~~\______/~~~~~   (includes bay/beach)
Simplified Line:    ~~~~~________~~~~~   (shifted inland)
                          ^ 
                   Coastal POI falls here (now in the "ocean" outside the polygon)
```

As a result, POIs situated on the actual shoreline (e.g., beaches, ports, piers, shoreline restaurants) fall outside the simplified polygons. When reverse-geocoded, they resolve to `null` for municipality and state.

---

## Strategy: Coastal Buffering with Neighbor Subtraction

To solve this, we implement an outward buffer on coastal borders. To prevent inland boundaries from overlapping, we use a selective subtraction technique.

### Detailed Workflow:

1. **Simplification**:
   - Geometries are first simplified using standard tolerances (e.g., from `doc/simplify-factors.csv` or a default tolerance) to reduce overall size.
   - Using JTS `CoverageSimplifier` is recommended for adjacent polygons to prevent gaps and overlaps on shared edges.

2. **Ocean Buffering**:
   - For each simplified geometry $G_i$, we apply a small outward buffer of distance $d$ (e.g., $0.01$ degrees, approx. 1km):
     $$G_{i,\text{buf}} = G_i.\text{buffer}(d)$$

3. **Neighbor Subtraction**:
   - To prevent the buffer from expanding into neighboring land boundaries (which would cause coordinate resolve overlaps), we find all overlapping neighboring geometries $G_j$ using a spatial index (STRtree).
   - We subtract the original neighbor geometries from the buffered geometry:
     $$G_{i,\text{final}} = G_{i,\text{buf}} \setminus \bigcup_{j \neq i} G_j$$
   - This keeps the shared boundaries on land exactly touching (without overlaps), while allowing the boundary to expand into the ocean.

---

## Command Line Configuration

The updated `GeoJSONSimplify` command line interface will support the following options:

*   `-i, --input`: Path to the input `.geojsonseq` file containing the polygons.
*   `-t, --tolerance`: Simplification distance tolerance (default: `0.001`).
*   `-b, --buffer`: Coastal buffering distance in degrees (default: `0.0`, no buffering). Set to `0.01` for a 1km buffer.
*   `-c, --coverage`: Use topology-preserving coverage simplification (default: false).
