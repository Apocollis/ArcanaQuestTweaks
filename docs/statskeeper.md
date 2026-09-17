# Stats Keeper module (1.8)

Last updated: 2026-09-16.

No mixin. Optional bus handler if Stats Keeper is present. Tweaks cfg: `config/arcanaquesttweaks/aqtweaks_statskeeper.cfg`.

## Locked intent

Stop players from drinking `contenttweaker:life_elixir` (Elixir of Vitality) once Stats Keeper would refuse further max-health. Vanilla food finish would otherwise consume the stack after SK `addHealth` returns false. A successful drink plays the configured sound (default vanilla `ENTITY_PLAYER_LEVELUP`).

Do **not** gate on `player.getMaxHealth()`. Equipment, Baubles, and temporary max-health buffs must not lock the elixir before SK additional health has actually reached the configured cap.

Do **not** mixin Stats Keeper. Do **not** `@Mod required-after:stats_keeper`. Do **not** walk every `SKHealthConfig.health_items` entry — only the pack elixir.

## How the parent works

Stats Keeper (`stats_keeper`, jar `StatsKeeper-1.12.2-3.1.13.jar`) stores extra hearts on `IHealth` (`SKCapabilities.getCapability`). Pack `config/stats_keeper.cfg` (not Tweaks):

- Health enabled; min **20**, max **40**, starting **MIN**, reduction **0**
- Health item: `contenttweaker:life_elixir, 2` (+1 heart)

`HealthEventHandler.itemUse` (`PlayerInteractEvent.RightClickItem`) returns immediately when the stack’s use action is `DRINK`, so drinks never consume on click. `itemUseFinished` (`LivingEntityUseItemEvent.Finish`) then calls `addHealth`. That method returns false (no stack shrink) when `getCurrentHealth` (attribute **base** + `getAdditionalHealth()`) is already `>= SKHealthConfig.max_health`. Vanilla `ItemFood` still finishes the drink.

ContentTweaker registers the elixir as always-edible food with `itemUseAction = "DRINK"` (`scripts/contenttweaker/life_elixir.zs` in the pack, not this repo).

## Design plan

`LifeElixirCapHandler` on `MinecraftForge.EVENT_BUS` only when `Loader.isModLoaded("stats_keeper")`. Compile-hard `IHealth`, `SKCapabilities`, `SKHealthConfig`. Keep those imports off `CommonProxy` and `ArcanaQuestTweaksConfig`.

Cancel **before** Finish:

1. `LivingEntityUseItemEvent.Start` (client and server). Cancel when the player is not a spectator, the stack is `contenttweaker:life_elixir`, SK health is enabled, the cap is non-null, and `(int) MAX_HEALTH.getBaseValue() + cap.getAdditionalHealth() >= SKHealthConfig.max_health`. Server action bar: `chat.aqtweaks.life_elixir.max_health` (red).
2. `PlayerInteractEvent.RightClickItem` (client and server, **no** message). Same gates so a held RMB cannot start the drink animation. Message stays on `Start` so hold-RMB does not spam.
3. `LivingEntityUseItemEvent.Finish` (server only). If the stack is still the elixir and SK health is enabled, resolve `StatsKeeperModuleConfig.general.elixirDrinkSound` once (empty = silent; unknown/invalid = `ENTITY_PLAYER_LEVELUP`) and `world.playSound(null, …, sound, PLAYERS, 0.75F, 1.0F)`. Cap cancels `Start`, so this does not run when the drink is refused. No tick or packet path.

Missing Stats Keeper jar → handler not registered; Tweaks still boots. Compile still needs the jar in `libs/`.

## Files

| Piece | Role |
| --- | --- |
| `statskeeper/LifeElixirCapHandler.java` | Cancel elixir use at SK cap; configured sound on successful Finish |
| `ArcanaQuestTweaksConfig.StatsKeeperModuleConfig` | Nested `general`; `aqtweaks_statskeeper.cfg` |
| `assets/aqtweaks/lang/en_us.lang` | `chat.aqtweaks.life_elixir.max_health` |

## Live config (`config/arcanaquesttweaks/aqtweaks_statskeeper.cfg`)

| Category / Key | Type | Default | Description |
| --- | --- | --- | --- |
| `general` / `Elixir Drink Sound` | string | `minecraft:entity.player.levelup` | Sound event id on a successful elixir drink. Empty disables. Unknown or invalid ids fall back to level-up. Hand-edit needs a restart; in-game Tweaks GUI syncs without one. |

Cap still follows live `SKHealthConfig.max_health` / `enabled`. Item id is pack-pinned.

## Do not regress

- Do not add a Stats Keeper mixin json or put SK types on always-on bus classes.
- Do not use `player.getMaxHealth() >= 40` as the lock.
- Do not cancel `Finish` instead of `Start` (too late; vanilla already consumed). Finish is sound-only.
- Do not send the action-bar message from `RightClickItem`.
- Do not play a drink sound when the drink is cancelled at cap.
- Do not import Stats Keeper types from `ArcanaQuestTweaksConfig`.
- Do not put string/primitive fields on a `@Config(..., category = "")` class (Forge empty-category crash). Nest them under `general`.
- Do not treat ContentTweaker as a compile parent.

## Verify

1. Build succeeds with `options.release = 21` via `build_gradle.ps1`.
2. At 10 hearts: drink consumes, +1 heart, no Tweaks message, configured drink sound (default level-up). Empty cfg string: silent. Bad id: level-up.
3. At 20 hearts: drink cancelled immediately, stack remains, red action bar `Your vitality is already at its peak!`, no stuck drink animation.
4. Temporary max-health buff / Baubles while additional health is still below cap: elixir still usable.
5. Strip Stats Keeper: Tweaks still loads; no SK mixin to skip.
