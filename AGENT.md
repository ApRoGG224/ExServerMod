# AGENT INSTRUCTIONS: ExServerMod

## CRITICAL: Mandatory Documentation Reading
Before planning, designing, or implementing any code or feature in this repository, the agent MUST read the core documentation in `docs/`:
- [docs/architecture.md](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/docs/architecture.md): Core stack, Kotlin exclusivity, and deployment specs.
- [docs/features/hide-and-seek.md](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/docs/features/hide-and-seek.md): Complete game design specification (Hide & Seek, Mob Disguise, Visual Roulette Screen, Landmarks).
- [docs/gameplay-rules.md](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/docs/gameplay-rules.md): Invariants on world integrity, block protection, and anti-cheat vanilla balance.
- [docs/development-and-testing.md](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/docs/development-and-testing.md): Fast local testing workflow (Hot-Reload, runClient sandbox, unit tests).
- [docs/current-state.md](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/docs/current-state.md): Active status and pending tasks.
- [docs/decisions/](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/docs/decisions/): Architectural Decision Records (ADRs).

## Project Overview
Interactive multiplayer mod for the FoMe survival server on Fabric (Minecraft 1.21.1) written **strictly in Kotlin**.
The primary feature is a **Hide & Seek duel mini-game** with an animated visual mob roulette wheel (`RouletteScreen`), mob disguises with dynamic hitbox scaling (`Attributes.SCALE`), camera height adjustments, landmark proximity hints, and strict world protection invariants.

## Mandatory Technical Constraints
- Primary Language: **Kotlin ONLY** (`.kt`). Mod logic must never be written in Java.
- Platform: Fabric Loader + Fabric Language Kotlin (`fabric-language-kotlin`).
- **Strict i18n Invariant**: ALL user-facing text (chat messages, action bar text, button labels, hover tooltips, command feedback) must use `Component.translatable(key, *args)`. Hardcoded `Component.literal("...")` for user-visible strings is **strictly prohibited**. All keys must be defined in `src/main/resources/assets/exservermod/lang/en_us.json`.
- Comments: English only. Strictly raw, technical, and to the point. No emojis, stickers, or decorative visuals.
- Build System: Gradle Kotlin DSL (`build.gradle.kts`) with Fabric Loom.
- Task Automation: `Taskfile.yml`.

## Core Gameplay & World Integrity Invariants
- Zero Block Destruction: Abilities, transformations, and projectiles must NEVER destroy or grief world blocks.
- No Teleportation: Teleportation abilities are strictly forbidden. The only exception is an automated anti-stuck / unstuck fallback when a player is suffocating or clipped in blocks.
- Thoughtful Vanilla Balance: No cheats, no god-mode abilities. Everything must integrate seamlessly into vanilla survival balance.

## Command Reference
- `task lint`: Static analysis and style check via ktlint.
- `task format`: Automated code formatting.
- `task test`: Execute test suite (JUnit 5 / Fabric test API).
- `task pre-commit`: Mandatory verification gate (`lint` + `test`). Must pass with 0 errors.
- `task build`: Compile production `.jar` into `build/libs/`.
- `task version`: Output current mod version from `gradle.properties`.
- `task deploy`: Bump minor version (`1.0` -> `1.1`), build jar, and deploy to Exaroton server.
- `task deploy:minor`: Alias for `task deploy`.
- `task deploy:major`: Bump major version (`1.x` -> `2.0`), build jar, and deploy to Exaroton server.
- `task deploy:patch`: Bump patch version (`1.0.0` -> `1.0.1`), build jar, and deploy to Exaroton server.

## Fast Testing Workflow
- Run sandbox client: `./gradlew runClient`
- Hot-Reload code edits in Debug mode: `Cmd + F9` (via JetBrains Runtime) without restarting the game.
- Sub-second unit tests: `task test`
