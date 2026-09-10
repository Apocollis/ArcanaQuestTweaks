# Comfort module (1.8)

Last updated: 2026-09-09.

JSON (two files under `config/arcanaquesttweaks/`):

- `aqtweaks_comfort_settings.json` — thresholds, promote, pet value, penalties, bonuses
- `aqtweaks_comfort_blocks.json` — `category_limits` and `categories`

Always registered. No parent “Comfort mod” — Tweaks-owned, with optional hooks into other mods’ potions and warp.

The old combined `aqtweaks_comfort.json` is **not** loaded. Leave it on disk until you have copied `categories` / `category_limits` into the blocks file.

There is no Forge `@Config` for comfort and no mixin into those mods. Missing mods skip that benefit.

## Locked intent

A homestead rest loop: scan nearby “cozy” blocks and tamed pets, add potion bonuses, subtract player-state penalties, then apply a custom **Homestead** potion plus Soot XP boost, (II/III) Elenai stamina potions, and (if loaded) Thaumcraft temp-warp drain. Also grants Simple Difficulty **cold resist** while standing in Biomes O' Plenty hot spring water.

Category caps are the design. Uncapped sums turn a chandelier farm into Homestead III. Penalties **do not** have a global cap: eat, drink, heal, warm/cool, and rest to recover.

## How parent/vanilla pieces work

| Source | What it provides | Tweaks use |
| --- | --- | --- |
| Vanilla | Sleep, sneak, ride, potions, food, health, `EntityTameable` | Rest detection, pet scan, hunger/health penalties |
| Tweaks | `PotionHomestead` | HUD icon `assets/aqtweaks/textures/gui/homestead_icon.png`, registered on `RegistryEvent.Register<Potion>` as `aqtweaks:homestead` |
| Lang | `en_us.lang` | `effect.aqtweaks.homestead=Homestead` (`getName()` returns that key; no `setTranslationKey`) |
| Thaumcraft | Temp warp capability | Same `ThaumcraftHelper.reduceWarp` as the TC module; progress NBT `WarpCleansingProgress` (not `WarpExposureProgress`) |
| Simple Difficulty | thirst, body temp, `cold_resist` / `heat_protection` / `cold_protection` / `heat_resist` | `Reflect` for thirst/temp; potions by resource name — null-safe |
| Somnia | `somnia:sleepy` / `exhausted` / `fading` | default rows in `penalties.effects` |
| Farmer's Delight | `farmersdelight:comfort` | default row in `bonuses.effects` |
| Soot | `soot:experience_boost` | Homestead refresh 8:00 |
| Elenai Dodge 2 | `elenaidodge2:endurance`, `elenaidodge2:replenishment` | II/III only |
| Biomes O' Plenty | `biomesoplenty:hot_spring_water` | Block at feet or head |

## Design plan (rest loop)

Server only. `TickEvent.PlayerTickEvent` **END**, every **300 ticks** (15s) on `ticksExisted % 300 == 0`.

1. If NBT `AQTComfortResting` is false: player must pass `isPlayerResting`; **effective** score ≥ Homestead I → set tag, granted band **I**, stamp `AQTComfortBandSince`, apply I benefits.
2. If already resting: **do not** require sneak/sleep/still. Rescan effective score. Below I → clear tag, ladder NBT, and Homestead (XP/Elenai potions are **not** stripped).
3. Granted band promotes **one step** after `promote_ticks` (default 1200 / 60s) while score still supports the next band. Demote **immediately** if score cannot support the granted band.
4. `LivingHurtEvent` on a player, or `AttackEntityEvent`, clears the tag and ladder immediately (any hurt, not “damage after armor”).

`isPlayerResting` (entry only):

- Sleeping in a bed, **or**
- **`isRiding()`** (mounts, boats, minecarts, chairs that use riding — not a sit-pose check), **or**
- Sneaking, **or**
- Nearly stationary: `motionX² + motionZ² < 0.001` (Y motion ignored).

## Scoring

Scan **25×5×25** (3,125 cells) from the player position: horizontal radius **±12 inclusive**, **dy -2..+2**. Skip unloaded blocks. Look up each block’s registry id in `COZY_BLOCKS`.

Pets: `EntityTameable` in AABB grown **16** from the player. Count if `isTamed()` and `ownerId` equals the player. Each pet adds `pet_comfort_value` under category `pets`.

Per category: sort weights descending, sum only the top **N** (`category_limits`, default 1 if missing). Same block id in two categories cannot happen; last apply wins if the JSON repeats an id.

Then add **all** active `bonuses.effects` amounts. Then subtract formula penalties plus **all** active `penalties.effects` (no global cap):

| Source | Formula |
| --- | --- |
| Temperature (SD) | 1.5 per body-temp point outside 11–14; **0** if heat potions while hot or cold potions/resist while cold |
| Thirst (SD) | `(20 − thirst) × 0.75` |
| Hunger | `(20 − food) × 0.5` |
| Health | `(1 − hp/maxHp) × 15` |
| Potion effects | sum of `penalties.effects` whose potion is active (defaults: Somnia sleepy 10, exhausted 25, fading 40) |

