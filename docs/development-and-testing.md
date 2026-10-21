# Development & Testing Workflow

## 1. Zero-Friction Modern Testing (No Manual Jar Copying)

### 1.1 Running Isolated Client & Server via Gradle
Never copy jars manually into `.minecraft`. Gradle manages sandbox runtime environments in `run/`:
- **Run Client Sandbox**:
  ```bash
  ./gradlew runClient
  ```
  Launches a deobfuscated Minecraft client with `exservermod` pre-installed and asset hot-reloading enabled.
- **Run Dedicated Server Sandbox**:
  ```bash
  ./gradlew runServer
  ```
  Launches a local Fabric dedicated server for testing multiplayer packets and server-authoritative logic.

---

## 2. Hot-Reload (Code Swap in ~1 Second)
When testing in IDE (IntelliJ IDEA / VS Code with Kotlin Debugger) on **JetBrains Runtime (JBR)**:
1. Launch the game in **Debug** mode (`Debug 'Minecraft Client'`).
2. Make code edits in `.kt` files (state machine logic, UI positions, animation curves, timer constants, colors).
3. Trigger **Reload Changed Classes** (`Cmd + F9` in IntelliJ, or Hot-Swap in VS Code).
4. **The JVM swaps the bytecode in the active running Minecraft instance within 1 second.**
   - No client restart required.
   - Continue testing immediately in the running world.

---

## 3. Sub-Second Automated Unit Tests
Pure game logic, math algorithms, and state machines are decoupled from the Minecraft renderer and tested via JUnit 5 / Kotlin Test:
```bash
task test
# or
./gradlew test
```
- Tests run in **< 500 milliseconds**.
- Test coverage covers:
  - Game state machine transitions (`IDLE` -> `VOTING` -> `HIDING` -> `SEEKING` -> `INTERMISSION`).
  - Landmark horizontal distance calculation algorithms.
  - Mob selection randomizer and blacklist filters.
  - Easing interpolation math (`easeOutCubic`) for roulette animations.

---

## 4. Pre-Commit Verification Gate
Before any commit or deployment, run:
```bash
task pre-commit
```
Runs both `task lint` (ktlint style check) and `task test`. Must finish with 0 errors.

---

## 5. Production Pitfalls & Combat Solutions (Troubleshooting)
Key operational lessons from real deployments:
- **BlueMap Resource Download**: BlueMap fails to load unless `accept-download: true` is explicitly configured in `/server/config/bluemap/core.conf`.
- **Command Clicks in Modern Minecraft**: When using clickable text components in chat, modern Minecraft clients verify commands through `restrictedSuggestionsProvider`. Complex subcommands may be blocked; use direct literals (e.g. `/games classic`, `/games mob`).
- **Disguises vs Vanilla Invisibility**: Never rely on `MobEffects.INVISIBILITY` for mob disguises as it creates client rendering bugs and remote player invisibility. Use packet-level interception (`ServerEntityMixin.java`) and `ClientDisguiseHolder.kt`.
- **Detailed Runbook**: See [docs/combat-rules-and-troubleshooting.md](file:///Users/soroqn/me/private-code/Minecraft-FoMe/ExServerMod/docs/combat-rules-and-troubleshooting.md).
