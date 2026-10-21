# Project Current State

## Summary
The project documentation, gameplay rules, testing workflow, and architecture are fully initialized for **ExServerMod** — a Kotlin-only Fabric mod for Minecraft 26.2+ (on Java 25) implementing Hide & Seek, Mob Disguises, and a Visual Roulette Wheel.

## Active Status
**Task 4 Completed**. Ready for **Task 5: Visual Roulette Screen & Client Animation**.

---

## Implemented Components
- [AGENT.md](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/AGENT.md): Root guidelines, command reference, strict i18n rule, and constraints.
- [docs/features/hide-and-seek.md](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/docs/features/hide-and-seek.md): Complete game design specification for Hide & Seek, Mob Mode, Roulette Wheel, and Landmarks.
- [docs/development-and-testing.md](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/docs/development-and-testing.md): Fast local testing guide (sandbox `runClient`, Hot-Reload, JUnit 5).
- [docs/architecture.md](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/docs/architecture.md): Architecture, language exclusivity (Kotlin), and deployment specs.
- [docs/gameplay-rules.md](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/docs/gameplay-rules.md): Gameplay invariants (zero griefing, no teleportation, anti-stuck only).
- [docs/decisions/0003-strict-i18n-localization.md](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/docs/decisions/0003-strict-i18n-localization.md): ADR 0003 enforcing strict `Component.translatable` for all user-facing strings.
- [src/main/resources/assets/exservermod/lang/en_us.json](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/src/main/resources/assets/exservermod/lang/en_us.json): Comprehensive central translation catalog for game and landmark messages.
- [Taskfile.yml](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/Taskfile.yml): Task pipeline (`task pre-commit`, `task test`, `task build`, `task deploy`).
- [scripts/bump_version.py](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/scripts/bump_version.py): Version bumper.
- [scripts/deploy_exaroton.py](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/scripts/deploy_exaroton.py): Exaroton REST deployer.
- [gradle.properties](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/gradle.properties): Configured for Minecraft 26.2+ and Fabric Kotlin on Java 25.
- [settings.gradle.kts](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/settings.gradle.kts) & [build.gradle.kts](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/build.gradle.kts): Operational build pipeline with Fabric Loom 1.19.2 and Kotlin 2.4.21.
- [src/main/kotlin/com/fome/exservermod/game/](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/src/main/kotlin/com/fome/exservermod/game/):
  - [HideAndSeekGame.kt](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/src/main/kotlin/com/fome/exservermod/game/HideAndSeekGame.kt): Core state machine (IDLE, VOTING, HIDING, SEEKING, INTERMISSION).
  - [GameEventHandler.kt](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/src/main/kotlin/com/fome/exservermod/game/GameEventHandler.kt): Fabric event integration with strict i18n, damage immunity, block protection, elytra restrictions, periodic landmark proximity hints, 20-block movement warning, and seeker hit detection.
  - [GameCommands.kt](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/src/main/kotlin/com/fome/exservermod/game/GameCommands.kt): Command `/games` (`vote`, `startsearch`, `continue`, `leave`) with clickable translatable buttons.
- [src/main/kotlin/com/fome/exservermod/landmarks/](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/src/main/kotlin/com/fome/exservermod/landmarks/):
  - [Landmark.kt](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/src/main/kotlin/com/fome/exservermod/landmarks/Landmark.kt): Landmark model with pure distance-to-perimeter calculation.
  - [LandmarkManager.kt](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/src/main/kotlin/com/fome/exservermod/landmarks/LandmarkManager.kt): Persistence via `config/exservermod/landmarks.json` and closest landmark lookup.
  - [LandmarkCommands.kt](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/src/main/kotlin/com/fome/exservermod/landmarks/LandmarkCommands.kt): Commands `/location-add`, `/location-remove`, `/location-list` with strict i18n.
- [src/main/kotlin/com/fome/exservermod/disguise/](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/src/main/kotlin/com/fome/exservermod/disguise/):
  - [MobPool.kt](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/src/main/kotlin/com/fome/exservermod/disguise/MobPool.kt): Curated pool of 34 balanced ground mobs, filtering out bosses and flying entities, with pure scale calculation formula.
  - [DisguiseManager.kt](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/src/main/kotlin/com/fome/exservermod/disguise/DisguiseManager.kt): Mob disguise engine managing hitbox scale (`Attributes.SCALE`), infinite particle-free invisibility, safe anti-stuck relocation, and lifecycle cleanup.
- [src/test/kotlin/](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/src/test/kotlin/):
  - [HideAndSeekGameTest.kt](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/src/test/kotlin/com/fome/exservermod/game/HideAndSeekGameTest.kt): Unit tests for game transitions, roles, and timers.
  - [LandmarkTest.kt](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/src/test/kotlin/com/fome/exservermod/landmarks/LandmarkTest.kt): Unit tests for perimeter distance calculation, closest search, and JSON persistence.
  - [DisguiseTest.kt](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/src/test/kotlin/com/fome/exservermod/disguise/DisguiseTest.kt): Unit tests for 34 curated mob pool verification, exclusion of prohibited entities, scale math, and state tracking.

---

## Task Backlog

- [x] **Task 1: Initialize Gradle Build Foundation**
  - Create `settings.gradle.kts` and `build.gradle.kts` using Fabric Loom and Kotlin DSL.
  - Setup ktlint Gradle plugin for `task lint` / `task format`.
  - Generate Gradle wrapper (`gradlew`).
  - Add minimal entrypoint `src/main/kotlin/com/fome/exservermod/ExServerMod.kt` and `fabric.mod.json`.
  - Verify that `task pre-commit` and `task build` pass with 0 errors.
- [x] **Task 2: Hide & Seek Core State Machine (Server-side)**
  - Pure Kotlin state machine with phases (IDLE, VOTING, HIDING 120s, SEEKING, INTERMISSION 60s) and modes (CLASSIC, MOB).
  - Fabric events for damage immunity, block break prevention, elytra restriction, and hit detection.
  - Command `/games` registration with interactive chat buttons.
  - Unit tests covering phases, voting, and role swapping.
- [x] **Task 3: Landmarks & Proximity Hint System**
  - Strict i18n refactoring with ADR 0003 (`Component.translatable` only, `en_us.json`).
  - Landmark model with perimeter distance formula (`max(0.0, centerDist - radius)`).
  - LandmarkManager with JSON persistence (`config/exservermod/landmarks.json`).
  - Commands `/location-add`, `/location-remove`, `/location-list`.
  - Proximity hint broadcast (every 50s) and 20-block movement alert in Hide & Seek.
  - Unit tests in `LandmarkTest.kt`.
- [x] **Task 4: Mob Disguise Engine & Scale Hitbox System**
  - Curated 34 balanced ground mob pool (zero bosses, zero flying entities).
  - Dynamic scale adjustment via vanilla `Attributes.SCALE` (`max(mobWidth / 0.6, mobHeight / 1.8)`).
  - Infinite invisibility without particles (`MobEffects.INVISIBILITY`).
  - Automated anti-stuck safeguard relocating trapped players to safe air blocks.
  - Strict stealth invariant: only hider knows their disguise during hiding/seeking; revealed at intermission.
  - State tracking, cleanup on disconnect, intermission, idle, and server stop.
  - Comprehensive unit tests in `DisguiseTest.kt`.
- [ ] **Task 5: Visual Roulette Screen & Client Animation**
