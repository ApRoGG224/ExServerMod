# ADR 0003: Strict i18n Localization and Prohibition of Hardcoded String Literals

## Status
Accepted

## Context
In previous iterations, user-facing text, button labels, hover tooltips, and game alerts were scattered across Kotlin source files as hardcoded string literals (`Component.literal("...")`). This violates clean code standards, makes text adjustments error-prone, pollutes game logic with raw formatting codes, and prevents seamless multi-language translation.

## Decisions

### 1. Mandatory i18n via Translation Keys
- All user-facing text (chat broadcasts, command responses, interactive button labels, hover event descriptions, action bar messages, and scoreboard labels) must be rendered using `Component.translatable(key, *args)`.
- Direct user-facing string literals via `Component.literal(...)` are strictly prohibited in game logic.

### 2. Centralized Language File & Primary Language
- Primary default language is Russian:
  - `src/main/resources/assets/exservermod/lang/ru_ru.json` contains full Russian translations.
  - `src/main/resources/assets/exservermod/lang/en_us.json` also defaults to Russian so that clients without custom locale or server fallback render Russian out of the box.
  - English translations are preserved in `src/main/resources/assets/exservermod/lang/en_backup.json` for Telegram bot language switching.
- Key naming convention: hierarchical dot-notation starting with mod id:
  `exservermod.<subsystem>.<component>.<identifier>`
  (e.g., `exservermod.game.button.vote_classic`, `exservermod.landmarks.hint.closest`).

### 3. Argument Interpolation
- Dynamic values (player names, time remaining, landmark names) must use standard Minecraft translation format specifiers (`%s`, `%d`) defined in `en_us.json`.

## Consequences
- Existing hardcoded strings in Task 2 components (`GameCommands.kt`, `GameEventHandler.kt`) must be migrated to `en_us.json`.
- All future tasks must define translation keys in `en_us.json` before writing command or UI code.
