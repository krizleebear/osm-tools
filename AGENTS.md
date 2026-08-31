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
3. **Explanation Preceding Git Actions Invariant (Explain First, Commit Second):**
   The agent must always first output a clear, comprehensive explanation of the diagnosis, the rationale, and the exact changes in the visible response text before requesting permission or attempting to execute `git commit`, `git push`, or pipeline triggers. Never trigger permission prompts for Git actions without the user having seen the complete explanatory context first.

---

## Spatial Coding Conventions & Invariants

- **No Emojis in Documentation:** Avoid using decorative emojis in markdown documentation, section headers, or viewer UIs.
- **Locale Independence:** When formatting spatial configurations, coordinates, distances, or numbers, ALWAYS format using `Locale.ROOT` in Java (e.g. `String.format(Locale.ROOT, "%.4f", value)`) to prevent comma decimal bugs (`0,01` vs `0.01`) in non-English localizations.
- **Strict Polygonal Coverage Simplification:** `CoverageSimplifier` strictly requires polygonal geometries. Non-polygonal geometries (`Point`, `LineString`) must be separated and simplified individually to prevent `ClassCastException` failures.
- **Hierarchy Grouping:** Geometries must be grouped by hierarchy level (`subtype` or `admin_level`) prior to coverage simplification to prevent topological errors between overlapping administrative levels (e.g., states overlapping municipalities).
- **Tag Continuity:** Never drop standard OSM properties during simplification (such as `name`, `subtype`, `admin_level`, `ISO3166-1`, and `ISO3166-2`) as these are parsed downstream to construct geocoder spatial indices.
- **Maritime Coastline Preservation:** Ocean-facing borders must be buffered outward before simplification to prevent coastal coordinate drops on ports, shorelines, beaches, and harbors.
- **Automated Consistency Verification:** Run `GeoJSONSimplifyVerifier` (or pass input/output files) to validate feature count completeness, geometry validity, tag continuity, area coverage preservation, and inland non-overlap invariants. See [QUALITY_GOALS.md](file:///Users/krizleebear/development/osm-tools/doc/QUALITY_GOALS.md) for threshold metrics.
- **Transparent Failure Policy (No Hiding Errors / No Silent Fallbacks):** Processing tools and pipeline scripts must never mask missing input data or execute silent fallbacks to external endpoints. Errors must fail fast and explicitly with clear diagnostic logging detailing the missing file, root cause, and remediation steps.
- **Downstream Pipeline Image Synchronization:** Whenever code modifications or engine optimizations in `osm-tools` are committed and pushed to `master`, downstream consumer pipeline definitions (specifically `resources.containers` and fallback commit definitions in `osm-polygons/polygon-export-pipeline.yml` and `osm-polygons/polygon-release-pipeline.yml`) MUST immediately and proactively be updated to reference the new commit SHA (`mirror.gcr.io/krizleebear/osm-tools:master-<SHA>`). Never leave downstream pipelines pointing to stale engine versions.
- **Evidence-Based Issue Analysis & Remote DuckDB Diagnostics:** Never make assumptions about artifact contents based solely on commit history. When investigating issues across upstream/downstream boundaries, always gather empirical evidence by directly querying release artifacts using DuckDB with `httpfs` (`duckdb -c "INSTALL httpfs; LOAD httpfs; SELECT ... FROM 'https://github.com/.../releases/download/.../....parquet'"`). Include reproducible diagnostic SQL queries in bug reports and issue responses.



