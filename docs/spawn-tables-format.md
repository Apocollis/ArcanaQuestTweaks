# Spawn tables: file format (loader and converter built; the rules do not run yet)

Replaces `spawn.json` (In Control's spawn rules) with three files that Tweaks reads. In Control stays installed and keeps making the real decisions until a comparison log shows Tweaks would decide the same. Loot stays in In Control. Everything here was checked against the pack's `spawn.json`, In Control 3.10.5 and McJty Tools 1.12-0.0.21.

Files live in the pack folder (`config/arcanaquest/`) next to the classification files. All three are JSON arrays of rules, one rule per line, with `//` comments and trailing commas allowed (the same reading as the pack scripts). **Row order is evaluation order.**

| File | Decides | Match rule |
| --- | --- | --- |
| `spawn_conditions.json` | whether a mob may spawn | first matching row wins |
| `spawn_modifiers.json` | what a spawned mob gets (stats, potions, angry) | each stat set by the first matching row that sets it; potions from every matching row; `stop` ends the search |
| `spawn_gear.json` | what a spawned mob holds and wears | first matching row for the mob wins, as a whole |

Rows use In Control's own key names (`mob`, `dimension`, `minheight`, `seesky`, `gamestage`, `maxcount`, ...), so a row copies across from `spawn.json` unchanged and the old file can be converted by script.

## Loading and checking (built)

`spawning/rules/` reads the three files and checks every rule before anything uses it. A mistake is reported as `file:line: message`, where the line is the one the rule starts on: an unknown key (with a "did you mean" suggestion), a wrong type, a `result` or `phase` that is not allowed, a bad `time` window, a `biomecat` that is not in `biome_mappings.json`, a count in a form In Control does not accept, `minheight` above `maxheight`, a row that changes or equips nothing, a malformed or unclosed rule. A file with any problem is not used.

Commands (`/aqspawntable`):

- `check` validates `spawn_conditions.json`, `spawn_modifiers.json` and `spawn_gear.json` in the pack folder and lists every problem.
- `convert` reads `config/incontrol/spawn.json` and writes the three files, plus `convert_report.txt`, to `config/arcanaquesttweaks/converted/`. Nothing in the pack folder is touched; copy the files across when you are happy with them. The report lists what was moved, merged or dropped. On the pack's current `spawn.json`: 78 rules become 48 condition rows, 34 modifier rows and 9 gear rows; the 19 rules that were not `onjoin` become `phase: spawn`; the three Crimson cultist rows move above the cultist underground row; 23 rules that carried both a result and a stat lose the result (no condition rule comes after them, so there was nothing for it to shield).

The rules themselves are not evaluated yet.

## When things run

1. **Spawn check** (`LivingSpawnEvent.CheckSpawn`: natural spawns and spawners). Conditions rows with `phase` of `spawn` or `both` are walked in order. The first match sets the result (allow, deny or default) and ends the walk.
2. **Join** (`EntityJoinWorldEvent`). Conditions rows with `phase` `both` are walked in order; a deny cancels the join. Then, for a mob that joined, the modifiers table and the gear table are applied **once** (a tag on the mob stops a chunk reload applying them again).

`phase: "spawn"` is In Control's rule without `onjoin`; `phase: "both"` is `onjoin: true`. A mob that arrives by another route (egg, command, another mod) skips step 1 and is checked only by the `both` rows at step 2, which is why the height and cap rows are `spawn` and the stage gates are `both`.

**Default is `both`**: a row without `phase` runs at both moments, and a row runs at the spawn check alone only when it says `"phase": "spawn"`. Converted rows always carry their phase explicitly, taken from the rule's own `onjoin` flag (59 `both`, 19 `spawn` for the current file).

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
  { "mob": "minecraft:villager_golem", "dimension": 0, "mincount": 40, "result": "deny", "stop": true },
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
| `stop` | `true` ends the search after this row: it applies and no row below it does |

How rows combine: rows are walked in order and **every matching row contributes** until a row with `stop: true` has applied. Health, damage, speed, armour and the other stats are each set by **the first matching row that sets them**; later rows cannot change a stat that is already set (so nothing multiplies up; In Control tags the mob for health, damage and speed for the same reason). **Potions** from every matching row are added, and `angry` applies if any matching row has it. Put specific mobs above broad rules, and give `stop: true` to any row that must not pick up the broad rows below it: the Vex and Mimic nerfs stay exactly as written because their rows stop.

```json
[
  // specific mobs first; stop keeps the depth and dimension rows below from touching them
  { "mob": "minecraft:creeper", "dimension": 0, "seesky": true, "weather": "thunder", "random": 0.25, "potion": "potioncore:lightning,1,0", "healthmultiply": 2.0, "stop": true },
  { "mob": "minecraft:enderman", "dimension": 0, "maxheight": 32, "angry": true, "stop": true },
  { "mob": "minecraft:vex", "dimension": 0, "healthmultiply": 0.25, "damagemultiply": 0.5, "stop": true },
  { "mob": "artifacts:mimic", "dimension": 0, "healthmultiply": 0.75, "damagemultiply": 1.25, "potion": "minecraft:night_vision,999999,0", "stop": true },
  { "mob": "minecraft:villager_golem", "dimension": 0, "structure": "Village", "healthmultiply": 2.5, "damagemultiply": 2.0, "potion": ["minecraft:fire_resistance,9999999,0", "extraalchemy:effect.concentration,1,0"], "stop": true },
  { "mob": "crimsonrevelations:cultist_archer", "healthmultiply": 2.0, "damagemultiply": 2.5, "armoradd": 8.0, "potion": ["minecraft:night_vision,999999,0", "potioncore:archery,999999,1"], "stop": true },

  { "generate": "tier_underground" },    // per-tier multipliers for the gated mobs underground
  { "generate": "dimension_buffs" },     // Nether, End, Aether, Twilight, Beneath, Atum, Betweenlands, Aurorian, Emptiness
  { "generate": "underground_tiers" }    // Deadlier Underground, Y <= -15, 15, 55
]
```

Generated placeholders read `mob_spawnrules.cfg` (`du_tier*`, `dim_*`, `deadly_mob_*`) the same way the script does.

## spawn_gear.json

Runs at join, once per mob, after the modifiers. One row per mob (or loadout) with every slot it sets. A slot left out is left as the mob spawned. Slots: `hand`, `offhand`, `head`, `chest`, `legs`, `feet`. An Enderman's `hand` is the block it carries. Optional match keys: `dimension`, `minheight`, `maxheight`, `seesky`, `weather`. The first matching row for a mob supplies the whole loadout; rows never merge slot by slot. A row with `random` (a chance, as in the other files) matches only when the roll succeeds, so a chance of a different whole set is a row with `random` above the plain row; further sets chain the same way, each roll applying to what is left. There are no per-slot weighted lists (In Control's pick each slot independently and would mix sets); an N-way `weight` key can be added if chaining proves awkward.

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

