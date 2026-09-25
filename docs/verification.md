# Verification (1.8)

Last updated: 2026-09-20.

Manual release / smoke checklist. **No automated tests.** Harness: CurseForge **Arcana Quest DEVBOX**, remapped `ArcanaQuestTweaks-1.8.jar` in `mods/`. Algorithms and full checklists stay in module docs; this is the pack-level pass/fail.

Worldgen applies to **new chunks only**.

## Build artifact

- [ ] `.\gradlew.bat build` or `.\build_gradle.ps1` succeeds (`verifyReleaseJar`: class major 65, `VillagePlate.class`, mixin json + `mixins.aqtweaks.refmap.json`).
- [ ] Instance `mods/` has `ArcanaQuestTweaks-1.8.jar`, **not** `-dev`.
- [ ] Only one Tweaks jar (the deploy script deletes other `ArcanaQuestTweaks-*.jar`).

## Boot

- [ ] Client starts the full pack; Charm is present (`required-after:charm`). No mixin apply crash from `mixins.aqtweaks.json`, `mixins.aqtweaks.early.json`, or `mixins.aqtweaks.charm.json`. Log must not say `MixinWorldRiftLight` / `World was loaded too early`, `MixinMinecraftMouseGrab` / `Minecraft was loaded too early`, `MixinEntityRendererMouse` / `EntityRenderer was loaded too early`, `MixinMobSpawnerBaseLogic` / `MobSpawnerBaseLogic was loaded too early`, `MixinTileEntityLockableLoot` / `TileEntityLockableLoot was loaded too early`, `MixinItemStackQualityDurability` / `MixinItemStackDurability` / `ItemStack was loaded too early`, `MixinBlockCropsSeed` / `BlockCrops was loaded too early`, `MixinASMHooksVillagePaste` / `ASMHooks was loaded too early`, `MixinWorldGenLakes` / `field_150589_a was not located` / `WorldGenLakes in invalid classes`, `empty category` / `StatsKeeperModuleConfig` / `BetterMineshaftsModuleConfig`, BM `setBoundingBox` / `func_75072_c` was not located, `MixinRPOTeleporter` / `field_85192_a was not located` in `RPOTeleporter`, `parseStructureData is not cancellable`, `StackOverflowError` / `ItemTechnomancerScribingTools` / `QualityDurability.afterSetDamage` during recipe init, or world-tick `StackOverflowError` / `QualityStamp.stampInventory` / `fillWithLoot`. Optional `mixins.aqtweaks.gaia.json` must not log `InvalidInjectionException` (vanilla INVOKEs must be MCP + `remap = true`; a miss boots anyway because `required: false`).
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
| Animania | No `MixinAddonHandler`; stock world-load advancement reload + Farm/Extra inject |
| Somnia Refreshed | No `MixinSomniaUtil`; stock Somnia light check behavior |
| InControl | No `MixinStructureCache`; stock origin-chunk `isInStructure` (Tweaks still loads) |
| YUNG’s Better Mineshafts | No locate/stub mixins; stock BM Y=64 `/locate Mineshaft` |
| Stats Keeper | No `LifeElixirCapHandler`; stock SK Finish refuse + vanilla consume |
| RandomPortals | No `MixinRPOTeleporter`; stock RP generate (leaves/Y≥70 fallback). Handler also off if TF missing |
| Twilight Forest | No `TfPortalLandingHandler` unless RP is also present |
| The Aether | No `AetherPortalLandingHandler` unless RP is also present |
| Quark | Helper not called; +Y stock Quark unchanged; Deepslate columns/spikes still generate |
| Chisel | No Chisel mixins; stock GUI / in-world carving |
| Recipe Stages | No `setRecipeStage` capture; Chisel gate has no CT index (fallback map also missing) |
| Game Stages | `lockedStage` no-ops; Chisel mixins still apply if Chisel is present |
| Quality Tools | No `mixins.aqtweaks.qualitytools.json`; vanilla loot/durability mixins still apply (pack ships QT); no rune recipes |

### Do not treat as optional

Missing **RTG, Depths Update, Better Caves, CoFH World, Recurrent Complex, or IvToolkit** with the current required mixin json can **fail mixin apply** at boot. This pack always ships them. Elenai Extended and **Charm** are `@Mod` **required-after**. InControl is `@Mod` **after** and compile-hard for pack fill.

## Config

- [ ] After a Java default change, instance cfg still has the **old** value until edited (e.g. RTG Coast Buffer 32 vs 16).
- [ ] Change a live cfg in-game: stamina DSS costs refresh (`DssSkillCosts.invalidate`). Comfort JSON needs a restart (loaded in preInit). Settings vs blocks are separate files; the old combined `aqtweaks_comfort.json` is ignored.

