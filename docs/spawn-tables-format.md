# Spawn tables: proposed file format (draft, not built)

Replaces `spawn.json` (In Control's spawn rules) with three files that Tweaks reads. In Control stays installed and keeps making the real decisions until a comparison log shows Tweaks would decide the same. Loot stays in In Control. Everything here was checked against the pack's `spawn.json`, In Control 3.10.5 and McJty Tools 1.12-0.0.21.

Files live in the pack folder (`config/arcanaquest/`) next to the classification files. All three are JSON arrays of rules, one rule per line, with `//` comments and trailing commas allowed (the same reading as the pack scripts). **Row order is evaluation order.**

| File | Decides | Match rule |
| --- | --- | --- |
| `spawn_conditions.json` | whether a mob may spawn | first matching row wins |
| `spawn_modifiers.json` | what a spawned mob gets (stats, potions, angry) | first matching row applies, as a whole (stats, potions and `angry`); no other row adds to it |
| `spawn_gear.json` | what a spawned mob holds and wears | first matching row for the mob wins, as a whole |

Rows use In Control's own key names (`mob`, `dimension`, `minheight`, `seesky`, `gamestage`, `maxcount`, ...), so a row copies across from `spawn.json` unchanged and the old file can be converted by script.

## When things run

1. **Spawn check** (`LivingSpawnEvent.CheckSpawn`: natural spawns and spawners). Conditions rows with `phase` of `spawn` or `both` are walked in order. The first match sets the result (allow, deny or default) and ends the walk.
2. **Join** (`EntityJoinWorldEvent`). Conditions rows with `phase` `both` are walked in order; a deny cancels the join. Then, for a mob that joined, the modifiers table and the gear table are applied **once** (a tag on the mob stops a chunk reload applying them again).

`phase: "spawn"` is In Control's rule without `onjoin`; `phase: "both"` is `onjoin: true`. **Default is `both`.** A mob that arrives by another route (egg, command, another mod) skips step 1 and is checked only by the `both` rows at step 2, which is why the height and cap rows are `spawn` and the stage gates are `both`.

## spawn_conditions.json

Match keys (all optional, all must hold): `mob` (id or list), `mod`, `dimension` (int or list), `minheight`, `maxheight`, `seesky`, `mintime`, `maxtime` (0 to 24000), `time`, `weather`, `biomecat`, `structure`, `biomereg`, `hostile`, `spawner`, `notcolliding`, `gamestage`, `random` (chance the row applies), `minplayerdist`, `maxplayerdist` (to the nearest player within 100 blocks, the pack's `maxPlayerDistance`), `mincount`, `maxcount` (In Control's count form: `{"amount", "perplayer" | "perchunk", "mob" | "mod" | "hostile" | "passive"}`; `mincount` matches when the count has reached `amount`, `maxcount` when it is still below).

`time` is a name or a list of windows, for the cases `mintime`/`maxtime` cannot express because a day wraps around midnight: `"day"` is 0 to 13000 plus 23000 to 24000 (the windows the pack already uses), `"night"` is 13000 to 23000, or a list such as `[[0, 6000], [18000, 24000]]`. `mintime`/`maxtime` still work. So the two Dark Druid rows become `{ "mob": "thebetweenlands:dark_druid", "time": "day", "result": "deny" }`.

`biomecat` is a biome category name (or a list) from `biome_mappings.json`, expanded to that category's biomes when the file loads, so a row can say `"biomecat": "spooky"` instead of listing biome ids. It can sit beside `biomereg`, and works in all three files.

Result: `"result": "allow" | "deny" | "default"`. `allow` forces the spawn past vanilla's own checks (used for Mineshaft mobs and the daytime pool), `deny` blocks it, `default` leaves vanilla to decide **and ends the walk**, which is how the Ghoul, Grue and Depths bypasses skip the denies below them.

Rows from the current `spawn.json`:

```json
[
  // spawner cages: refuse a colliding spawn, cap Bone Knights at 1 per chunk, allow the rest in open air
  { "spawner": true, "notcolliding": false, "result": "deny", "phase": "spawn" },
  { "mob": "grimoireofgaia:bone_knight", "spawner": true, "mincount": { "amount": 1, "perchunk": true }, "result": "deny", "phase": "spawn" },
  { "spawner": true, "notcolliding": true, "result": "allow", "phase": "spawn" },

  // hostile mobs stay out of villages by day
  { "structure": "Village", "dimension": 0, "minheight": 50, "mintime": 0, "maxtime": 13000, "hostile": true, "result": "deny" },

  // Ghoul: Ominous Woods at any time of day (default ends the walk, so the denies below never see it)
  { "mob": "lycanitesmobs:ghoul", "biomereg": "biomesoplenty:ominous_woods", "seesky": true, "result": "default" },

  // Crimson cult: stage 'crimsoncult' and under 5 per player, otherwise denied
  { "mob": ["crimsonrevelations:cultist_archer", "thaumcraft:cultistcleric", "thaumcraft:cultistknight"], "gamestage": "crimsoncult", "maxcount": { "amount": 5, "perplayer": true }, "result": "default", "phase": "spawn" },
  { "mob": ["crimsonrevelations:cultist_archer", "thaumcraft:cultistcleric", "thaumcraft:cultistknight"], "result": "deny", "phase": "spawn" },

  // caps and throttles
  { "dimension": 0, "mincount": { "amount": 250, "hostile": true, "perplayer": true }, "result": "deny", "phase": "spawn" },
  { "dimension": 17, "hostile": true, "random": 0.4, "result": "deny", "phase": "spawn" },
  { "mob": "minecraft:villager_golem", "dimension": 0, "mincount": 40, "result": "deny" }
]
```

### Generated rows

The pack scripts build most rows from your classification files. A placeholder row says where the generated rows go, so you keep control of the order:

```json
{ "generate": "stage_gating" }
```

| Placeholder | Built from | Rows it stands for |
| --- | --- | --- |
| `structure_allow` | `mob_structurespawns.json` | allow per structure (Mineshaft: `maxheight` 50, `seesky` false) |
| `depths_bypass` | `mob_depthsspawns.json` | default for the Depths pool at its `maxheight` |
| `stage_gating` | `mob_tier.json`, `mob_spawnrules.cfg` | per tier: underground bypasses, stage-and-cap default, deny fallback (dragon, deadly, elite, crimsoncult) |
| `layer_restrictions` | `mob_overworldspawntype.json`, `mob_structurespawns.json` | surface-only mobs denied below Y 60 without sky, underground-only denied above Y 60 |
| `day_spawns` | `mob_daypool.json` (below), or `mob_dayspawns.json` + `mob_staged_dayspawns.json` when it is absent; cfg | daytime distance deny, cap deny, allow and staged allow |

The verify command compares each placeholder's rows with what `regenerate_spawn_limits.py` wrote into `spawn.json`. Rows you write by hand go between the placeholders. A placeholder missing from the file means those rows are not generated.

### The daytime pool: mob_daypool.json (optional)

One entry per mob that may spawn by day on the surface. It carries what the two older files hold, plus a biome limit:

```json
{
  "grimoireofgaia:centaur": {},
  "grimoireofgaia:orc": { "stage": "elite" },
  "lycanitesmobs:ghoul": { "biomes": ["spooky"] },
  "grimoireofgaia:vampire": { "biomes": ["spooky", "swamp"], "stage": "elite" }
}
```

`{}` means every biome and no stage. `biomes` are category names from `biome_mappings.json`. `stage` is a game stage the nearest player must have. With the file present, `day_spawns` reads it; without it, `mob_dayspawns.json` and `mob_staged_dayspawns.json` give today's rows exactly. The allow rows are grouped by stage and biome set and keep the current limits (30 blocks from players, 20 per player across the whole pool). The distance and cap denies cover every pool mob in every biome, and the deny for a player without the stage is not limited by biome, as the Orc's is now. Verify warns when a mob is allowed by day in a category but is not in the potential-spawn pool for those biomes, since it could never spawn there.

## spawn_modifiers.json

Runs at join, once per mob. Match keys: the same as conditions, except `phase`, `result`, the count keys, `spawner` and `notcolliding`, which do not apply here.

Actions (In Control's names; `multiply` acts on the base value, `add` is added, `x * multiply + add`):

| Key | Does |
| --- | --- |
| `healthmultiply`, `healthadd` | max health, then current health is set to the new max |
| `damagemultiply`, `damageadd` | attack damage |
| `speedmultiply`, `speedadd` | movement speed |
| `armormultiply`, `armoradd` | base armour. Worn gear adds on top of this, so `armoradd` 8 is 8 more than the gear gives. The game caps armour at 30 |
| `armortoughnessmultiply/add`, `knockbackresistancemultiply/add`, `followrangemultiply/add`, `flyingspeedmultiply/add` | as named |
| `potion` | `"id,duration,amplifier"` or a list; added to the mob, merging with an effect it already has |
| `angry` | the mob targets the nearest player within 50 blocks (Zombie Pigmen get a revenge target); does nothing if nobody is that close |

How rows combine: **the first matching row applies, as a whole, and no other row adds to it.** A thundering cave creeper gets the creeper row and nothing from the depth rows below, which is how `firstMatchOnly` works in In Control today. Put specific mobs above broad rules (dimension and depth buffs). Because only one row ever applies, each stat is applied once and nothing compounds.

```json
[
  // specific mobs first: the first matching row applies and the broad rows below are not reached for them
  { "mob": "minecraft:creeper", "dimension": 0, "seesky": true, "weather": "thunder", "random": 0.25, "potion": "potioncore:lightning,1,0", "healthmultiply": 2.0 },
  { "mob": "minecraft:enderman", "dimension": 0, "maxheight": 32, "angry": true },
  { "mob": "minecraft:vex", "dimension": 0, "healthmultiply": 0.25, "damagemultiply": 0.5 },
  { "mob": "artifacts:mimic", "dimension": 0, "healthmultiply": 0.75, "damagemultiply": 1.25, "potion": "minecraft:night_vision,999999,0" },
  { "mob": "minecraft:villager_golem", "dimension": 0, "structure": "Village", "healthmultiply": 2.5, "damagemultiply": 2.0, "potion": ["minecraft:fire_resistance,9999999,0", "extraalchemy:effect.concentration,1,0"] },
  { "mob": "crimsonrevelations:cultist_archer", "healthmultiply": 2.0, "damagemultiply": 2.5, "armoradd": 8.0, "potion": ["minecraft:night_vision,999999,0", "potioncore:archery,999999,1"] },

  { "generate": "tier_underground" },    // per-tier multipliers for the gated mobs underground
  { "generate": "dimension_buffs" },     // Nether, End, Aether, Twilight, Beneath, Atum, Betweenlands, Aurorian, Emptiness
  { "generate": "underground_tiers" }    // Deadlier Underground, Y <= -15, 15, 55
]
```

Generated placeholders read `mob_spawnrules.cfg` (`du_tier*`, `dim_*`, `deadly_mob_*`) the same way the script does.

## spawn_gear.json

Runs at join, once per mob, after the modifiers. One row per mob (or loadout) with every slot it sets. A slot left out is left as the mob spawned. Slots: `hand`, `offhand`, `head`, `chest`, `legs`, `feet`. An Enderman's `hand` is the block it carries. Optional match keys: `dimension`, `minheight`, `maxheight`, `seesky`, `weather`. The first matching row for a mob supplies the whole loadout; rows never merge slot by slot.

```json
[
  { "mob": "minecraft:vindication_illager", "hand": "minecraft:iron_axe" },
  { "mob": "aqtweaks:deep_dwarf", "hand": "embers:pickaxe_dawnstone" },
  { "mob": "minecraft:stray", "hand": "minecraft:bow" },

  // two loadouts for one mob: the Thaumium row comes first and only matches at Y <= 15
  { "mob": "eerieentities:cursed_armor", "maxheight": 15, "hand": "thaumcraft:thaumium_sword", "head": "thaumcraft:thaumium_helm", "chest": "thaumcraft:thaumium_chest", "legs": "thaumcraft:thaumium_legs", "feet": "thaumcraft:thaumium_boots" },
  { "mob": "eerieentities:cursed_armor", "hand": "minecraft:iron_sword", "head": "minecraft:iron_helmet", "chest": "minecraft:iron_chestplate", "legs": "minecraft:iron_leggings", "feet": "minecraft:iron_boots" },

  { "mob": "crimsonrevelations:cultist_archer", "hand": "minecraft:bow", "head": "crimsonrevelations:crimson_archer_helmet", "chest": "crimsonrevelations:crimson_archer_chestplate", "legs": "crimsonrevelations:crimson_archer_leggings", "feet": "thaumcraft:crimson_boots" },
  { "mob": "thaumcraft:cultistknight", "hand": "thaumcraft:crimson_blade", "head": "thaumcraft:crimson_plate_helm", "chest": "thaumcraft:crimson_plate_chest", "legs": "thaumcraft:crimson_plate_legs", "feet": "thaumcraft:crimson_boots" }
]
```

## Moving from spawn.json without changing behaviour

1. A converter reads `spawn.json` and writes the three files. Each rule is split into its condition part and its action part. Modifiers keep In Control's first-match rule, so the first version decides exactly as today.
2. The comparison log runs with In Control still in charge. At each spawn check and join it records the result and the actions Tweaks would give, next to what In Control did, and lists every difference.
3. When the log is quiet over real play, switch the Tweaks tables on, one file at a time (conditions, then modifiers, then gear), each with its own switch.
4. After that you can reorder rows or change values freely.

## Differences from In Control, on purpose

- Modifier-only and gear-only rules no longer end the search for the conditions (in In Control any matching rule does, and your file order hides this).
- Modifier-only and gear-only rules no longer end the search for the conditions, but within the modifiers table nothing changes: one matching row, applied whole.
- Conditions and actions are separate rows, so a rule that did both (the Depths bypass, the Elite underground allow) becomes one row in each file.
- A converted file is checked for rows that mix the two. The converter lists them for you.

## Open questions

- **Weighted gear:** In Control accepts a weighted list for a slot (a random pick). Not in the first version unless you use it. Your `spawn.json` does not.
- **Default `phase`:** `both`, because most of your rules are `onjoin`. Say if you would rather it were `spawn`.
