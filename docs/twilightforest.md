# Twilight Forest portal landing (1.9)

Last updated: 2026-11-09.

Portal landing is the rest of this file. The Carminite Reactor phlogiston and Void Breach rituals follow.

Optional bus handler if RandomPortals **and** Twilight Forest are present. Optional mixin json `mixins.aqtweaks.randomportals.json` (`required: false`). Tweaks cfg: `config/arcanaquesttweaks/aqtweaks_twilightforest.cfg`.

Does **not** change Tweaks’ own rift items ([portal.md](portal.md)).

## Locked intent

RandomPortals destinations into Twilight Forest (pack dim **7**) must not generate in progression-locked / hazardous biomes, inside TF landmarks, or on tall-tree canopy. New pads sit on **`Material.GRASS`** (same bar as TF `isIdealForPortal`).

Do **not** cancel `NetherPortalEvent.Teleport.SearchingForDestination` (RP then skips search/build). Do **not** `@Mod required-after` either parent. Do **not** call private `TFTeleporter.findSafeCoords`. Do **not** break already-linked return trips.

## How the parents work

RandomPortals (`randomportals`, jar `randomportals-cleanroom0.1.0.jar`) `RPOTeleporter.placeInPortal` scales X/Z, then `placeInExistingPortal` / `makePortal` use `entity.posX/Y/Z`. At the start of `placeInExistingPortal` it posts cancelable `SearchingForDestination` on `MinecraftForge.EVENT_BUS`. Cancel → return true, no move.

If the sending `NetherPortal` already has a receiving frame, RP teleports there and ignores entity XZ.

`isValidPortalPosition` only requires solid under air, so leaves count. `findTopLeft` last-resort clamps Y to ≥70 and skips that check.

Twilight Forest (`twilightforest`, jar `twilightforest-1.12.2-3.15.1.jar`) `TFTeleporter.moveToSafeCoords` uses public `isSafeAround` (world border, `TFWorld.isBiomeSafeFor` for **players**, `ChunkGeneratorTFBase.isBlockInFullStructure`). Search ranges 200 then 400, `range/8` random samples at Y=100. `findSafeCoords` is private. `isBiomeSafeFor` is true for non-players.

## Design plan

`TfPortalLandingHandler` on `MinecraftForge.EVENT_BUS` only when **both** mods are loaded. Compile-hard RP event/teleport types and `TFTeleporter` / `TFWorld`. Keep those imports off `CommonProxy` and `ArcanaQuestTweaksConfig`.

On `SearchingForDestination` (server), cfg enabled, dest dim = cfg id (default 7), no receiving frame:

1. `TFTeleporter.getTeleporterForDim(server, dim)`.
2. Column OK only if `isSafeAround`, biome passes cfg denylist / optional allowlist, and the column has a grass block from TF sea level up (`TfPortalGrass.findGrassBlockY`). Dirt/stone/canopy-only columns fail.
3. If not OK: same random search as TF (200, then 400).
4. `entity.setLocationAndAngles(x+0.5, grassY+1, z+0.5, …)`. Never cancel the event. If no grass-safe column, leave the entity.

Mixin `MixinRPOTeleporter` (`remap = false`), only when `dimensionID` matches cfg (TF dest). Aether island pads use the same mixin when the Aether dest dim matches ([aether.md](aether.md)):

- `isValidPortalPosition` RETURN: parent true still fails unless every platform cell at `y-1` is `Material.GRASS`. Vertical frames also need one extra air cell above so they can sit **on** the grass.
- `findTopLeft` RETURN: vertical dest frames are shifted **up 1** so the bottom row rests on the grass instead of replacing it (RP otherwise puts the bottom in the platform layer). If the pad is not grass, search generate-radius for a grass pad. If none, keep the parent return.

Nether and other RP dims: mixin no-ops. Missing either jar: handler not registered; mixin json skipped.

## Files

| Piece | Role |
| --- | --- |
| `twilightforest/TfPortalLandingHandler.java` | Search-event reroute |
| `twilightforest/TfPortalGrass.java` | Grass column / pad / biome lists |
| `mixin/randomportals/MixinRPOTeleporter.java` | Grass gate on generate |
| `mixins.aqtweaks.randomportals.json` | Optional late json |
| `ArcanaQuestTweaksConfig.TwilightForestModuleConfig` | `aqtweaks_twilightforest.cfg` |

