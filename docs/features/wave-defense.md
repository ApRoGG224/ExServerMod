# Wave Defense (Mob Waves) — Mini-Game Design

## Summary
`/waves` is a co-op survival mini-game: defend against 8 escalating waves of
vanilla mobs near the starting point. Designed for the FoMe SMP duo (+ optional
AFK bot spectator). Zero world damage: creeper explosions are cancelled for
game-spawned mobs, zombies never break doors, champions never drop their gear.

> **Planned evolution:** a dedicated Halloween arena location, ultra-hard
> challenge balance, custom mob attacks, and nametag-driven resource-pack mob
> reskins (broom witches, ghost creepers). Full write-up in
> [../ideas.md](../ideas.md).

## Commands
- `/waves` / `/waves status` — status and start/stop buttons.
- `/waves start` — arms the arena at the caller's position. All real players
  (Carpet fake players excluded) become defenders.
- `/waves stop` — stops the defense, discards every wave mob, keeps vanilla loot.

## Phases (pure state machine `WaveDefenseGame`)
`IDLE -> COUNTDOWN(15s) -> FIGHT -> (INTERMISSION(8s) -> FIGHT)* -> VICTORY(20s) -> IDLE`

- **COUNTDOWN** — action bar countdown with rising pling sounds.
- **FIGHT** — wave spawns on a 10-18 block ring around the arena center;
  surface spawn via heightmap, underground arenas spawn near the center.
  Action bar: wave progress, mobs left, kills.
- **INTERMISSION** — 8 s breather between waves.
- **VICTORY** — title + fireworks + XP orb drop at the arena center.
  Game resets to IDLE afterwards.

## Wave Composition (vanilla only)
| Wave | Mobs |
|------|------|
| 1 | 4 zombies |
| 2 | 3 zombies + 2 skeletons |
| 3 | 3 spiders + 3 zombies |
| 4 | 3 skeletons + 3 zombies + 2 cave spiders |
| 5 | 💀 1 champion zombie + 4 zombies |
| 6 | 2 creepers + 3 zombies + 3 skeletons |
| 7 | 1 witch + 3 skeletons + 3 drowned |
| 8 | 💀 2 champions + 3 spiders + 2 creepers + 3 zombies |

Champions: vanilla diamond armor + sword, 40 HP, custom name, 0% drop chance
(no economy impact).

## World Integrity Invariants
- Explosion guard: `ExplosionEvents.START` cancels any explosion whose source
  is a tracked wave mob → 0 block damage, 0 fire.
- Zombies spawn with `setCanBreakDoors(false)`.
- Withers/ghasts/ravagers are explicitly excluded from the composition.
- Wave mobs are tracked by UUID and discarded on stop/abort/server stop.
- Stuck-wave protection: mobs that despawn or unload are reaped every second,
  so a wave can never soft-lock the game.
- If every defender leaves the server, the defense aborts and mobs are cleared.

## Testing
`WaveDefenseGameTest` covers the pure state machine: phase transitions, kill
counting, victory idempotency, boss waves, and the zero-grief composition
whitelist.