`bonuses.effects` default is `farmersdelight:comfort` **+10**. Duplicate potion rows stack. Somnia only leaves one fatigue effect on the player, so those three rows do not stack in play.

`effective_score = max(0, cozy + bonuses − penalties)`.

Thresholds are floats in JSON (defaults 15 / 40 / 60). They set the **maximum** band the score allows. Warp and extra potions use the **granted** band:

| Granted | After | HUD | Other |
| --- | --- | --- | --- |
| I | immediately when score ≥ I | Homestead I (amp 0) | +9 warp / 15s; `soot:experience_boost` amp 0, 8:00 |
| II | 60s at I while score ≥ II | Homestead II (amp 1) | +13 warp; XP boost amp 1, 8:00; `elenaidodge2:endurance` amp 0, 8:00; `elenaidodge2:replenishment` 4:00 |
| III | 60s at II while score ≥ III | Homestead III (amp 2) | +25 warp; XP boost amp 2, 8:00; endurance amp 1, 8:00; replenishment 8:00 |

Homestead potion duration is **340** ticks. XP / endurance / replenishment are **re-applied** each scan while that band is held; when Homestead ends they **count down** (not stripped). All `PotionEffect`s use ambient **true**, particles **false**. No regen, saturation, or SD thermals from Homestead.

### Warp cleanse math

Only if `thaumcraft` is loaded. Progress is persisted NBT `WarpCleansingProgress`. At **100**, if temporary warp (`ThaumcraftHelper` type **1**) is &gt; 0, reduce 1 and `syncWarp`. Progress then **resets to 0** even if warp was already 0 (progress is spent).

Approximate time to 1 temp warp at the interval:

- I: +9 / 15s → ~167s
- II: +13 / 15s → ~115s
- III: +25 / 15s → **60s** (the “1 warp / 60s” figure is III only)

Comfort drain is **temporary warp only**. Sleep drain is the Thaumcraft module ([thaumcraft.md](thaumcraft.md)).

### Hot springs (separate tick)

`PlayerTickEvent` **START**, every **20 ticks**. Feet block (bounding-box min Y) or the block above: registry `biomesoplenty:hot_spring_water`. Apply `simpledifficulty:cold_resist` for **200 ticks**, amp 0, ambient, no particles. Independent of Homestead score.

## JSON schema

Gson. Unknown fields ignored. Failure of **one** file uses in-memory defaults for that file only.

Both files load **once**, in `ComfortConfigLoader.load` from preInit. `ConfigEventHandler` does not reload them (it only reloads the spawn JSON), so an edit needs a **restart** — a cfg save in the in-game GUI will not pick it up.

**Settings** (`aqtweaks_comfort_settings.json`):

```json
{
  "pet_comfort_value": 3.0,
  "threshold_homestead_1": 15.0,
  "threshold_homestead_2": 40.0,
  "threshold_homestead_3": 60.0,
  "promote_ticks": 1200,
  "penalties": {
    "enabled": true,
    "temperature": {
      "enabled": true,
      "comfort_min": 11,
      "comfort_max": 14,
      "per_point_outside": 1.5,
      "heat_ignore_potions": [
        "simpledifficulty:heat_protection",
        "simpledifficulty:heat_resist"
      ],
      "cold_ignore_potions": [
        "simpledifficulty:cold_protection",
        "simpledifficulty:cold_resist"
      ]
    },
    "thirst": { "enabled": true, "per_missing_point": 0.75 },
    "hunger": { "enabled": true, "per_missing_point": 0.5 },
    "health": { "enabled": true, "per_missing_fraction": 15.0 },
    "effects": [
      { "enabled": true, "potion": "somnia:sleepy", "amount": 10.0 },
      { "enabled": true, "potion": "somnia:exhausted", "amount": 25.0 },
      { "enabled": true, "potion": "somnia:fading", "amount": 40.0 }
    ]
  },
  "bonuses": {
    "enabled": true,
    "effects": [
      { "enabled": true, "potion": "farmersdelight:comfort", "amount": 10.0 }
    ]
  }
}
```

The four formula penalties are their own objects under `penalties`. A missing object uses the defaults below; `penalties.enabled` false skips all of them.

| Object | Key | Default | Meaning |
| --- | --- | --- | --- |
| `penalties.temperature` | `enabled` | true | Off = body temp never costs |
| `penalties.temperature` | `comfort_min` / `comfort_max` | 11 / 14 | Free band |
| `penalties.temperature` | `per_point_outside` | 1.5 | Per body-temp point past the band |
| `penalties.temperature` | `heat_ignore_potions` | SD `heat_protection`, `heat_resist` | Any active while **hot** → penalty 0 |
| `penalties.temperature` | `cold_ignore_potions` | SD `cold_protection`, `cold_resist` | Any active while **cold** → penalty 0 |
| `penalties.thirst` | `enabled` / `per_missing_point` | true / 0.75 | `(20 − thirst) × per_missing_point` |
| `penalties.hunger` | `enabled` / `per_missing_point` | true / 0.5 | `(20 − food) × per_missing_point` |
| `penalties.health` | `enabled` / `per_missing_fraction` | true / 15.0 | `(1 − hp/maxHp) × per_missing_fraction` |

