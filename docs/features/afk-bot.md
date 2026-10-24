# AFK Bot Control (Carpet fake player "Steve")

## Summary
The AFK chunk-loader bot is a Carpet fake player (default nick `Steve`,
spawned via Telegram or `/player Steve spawn`). The mod now treats it as a
first-class object:

1. **Games exclusion** — the bot can never be picked as a hide & seek opponent,
   never counts towards the 2-player requirement, never joins wave defense, and
   its disconnects never cancel an active round (detection: Carpet
   `EntityPlayerMPFake` class name or the reserved nick).
2. **Landmark relocation** — the bot teleports to any landmark registered via
   the regular `/location-add` command. One shared list for hints and bot
   travel, exactly as intended.

## Commands
- `/afk` — bot status (online? position?) + clickable list of landmarks;
  clicking sends the bot there.
- `/afk tp <landmark>` — teleport the bot to a landmark (tab-completion).
- `/afk random` — teleport to a random landmark (never the current nearest).
- `/afk here` — teleport the bot to the sender (safe column nearby).
- `/afk spawn [landmark]` — spawn the bot via Carpet at a landmark
  (random if omitted; falls back to world spawn when no landmarks exist).
- `/afk kill` — remove the bot (runs Carpet `player Steve kill`).

## Mechanics
- Teleport target is a safe surface column: heightmap top, air feet+head,
  sturdy non-liquid floor; spiral fallback up to 3 blocks.
- Cross-dimension landmarks resolve via `server.getLevel`; unsupported
  dimensions report a clear error.
- Portal particles + enderman sound on both ends (cosmetic only).
- Telegram button "🧭 АФК → локация" (`/afk_goto`) runs `/afk random` through
  the Exaroton command API.

## Tests
`AfkManager.pickRandomLandmark` logic is exercised indirectly through game
tests; landmark lookup itself reuses `LandmarkManager`.
