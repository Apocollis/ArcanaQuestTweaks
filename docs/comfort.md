# Comfort module (1.10)

Last updated: 2026-11-01.

JSON (two files under `config/arcanaquesttweaks/`):

- `aqtweaks_comfort_settings.json` — thresholds, promote, pet value, entry furniture, combat cooldowns, penalties, bonuses
- `aqtweaks_comfort_blocks.json` — `category_limits` and `categories`

Always registered. No parent “Comfort mod” — Tweaks-owned, with optional hooks into other mods’ potions and warp.

The old combined `aqtweaks_comfort.json` is **not** loaded. Leave it on disk until you have copied `categories` / `category_limits` into the blocks file.

There is no Forge `@Config` for comfort and no mixin into those mods. Missing mods skip that benefit.

## Locked intent

A homestead rest loop: scan nearby cozy blocks and tamed pets, add potion bonuses and a small sitting or lying bonus, subtract player-state penalties, then grant **Rested** (registry `aqtweaks:homestead`). Rested lingers after the player leaves. While they are resting at home it is held at full duration, and **Hearthside** (registry `aqtweaks:clarity`, class `PotionClarity`) shows the at-home temporary-warp cleanse when Thaumcraft is loaded. Hearthside ends with the resting state. Also grants Simple Difficulty **cold resist** while standing in Biomes O' Plenty hot spring water.

Category caps stay. Repeats of one family decay. Penalties **do not** have a global cap. A cave and a garden both count. There is no roof gate. The shell tag only changes structure decay, and `non_shell_weight` can discount blocks the rays did not hit. A new settings file uses thresholds **24 / 48 / 66**. A file that already has the three threshold keys keeps those numbers.

## How parent/vanilla pieces work

| Source | What it provides | Tweaks use |
| --- | --- | --- |
| Vanilla | Sleep, sneak, ride, potions, food, health, `EntityTameable` | Rest detection, pet scan, hunger/health penalties |
| Tweaks | `PotionHomestead` | Shown as Rested. Registry `aqtweaks:homestead`. Icon `homestead_icon.png`. Max health +2 half-hearts per tier. Milk does not clear it |
| Tweaks | `PotionClarity` | Shown as Hearthside. At-home warp indicator. Registry `aqtweaks:clarity`. Not applied without Thaumcraft |
| Lang | `en_us.lang` | `effect.aqtweaks.rested=Rested` (`PotionHomestead.getName`), `effect.aqtweaks.hearthside=Hearthside` (`PotionClarity.getName`); the old `homestead` and `clarity` keys are kept. A resource pack lang (ContentTweaker) can override a key, which is why the display keys were renamed |
| Thaumcraft | Temp warp capability | Same `ThaumcraftHelper.reduceWarp` as the TC module; progress NBT `WarpCleansingProgress` (not `WarpExposureProgress`) |
| Simple Difficulty | thirst, body temp, `cold_resist` / `heat_protection` / `cold_protection` / `heat_resist` | `SimpleDifficultyHelper` (guarded compile-hard) for thirst/temp; potions by resource name — null-safe |
| Somnia | `somnia:sleepy` / `exhausted` / `fading` | default rows in `penalties.effects` |
| Farmer's Delight | `farmersdelight:comfort` | default row in `bonuses.effects` |
| Extra Alchemy | `extraalchemy:effect.learning` | Not granted unless a `rested.potions_*` row names it. A row with `learning: true` is removed on granted II/III |
| Soot | `soot:experience_boost` | Not granted unless a `rested.potions_*` row names it |
| Elenai Dodge 2 | `FeathersHelper`, `MaxFeathersEvent` | Vigor interval and Fortitude max feathers. Compile-hard. A `rested.potions_*` entry can still apply `endurance` / `replenishment` by name |
| Biomes O' Plenty | `biomesoplenty:hot_spring_water` | Block at feet or head |

## Design plan (rest loop)