## Live config (`config/arcanaquesttweaks/aqtweaks_twilightforest.cfg`)

| Category / Key | Type | Default | Description |
| --- | --- | --- | --- |
| `general` / Enable Twilight Forest Portal Safety | boolean | true | Master switch for handler + mixin grass gate |
| `general` / Destination Dimension ID | int | 7 | Dim the hook applies to |
| `general` / Always Unsafe Biomes | string[] | dark forest center, fire swamp, twilight glacier, thornlands, highlands center | Never land here (all entities) |
| `general` / Safe Biome Allowlist | string[] | empty | If any names set, biome must also be listed |

## Do not regress

- Do not cancel `SearchingForDestination`.
- Do not import RP/TF types from always-on bus classes or the `@Config` class.
- Do not `@Shadow` vanilla `Teleporter.world` / `field_85192_a` on `RPOTeleporter` (`remap = false`, no refMap). Dest world is `entity.world` or `DimensionManager.getWorld(dimensionID)`.
- Do not retarget already-linked receiving frames (return trips).
- Do not require grass in the Nether or other RP destination dims.
- Do not use `getTopSolidOrLiquidBlock` for stand Y (canopy / water).
- Arcane Tunnel rifts stay [portal.md](portal.md).

## Verify

1. Build with `options.release = 21` via `build_gradle.ps1`.
2. First Overworld→TF portal whose 1:1 column is a listed unsafe biome: dest in a safe biome, **on grass** (bottom frame row **on** the grass, not replacing it), not in a landmark, **not on tall trees**.
3. Second trip through the same sending portal: same dest.
4. Nether RandomPortals travel unchanged.
5. Strip RandomPortals or TF: Tweaks still loads; mixin json skipped / landing handler not registered.

## Carminite Reactor phlogiston ritual

Bus handler `CarminiteReactorRitualHandler`, registered from `CommonProxy` only when **all** of `twilightforest`, `thaumcraft`, `gamestages`, `thebetweenlands`, and `contenttweaker` are loaded. No mixin. No config key. Version stays 1.10. Spec: `.plans/20261108-1805-carminite-reactor-phlogiston-synthesis.md`.

### Locked intent

Right-click an inactive Carminite Reactor (`twilightforest:tower_device` variant `REACTOR_INACTIVE`, meta 12) with `thaumcraft:salis_mundus` while all six faces are `thebetweenlands:octine_block`. That starts a 200-tick charge and then drops 5–7 `contenttweaker:phlogiston_matrix`.

The shell check comes **before** the stage check. Anything short of six Octine faces returns without cancelling, so Twilight Forest's own `onBlockActivated` can still arm a ready reactor.

GameStage `adamantine` is required only on a complete shell. Tweaks does not grant that stage; the pack does. Refusal uses `chat.aqtweaks.twilightforest.reactor_unlearned` (purple) plus `.detail` (gray). Salis Mundus is not consumed. The click is cancelled so the vanilla reactor does not also arm.

Salis Mundus is shrunk on the server, and only after the shell, the stage, and `contenttweaker:phlogiston_matrix` all resolve. Creative mode does not spend it. A missing matrix item logs `[AQ-TF]` once and leaves the click alone.

### Charge and climax

Server `WorldTickEvent` END. While the chunk is unloaded the ritual stays registered and does not advance. A broken shell fizzles (smoke, extinguish) and leaves whatever blocks remain. Salis Mundus is not refunded.

Every 5 ticks plays `BLOCK_PORTAL_TRIGGER`, pitch `0.5 + (ticks / 200) * 1.5`. The last cue is tick 195 (pitch about 1.96). That sound is long enough to overlap; listen in game before changing the cadence.

Inward motes use `WorldServer.spawnParticle` with **count 0**, which the client reads as velocity. Portal particles spawn at the core with motion toward the Octine face, so they travel from the face back to the core. Flame along each face moves inward. A flame ring uses tangential velocity. Lava sparks at the Octine faces stay a count-1 scatter.

At tick 200, if the matrix item is missing, fizzle and do not clear the shell. Otherwise play `ENTITY_GENERIC_EXPLODE`, `EXPLOSION_LARGE`, and a burst of `EXPLOSION_NORMAL`. Do **not** call `World.createExplosion`: in 1.12.2 the boolean only skips block damage, and strength 2.5 still hurts entities and can destroy dropped items. Then set the reactor and the six Octine blocks to air, spawn one `EntityItem` of 5, 6, or 7 matrix items, and play `ENTITY_PLAYER_LEVELUP` plus `BLOCK_ENCHANTMENT_TABLE_USE`.

