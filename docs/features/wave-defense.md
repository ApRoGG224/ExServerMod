# Wave Defense (Mob Horde) — Mini-Game Design

## Summary
`/waves` is a hardcore co-op survival mini-game: defend against 11 escalating,
brutal waves of vanilla mobs near the arena center. Designed for the FoMe SMP duo
(+ optional AFK bot spectator, who is excluded from gameplay). Zero world damage:
creeper explosions are harmless (radius 0) and spawn spectral lingering clouds;
small fireballs are intercepted on block hit to prevent fire; zombies never break doors;
champions never drop equipment.

Raid-style Bossbar at the top of the screen tracks current wave ("Wave X of 11") and
remaining horde count. Hardcore permadeath: death of a single defender immediately terminates
the defense, dissolving all remaining mobs into vapor, steam, and soul particles with ghost audio.

## Commands
- `/waves` / `/waves status` — status and start/stop buttons.
- `/waves start [landmark]` — arms the arena at the given landmark or caller's position.
  All real players (excluding Carpet fake players / Steve bot) become defenders.
  If an arena landmark (`mansion`, `arena`, or `horde`) exists, it is automatically selected.
- `/waves stop` — stops the defense, discards every wave mob, preserves vanilla integrity.

## Phases (pure state machine `WaveDefenseGame`)
`IDLE -> COUNTDOWN(15s) -> FIGHT -> (INTERMISSION(15s) -> FIGHT)* -> VICTORY(20s) -> IDLE`

- **COUNTDOWN** — yellow bossbar and action bar countdown with rising pling sounds.
- **FIGHT** — wave spawns on a 12-22 block ring around the arena center;
  surface spawn via heightmap, underground/indoor arenas spawn near the center.
  Raid bossbar (Red for standard, Purple for Boss waves) dynamically drains as mobs die.
- **INTERMISSION** — 15 s tactical breather between waves (Green bossbar).
- **VICTORY** — title + fireworks + XP orb drop at the arena center.
  Game resets to IDLE afterwards.

## 11-Wave Nightmare Progression (~1.5 Hours Full Gauntlet)
| Wave | Archetype | Composition |
|------|-----------|-------------|
| 1 | The Gathering | 10 zombies + 6 skeletons + 4 spiders (20 mobs) |
| 2 | Spectral Chill | 8 skeletons + 6 zombies + 4 cave spiders + 4 ghost creepers (22 mobs) |
| 3 | The Coven | 4 witches + 8 drowned + 6 skeletons + 4 spiders (22 mobs) |
| 4 | Wall Infiltrators | 6 spectral vexes + 8 strays + 8 zombies + 4 ghost creepers (26 mobs) |
| 5 | 💀 BOSS 1: Blaze Champion | 1 Blaze Champion (120 HP) + 6 blazes + 8 wither skeletons + 6 skeletons + 4 ghost creepers (25 mobs) |
| 6 | Ghost Incursion | 8 spectral vexes + 6 ghost creepers + 8 drowned + 8 skeletons (30 mobs) |
| 7 | Toxic Swarm | 12 cave spiders + 6 spiders + 8 zombies + 6 spectral vexes + 4 witches (36 mobs) |
| 8 | 💀 BOSS 2: Coven Mistress | 1 Witch Champion (140 HP) + 6 witches + 8 spectral vexes + 8 skeletons + 6 ghost creepers (29 mobs) |
| 9 | Nightmare Siege | 1 Dread Champion + 8 spectral vexes + 6 ghost creepers + 8 strays + 8 wither skeletons + 6 drowned (37 mobs) |
| 10 | The Dread Legion | 2 Dread Champions + 6 blazes + 8 spectral vexes + 6 ghost creepers + 6 witches + 8 wither skeletons (36 mobs) |
| 11 | 💀 APOCALYPSE / NIGHTMARE | 1 Blaze Champion + 1 Witch Champion + 2 Dread Champions + 10 spectral vexes + 6 blazes + 6 ghost creepers + 8 wither skeletons + 6 witches (40 mobs) |

## Custom Mob Mechanics & Resource Pack Nametags
- **Blaze Champion** (`exservermod.waves.mob.blaze_champion`):
  - 120 HP, 10 Armor, 0.8 Knockback Resistance, glowing outline.
  - **Hellfire Barrage**: triple spread fireball volley (intercepted by `SmallFireballMixin` so zero fire is placed on blocks).
  - **Infernal Heat Wave**: radiates through mansion walls every 12s within 24 blocks, igniting and dealing magic damage.
- **Witch Champion** (`exservermod.waves.mob.witch_champion`):
  - 140 HP, Speed 0.32, 0.7 Knockback Resistance, glowing outline, brush/broomstick item.
  - **Broom Dash**: dashes forward with levitation and speed glide.
  - **Potion Volley**: throws volleys of splash potions (slowness, harming, poison, weakness).
- **Ghost Creeper** (`exservermod.waves.mob.ghost_creeper`):
  - Explosion radius 0 (0 block damage, 0 grief).
  - Upon detonation or death, spawns an `AreaEffectCloud` of lingering spectral fog (Darkness, Slowness II, Poison) with phantom screams.
- **Spectral Vex** (`exservermod.waves.mob.spectral_vex`):
  - 24 HP, natively phases through blocks, ceilings, and doors to flush out players hiding inside the mansion.
- **Dread Champion** (`exservermod.waves.mob.dread_champion`):
  - 90 HP, full diamond armor and sword, ground slam shockwaves.

## Anti-Turtle Island Mansion Mechanics
- **Wall-Piercing Evoker Fangs**:
  If a defender camps in a room or closet without moving for > 8 seconds, or periodically during boss encounters,
  evoker fangs erupt from the floor beneath them (with a 1.0s audio warning). Works on any floor of the mansion.
- **Spectral Vexes & Heat Wave**:
  Spirits and infernal heat wave ignore line-of-sight and physical barriers.

## Permadeath Failure
If ANY defender dies during active defense:
- Game terminates immediately (no continuation).
- Every remaining wave mob dissolves into steam/vapor particles (`POOF`, `CLOUD`, `SOUL`) with extinguishing and soul audio.
- Defeat title and subtitle displayed to defenders; bossbar removed.

## World Integrity Invariants
- `SmallFireballMixin`: cancels block hits from wave fireballs → 0 fire blocks placed.
- Creepers: explosion radius set to 0 → 0 block damage.
- Zombies: `setCanBreakDoors(false)` → doors cannot be broken.
- Loot: all custom/champion armor and weapon drop chances set to 0.0f → 0 economy duplication.
- Wave mobs are tracked by UUID and discarded on server stop or command abort.
