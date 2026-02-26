# Rewrite QA Checklist

This checklist validates the v6 rewrite on Forge 1.8.9 with the new game-state manager, provider system, and Bedwars module logic.

## Preconditions
- Use latest rewrite build and ensure OneConfig opens.
- Test on Hypixel with a real account.
- For provider tests, have at least one valid Hypixel API key available.

## 1. Startup and Config
- Launch client and confirm mod loads without startup errors.
- Open OneConfig and verify `Stats Provider` has:
  - `Hypixel Public API`
  - `Nadeshiko`
  - `Abyss`
- Verify `Hypixel API Key` field is visible under `Stats`.

Pass criteria:
- No crashes or missing config sections.

## 2. Provider Selection and Key Behavior

### 2.1 Hypixel provider without key
- Set provider to `Hypixel Public API`.
- Leave API key empty.
- Run `/bw <known-player>`.

Pass criteria:
- Mod does not crash.
- You get a warning indicating Hypixel provider is selected without key.
- Stats-dependent paths degrade gracefully (no hard failure of the mod).

### 2.2 Hypixel provider with valid key
- Set valid key.
- Run `/bw <known-player>` and `/who` in Bedwars.

Pass criteria:
- Bedwars stats return normally.
- Tab stats populate.

### 2.3 Nadeshiko and Abyss manual selection
- Switch to `Nadeshiko`, then `Abyss`.
- Run `/bw <known-player>` and `/who` for each provider.

Pass criteria:
- Provider switches take effect manually.
- No automatic fallback occurs.
- Data retrieval works according to selected provider.

## 3. Game State Detection

### 3.1 Lobby and game transitions
- Join Hypixel lobby, then queue Bedwars.
- Observe transitions:
  - Lobby/prequeue
  - Bedwars pregame
  - Active match
  - Return to lobby after game

Pass criteria:
- State transitions occur without stale state.
- No leftover timer values after leaving game.

### 3.2 Pregame detection behavior
- In Bedwars pregame, wait for players to chat.
- Confirm pregame stat lookups trigger once per player.
- Once match starts, pregame behavior stops.

Pass criteria:
- Pregame stats only occur in pregame.
- No repeated spam per same player.

### 3.3 Party state updates
- Join/leave a party and promote a member if possible.

Pass criteria:
- No errors/crashes while party changes happen.
- Party updates are consumed by the game-state layer without desync.

## 4. Timer Validation (Emerald and Diamond)

### 4.1 Start and progression
- Enter an active Bedwars match.
- Enable both HUDs:
  - Emerald Counter HUD
  - Diamond Counter HUD
- Observe values through multiple stage changes.

Pass criteria:
- Counters increment predictably.
- Next-spawn times decrement correctly.
- No negative or frozen values.

### 4.2 Stage transition correctness
- Verify behavior around:
  - Diamond II (~6:00)
  - Emerald II (~12:00)
  - Diamond III (~18:00)
  - Emerald III (~24:00)

Pass criteria:
- Spawn intervals update to expected tier values.

### 4.3 Reset behavior
- Leave game to lobby and queue again.

Pass criteria:
- Timer state fully resets between matches.

## 5. Star Formatting Validation
- Check players across multiple star ranges (sub-1000, 1000+, etc.).
- Compare displayed star style against expected prestige formatting.

Pass criteria:
- Star text is formatted consistently with prestige mapping.
- No `NaN`/fallback artifacts for normal inputs.

## 6. Existing Feature Parity

### 6.1 Commands
- Validate:
  - `/bw <username>`
  - `/mellow`
  - `/blacklist ...`
  - `/denick ...`
  - `/skindenick <username>`
  - `/urchin ...`
  - `/seraph ...`

Pass criteria:
- Commands execute with expected behavior and no regressions.

### 6.2 Tab stats + tags
- Enable tab stats and tag display.
- Trigger `/who` in Bedwars.

Pass criteria:
- Tab decorations render correctly.
- Urchin/Seraph tags still appear per config.

### 6.3 Upgrades & traps HUD
- Purchase upgrades/traps and trigger traps in-game.

Pass criteria:
- HUD updates correctly.
- State resets at new game start.

## 7. Stability and Regression
- Play multiple consecutive games.
- Switch providers between games.
- Trigger rapid world changes (queue hopping/lobby switches).

Pass criteria:
- No crashes, lockups, or severe chat spam.
- No stale cache/state causing obviously incorrect output.

## Optional Debug Commands
- `./gradlew compileJava --no-daemon`

Pass criteria:
- Build succeeds locally.