Leaving the dimension pauses the charge the same way an unloaded chunk does: the ritual stays registered and resumes when that world ticks again. A server restart, or leaving that world (quit to title), during the 10 seconds spends the Salis Mundus and leaves the shell; the handler drops its rituals on `WorldEvent.Unload` so a ritual cannot tick in a later world.

### Verify

1. Incomplete shell, Salis Mundus in hand: click is not cancelled. A ready vanilla reactor still arms.
2. Full shell, no `adamantine`: purple/gray refusal, item kept, reactor stays inactive.
3. Full shell, stage present, survival: one Salis Mundus spent, 10-second charge, player takes no explosion damage, neighboring blocks stay, drop is 5–7 matrix.
4. Creative: same climax, item not spent.
5. Break one Octine during the charge: fizzle, remaining blocks stay.
6. Strip Betweenlands or ContentTweaker: handler not registered; inactive reactor right-click is Twilight Forest's.

## Carminite Reactor Void Breach ritual

Bus handler `CarminiteReactorVoidBreachHandler`, registered from `CommonProxy` only when **all** of `twilightforest`, `thaumcraft`, `thaumicaugmentation` and `gamestages` are loaded. Compile-hard against Thaumic Augmentation (`ThaumicAugmentation-1.12.2-2.1.14.jar`). `@Mod` has `after:thaumicaugmentation`. No mixin. Version stays 1.10. Spec: `.plans/archive/20261112-1715-carminite-reactor-void-breach.md`.

### Locked intent

Right-click an inactive Carminite Reactor (`twilightforest:tower_device`, meta 12) with `thaumcraft:salis_mundus` while it is encased in the configured casing (default six `thaumcraft:metal_void`). After a charge it collapses into a **permanent pair** of Thaumic Augmentation fractures: one at the reactor, one in The Emptiness beside the nearest Eldritch Spire. The ritual works **anywhere** the reactor stands (it is not tied to the Dark Tower) and is refused only in The Emptiness itself and when TA's `DisableEmptinessDimension` is true (`TADimensions.EMPTINESS` is null).

The fractures are plain `EntityDimensionalFracture` entities. Tweaks does not touch them after spawning, so they **spawn Eldritch Guardians and otherwise behave exactly as TA's natural fractures do**. Do not suppress guardians.

### Shell, stage, consumption

- Main hand only. Casing on the six faces in the order UP, DOWN, NORTH, SOUTH, EAST, WEST; `Cap Block` (empty means the casing block) is used for UP and DOWN. The pure helper `VoidBreachShell` matches and validates it.
- The Octine shell belongs to the phlogiston ritual: a full Octine shell is left to `CarminiteReactorRitualHandler`. Air, the reactor block and the Octine block are not allowed as casing or cap (one warning, falls back to Void Metal / the casing), so the two rituals never share a shell.
- Any shell short of the full casing returns without cancelling, so Twilight Forest can arm a normal reactor.
- On a full shell the `Required GameStage` (`voidmastery`, granted by the pack, empty means none) is checked. Refusal: `chat.aqtweaks.twilightforest.reactor_void_unlearned` (purple) plus `.detail` (gray), click cancelled, Salis Mundus kept. Then the Emptiness-availability guard (`reactor_void_unavailable`, nothing consumed). Salis Mundus is shrunk on the server unless the player is in creative.

### Charge and climax

Server `WorldTickEvent` END, `Charge Duration Ticks` (default 200). While the chunk is unloaded the ritual pauses. A broken shell fizzles (no refund, blocks stay). **A server restart or unloading the world forgets the ritual** (in-memory, dropped on `WorldEvent.Unload`): the Salis Mundus is spent and the shell stays. Audio: cave rumble every 20 ticks, portal trigger every 6 ticks at pitch `0.4 + (ticks / duration) * 0.3`. Particles: inward portal motes, deep-purple `SPELL_MOB`, swirling `SMOKE_LARGE`, `DRAGON_BREATH` around the side casings.