## Worldgen (new chunks)

`Village Flatten Debug` is **off** by default. Turn it on only while diagnosing; it appends every line to instance `logs/villagepatch.log` (not `latest.log`) and stalls chunk gen.

| Check | Expect | Doc |
| --- | --- | --- |
| Flying new chunks (near and far from villages) | Away from villages, chat TPS stays near 20 (no growing hitch as more towns exist). A village still plates. With debug off, `villagepatch.log` stays empty | [rtg.md](rtg.md) |
| Coastal village ocean face (new chunks) | Cleaner XZ outline (open 1-block sea inlets filled). Stone brick **only** where the plate cliffs ≥2 (including top); sand/grass on level rims; no open-water pier; inland hill cliffs not bricked | rtg |
| Village interior (new chunks) | No stone brick patches in yards/farms or along chunk lines; no pad across a Land of Lakes / forest watercourse; no bare plate spur without a building | rtg |
| Village path across a river (new chunks) | Stone brick deck at plate Y, cobble wall rails, piers on runs >4; water under it; no oak planks at water level. `villagepatch.log`: `bridge clip=` lines. Ocean paths still omitted | rtg |
| Village small shrine / waystone | Complete shrine on the plate (not half over a pond). No dirt/grass collar. F3 River/ocean columns still skipped | rtg |
| River through forest/shrubland (new chunks) | No well in the channel; no house/RC/plank dock on F3 River; land plate inland | rtg |
| RC church/inn next to river or ocean (new chunks) | Building on the plate inland; water unplated. `villagepatch.log`: `rc aabb wet` then retry hit or omit | rtg |
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
| Approach a new village from unloaded chunks | No `ConcurrentModificationException` in `StructureStart.generateStructure`. Relog not required. Well not sitting on ocean/river | rtg |
| Stamina after G2 (sprint / climb / grapple) | Same costs and cancel behavior as before Comfort remap; no extra `Reflect` lag on the tick | stamina |
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
| Spark while flying new terrain | `UpperTunnelNetwork.forColumn`, `columnStrength`, `getSurfaceAltitudeForColumn` and chunk/BC `Reflect` getBlock/getMaterial well down; Tweaks no longer ~half of chunk gen. Same-seed bridges in the same places | depths |
| Fog / sky below Y0 | Dark fog ~32–52; no skybox | depths |
| Deep cave floor/ceiling (new chunks, Quark on) | Tapered Quark stone speleothems mixed with existing Deepslate columns/spikes/bridges. +Y Better Caves still have stock Quark clusters. Quark jar removed or cfg off: no deep speleothems; cave shape unchanged | depths |
| Deep cave pillars (new chunks) | Denser 20-block-cell pillars (~1.44× vs old 24). Same-seed **bridges** match the previous build; pillar sites shift. Old chunks stay sparse | depths |
| New `/locate Mineshaft` | Unexplored pin has tunnels; `isInsideStructure` true at the pin. Old generated-empty pin is skipped. New terrain rarer than every-land 0.003 (Tweaks 0.001 + spacing 4); hill openings still happen | [bettermineshafts.md](bettermineshafts.md) |

Log snippets if debug on: `veto chunk=`, `forget chunk=`, `flatten chunk=`, `seal chunk=`, `waystone relocate`, `village piece skip water floor charm`, `astral small shrine village piece`.

## Stamina

Use the full list in [stamina.md](stamina.md) **Verify**. Minimum: jump costs/blocks, melee hit spend, bow draw, climb slide when empty, grapple hang vs climb vs grounded, HUD when dodge locked. Temperature: while hypothermic or hyperthermic, no periodic thermia hearts, gold feathers and the Feathers potion stay off, the feather cap falls about one half-feather every 5 seconds, holds the last half-feather for one ramp, then drops to 0. At that step the death message is that Simple Difficulty source (hyperthermia, not dehydration). After the potion ends the cap returns one half-feather every 10 ticks. Hyperthermia also drops about one thirst point per 10 seconds. Slowness ramps I → III in the cold and clears when hypothermia ends. With armor + toughness + thirst: toughness **left-to-right** one row above **armor** (left); feathers above **thirst** (right), not overlapping. Iron pick tooltip: `Vanilla Tools` + harvest stars + durability + efficiency. Metallurgy pick: no duplicate Tweaks harvest line. See [client.md](client.md).

## Other modules (one-line)