Server only. Furnishings scan on `TickEvent.PlayerTickEvent` **END**, every **600 ticks** (30s), staggered by `(ticksExisted + entityId) % 600`. Bonuses, penalties, the rest pose and the ladder refresh every **40 ticks** in a home context (cached scan has entry furniture, or Rested is active). A cached scan older than **1200 ticks** is refreshed first. Eating and waking refresh immediately. Entry is checked on the 2s pass (the cached scan can be up to 30s old), and also on eating and waking. Hurt, attack, lost furniture, or score below I end the **resting** state at once and do **not** remove Rested. Clarity is removed at once.

1. If NBT `AQTComfortResting` is false: player must pass `isPlayerResting`; not be inside the hurt or attack cooldown; the scan must include a block in `entry_require_categories` (default hearth / bedding / seating); **effective** score ≥ Rested I → set tag, granted band **I**, stamp `AQTComfortBandSince`, apply I.
2. If already resting: **do not** require sneak/sleep/still. Below I, or no required furniture in range → clear the tag and the ladder NBT. Do **not** remove Rested. Remove Clarity. A configured potion entry counts down; a `learning` entry is removed only when the granted band becomes II or III.
3. Granted band promotes **one step** after `promote_ticks_2` or `promote_ticks_3` (default 1200 / 60s) while the score still supports the next band. Demote **immediately** while resting if the score cannot support the granted band.
4. `LivingHurtEvent` on a player (amount **> 0**) ends the resting state and stamps `AQTComfortHurtAt`. Re-entry is blocked for `damage_cooldown_ticks` (default **600** / 30s). Amount 0 does nothing. Missing tag is eligible (do not treat `0` as a stamp).
5. `AttackEntityEvent` ends the resting state and stamps `AQTComfortAttackAt`. Re-entry is blocked for `attack_cooldown_ticks` (default **300** / 15s). Melee attack event only, not projectiles. Entry waits until **both** cooldowns expire. Rested is not removed.

`isPlayerResting` (entry only):

- Sleeping in a bed, **or**
- **`isRiding()`** (mounts, boats, minecarts, chairs that use riding — not a sit-pose check), **or**
- Sneaking, **or**
- Nearly stationary: `motionX² + motionZ² < 0.001` (Y motion ignored).

## Scoring

Scan **25×5×25** (3,125 cells) from the player position: horizontal radius **±12 inclusive**, **dy -2..+2**. Skip unloaded blocks. Look up each block’s registry id in `COZY_BLOCKS`.

Pets: `EntityTameable` in a ±16 box around the player **block** (`getPosition()`, inclusive +1 on the far edge). Count if `isTamed()` and `ownerId` equals the player. Each pet adds `pet_comfort_value` under category `pets`.

Per category: group by family, sort weight descending, then sum `weight × decay^index` for the top **N** (`category_limits`, default 1 if missing). Default decay is 0.6. `decay_overrides` can set a category (structure 0.5, pets 1.0). Same block id in two categories cannot happen; last apply wins if the JSON repeats an id. The shell tag (roof, wall, window, floor) is taken on this scan. It changes which structure blocks share a decay group. It does not reject a room.

Then add **all** active `bonuses.effects` amounts and the sitting or lying bonus. Then subtract formula penalties plus **all** active `penalties.effects` (no global cap):

| Source | Formula |
| --- | --- |
| Temperature (SD) | 1.5 per body-temp point outside 11–14; **0** if heat potions while hot or cold potions/resist while cold |
| Thirst (SD) | `(20 − thirst) × 0.75` |
| Hunger | `(20 − food) × 0.5` |
| Health | `(1 − hp/baseMax) × 15`. Fortitude's own max-health bonus is taken out of `baseMax` first, and the ratio is clamped at 1 |
| Potion effects | sum of `penalties.effects` whose potion is active (defaults: Somnia sleepy 10, exhausted 25, fading 40) |

`bonuses.effects` default is `farmersdelight:comfort` **+10**. Duplicate potion rows stack. Somnia only leaves one fatigue effect on the player, so those three rows do not stack in play.

