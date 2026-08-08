# AGENTS.md — Development Guidelines for `osm-tools`

To ensure a consistent development workflow, clean spatial processing, and clear commit conventions across `osm-tools` and downstream projects, AI agents and developers working on this repository must adhere to the following rules:

---

## Build & Runtime Environment

- **Direct Host Execution:** If the host system has Java 21+ and Maven installed, commands can be run directly on the host.
- **Docker Verification:** For clean-room verification or environments lacking a native Maven setup, run tests using Docker:
  ```bash
  docker run --rm -v $(pwd):/workspace -v ~/.m2:/root/.m2 -w /workspace maven:3-sapmachine mvn test
  ```
- **Performance Rule:** Do NOT rebuild Docker containers repeatedly during iteration; mount the workspace directory dynamically to keep feedback loops under 5 seconds.

---

## Git & Commit Workflow

1. **Verify Before Committing:** ALWAYS run the unit test suite (`mvn test` or via Docker) and verify spatial test cases before committing code.
2. **Conventional Commits Standard:** Use strict commit prefixes:
   - `feat:` Enhancements to polygon generation, simplification, or export logic (e.g. `feat: buffer maritime coastlines by 1km`).
   - `fix:` Bug fixes (e.g. `fix: filter non-polygonal geometries from coverage simplifier`).
   - `test:` Adding or updating unit/E2E test suites.
   - `docs:` Updates to documentation, READMEs, or architectural docs.
   - `refactor:` Code refactoring without changing user-facing behavior.

---

## Spatial Coding Conventions & Invariants

- **No Emojis in Documentation:** Avoid using decorative emojis in markdown documentation, section headers, or viewer UIs.
- **Locale Independence:** When formatting spatial configurations, coordinates, distances, or numbers, ALWAYS format using `Locale.ROOT` in Java (e.g. `String.format(Locale.ROOT, "%.4f", value)`) to prevent comma decimal bugs (`0,01` vs `0.01`) in non-English localizations.
- **Strict Polygonal Coverage Simplification:** `CoverageSimplifier` strictly requires polygonal geometries. Non-polygonal geometries (`Point`, `LineString`) must be separated and simplified individually to prevent `ClassCastException` failures.
- **Hierarchy Grouping:** Geometries must be grouped by hierarchy level (`subtype` or `admin_level`) prior to coverage simplification to prevent topological errors between overlapping administrative levels (e.g., states overlapping municipalities).
- **Tag Continuity:** Never drop standard OSM properties during simplification (such as `name`, `subtype`, `admin_level`, `ISO3166-1`, and `ISO3166-2`) as these are parsed downstream to construct geocoder spatial indices.
- **Maritime Coastline Preservation:** Ocean-facing borders must be buffered outward before simplification to prevent coastal coordinate drops on ports, shorelines, beaches, and harbors.
- **Automated Consistency Verification:** Run `GeoJSONSimplifyVerifier` (or pass input/output files) to validate feature count completeness, geometry validity, tag continuity, area coverage preservation, and inland non-overlap invariants. See [QUALITY_GOALS.md](file:///Users/krizleebear/development/osm-tools/doc/QUALITY_GOALS.md) for threshold metrics.
