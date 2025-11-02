INSTALL spatial;
LOAD spatial;
COPY (
      SELECT
         id,
         version,
      -- We are casting these columns to JSON in order to ensure compatibility with our GeoJSON output.
      -- These conversions may be not necessary for other output formats.
         CAST(names AS JSON) AS names,
         CAST(categories AS JSON) AS categories,
         confidence,
         basic_category,
         confidence,
         CAST(websites AS JSON) AS websites,
         CAST(socials AS JSON) AS socials,
         --CAST(emails AS JSON) AS emails,
         CAST(phones AS JSON) AS phones,
         CAST(brand AS JSON) AS brand,
         CAST(addresses AS JSON) AS addresses,
         --CAST(sources AS JSON) AS sources,
         addresses[1].country as country,
         addresses[1].region as region,
         geometry
  FROM places
  USING SAMPLE 1000000
) TO 'random_places.geojson' WITH (FORMAT GDAL, DRIVER 'GeoJSONSeq', SRS 'EPSG:4326');