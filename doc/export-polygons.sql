INSTALL spatial;
LOAD spatial;
COPY (
    SELECT
        id, 
        names.primary AS name,
        country, 
        region, 
        subtype, 
        --"class",
        --CAST(names AS JSON), 
        --is_land, 
        is_territorial, 
        division_id, 
        "type",
        -- or 0.0001 ?
        ST_SimplifyPreserveTopology(geometry, 0.001) AS geometry
    FROM
        division_areas
    WHERE
        is_territorial=true
    ORDER BY 
        country, region
) TO 'polygons.geojsonseq' WITH (FORMAT GDAL, DRIVER 'GeoJSONSeq');