`effective_score = max(0, cozy + bonuses − penalties)`.

Thresholds are floats in JSON. A **new** file uses 24 / 48 / 66. A file that already stores the three keys keeps those numbers (the DEVBOX file is still 15 / 40 / 60). They set the **maximum** band the score allows. Warp and the native bonuses use the **granted** band:

| Granted | After | Rested | At home |
| --- | --- | --- | --- |
| I | immediately when score ≥ I | 5 min, Insight +10%, Vigor 25 ticks, Fortitude +1 heart / +1 feather | Clarity, +2 warp / 30s |
| II | 60s at I while score ≥ II | 10 min, +20%, 20 ticks, +2 / +2 | Clarity, +3 warp |
| III | 60s at II while score ≥ III | 15 min, +30%, 15 ticks, +3 / +3 | Clarity, +6 warp |

Rested duration is held full while resting and then counts down. Vigor uses `FeathersHelper.decreaseRegenModifier` by 5 / 10 / 15 against base 30, tracked in entity NBT `AQTVigorDelta` (not the persisted tag). Fortitude health is the potion attribute (`+2` half-hearts times amplifier + 1). Fortitude stamina is `MaxFeathersEvent.addMaximum` and does not refill. Insight multiplies XP orbs and keeps the fraction in `AQTInsightXpCarry`. Native potion lists ship empty. A `rested.potions_*` row is refreshed while that band is held and then counts down.

All `PotionEffect`s use ambient **true**, particles **false**. No regen, saturation, or SD thermals from Rested. Milk does not clear Rested. Death does. The placement burst is a world particle, not a potion particle.

### `/aqcomfort` (OP)

`ArcanaQuestTweaks.serverStarting` registers `CommandAqComfort` (perm **2**, player only) next to `/aqvillage`. No args scans. `reload` and `families` are the other forms.

Runs the same 25×5×25 scan as the 30s tick, **applies** the ladder immediately (entry still needs rest pose, furniture, score, and no combat cooldown), then prints:

- resting tag, rest pose, furniture gate, score band vs granted, hurt/attack remaining ticks, ticks until promote (or `-`)
- cozy / bonus / penalty / effective
- capped cozy **by category**
- each active `bonuses.effects` row
- temp, thirst, hunger, health, plus each active `penalties.effects` row

Non-OP denied. Console without a player errors.

### Warp cleanse math

Only if `thaumcraft` is loaded **and** the player is resting (Clarity would be showing). Progress is persisted NBT `WarpCleansingProgress`. Each **600-tick** scan adds **2 / 3 / 6** (granted I / II / III). The 40-tick pass does not add progress. If the counter is **≥ 12**, reduce 1 temporary warp (`ThaumcraftHelper` type **1**) if any, `syncWarp`, and set the counter to **0** (remainder discarded; a firing with 0 temp warp still spends the bar). Sleep credit: `applyAcceleratedWarpCleanse` is unchanged. Losing the resting state does **not** clear the counter and does remove Clarity. Stored values **> 12** are treated as 0 once.

Time to 1 temp warp at the 30s interval:

- I: +2 → 6 scans → **180s**
- II: +3 → 4 scans → **120s**
- III: +6 → 2 scans → **60s**

Comfort drain is **temporary warp only**. Sleep in the Thaumcraft module is **sticky only** by default; temp during a Somnia night is this Rested bar plus TC decay. See [thaumcraft.md](thaumcraft.md).

### Hot springs (separate tick)

`PlayerTickEvent` **START**, every **20 ticks**. Feet or the block above: `biomesoplenty:hot_spring_water`. Apply `simpledifficulty:cold_resist` for **200 ticks**, amp 0, ambient, no particles. Independent of Rested.

## JSON schema

Gson. Unknown fields ignored. Failure of **one** file uses in-memory defaults for that file only.

