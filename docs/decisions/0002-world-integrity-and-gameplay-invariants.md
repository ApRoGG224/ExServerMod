# ADR 0002: World Integrity and Non-Destructive Gameplay Invariants

## Status
Accepted

## Context
The FoMe server runs a persistent multiplayer survival world. Custom mod abilities and mob transformations must not cause accidental or intentional destruction to player builds, terrain, or survival balance.

## Decisions

### 1. Total Prohibition of Block Destruction by Abilities
- All custom abilities, spells, projectiles, and transformations are forbidden from destroying, replacing, or griefing blocks.
- Entity behaviors with native block-breaking capabilities (such as the Ender Dragon's block destruction behavior) must be intercepted and suppressed.

### 2. Prohibition of Teleportation Mechanics
- Teleportation abilities (such as blink or point-to-point warp) are prohibited to preserve vanilla movement dynamics.
- Sole permitted exception: anti-stuck / unstuck fallback logic to recover players glitched or suffocating in solid geometry.

### 3. Server-Authoritative Vanilla Balance
- All mechanics must be server-validated.
- Gameplay additions must feel like thoughtful, integrated vanilla extensions rather than cheats.

## Consequences
- Requires event listeners/mixins to cancel block modification where vanilla entity AI or abilities would naturally break blocks.
- Movement mechanics must rely on physics, momentum, or status effects rather than coordinate offsets.
