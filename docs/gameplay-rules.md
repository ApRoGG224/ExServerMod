# Gameplay & World Integrity Invariants

## 1. Core Design Philosophy
- **Thoughtful Vanilla**: The mod expands vanilla survival without turning the game into an unbalanced sandbox or cheat-like environment.
- **Fair Survival Balance**: No god-modes, no unearned flight, no destructive mechanics that ruin vanilla server balance.

## 2. World Integrity & Block Protection
- **Zero World Griefing**: Custom abilities, transformations, entities, and projectiles must NEVER break, replace, or destroy blocks in the world.
- **Transformation Invariant (e.g., Ender Dragon)**:
  - If a player transforms into or summons an Ender Dragon (or similar destructive boss mob), block-breaking logic (`destroyBlocks`) must be explicitly disabled/cancelled.
  - Entities must pass through blocks or collide harmlessly without modifying terrain.
- **Explosion / Ability Safeguards**: Any custom area-of-effect or explosive effect must be configured to cause 0 block damage (`BlockInteraction.KEEP` or cancelling block destruction events).

## 3. Movement & Teleportation Invariants
- **No Teleportation Abilities**: Custom movement mechanics must not include instant teleportation (blink, warp, instant dash through solid matter).
- **Anti-Stuck Exception ONLY**: Teleportation is strictly forbidden except as an automated anti-stuck / unstuck safeguard:
  - Triggered only when a player is stuck, suffocating inside solid blocks, or clipped through geometry due to desync/lag.
  - Relocates the player strictly to the nearest valid safe air space above solid ground.

## 4. Anti-Cheat & Exploit Prevention
- All state changes, cooldowns, and ability validations must run on the dedicated server side.
- No client-authoritative checks or mechanics.