At the climax nothing is changed until a **pre-flight** passes: the Emptiness world resolves (`DimensionManager.getWorld`, else `initDimension`) and a destination is found. If either fails the Salis Mundus is **refunded** (dropped at the reactor), the shell and reactor stay, and `[AQ-TF]` is logged. Destination: `FractureUtils.movementRatio(world)` and `scaleBlockPosToEmptiness`, then the nearest `EldritchSpire` (`getNearestStructurePos`, which TA answers only when `GenerateEldritchSpires` is true); the 3x3 chunks around it are generated; a standable column (a solid block with two clear blocks above, liquids count as solid, `ColumnSearch`) is chosen on a ring just outside the spire's bounding box. With no spire it uses the scaled column. A spot with a fracture within 4 blocks is skipped, so fractures never stack.

Then the remote fracture and the local fracture are spawned and linked to each other (`setLinkedDimension`, `setLinkedPosition`, `setDestinationBiome`, `setLinkLocated(true)`, `open(true)`), exactly as TA's `WorldGenDimensionalFracture` and `/ta makefracture` do. If either spawn fails the first is removed and the Salis Mundus refunded. Both ends are registered with `CapabilityFractureLocations` and `FractureLocatorSearchManager`. Last, the reactor and casing become air with a cosmetic explosion (never `World.createExplosion`) and `BLOCK_END_PORTAL_SPAWN` plus `SoundsTC.shock` at both ends.

### Config (`aqtweaks_twilightforest.cfg`, category `Void Breach Ritual`)

| Key | Default | Notes |
| --- | --- | --- |
| `Enable Void Breach Ritual` | true | false leaves the reactor to Twilight Forest |
| `Required GameStage` | `voidmastery` | the pack grants it; empty means none |
| `Casing Block` | `thaumcraft:metal_void` | `modid:block` or `modid:block:meta` |
| `Cap Block` | empty | top and bottom faces; empty means the casing block |
| `Charge Duration Ticks` | 200 | 40 to 1200 |

### Do not regress

- Keep the shell check before the stage check; keep the full Octine shell for the phlogiston handler.
- Do not suppress guardians or alter the fracture entities after they spawn.
- Keep the pre-flight before any block is removed.

### Verify

See `docs/verification.md` (Void Breach ritual).

## Ley-Rupture (arcane surges rising from the ground)

