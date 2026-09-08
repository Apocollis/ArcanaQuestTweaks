# Verification (1.8)

Last updated: 2026-09-07.

Manual release / smoke checklist. **No automated tests.** Harness: CurseForge **Arcana Quest DEVBOX**, remapped `ArcanaQuestTweaks-1.8.jar` in `mods/`. Algorithms and full checklists stay in module docs; this is the pack-level pass/fail.

Worldgen applies to **new chunks only**.

## Build artifact

- [ ] `.\gradlew.bat build` or `.\build_gradle.ps1` succeeds.
- [ ] Instance `mods/` has `ArcanaQuestTweaks-1.8.jar`, **not** `-dev`.
- [ ] Only one Tweaks jar (the deploy script deletes other `ArcanaQuestTweaks-*.jar`).

## Boot

- [ ] Client starts the full pack; no mixin apply crash from `mixins.aqtweaks.json` or `mixins.aqtweaks.early.json`. Log must not say `MixinWorldRiftLight` / `World was loaded too early`, `MixinWorldServerSpawnList` / `WorldServer was loaded too early`, or `MixinWorldGenLakes` / `field_150589_a was not located` / `WorldGenLakes in invalid classes`. Optional `mixins.aqtweaks.gaia.json` must not log `InvalidInjectionException` (vanilla INVOKEs must be MCP + `remap = true`; a miss boots anyway because `required: false`).
- [ ] Mixin log does **not** say Tweaks mixins require class version 69 (Java 21 class files).
- [ ] Wait through full JEI / ThaumicJEI / **TC6 Aspects 4 JEI** load. Title screen stays up. No `hs_err_pid*.log`.
- [ ] Dedicated server: **not routinely tested** in this repo. If you ship a server, start one with the same mods and confirm it reaches “Done”.

### Optional-mod absences (safe)

These json files are `required: false`. Removing the parent should skip that json, not crash Tweaks:

| Remove | Expect |
| --- | --- |
| Grapple | No motor mixin; grapple stamina no-ops if not loaded |
| Dynamic Sword Skills | No skill feather gate |
| Toughness Bar | No HUD mixin; feathers still on the right; tooltip compat still runs |
| Astral Sorcery | No shrine mixins; no village `AQTSmallShrine` |
| Bewitchment | No Cambion/circle mixins; no ritual wrap (handler not registered) |
| Mystical World | No hut skip/settle |
| Biomes O' Plenty | No BOP lake village skip mixin; vanilla water-lake skip still runs; hot spring comfort no-ops |
| Grimoire of Gaia | No Gaia mixins; pierce and MAGIC bolts/bombs stay parent vanilla |
| Reskillable | No per-level bonuses; stamina perk lookups no-op |
| Effortless Building | No Building place-reach / max-blocks mixin |
| Thaumcraft | No focus mixins; frost stays `thrown` / not Magic; warp handler not registered |

### Do not treat as optional

Missing **RTG, Depths Update, Better Caves, CoFH World, Recurrent Complex, or IvToolkit** with the current required mixin json can **fail mixin apply** at boot. This pack always ships them. Elenai Extended is `@Mod` **required-after**. InControl is `@Mod` **after** and compile-hard for pack fill.

## Config

- [ ] After a Java default change, instance cfg still has the **old** value until edited (e.g. RTG Coast Buffer 32 vs 16).
- [ ] Change a live cfg in-game: stamina DSS costs refresh (`DssSkillCosts.invalidate`). Comfort JSON needs a restart (loaded in preInit). Settings vs blocks are separate files; the old combined `aqtweaks_comfort.json` is ignored.

## Worldgen (new chunks)

`Village Flatten Debug` is **off** by default. Turn it on only while diagnosing; it appends every line to instance `logs/villagepatch.log` (not `latest.log`) and stalls chunk gen.

