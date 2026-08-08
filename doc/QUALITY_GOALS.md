# High-Level Quality Goals for `osm-tools`

This document outlines the core architectural and spatial quality goals of `osm-tools`. These high-level objectives ensure that simplified polygon coverages remain topologically valid, complete, and highly performant for downstream spatial indexing and reverse-geocoding pipelines.

---

## 1. Vollständigkeit (Completeness)
- **Tag- & Attribut-Vollständigkeit:** Alle wesentlichen OpenStreetMap-Eigenschaften (`name`, `@id`, `admin_level`, `subtype`, `ISO3166-1`, `ISO3166-2`) bleiben bei der Vereinfachung vollständig erhalten.
- **Feature-Vollständigkeit:** Jedes administrative Element der Eingabedatei ist in der vereinfachten Ausgabedatei enthalten. Keine Polygone oder administrative Einheiten gehen verloren.

## 2. Topologische Integrität & Küstenschutz
- **Grenztreue im Inland:** Die Vereinfachung und Pufferung von Binnengrenzen darf niemals zu einer Intrusion in benachbarte Binnengemeinden oder Bundesländer derselben Hierarchieebene führen.
- **Küstenschutz & Seegrenzen:** Meeresanstößige Grenzen werden leicht nach außen ins offene Meer gepuffert ($\sim 1\text{ km}$), damit Häfen, Buchten, Strände und Küstenpunkte beim Reverse-Geocoding nicht im freien Ozean liegen.

## 3. Performance & Skalierbarkeit
- **Verarbeitungsgeschwindigkeit:** Die Pipelines sind auf maximalen Durchsatz optimiert. Große Länderdatensätze (wie Deutschland mit $> 23.000$ Polygonen) werden in **unter 1 Minute** verarbeitet.
- **Speichereffizienz & Streaming:** Das Tool verarbeitet Daten im Streaming-Verfahren mit minimalem, konstantem Arbeitsspeicherbedarf ($O(1)$ Heap-Overhead), sodass auch planetenweite Datensätze stabil laufen.

## 4. Robustheit & CI/CD-Kompatibilität
- **Locale-Unabhängigkeit:** Alle numerischen Werte und Geometrien werden strikt locale-unabhängig (`Locale.ROOT`) verarbeitet und ausgebunden, um Formatierungsfehler (z. B. Dezimalkomma vs. Punkt) in internationalen Pipeline-Umgebungen auszuschließen.

## 5. Rückverfolgbarkeit, Lesbarkeit & Usability (Traceability & Usability)
- **Lückenlose Rückverfolgbarkeit:** Jeder Log-Ausdruck enthält den exakten Git-Commit-Hash und Build-Zeitstempel der ausgeführten Anwendungsversion, um Pipeline-Läufe eindeutig zuordnen zu können.
- **Lesbarkeit & Gerundete Einheiten:** Dateigrößen, Durchsätze und Ausführungszeiten werden menschenlesbar und gerundet ausgegeben (z. B. `340 MB -> 70 MB` statt unleserlicher Byte-Kolonnen).
- **Benutzerfreundlichkeit (Usability):** Der visuelle Map Viewer ermöglicht die einfache Inspektion von GeoJSON-Dateien per intuitiver Drag & Drop Interaktion.