| Module | Smoke |
| --- | --- |
| Gaia | Melee: one physical hit, names the mob, **no MAGIC 6** (diamond must not take a flat ~3 hearts of magic). Unarmored may exceed JSON if the mob holds a sword. Hard archer: no MAGIC tip. Bolts: armor skip, Magic Protection works, death names shooter. Bomb: armor + Blast Protection, no extra 2.0, facing shield zeroes. Log: no `mixins.aqtweaks.gaia.json` injection failure. JSON `grimoireofgaia:orc` changes orc melee and bolts after restart. Default JSON HP/armor: orc 30/4, dwarf 60/8, feral goblin 15/4, sporeling 15/2. Deep Dwarf: `/summon aqtweaks:deep_dwarf` hostile, purple face, white/gray hair and beard, red eyes, stock gear; JSON attack 10 / HP 60 / armor 8. Deathword: ranged Gaia magic (no melee), piercing like orc bolt, wither +30 ticks stacked, summons/beacon stay. |
| Thaumcraft | First Nether visit warps after ~2s (+5 sticky +5 temp); sleep at dawn reduces **sticky** only (no extra temp); whispers ~every 5 min while exposed (30s silent score ticks). Warp Ward pauses exposure banks. Focus pouch in a BaublesEX slot ≥ 4: cycle focus onto the gauntlet, focus is not deleted, no `NoSuchFieldError` / `ClassCastException` from `fetchFocusFromPouch`. Log: no `mixins.aqtweaks.thaumcraft.json` injection failure. Fire/frost foci `isMagic=true classified=true`; Heal on self scales with Magic; snowball does not. Runic gear (overhaul off): red hearts plus runes, extra rows on empty sockets past 10, armor above those rows; gold hearts only for absorption above the gear cap. No runic gear: golden apple stays gold hearts |
| Bewitchment | Listed ritual **finish** grants warp; halt does not. CraftTweaker `mods.bewitchment.SpinningWheel.addRecipe` / `removeRecipe` updates JEI and the wheel; unnamed ids are `crafttweaker:<name>` |
| Comfort | Homestead I in a scored room that also has a hearth, bed, or seat, while healthy: Learning 8:00, no Soot XP boost. Lanterns/structure alone do not start it. Penalties can deny it. Scan every 30s; Homestead potion 45s. Hurt: clear + 30s before re-entry. Attack entity: clear + 15s. II: Learning gone, XP boost I + endurance I + replenishment 4:00. III: XP boost II + endurance II + replenishment 8:00. Temp warp cleanse +2/+3/+6 per scan, fire at 12 (180s / 120s / 60s); counter survives Homestead drop. OP `/aqcomfort` prints the breakdown and can refresh without waiting 30s; non-OP denied. Hot spring → cold resist if SD+BOP |
| Portal | Arcane Tunnel binds on air or block then opens a 60s two-way rift (cross-dim if bound elsewhere); land at dest rift XYZ after ~3s gate; Unstable Arcane Tunnel lands ~4000–6000 same dim; villagers/mobs in the box teleport; sitting pet stays; lead follows. Dark cave mid-cell lights like glowstone. Sit in an open rift: no `updateClouds` CME. Do not DS full-bypass the rift (blanks the cylinder). See [portal.md](portal.md) |
| Client | Toughness LTR above armor; iron pick shows Vanilla Tools stats; Metallurgy pick not duplicated |
| MineMenu | Reskillable, BetterQuesting, and Hwyla config: look frozen while open. Close from a corner: cursor grabbed at center, no yaw snap, next look starts from center. Inventory, Baubles, and Esc the same. MineMenu Baubles entry opens the expanded GUI once; the real Baubles key still once and does not crash. DSS Skills GUI once from MineMenu with the key unbound or assigned; space does not open it while unbound; the real key opens once and does not crash; `/dssgui` opens it. Boot: no `Minecraft was loaded too early` / `EntityRenderer was loaded too early`. See [minemenu.md](minemenu.md), [stamina.md](stamina.md) |
| Recipes | Pack boots without Metallurgy `generated/item/spartanweaponry` recipe spam or `Parsing error loading recipe` stacks for the 40 known-missing items and `draugr_ingot_from_block`; one-shot skip lines logged per missing item ID; new/unexpected missing items still dump |
| Reskillable | Attack 16 → +2 damage; Mining Expert wood pick drops diamond ore; stamina perks on tree; Magic school mutex holds in both directions after the LOWEST restamp; Tunnel Sense glow lasts 5 seconds; Fortify shield stamina is 0. Full list: [reskillable.md](reskillable.md) |
| Spawning | Boot log loads spawn types, structure spawns, parties, and pack group sizes. First ticks: no `parseStructureData is not cancellable`. Closed cave: dwarf/cave_spider/krake yes, Dryad/witch/Wildkin no, zombie/goblin still yes. Mineshaft non-origin chunk: witch/illager/pillager can appear; ordinary cave still no. Night surface: reverse exclusives; zombie still yes. Creeper packs 1–2 not 4; enderman 1; zombie/skeleton/spider 2–4 mixed. Default `goblin_feral=3-5`. Fill Pack Size off: old singles. Natural Overworld cleric: knights + CR archers; cage/portal cleric: no party. Failed cultist/blaze cage Delay ≈ 20 (cfg) not 0 and not 200–800; zombie cage still attempts after 1→0; success still 200–800. Hostile cap default 200. No mixin fail on `MixinWorldEntitySpawner`, `MixinMobSpawnerBaseLogic`, or `MixinStructureCache`. See [spawning.md](spawning.md) |
| Advancement | Join log: no `AddonHandler.onWorldLoad` → `ForgeHooks.loadAdvancements`. Mixin json applied. Animania animals still spawn/register. No Farm/Extra Animania advancement trees. See [advancement.md](advancement.md) |
| Better Mineshafts | `/locate Mineshaft` TPs to tunnels; new chunks use `aqtweaks_bettermineshafts.cfg` rate/spacing; log: no `mixins.aqtweaks.bettermineshafts.json` `setBoundingBox` / `func_75072_c` apply failure. See [bettermineshafts.md](bettermineshafts.md) |
| Stats Keeper | 10 hearts: elixir consumes, +1 heart, drink sound from `aqtweaks_statskeeper.cfg` (default level-up; empty = silent). 20 hearts: drink cancelled, stack remains, red action bar `Your vitality is already at its peak!`, no drink sound. Baubles/buffs above 20 hearts with unused SK additional still drink. See [statskeeper.md](statskeeper.md) |
| Twilight Forest | New Overworld→TF RandomPortals trip into a locked/hazard column lands in a safe biome **on grass** (frame bottom **on** grass, not sunk), not a landmark, not tree canopy. Return through the same sending portal stays linked. Nether RP unchanged. Rift items unchanged. See [twilightforest.md](twilightforest.md) |
| Aether | New Overworld→Aether RandomPortals trip into open sky lands on island grass/dirt/holystone, not a floating Y≥70 frame. Return through the same sending portal stays linked. TF and Nether RP unchanged. See [aether.md](aether.md) |
| Game Stages | No `apprentice_builder`: chiseling a staged output (GUI and in-world) fails, red action bar, input remains. After the stage is granted, that tier chisels; higher builder tiers stay locked. Unstaged variants and crafting-table Recipe Stages unchanged. See [gamestages.md](gamestages.md) |
| Quality Tools | Crafted sword has no Quality tag. Loot chest stamps before pickup (`dark_gray` ~25% remaining, `gray` ~50%). Dawnstone: tool then rune; same-tier reroll; wrong rung action bar, rune kept. Break without Salvage drops Broken; Salvage uses Charm. See [qualitytools.md](qualitytools.md) |