| Check | Expect | Doc |
| --- | --- | --- |
| Flying new chunks (near and far from villages) | Away from villages, chat TPS stays near 20 (no growing hitch as more towns exist). A village still plates. With debug off, `villagepatch.log` stays empty | [rtg.md](rtg.md) |
| Coastal village ocean face (new chunks) | Cleaner XZ outline (no 1-block jetties; 1-block ocean notches in the pad filled). Stone brick on the whole rim below the plate; sand/grass on top; no open-water pier; inland hill cliffs not bricked | rtg |
| Village small shrine / waystone | Complete shrine on the plate (not half over a pond). No dirt/grass collar. F3 River/ocean columns still skipped | rtg |
| River through forest/shrubland (new chunks) | No well in the channel; no house/RC/plank dock on F3 River; land plate inland | rtg |
| Large Astral temple (new chunks) | Raw marble under the pad, not dirt; no floating logs/leaves in or above the AABB | rtg |
| Cambion house on a slope (new chunks) | Pad flush with plains; cobble on the grass; door +1 above cobble; holes under footprint filled; no mesa; village overlap still cancels | rtg |
| Mystical barrow or hut next to a village | Relocates (step 8, up to 32) or skips; barrow not plated | rtg |
| Bewitchment circle / menhir / wickerman next to a village | Relocates or skips; no extra plate | rtg |
| Desert village (new chunks) | One sand plate through yards; no toothed red-sand holes; no BOP quicksand in land boxes | rtg |
| Village pad water pond (new chunks) | No 6–16 block water lake through yards or house foundations. Ponds still generate off the 12-pad. Lava lakes still generate | rtg |
| Inland plains (example `-2897, 97, -2119`) | Flat plate, houses on it, blend to hills | rtg |
| Sea-level forest (`-524, 64, 5893`) | Path, lamps, houses **same Y** | rtg |
| Beach ~16 from water | Village may start; buildings inland; **no** sand piers or plank bridges | rtg |
| Coral reef / kelp / ocean well (`-3452, 63, -2191`) | No village start unless a dry slot exists in retry range (`veto` `ocean_well` / `coast_ocean`) | rtg |
| River well | Walks inland (`well-walk`); no plank dock; plate at land Y not riverbed | rtg |
| Dry plains well below Y 64 | Kept; `plateSample … target=64`; not `flooded_well` | rtg |
| Flooded plains well | Raised to min well height if not never-raise; `/locate Village` can find it | [villagegen_info.md](villagegen_info.md) |
| Dry plains village | RC paste Y ≈ plate Y; well chunk `pad>0` | villagegen_info |
| House/RC on a plains lake edge | Omitted, **or** dirt pad for the **12-block** footprint (lake beyond the pad stays water) | rtg |
| `/aqvillage` (OP) | Relog Overworld, run **before** new chunks: feet on ground ~6 off the well (`unexplored` or `known`), not inside a hill, not `No village generator on this world`. Nether still errors (INFO line in `latest.log`). Non-OP denied. | rtg |
| Village `AQTSmallShrine` (new villages) | Uncommon (weight 5). Fly several towns until one appears; then complete marble, no dirt collar, at most one. Missing shrine in some villages is OK. | rtg |
| Pad top | Native RTG surface (sand/grass). `biomesoplenty:mud` → loamy `grass:2` | rtg |
| New inland `/locate Village` | Houses and roads present; `villagepatch.log` `landBoxes` ≫ 1. Already-visited ghost wells stay empty | rtg |
| Water pieces | No waystone/house in a lake; wet path retries inland; no oak plank path over leftover open water; **no** roads/houses in F3 River or ocean | rtg |
| Swamp village plate | Well on grass, not over a ravine; roads level with houses; overlapping pads; `seal chunk=` in debug | rtg |
| Hill village (new chunks) | All pieces on **one** well Y; no chunk-border stone wall; unused AABB corners stay hills | rtg |
| Under a house at plate-4 | `isInsideStructure("Village")` true; below well floor false | rtg |
| Yard between path and house (12-pad / Hermite) | `isInsideStructure("Village")` true after new gen and after relog. Outside pad+falloff false. Plate+31 false if box height is 30 | rtg |
| Night village lamps (new chunks) | Path torch and fence lamp light the plate and nearby house walls; not a 1-block puddle | rtg |
| Below Y0 Overworld | Deepslate fill, AQ caves, Y0 mouths on land, **no** ocean drain | [depths.md](depths.md) |
| -Y caves after a perf change (new chunks) | Same seed, same chunks: tunnels, chambers, pillars, bridges, stalactites and floater cleanup unchanged. Perf work here is exact-equivalence, so any visible difference is a bug | depths |
| Spark while flying new terrain | `UpperTunnelNetwork.forColumn`, `columnStrength`, `getSurfaceAltitudeForColumn` and `Reflect.getBlockState` all well down; chunk gen no longer ~half Tweaks | depths |
| Fog / sky below Y0 | Dark fog ~32–52; no skybox | depths |

