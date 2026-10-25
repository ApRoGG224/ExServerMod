# Ideas & Future Plans (owner: soroqn)

Raw idea backlog recorded straight from the owner. Nothing here is implemented
yet — each item lists what the mod side would need. Pick items from here when
planning new rounds.

---

## Wave Defense: Halloween Challenge Arena (IMPLEMENTED — Round 3, 2026-10)

Implemented in Round 3:
- Dedicated arena landmark binding via `/waves start [landmark]`.
- 11-wave nightmare balance with raid-style bossbar and hardcore permadeath.
- Custom mobs & attacks: Blaze champion (Hellfire barrage + Infernal heat wave), Witch champion (Broom dash + Potion volleys + Coven curse), Ghost creepers (Lingering spectral darkness/slowness cloud, 0 block damage), Spectral vexes (phasing through walls).
- Anti-turtle Evoker fangs targeting stationary defenders in the mansion.
- Nametags formatted with regex-friendly identifiers for resource-pack reskins (`ex_blaze_champion`, `ex_witch_champion`, `ex_ghost_creeper`, `ex_spectral_vex`).

### 1. Dedicated Halloween arena
- The arena is a pre-built, hand-made location — not the sender's position.
- Mod side: bind the arena to a landmark (reuse `LandmarkManager`), e.g.
  `/waves start <landmark>` or a fixed `config/exservermod/arena.json` pin;
  spawn ring / announce positions anchor to that spot.
- Keep the zero-grief invariants — the arena is hand-built, mobs must never
  damage it (creeper radius-0 trick stays).

### 2. Ultra-hard challenge balance
- Goal: unprepared players should genuinely struggle; this is a challenge
  mode, not a casual farm.
- Mod side: difficulty flag (`/waves start nightmare`), elite/champion stats
  scaling, more mixed waves, maybe limited breathers on high waves.

### 3. Custom mob attacks
- Scripted special attacks for waves beyond vanilla AI.
- Candidate attacks (owner's ideas, open to expansion): witch spell volleys,
  broom-charge passes, creeper "ghost" lingering clouds instead of explosions.
- Constraints: all server-side, vanilla items/potions/particles only, still
  zero world damage.

### 4. Reskinned mobs via resource pack (nametag-driven)
- Owner plans a texture/resource pack that swaps mob visuals when the mob has
  a specific nametag: witch with a broom, piglin variants, "ghost creeper".
- Technique: OptiFine-style random entities properties (`name=<regex>` match
  against custom nametags) — the pack keys off the nametag text.
- Mod side: the mod already gives champions stable custom names
  (`exservermod.waves.champion_name`); extend with named elite variants
  (stable, i18n-agnostic codes if needed) so pack regexes can match reliably.
- The pack itself lives outside this repo.

---

## Earlier mini-game ideas (from the original request, 2026-10)

- Capture the point / capture the flag (owner builds point-based modes
  elsewhere; would love an in-game version).
- Bomb defusal style mode.
- "Or all of them together" — combining modes is on the table.
- Wave defense was the first one built (see [wave-defense.md](features/wave-defense.md)).
