# Feature Specification: Hide & Seek with Mob Disguise & Visual Roulette

## 1. Overview
Hide & Seek is an interactive duel mini-game built for the FoMe survival server on Fabric (Minecraft 1.21.1) in **Kotlin**.
It features a state machine, voting, custom phase timers, complete damage immunity during the event, a landmark proximity hint system, an in-game animated visual roulette wheel (`RouletteScreen`), and a Mob Disguise mode with dynamic hitbox scaling (`Attributes.SCALE`) and camera height adjustment.

---

## 2. Core Game Loop & State Machine

```mermaid
flowchart TD
    IDLE["IDLE (Standby)"] -->|/games vote| VOTING["VOTING Phase"]
    VOTING -->|2 votes for Classic| HIDING_CLASSIC["HIDING Phase (Classic)"]
    VOTING -->|2 votes for Mob Mode| HIDING_MOB["HIDING Phase (Mob Mode)"]
    VOTING -->|Cancel / Timeout| IDLE

    HIDING_CLASSIC -->|120s timer / Early start| SEEKING["SEEKING Phase"]
    HIDING_MOB -->|120s timer / Early start| SEEKING
    HIDING_MOB -->|Opens roulette screen| ROULETTE["Visual Roulette Spin"]

    SEEKING -->|Seeker hits Hider| INTERMISSION["INTERMISSION (Results)"]
    SEEKING -->|/games leave| IDLE

    INTERMISSION -->|Click Continue| SWAP_ROLES["Swap Roles & Next Round"]
    INTERMISSION -->|60s timeout / Click End| IDLE
    SWAP_ROLES --> HIDING_CLASSIC
```

### Phase Details:
1. **`IDLE`**: No game active. Scoreboard clean.
2. **`VOTING`**: Initiated via `/games`. Interactive chat buttons for `Classic` or `Mob Disguise`. Requires both players to vote for the same mode.
3. **`HIDING`** (120 seconds):
   - **Elytra**: Allowed for the Hider to reach high/distant spots.
   - **Action Bar**: Displays countdown timer.
   - **Early Start**: `[Start Search Early]` button in chat allows ending prep early if ready.
   - **Mob Mode**: Hider triggers/receives visual roulette spin, receives mob disguise and scaled hitbox.
4. **`SEEKING`** (Hunting phase):
   - **Elytra Invariant**: Hider is strictly forbidden from using Elytra (`stopFallFlying()`). Seeker is allowed to fly.
   - **Block Breaking**: Strictly cancelled for both players (`PlayerBlockBreakEvents.BEFORE`).
   - **Immortality**: Both players are immune to all damage (`ServerLivingEntityEvents.ALLOW_DAMAGE`).
   - **Hit Detection**: Seeker landing a hit on the Hider concludes the round (`AttackEntityCallback`).
   - **20-Block Warning**: If Hider strays >20 blocks from their hiding spot, a chat alert is broadcast.
   - **Proximity Hints**: Every 45–60 seconds, broadcasts which landmark the hider is closest to.
5. **`INTERMISSION`** (60 seconds auto-reset):
   - Announces winner and elapsed time.
   - Disguise and effects cleared.
   - Interactive buttons: `[ Continue Next Round ]` (swaps roles) or `[ End Game ]`.

---

## 3. Sub-Modes
1. **Classic Mode**: Standard player models, no disguises.
2. **Mob Disguise Mode**:
   - Random mob selected from curated pool of 34 balanced mobs (no bosses, no flyers, no griefers: Zombie, Skeleton, Creeper, Iron Golem, Warden, Sniffer, etc.).
   - Dynamic scaling via vanilla `Attributes.SCALE`.
   - **Collision Guardrail / Anti-Stuck**: Large mobs (Iron Golem, Warden) cannot clip into 1x1 or 1x2 crawlspaces.
   - Client camera eye height adjusted to match mob height.

---

## 4. Visual Roulette Wheel (`RouletteScreen`)
- Client-side custom screen (`Screen`) opening during mob selection.
- Spinning tape of mob icons with smooth easing (`easeOutCubic`).
- Audio feedback: tick clicks (`UI_BUTTON_CLICK`) on passing icons.
- Visual finish: stop animation, camera shake, particle flare, fanfare sound.

---

## 5. Landmark Proximity System
- Persistent storage: `config/landmarks.json`.
- Commands:
  - `/location-add <radius> <name>` (radius defaults to 50, Y spans -64..1000).
  - `/location-remove <name>`
  - `/location-list`
- During seeking phase, computes 2D horizontal distance to all landmark cylinder perimeters.
- Outputs hint: *"Out of all builds, the hider is closest to: <Landmark>"*.

---

## 6. Commands Reference
- `/games` — check status or start voting.
- `/games vote <classic|mob>` — vote for sub-mode.
- `/games startsearch` — start hunting phase early.
- `/games continue` — advance to next round with swapped roles.
- `/games leave` — abort and reset game state.

---

## 7. BlueMap Integration & Stealth Invariant
- During `HIDING` and `SEEKING` phases:
  - Participants' map visibility is set to hidden via BlueMap API (`setPlayerVisibility(false)`).
  - Breadcrumb trails / markers are frozen/cleared.
- On `INTERMISSION` or game termination:
  - Map visibility is restored (`setPlayerVisibility(true)`).