Log snippets if debug on: `veto chunk=`, `forget chunk=`, `flatten chunk=`, `seal chunk=`, `waystone relocate`, `village piece skip water floor charm`, `astral small shrine village piece`.

## Stamina

Use the full list in [stamina.md](stamina.md) **Verify**. Minimum: jump costs/blocks, melee hit spend, bow draw, climb slide when empty, grapple hang vs climb vs grounded, HUD when dodge locked. With armor + toughness + thirst: toughness **left-to-right** one row above **armor** (left); feathers above **thirst** (right), not overlapping. Iron pick tooltip: `Vanilla Tools` + harvest stars + durability + efficiency. Metallurgy pick: no duplicate Tweaks harvest line. See [client.md](client.md).

## Other modules (one-line)

| Module | Smoke |
| --- | --- |
| Gaia | Melee: one physical hit, names the mob, **no MAGIC 6** (diamond must not take a flat ~3 hearts of magic). Unarmored may exceed JSON if the mob holds a sword. Hard archer: no MAGIC tip. Bolts: armor skip, Magic Protection works, death names shooter. Bomb: armor + Blast Protection, no extra 2.0, facing shield zeroes. Log: no `mixins.aqtweaks.gaia.json` injection failure. JSON `grimoireofgaia:orc` changes orc melee and bolts after restart. |
| Thaumcraft | First Nether visit warps after ~2s; sleep at dawn reduces warp; whispers underground on interval. Log: no `mixins.aqtweaks.thaumcraft.json` injection failure. Fire/frost foci `isMagic=true classified=true`; Heal on self scales with Magic; snowball does not |
| Bewitchment | Listed ritual **finish** grants warp; halt does not |
| Comfort | Homestead I in a scored room while healthy; penalties (hungry/thirsty/hurt/sleepy) can deny it; II after ~1:00 if score ≥40, III after another 1:00 if ≥60. No regen/saturation/SD thermals from Homestead. XP boost refreshes 8:00 while held, counts down after cancel. Hot spring → cold resist if SD+BOP |
| Portal | Arcane Tunnel binds then opens a 60s two-way rift (cross-dim if bound elsewhere); Unstable Arcane Tunnel lands ~4000–6000 same dim; villagers/mobs in the box teleport; sitting pet stays; lead follows. Dark cave mid-cell lights like glowstone. See [portal.md](portal.md) |
| Client | Toughness LTR above armor; iron pick shows Vanilla Tools stats; Metallurgy pick not duplicated |
| Recipes | Pack boots without Metallurgy `generated/item/spartanweaponry` recipe spam |
| Reskillable | Attack 16 → +2 damage; Mining Expert wood pick drops diamond ore; stamina perks on tree. Full list: [reskillable.md](reskillable.md) |
| Spawning | Boot log loads `config/arcanaquest/mob_overworldspawntype.json`. Closed cave: dwarf/cave_spider/krake yes, Dryad/witch/Wildkin no, zombie/goblin still yes. Night surface: reverse exclusives; zombie still yes. InControl 2..4 packs are 2–4 together, not stuck at 1. Default `goblin_feral=3-5`. Fill Pack Size off: old singles. No mixin fail on `MixinWorldEntitySpawner`. See [spawning.md](spawning.md) |

## After mixin / parent bumps

Re-read mixin targets ([compatibility-matrix.md](compatibility-matrix.md) upgrade check). RC version bumps: confirm `RayMatcher.cast` `@Overwrite` still matches.