Both files load in `ComfortConfigLoader.load` from preInit. `/aqcomfort reload` reads them again. A cfg save in the in-game GUI does not.

**Settings** (`aqtweaks_comfort_settings.json`):

```json
{
  "pet_comfort_value": 3.0,
  "threshold_homestead_1": 24.0,
  "threshold_homestead_2": 48.0,
  "threshold_homestead_3": 66.0,
  "promote_ticks": 1200,
  "damage_cooldown_ticks": 600,
  "attack_cooldown_ticks": 300,
  "entry_require_categories": ["hearth", "bedding", "seating"],
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
| (root) | `damage_cooldown_ticks` | 600 | After amount > 0 hurt, skip resting **entry** this many ticks |
| (root) | `attack_cooldown_ticks` | 300 | After `AttackEntityEvent`, skip resting **entry** this many ticks |
| (root) | `entry_require_categories` | `hearth`, `bedding`, `seating` | Scan must include a cozy block in one of these; empty list = no furniture gate |
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

- `comfort/ComfortSystemHandler.java` — 30s furnishing scan, 2s state pass, decay, shell tag, lingering Rested, Clarity, hot springs
- `comfort/ComfortFamilies.java` — family id and `/aqcomfort families`
- `comfort/ShellSampler.java` — roof / wall / window / floor tag
- `comfort/RestedBonuses.java` — Insight, Vigor, Fortitude
- `comfort/PotionHomestead.java`, `comfort/PotionClarity.java`
- `comfort/ComfortFeedback.java` and `PacketComfortStatus`, `PacketComfortCatalog`, `PacketComfortPlaced`
- `comfort/ComfortHud.java`, `ComfortTooltips.java`, `ComfortToast.java`, `ComfortToggleButton.java`, `ComfortClientState.java` — client only
- `comfort/CommandAqComfort.java` — `/aqcomfort`, `reload`, `families`
- `comfort/ComfortSettings.java`, `ComfortBlocks.java`, `ComfortConfigLoader.java`
- `mixin/comfort/` — client mixin json `mixins.aqtweaks.comfort.json`
- `assets/aqtweaks/lang/en_us.lang`
- `assets/aqtweaks/textures/gui/homestead_icon.png`, `clarity_icon.png` (placeholder copy)

## Do not regress

- Potion effects stay ambient, particles off (`true, false`). The placement burst is a separate world particle.
- Rested does **not** apply regen, saturation, or SD heat/cold protection. Hot-spring `cold_resist` stays on its own tick.
- A `rested.potions_*` entry counts down when rest ends. A `learning` entry is removed on granted II/III.
- Keep category caps. Blocks JSON without `crafting` gets limit 1 and `minecraft:crafting_table`; a player-defined `crafting` key is not overwritten. Combined `aqtweaks_comfort.json` is ignored.
- Comfort warp NBT is `WarpCleansingProgress`. Do not wipe it when rest ends. Adds are 2/3/6 on the 600-tick scan only, and only while Clarity would be active. Fire at ≥ 12.
- The cleanse calls compile-hard `ThaumcraftHelper`, only behind `thaumcraftLoaded()`.
- Thermals and cold resist look up potions by name so Simple Difficulty absence never classloads SD.
- Entry requires the loose rest pose **and** entry furniture. Continuing rest allows walking while that furniture stays in the scan.
- Hurt amount > 0, attack, lost furniture, and score below I end the resting state and do **not** remove Rested. Amount 0 does nothing.
- `isRiding()` is the entry sit check. The score bonus rejects living mounts, boats and minecarts.
- `AQTVigorDelta` stays on `getEntityData()`, not the persisted tag. One reconcile writes the difference. Do not call `FeathersHelper.getRegenModifier` on the server.
- Shell rays run on the furnishing scan, not the 40-tick pass.
- The inventory mixin stays in the comfort json `"client"` array.

## Out of scope unless asked

- Draining normal/permanent warp from the rest loop
- Turning the shell tag into a roof gate