## Edge cases

| Check | Expect | Doc |
| --- | --- | --- |
| Empty-stamina melee against a **mob** | Short-stamina hit on a zombie takes the reduced multiplier once, same as a hit on a player, then the attacker tag clears. Mobs are not exempt | [stamina.md](stamina.md) |
| Two players join on the same tick | Each ends with weight from their **own** armor. Neither is left on an emptied weight array or the other player's value | stamina |
| Cross-dimension rift trip that fails | Entity stays in the origin dimension. No teleport loop, no entity stuck in the AABB re-firing every tick, no ghost copy at the destination | [portal.md](portal.md) |
| Expert Climber with the perk unlocked, low feathers | Server permits the climb and the client does **not** slide. No rubber-band between a client-side `motionY = -0.15` and the server position | stamina, [reskillable.md](reskillable.md) |
| World A → title screen → world B on a different seed | No village plate heights carried over from A. `getRawLight` is back on its no-rift fast path (no lit cells at B's spawn) | [rtg.md](rtg.md), portal |
| Leave world / stop integrated server | No `NoClassDefFoundError: VillagePlate`. Log has no `EventSubscriptionTransformer` AIOOBE on Tweaks classes | rtg |
| Evasion perk | Purchasable at agility 16. No `trait\|elenaidodge2:dodge`. Boot may log that that row was dropped | [reskillable.md](reskillable.md) |
| `minWorldY` | Pinned at **-64**. No cfg knob to change it; bedrock floor, CoFH `Math.max` floor, and `RayMatcher.cast` all read that constant | [depths.md](depths.md) |

## After mixin / parent bumps

Re-read mixin targets ([compatibility-matrix.md](compatibility-matrix.md) upgrade check). RC version bumps: confirm `RayMatcher.cast` `@Overwrite` still matches.
