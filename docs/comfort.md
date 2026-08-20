# Comfort module (1.6)

Last updated: 2026-08-20.

JSON: `config/arcanaquesttweaks/aqtweaks_comfort.json`. Always registered. No parent “Comfort mod” — Tweaks-owned, with optional hooks into other mods’ potions and warp.

Legacy files renamed on first load if the new name is absent:

- `config/arcanaquesttweaks_comfort.json`
- `config/arcanaquesttweaks/arcanaquesttweaks_comfort.json`

→ `config/arcanaquesttweaks/aqtweaks_comfort.json`

There is no Forge `@Config` for comfort and no mixin into those mods. Missing mods skip that benefit.

## Locked intent

A homestead rest loop: scan nearby “cozy” blocks and tamed pets, score with **per-category caps**, then apply a custom **Homestead** potion plus regen/saturation and (if loaded) Simple Difficulty thermals and Thaumcraft temp-warp drain. Also grants Simple Difficulty **cold resist** while standing in Biomes O' Plenty hot spring water.

Category caps are the design. Uncapped sums turn a chandelier farm into Homestead III.

## How parent/vanilla pieces work

| Source | What it provides | Tweaks use |
| --- | --- | --- |
| Vanilla | Sleep, sneak, ride, potions, `EntityTameable` | Rest detection, regen/saturation, pet scan |
| Tweaks | `PotionHomestead` | HUD icon `assets/aqtweaks/textures/gui/homestead_icon.png`, registered on `RegistryEvent.Register<Potion>` as `aqtweaks:homestead` |
| Lang | `en_us.lang` | `effect.aqtweaks.homestead=Homestead` (`getName()` returns that key; no `setTranslationKey`) |
| Thaumcraft | Temp warp capability | Same `ThaumcraftHelper.reduceWarp` as the TC module; progress NBT `WarpCleansingProgress` (not `WarpExposureProgress`) |
| Simple Difficulty | `simpledifficulty:cold_resist`, `heat_protection`, `cold_protection` | `Potion.getPotionFromResourceLocation` — null-safe |
| Biomes O' Plenty | `biomesoplenty:hot_spring_water` | Block at feet or head |

## Design plan (rest loop)

Server only. `TickEvent.PlayerTickEvent` **END**, every **300 ticks** (15s) on `ticksExisted % 300 == 0`.

1. If NBT `AQTComfortResting` is false: player must pass `isPlayerResting`; score ≥ Homestead I → set tag and apply benefits.
2. If already resting: **do not** require sneak/sleep/still. Rescan score. Below I → clear tag and remove Homestead (regen/saturation/thermals expire on their own).
3. `LivingHurtEvent` on a player, or `AttackEntityEvent`, clears the tag immediately (any hurt, not “damage after armor”).

`isPlayerResting` (entry only):

- Sleeping in a bed, **or**
- **`isRiding()`** (mounts, boats, minecarts, chairs that use riding — not a sit-pose check), **or**
- Sneaking, **or**
- Nearly stationary: `motionX² + motionZ² < 0.001` (Y motion ignored).

## Scoring

Scan **24×5×24** from the player position: horizontal radius 12, **dy -2..+2**. Skip unloaded blocks. Look up each block’s registry id in `COZY_BLOCKS`.

Pets: `EntityTameable` in AABB grown **16** from the player. Count if `isTamed()` and `ownerId` equals the player. Each pet adds `pet_comfort_value` under category `pets`.

Per category: sort weights descending, sum only the top **N** (`category_limits`, default 1 if missing). Same block id in two categories cannot happen; last apply wins if the JSON repeats an id.

Thresholds are floats in JSON (defaults 5 / 15 / 30):

| Band | Score | HUD | Other |
| --- | --- | --- | --- |
| I | ≥5 and &lt;15 | Homestead I (amp 0) | +9 warp-cleanse progress / 15s |
| II | ≥15 and &lt;30 | Homestead II (amp 1) | Regen I, +13 progress, SD heat+cold protection |
| III | ≥30 | Homestead III (amp 2) | Regen II, Saturation I, +25 progress, SD thermals |

Potion duration is **340 ticks** (15s + 40) so the HUD does not flicker between scans. All `PotionEffect`s use ambient **true**, particles **false**.

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

Gson → `ComfortConfig`. Unknown fields ignored. Load failure → in-memory defaults (file may be left broken).

```json
{
  "category_limits": { "hearth": 1, "pets": 2 },
  "pet_comfort_value": 3.0,
  "threshold_homestead_1": 5.0,
  "threshold_homestead_2": 15.0,
  "threshold_homestead_3": 30.0,
  "categories": {
    "hearth": { "farmersdelight:stove": 4.0 }
  }
}
```

`categories` maps category name → (block id → weight). Limits and category names must match; a category with blocks but no limit uses **1**.

### Default limits

| Category | Limit |
| --- | --- |
| hearth | 1 |
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

- `comfort/ComfortSystemHandler.java` — tick, score, benefits, hot springs, cancel on hurt/attack
- `comfort/ComfortConfig.java` — Gson DTO
- `comfort/ComfortConfigLoader.java` — load/rename/defaults/`apply` into handler maps
- `comfort/PotionHomestead.java` — potion + `RegistrationHandler`
- `assets/aqtweaks/lang/en_us.lang`
- `assets/aqtweaks/textures/gui/homestead_icon.png`

## Do not regress

- Benefits are **ambient, no particles** (`true, false` on `PotionEffect`).
- Keep category caps.
- Comfort warp NBT is `WarpCleansingProgress`, not Thaumcraft exposure `WarpExposureProgress`.
- Thermals and cold resist look up potions by name so Simple Difficulty absence never classloads SD.
- Entry requires rest pose/stillness; **continuing** rest allows walking inside the scored area.
- `isRiding()` is the sit check. Do not switch to a missing “isSitting” API and drop chair/boat rest.

## Out of scope unless asked

- Client-only particles
- Forge cfg mirror of the JSON
- Draining normal/permanent warp from Homestead
