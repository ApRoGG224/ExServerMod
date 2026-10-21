# ADR 0001: Technology Stack, Kotlin Exclusivity, and Exaroton Deployment

## Status
Accepted

## Context
A server-side Minecraft mod needs to be developed for the FoMe server hosted on Exaroton. The development workflow requires strict code quality, zero boilerplate bloat, fast automated verification, and frictionless on-demand deployment without manual file transfers.

## Decisions

### 1. Primary Language: Strictly Kotlin
- All mod code, configuration logic, and utilities must be written in Kotlin (`.kt`).
- Java files for mod logic are not permitted.
- Interoperability with Minecraft/Fabric Java APIs is handled via Kotlin's native Java interop and `fabric-language-kotlin`.

### 2. Modding Platform: Fabric Loader on Minecraft 26.2+
- Targeted for Minecraft 26.2+ on Java 25.
- Uses `net.fabricmc.fabric-loom` (1.19.2) without obfuscation mappings, as Minecraft 26.x ships cleanly without obfuscation.
- Dependencies use standard `implementation` and Kotlin 2.4.21. Fabric Language Kotlin 1.14.2 is enforced.

### 3. Task Automation & Quality Standards
- `Taskfile.yml` serves as the universal developer interface:
  - `task lint`: Kotlin style and static analysis.
  - `task test`: Automated test suite execution.
  - `task pre-commit`: Combined verification gate before commits.

### 4. Semantic Versioning & Automated Bumping
- Standard deployments trigger minor version increments (`1.0` -> `1.1`).
- Breaking/overhaul releases trigger major increments (`1.x` -> `2.0`) via `task deploy:major`.
- Version is tracked in `gradle.properties` (`mod_version`).

### 5. Atomic Exaroton REST API Deployment
- Direct deployment to Exaroton via HTTP API (`api.exaroton.com/v1`).
- Outdated versions of the mod jar in `mods/` are detected and deleted prior to/along with uploading the new jar, eliminating file accumulation and runtime version conflicts.
- Deployments occur strictly on explicit user command (`task deploy`).

## Consequences
- Requires `.env` configuration with valid Exaroton credentials prior to first deployment.
- Requires target Minecraft version specification to finalize Gradle build script.