Last updated: 2026-11-09. Lore (the user's): the realm's ancient leyline machinery failed, and the broken leylines still bleed unstable arcane vitality into the earth. Provider `WorldProviderTwilightForest` (compile-hard, jar in `libs/`); registered only when `twilightforest` is loaded. Tweaks cfg: `config/arcanaquesttweaks/aqtweaks_twilightforest.cfg`, category **Ley-Rupture** (`TwilightForestModuleConfig.ley`; the portal landing keeps its own category). Scheduling and the event packet: [hazards.md](hazards.md) (channel `rupture`, id 5). Overview: [dimension-hazards.md](dimension-hazards.md).

* **Timing:** a 25 to 30 s surge every 5 to 8 minutes, each preceded by a **30 s warning** (the engine warning phase). Client telegraph (`client/twilightforest/LeyRuptureClient`): ambient wildlife and Dynamic Surroundings / AmbientSounds animal and wind sounds fall silent from the start of the warning, a resonance hum builds in pitch, and in the last ~10 s glyphs rise from the ground, zigzag and scatter through the air and fade after about 2 s (`ParticleLeyEruption`, violet 70 percent and emerald 30 percent, full-bright), with taller violet motes. During the surge the particles are far denser: glyphs, wisps, sparks and dots spread over a wide square around the player (`Ley Particle Radius`, 48 blocks; nothing within 4 blocks of the camera) at the same density per square block as the old 32 by 32 box, plus eruption vent plumes on a ring 8 to 48 blocks out that burst upward in chaotic jitter. Far particles scale up to 1.6x, columns more than 16 blocks out are found from the height map so hills do not thin the field, vanilla motes use `ignoreRange` so they are not cut off at 32 blocks, and the plumes keep a reserved 40 percent of `Max Ley Particles`. A forced `/leyrupture surge` warns the same.
* **Exposure (`GroundReference`, `LeyTerrain`):** the reference point is the average ground surface (the player's column and four at 8 blocks N, S, E, W, each scanned down through leaves, logs, vines, plants and air to the first grass, dirt, stone, sand, gravel, ice or water; loaded chunks only) minus `Reference Offset Blocks`. Exposure = 1 at or below it, falling linearly to 0 at `Fade Height Blocks` above it. A player on a high branch counts the ground below the canopy; caves and hollow hills sit below the reference and get the full effect. It is recomputed once a second. Zeroed by a Comfort room (Rested) or by being inside the bounds of a feature on `Shelter Features` (`ChunkGeneratorTFBase.isBlockInStructureBB` and `getFeatureAt`, reached through `AccessorChunkProviderServer.chunkGenerator`; in-memory structure starts, no chunk is loaded). Hollow hills, the hedge maze, the naga courtyard and the like are not on the default list.
* **Scour (`LeyRuptureHandler`):** every 4 s after `Exposure Grace Ticks` (40), `aqtweaks_ley_rupture` armor-bypassing magic damage: `Unprotected Magic Scour Damage` (4.0) or `Proper Gear Magic Scour Damage` (1.5) with four slots on `Proper Arcane Gear Whitelist`, times the exposure and the biome intensity. Mundane armor does nothing. Each second also adds hunger exhaustion (`Hunger Burn`, 40% in proper gear) and, per hit, a `Warp Chance` for 1 temporary warp, capped at `Warp Cap Per Surge` per player (Thaumcraft's helper, if loaded). The server sends the exposure factor (`PacketLeyExposure`, id 13) for the violet and emerald edge vignette.
* **Biomes (`BiomeIntensity`):** `Biome Intensity` (`biome=multiplier`, TFBiomes field names, capped by `Max Biome Intensity`) scales damage, hunger and charging; `Biome Extras` (`biome=potion,ticks[,amplifier]`) adds one effect per hit (Blindness, Weakness, Slowness, Nausea, Mining Fatigue, ignition for the Fire Swamp, brief Levitation in the Enchanted Forest only with 4 blocks of head clearance). `/leyrupture biome` prints what is read at the caller.
* **Backlash (`LeyRuptureBacklash`, `ArcaneItemClassifier`):** using a magic item while exposed hurts: `Spell Backlash Damage` (3.0) times the exposure, at the proper-gear ratio in proper gear, with sparks and a hiss, once per `Backlash Cooldown Ticks`. Magic items are Thaumcraft `ICaster` items, Botania mana-using items, and registry names on `Magic Item Patterns`. Triggers: right click (item and block), starting to use an item, attacking with one, and mounting an entity whose registry name matches the patterns (brooms). A Thaumcraft caster also adds `Backlash Warp` against the same per-surge cap. The use succeeds unless `Backlash Cancels Use`. `/leyrupture item` reports the classification of the held item. Not built: the optional Vis cost multiplier on `ItemCaster.consumeVis`.
* **Charging (`SurgeCharging`, `VisBatteryTracker`):** in the active phase, every `Charge Pulse Seconds` at exposure of at least `Charge Min Exposure`, carried Embers ember-capability items and Botania mana items gain `Charge Percent Per Pulse` of capacity scaled by exposure and biome, and each pulse that charged something adds `Charge Warp` temporary warp up to `Charge Warp Cap Per Surge`. Thaumic Augmentation's caster battery is not in the pack's libs and is not handled. Placed Thaumcraft Vis Batteries gain `Battery Levels Per Pulse` (scaled by exposure and biome, random rounding) and add `Battery Flux Per Level` flux to the chunk aura; roofed (a real roof within 3 blocks), sheltered and canopy-high batteries do not charge. Batteries are found by a per-chunk scan done once when the chunk loads (one section per tick) and kept current by place and break events.
* **Ley mobs:** while active, every `Mob Pulse Seconds` per exposed player with chance `Mob Chance`, 1 to 2 mobs from `Ley Mob Pool` surface 12 to 24 blocks away (caps per player and in total), glowing, tagged `AQTLeyMob`, with no drops or experience; they dissipate in a particle burst when the surge ends.
* **Command:** `/leyrupture <surge|clear|status|item|biome> [seconds] [skipWarning]` (OP).
* **Not built (optional in the plan):** visible fissures (3.8).
* Files: `twilightforest/LeyChannels`, `LeyEruptionShape` (pure spread, colours, sprites and budgets, unit tested), `client/twilightforest/ParticleLeyEruption`,, `LeyRuptureHandler`, `LeyRuptureBacklash`, `ArcaneItemClassifier`, `GroundReference`, `LeyTerrain`, `BiomeIntensity`, `SurgeMath`, `SurgeCharging`, `VisBatteryTracker`, `ClientLey`, `CommandLeyRupture`; `network/PacketLeyExposure`; `client/twilightforest/LeyRuptureClient`, `LeyRuptureVignette`; tests `GroundReferenceTest`, `SurgeMathTest`, `BiomeIntensityTest`, `ArcaneItemClassifierTest`.

### Ley-Rupture config key reference

| Key | Default | Note |
| --- | --- | --- |
| Enable Ley-Ruptures | `true` | Timed arcane surges in the Twilight Forest that rise from the ground: magic damage, hunger burn, spell backlash. |
| Rupture Interval Min Seconds | `300` |  |
| Rupture Interval Max Seconds | `480` |  |
| Rupture Duration Min Seconds | `25` |  |
| Rupture Duration Max Seconds | `30` |  |
| Warning Seconds | `30` | Telegraph before the first damage: silence and a faint hum, then glyph wisps in the last third. |
| Unprotected Magic Scour Damage | `4.0` |  |
| Proper Gear Magic Scour Damage | `1.5` |  |
| Fade Height Blocks | `14` | The effect fades linearly to nothing this many blocks above the reference point. |
| Reference Offset Blocks | `2` | The reference point is the average ground surface minus this. |
| Exposure Grace Ticks | `40` | Delay before the first scour hit after a player becomes exposed. |
| Hunger Burn | `1.0` | Exhaustion added each second at full exposure (proper gear takes 40%). |
| Warp Chance | `0.25` | Chance per scour hit to absorb 1 temporary warp (Thaumcraft). |
| Warp Cap Per Surge | `1` | Most temporary warp the hazard itself (hits and backlash) gives one player in one surge. |
| Proper Arcane Gear Whitelist | `(list)` | Item registry names (wildcards supported). Four armor slots from this list count as proper arcane gear. |
| Shelter Features | `(list)` | Twilight Forest feature names whose structure bounds shelter a player. |
| Enable Spell Backlash | `true` | Using a magic item while exposed hurts the user. |
| Spell Backlash Damage | `3.0` |  |
| Backlash Warp | `1` | Temporary warp for a Thaumcraft caster's backlash (counts against Warp Cap Per Surge). |
| Backlash Cooldown Ticks | `20` |  |
| Backlash Cancels Use | `false` |  |
| Magic Item Patterns | `(list)` | Registry names (wildcards supported) of magic items besides Thaumcraft casters and Botania mana-using items. |
| Biome Intensity | `(list)` | biome=multiplier (TFBiomes field names). Unlisted biomes use 1.0. |
| Biome Extras | `(list)` | biome=potion,ticks[,amplifier] once per hit while exposed; the potion 'fire' sets the player on fire for ticks. |
| Max Biome Intensity | `1.6` |  |
| Enable Surge Charging | `true` | Carried Embers, Botania and similar energy items gain charge in the open during a surge, at the price of temporary warp. |
| Charge Min Exposure | `0.25` |  |
| Charge Pulse Seconds | `5` |  |
| Charge Percent Per Pulse | `10.0` |  |
| Charge Warp | `1` |  |
| Charge Warp Cap Per Surge | `12` |  |
| Charge Vis Batteries | `true` | Thaumcraft Vis Batteries placed in an exposed spot gain charge during a surge. |
| Battery Levels Per Pulse | `1` |  |
| Battery Flux Per Level | `0.25` | Flux added to the chunk aura for each charge level added to a battery. |
| Enable Ley Mobs | `true` | Impermanent mobs surface near exposed players during the active phase; they drop nothing. |
| Mob Pulse Seconds | `10` |  |
| Mob Chance | `0.35` |  |
| Ley Mob Pool | `(list)` |  |
| Ley Mobs Per Player | `4` |  |
| Ley Mobs Total | `16` |  |
| Enable Ley Vignette | `true` | Client: violet and emerald edge overlay while exposed. |
| Enable Ley Particles | `true` | Client: glyphs, wisps and eruption plumes rising from the ground. |
| Ley Particle Radius | `48` | Client: how far from the player the particles are spread (16 to 48). Density per square block is held constant, so a larger radius means more particles. |
| Max Ley Particles | `9000` | Client: cap on live glyph and eruption particles (100 to 14000; the eruption is meant to be heavy). |
| Ley Particle Density | `1.0` | Client: multiplier on the spawn attempts (0.25 to 4). |
| Enable Ley Audio | `true` | Client: ambient forest sounds fall silent and a resonance hum builds. |