**Legacy aliases.** Read only when `effects` is absent on that object; `effects` always wins.

| Legacy key | Shape | Replaced by |
| --- | --- | --- |
| `penalties.somnia` | `enabled`, `sleepy_potion` / `sleepy`, `exhausted_potion` / `exhausted`, `fading_potion` / `fading` | `penalties.effects` |
| `bonuses.farmers_delight_comfort` | one `{ enabled, potion, amount }` | `bonuses.effects` |

`penalties.somnia` with `enabled: false` and no `effects` yields **no** potion penalties. Neither `effects` nor the legacy key present → built-in defaults (the three Somnia rows / Farmer's Delight +10).

**Blocks** (`aqtweaks_comfort_blocks.json`):

```json
{
  "category_limits": { "hearth": 1, "crafting": 1, "pets": 2 },
  "categories": {
    "hearth": { "farmersdelight:stove": 4.0 },
    "crafting": { "minecraft:crafting_table": 3.0 }
  }
}
```

`categories` maps category name → (block id → weight). Limits and category names must match; a category with blocks but no limit uses **1**.

### Default limits

| Category | Limit |
| --- | --- |
| hearth | 1 |
| crafting | 1 |
| bedding | 1 |
| seating | 2 |
| lighting | 3 |
| study | 2 |
| decoration | 4 |
| nature | 3 |
| structure | 8 |
| pets | 2 |

`pet_comfort_value` default **3.0**.

### Default blocks (rebuild content)

| Category | Id | Weight |
| --- | --- | --- |
| hearth | `farmersdelight:stove` | 4 |
| crafting | `minecraft:crafting_table` | 3 |
| bedding | `comforts:hammock` | 3.5 |
| bedding | `minecraft:bed` | 3 |
| bedding | `comforts:sleeping_bag` | 2 |
| seating | `bibliocraft:seat` | 3 |
| lighting | `saltmod:salt_lamp` | 2 |
| lighting | `fancylamps:gothic_lamp` | 2 |
| lighting | `rustic:iron_lantern` | 2 |
| study | `bibliocraft:bookcase` | 1.5 |
| study | `inspirations:bookshelf` | 1.5 |
| decoration | `minecraft:carpet` | 1 |
| decoration | `minecraft:standing_banner` | 1.5 |
| decoration | `minecraft:wall_banner` | 1.5 |
| nature | `minecraft:flower_pot` | 1.5 |
| nature | `minecraft:red_flower` | 1 |
| nature | `minecraft:yellow_flower` | 1 |
| structure | `rustic:slate_chiseled` | 1 |
| structure | `earthworks:block_plaster` | 1 |
| structure | `earthworks:block_adobe` | 1 |
| structure | `earthworks:block_cob` | 1 |

Missing pack blocks simply never match; they do not crash.

## Files

- `comfort/ComfortSystemHandler.java` — tick, effective score, band ladder, benefits, hot springs, cancel on hurt/attack
- `comfort/ComfortSettings.java` — settings JSON DTO
- `comfort/ComfortBlocks.java` — blocks JSON DTO
- `comfort/ComfortConfigLoader.java` — generate/load two files; merge missing `crafting` on blocks only
- `util/Reflect.java` — SD thirst/temperature getters
- `comfort/PotionHomestead.java` — potion + `RegistrationHandler`
- `assets/aqtweaks/lang/en_us.lang`
- `assets/aqtweaks/textures/gui/homestead_icon.png`

## Do not regress

- Benefits are **ambient, no particles** (`true, false` on `PotionEffect`).
- Homestead does **not** apply regen, saturation, or SD heat/cold protection. Hot-spring `cold_resist` is independent.
- XP boost / endurance / replenishment are not stripped when Homestead ends.
- Keep category caps. Blocks JSON without `crafting` gets limit 1 and `minecraft:crafting_table`; a player-defined `crafting` key is not overwritten. Combined `aqtweaks_comfort.json` is ignored.
- Comfort warp NBT is `WarpCleansingProgress`, not Thaumcraft exposure `WarpExposureProgress`.
- Homestead cleanse calls `ThaumcraftHelper` (raw `Class` only). Generic `Class<?>` on that helper made Forge `SideTransformer` drop the class and crash the server tick.
- Thermals and cold resist look up potions by name so Simple Difficulty absence never classloads SD.
- Entry requires rest pose/stillness; **continuing** rest allows walking inside the scored area.
- `isRiding()` is the sit check. Do not switch to a missing “isSitting” API and drop chair/boat rest.

## Out of scope unless asked

- Client-only particles
- Forge cfg mirror of the JSON
- Draining normal/permanent warp from Homestead