1. A converter reads `spawn.json` and writes the three files. Each rule is split into its condition part and its action part. Every converted modifier row gets `stop: true`, which is In Control's first-match rule, so the first version decides exactly as today.
2. The comparison log runs with In Control still in charge. At each spawn check and join it records the result and the actions Tweaks would give, next to what In Control did, and lists every difference.
3. When the log is quiet over real play, switch the Tweaks tables on, one file at a time (conditions, then modifiers, then gear), each with its own switch.
4. After that you loosen the rows you choose by removing `stop` (for example the Stray, so it also picks up the Nether and depth buffs for the stats its own row does not set), and keep it on the ones that must stay as written (Vex, Mimic).

## Differences from In Control, on purpose

- Modifier-only and gear-only rules no longer end the search for the conditions (in In Control any matching rule does, and your file order hides this).
- Modifier rows can combine (each stat from the first row that sets it, potions from all), where In Control applies one rule only; converted rows keep `stop` so nothing changes until you remove it.
- Conditions and actions are separate rows, so a rule that did both (the Depths bypass, the Elite underground allow) becomes one row in each file.
- A converted file is checked for rows that mix the two. The converter lists them for you.

## Decisions recorded

- **Cultist rows:** the Crimson archer's buff row goes above the cultist underground row, so an archer gets its full buffs (2.0x health, 2.5x damage, +8 armour, potions) at every depth. Today, below Y 55, an archer gets 1.25x health and damage from the underground row, and the buff row adds only armour and potions. The converter makes this one deliberate change; the comparison log reports it as an expected difference.
- **Reordering:** rows can be reordered at any time; order is evaluation order, and `/aqspawntable reload` applies it.
