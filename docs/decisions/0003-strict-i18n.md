# ADR 0003: Strict Internationalization (i18n) via Translatable Components

## Status
Accepted

## Context
ExServerMod previously used hardcoded text strings via `Component.literal("...")` across chat messages, buttons, hover text, and action bar counters. To maintain professional quality, prevent fragmentation, and enable modular localization, a strict internationalization invariant is required.

## Decisions

### 1. Mandatory `Component.translatable`
- All user-facing text (chat messages, action bar counters, button texts, hover descriptions, command responses, and broadcast alerts) must be constructed using `Component.translatable(key, *args)`.
- Using `Component.literal("...")` for user-visible text is strictly prohibited.
- Formatting styles (such as `withStyle(ChatFormatting.COLOR)`) and interaction events (`withClickEvent`, `withHoverEvent`) remain permitted on translatable components.

### 2. Central Translation Catalog
- All translation keys are defined and maintained in `src/main/resources/assets/exservermod/lang/en_us.json`.
- Key naming convention: `exservermod.<module>.<submodule_or_action>.<detail>`, e.g., `exservermod.game.status.voting`, `exservermod.landmarks.hint.closest`.

## Consequences
- Requires refactoring of existing commands and event handlers.
- New features must define their language keys in `en_us.json` before implementation.
