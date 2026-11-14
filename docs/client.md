# Client module (1.9)

Last updated: 2026-11-14.

Config: `config/arcanaquesttweaks/aqtweaks_client.cfg`. Client-only. No `@Mod` parent. Optional mixin if Toughness Bar is present.

## Locked intent

HUD and tooltip presentation that is not stamina spend. Do **not** change Elenai feather or thirst icons. Do **not** mixin Metallurgy. Skip `metallurgy:` items so Reforged’s own shift stats are not doubled.

Comfort messages (the Rested panel, action bar, toast and placement particles) are `Comfort Messages` in this cfg, with the panel anchor and the inventory-tab offsets beside it. The panel is 37 px tall and defaults to **bottom-left** (`Panel Anchor`: `bottom-left`, `bottom-right`, `top-center`, `top-left`, `top-right`, `center-right`; the old `top` and `center` mean top-center and `bottom` means bottom-right), top edge at `H - 39`, below the lowest chat line and left of the hearts. Lines: title and score, a 3 px bar, `Next`, then `Held back by` last. It is hidden while the chat input is open. The toggle is a 32x28 tab on the left inventory frame under the last Reskillable tab, icon centered at `x + 10, y + 5`. The jar carries a neutral `tab_left.png`; the Stoneborn tab is a pack resource (`resources/aqtweaks/textures/gui/tab_left.png`). Hovering the Rested row shows Insight, Vigor and Fortitude and the time left; hovering the Hearthside row shows the warp rate and the room breakdown. Both rows are pinned to the top of the effect column. Tooltips stay on when messages are off.

## How the parents work

### Toughness Bar 2.4 (`toughnessbar`)

`EventHandlerClient.onRenderArmorToughnessEvent` listens to `FOOD` Post. Stock overlay uses `GuiIngameForge.right_height` and X `width/2 + 82`, then `IINC` **-8** (hunger column, right to left).

Vanilla armor uses `width/2 - 91` and steps **+8**. **Overloaded Armor Bar** cancels vanilla ARMOR and never increments `left_height`.

### Metallurgy 4 Reforged (`metallurgy`)

`ItemUtils.buildStatsTooltip` (shift-gated) adds harvest stars (`U+2B51`), durability `remaining/max`, and efficiency float with `ScaleFormatting` colors. Harvest stars only on pickaxes in Metallurgy; Tweaks still shows harvest for any mining tool that reports a harvest level. Creative tab string is `Metallurgy Tools`.

## Design plan

### Toughness HUD

Optional late mixin `mixins.aqtweaks.toughnessbar.json` (`required: false`), `MixinEventHandlerClient`.

When `Move Toughness Bar To Armor Side` is true:

- GET `right_height` → `left_height + 10` (sit one row above Overloaded Armor Bar).
- PUT `right_height` → `left_height`.
- Constant `82` → `-91` (armor left edge).
- Stock still `IINC` **-8**. Mixin does not rewrite IINC. Each `fullIcon` / `halfIcon` X is mirrored around the first draw (`2 * origin - x`) so icons fill left to right on that origin.

When false: stock right-side RTL. Missing jar → json skipped.

### Metallurgy tooltip compat

`ClientModule` on `ItemTooltipEvent`. If the flag is on, the stack is a mining tool (`ItemPickaxe` / `ItemAxe` / `ItemSpade` / `ItemTool`, or tool classes `pickaxe`/`axe`/`shovel`), and the registry domain is not `metallurgy`, append:

1. `§9{Name} Tools` — `minecraft` → Vanilla; else Forge mod display name (fallback modid).
2. `Harvest Level:` colored stars — max `getHarvestLevel` over tool classes; star count `clamp(harvest + 1, 1, 7)`; omit if all levels `< 0`.
3. `Durability:` remaining/max — skip if `maxDamage <= 0`. Color: remaining ratio `< 0.33` red, `< 0.66` yellow, else green.
4. `Efficiency:` `item.getDestroySpeed` on stone / log / dirt for the tool class (base speed, not Efficiency enchant). Color index `ceil(clamp(speed - 6, 0, 6))` into the same seven Metallurgy colors.

Always visible (Metallurgy’s own items stay shift-gated by Metallurgy). Swords and hoes are skipped unless they expose those tool classes.

## Client relight sweep

Light reaches the client only inside chunk data, or when the client runs its own check for a block change it receives. Emitters the server lit after it had sent the chunk, or whose client check was skipped because the 17-block area around them was not loaded yet, stayed dark on the client until a torch placed nearby forced a check (F3 light reads low there). Vanilla's own sweep for dark emitters (`Chunk.enqueueRelightChecks`) runs on the server world only.

`world/client/ClientRelightSweep` (`Side.CLIENT` subscriber): `ChunkEvent.Load` on a remote world queues the chunk. On `ClientTickEvent` END, up to 2 sections per tick are scanned once the chunk's 8 neighbors are loaded (or after 30 s); every non-empty section cell whose block has a vanilla `getLightValue()` above 0 is tested: stale if its stored block light is below that value, or an open (opacity below 15) neighbor holds less than value - 1. Liquid emitters (lava) are skipped: the first version found about 8000 stale cells a minute, nearly all underground lava, filled its 8192 queue and starved real torches (a spark of the `[AQ-CLIENT-RELIGHT]` log showed 0 to 52 checks a minute against a full queue). Stale positions (at most 64 per section, never re-queued once checked this session) go to a bounded queue (8192, oldest dropped) and up to 16 per tick run `World.checkLight` for entries within 128 blocks of the player once `isAreaLoaded(pos, 17)` is true; others rotate to the back; dropped after 2 minutes. Client thread only; clears when the client world changes. Config `Enable Client Relight Sweep` (default true) and `Client Relight Debug` (`[AQ-CLIENT-RELIGHT]` once a minute) in `aqtweaks_client.cfg` category `relight`. The server-side `DeferredRelight` ([rtg.md](rtg.md) item 28) is separate and unchanged.

## Files

| Piece | Role |
| --- | --- |
| `ArcanaQuestTweaksConfig.ClientModuleConfig` | `aqtweaks_client.cfg` |
| `client/ClientModule.java` | Tooltip handler; registered in `ClientProxy` |
| `mixin/atum/MixinAtumWeatherCommand.java`, `atum/AtumWeatherCommands.java`, `atum/AtumWeatherCommandHelper.java`, `atum/PendingStorm.java` | `/atumweather <sandstorm\|clear\|status> [seconds] [skipWarning]` runs through Tweaks so a forced storm warns first; the pure parsing and durations are unit tested; the forced duration lives on the provider (`PendingStorm`, implemented by `MixinWorldProviderAtum`) |
| `atum/AtumSandHandler.java`, `weather/LayerAccumulator.java`, `LayerRate`, `LayerShadow`, `LayerStack`, `LayerLeveling` | Server sand accumulation (shared with the Aurorian snow) |
| `mixin/atum/MixinWorldProviderAtum.java` | Storm intervals and durations, the forced storm duration, and switches off Atum's own weather sand loop while Tweaks accumulates sand |
| `atum/AtumStormGameplayHandler.java` | Server shelter result per player is cached for 10 ticks or until the player changes block (the client push stays per tick) |
| `client/atum/AtumFogHandler.java` | Overrides Atum fog with uniform linear fog and smooth cave transition |
| `client/atum/AtumOutdoorDustEmitter.java` | Spawns volumetric 3D drifting sand particles outdoors, dynamically scaling during sandstorms |
| `client/atum/ParticleAtumDustSheet.java` | Custom 3D billboard dust particle with soft depth blending |
| `atum/AtumPyramidPlate.java` | One pyramid plateau: apron Y, template origin lift, the 32×32 rotated footprint, entrance facing |
| `atum/AtumPyramidSmoothing.java` | Blends the chunk primer around a registered plate and clears the entrance walk (after the start exists, before the chunk is built) |
| `atum/BaublesGogglesHelper.java` | Baubles API access for the goggles protection toggle, isolated so Atum code loads without Baubles |
| `mixin/atum/MixinMapGenPyramidStart.java` | Moves the pyramid template origin so template Y=8 sits on the averaged footprint height; Atum still rejects a start whose own 5×5 sample is below Y 53 |
| `mixin/atum/MixinChunkGeneratorAtum.java`, `AccessorPyramidTemplate.java` | Smooth the primer once the start exists; read the entrance facing off Atum's template component |
| `mixin/atum/MixinMapGenRuin.java` | Makes the ruin-vs-pyramid overlap test independent of which pyramid starts are already stored (it looks up to 4 chunks away but only 8 are generated) |
| `mixin/atum/MixinAtumClientEvents.java` | Late mixin canceling Atum's yellow EXP fog and 2D screen-space `renderSand` storm overlay |
| `mixin/MixinGuiCreateWorld.java` | Sets default world type on Create New World |
| `mixin/toughnessbar/MixinEventHandlerClient.java` | Optional Toughness Bar overlay |

## Live config

| Knob | Default | Notes |
| --- | --- | --- |
| Move Toughness Bar To Armor Side | true | Armor column, LTR. Old `aqtweaks_stamina.cfg` HUD key is dead |
| Metallurgy Tooltip Compat | true | Non-Metallurgy mining tools only |
| Default World Type | "RTG" | Category "world creation". Registry name (e.g. RTG), not button label. Empty keeps Default |
| Disable Underground Fog | true | Category "Atum Fog". Completely cancels Atum's yellow EXP fog and 2D screen sandstorm in favor of linear fog and 3D particles |
| Enable Outdoor Dust Particles | true | Category "Atum Fog". Spawns volumetric 3D drifting sand particles in open outdoor air |
| Max Outdoor Dust Particles | 250 | Category "Atum Fog". Baseline cap on active simultaneous outdoor dust particles in clear weather |
| Max Sandstorm Dust Particles | 20000 | Category "Atum Fog". Cap during a sandstorm. Range 100..25000. A saved cfg value overrides this default |
| Enable Sandstorm Wind Sound | true | Category "Atum Fog". Continuous looping howling wind audio during Atum sandstorms |
| Sandstorm Wind Sound Event | "aqtweaks:sandstorm.wind" | Category "Atum Fog". Registry name of sound event for sandstorm gale audio |
| Sandstorm Wind Sound Volume | 1.0 | Category "Atum Fog". Volume multiplier (0.0 to 2.0) |
| Suppress Animal Sounds in Atum | true | Category "Atum Fog". Suppresses Dynamic Surroundings and AmbientSounds animal/bird sounds (birds, crickets, frogs, etc.) in Atum |
| Enable Storm Warning | true | Category "Atum Fog". Before a sandstorm the blowing sand gradually picks up (particles only; no fog, sound or gameplay change) |
| Storm Warning Seconds | 30 | Category "Atum Fog". How long before a sandstorm the warning starts to build (range 5..120) |
| Warning Dust Fraction | 0.6 | Category "Atum Fog". Fraction of full sandstorm intensity the dust reaches at the end of the warning; the storm ramps on from there |
| Suppress External Storm Wind in Atum | true | Category "Atum Fog". Suppresses external Dynamic Surroundings and AmbientSounds wind loops during Atum sandstorms |

## Atum visual overhaul & weather particles

Atum 2's default visuals have two major issues:
1. **Blinding Yellow Fog:** `ClientEvents.renderFog` forces OpenGL `EXP` fog (`0.08` density) with golden-yellow sky fog color, turning caves and pyramids into blinding yellow walls and snapping jarringly at cave thresholds.
2. **Indoor 2D Sandstorms:** Atum has no 3D weather particles; its sandstorm is a 2D HUD overlay in `ClientEvents.renderSand` rendering `sandstormoverlay.png` 6 times in screen space. Because its fade-out logic takes ~50 seconds upon entering shelter, players experience intense screen storms inside sealed rooms and deep underground.

### Tweaks Overhaul (`client/atum/` & `mixin/atum/`):
- **Late Mixin (`MixinAtumClientEvents`):** Injects at `HEAD` of `renderFog` and `renderSand(float, int...)` when `disableUndergroundFog` is enabled, canceling Atum's yellow EXP fog and 2D screen storm entirely.
- **Uniform Linear Fog (`AtumFogHandler`):** Enforces clean `FogMode.LINEAR`. The sky pass stays `0.0F` to `farPlane`. The terrain pass is `0.75 * farPlane` to `farPlane` in clear weather and under a roof (`canSeeSky` false at the camera), including during a storm. Outdoors in a sandstorm the terrain fog closes with storm strength: at full strength it starts at 0 and ends near `0.12 * farPlane`. Horizon colors are untouched. Atum's yellow exponential fog stays off.
- **Volumetric 3D Dust Particles (`AtumOutdoorDustEmitter` & `ParticleAtumDustSheet`):**
  - Particles render in true 3D space (`FX_LAYER_TRANSLUCENT`, depth blending enabled) with sandy golden tint `(0.92, 0.78, 0.44)`.
  - **Single Unified Wind Direction:** `AtumWindHelper` picks one heading from world time (a slow veer, plus a faster veer in a storm) when dynamic wind is on. Every particle uses that same vector.
  - **Dynamic Surroundings & Weather Suppression:** Late mixin `MixinDustJetEffect` suppresses DSurround's block dust motes in Atum, while `AtumOutdoorDustEmitter` installs an `IRenderHandler` weather suppressor to block DSurround's diagonal rain/dust precipitation, ensuring only one unified wind particle effect exists.
  - **Atmospheric Particle Mixture:** In a storm, 85% small flecks (0.6–1.1 blocks) and 15% moderate sheets (3.6–5.2 blocks). In clear weather, 65% flecks and 35% sheets. Sizes do not grow in a storm. Storm flecks fade up to about 0.95 alpha and sheets up to about 0.85, scaled by storm strength.
  - **Wind-aligned band:** Outdoor spawns sit on a rectangle around the player. In a storm the along-wind offset is −50 to +16 blocks (50 blocks of sand upwind) and the crosswind offset is ±35. In clear weather both axes are ±25. Indoor candidates stay in the look cone.
  - **Openings:** A spawn still requires open sky, so sand does not appear inside a room. Once spawned, a particle keeps moving under a roof and through a doorway until it hits a solid block or its life ends. Storm life starts at 33 ticks.
  - Clear weather keeps the base breeze untouched (`max(6, 32 * breezeFactor)` attempts, speed `0.16 + 0.16 * breezeFactor`) and the clear-weather cap.
  - During sandstorms (`/atumweather sandstorm`), wind speed is `2.05 + stormStrength * 1.05` blocks/tick (small flecks reach up to 3.56 blocks/tick). Spawn attempts are `1050 + stormStrength * 1550`. The cap is `Max Sandstorm Dust Particles` (default 20000).
- **Howling Wind Audio & Sound Filter (`AtumStormSound`, `AtumSounds`, `AtumSoundFilter`):**
  - Continuous streaming howling wind sound loop (`aqtweaks:sandstorm.wind` on `SoundCategory.WEATHER`).
  - Volume dynamically scales with `stormStrength` and smoothly fades in/out as storms build and subside.
  - Acoustic muffling: when indoors or deep underground (`canSeeSky` false), the volume is muffled to 25% with lowered pitch (0.85). Behind an upwind wall/barrier shelter, volume is reduced to 50% (pitch 0.92). Outdoors in the open desert, the full howling wind roars at 100% volume.
  - **Ambient Sound Filtering:** Client event handler `AtumSoundFilter` (`PlaySoundEvent`) suppresses Dynamic Surroundings and AmbientSounds animal/bird/insect audio (birds, owls, crickets, cicadas, frogs, etc.) in Atum at all times. During active sandstorms, it suppresses external wind and rain loops from DSurround and AmbientSounds so only AQTweaks' howling gale plays cleanly. In clear weather, external ambient wind loops continue playing normally.
  - **Exposure & Shelter** (`AtumStormGameplayHandler`, `aqtweaks_atum.cfg` and `aqtweaks_hazard_gear.cfg`): upwind barriers must block both feet and chest of the model to provide wind shadow shelter; 1-high blocks leaving the upper body exposed do not block the wind. Eye protection (`Eye Protection Gear Whitelist` in `aqtweaks_hazard_gear.cfg` `eye_protection`, checked across armor `HEAD` and Baubles `HEAD`) prevents stinging grit blindness (inhalation choking was removed, and any leftover `"AQTweaks_AtumAirDebt"` NBT tag is cleared). Missing chest, legs, or boots, or armor under 10, takes half a heart of abrasion every 4 seconds. A wanderer, mummy, or whitelisted resistant set on those slots stops the abrasion. A strong storm occasionally plays a low howl. Client wind push guards `player.isUser()` and scales by `WindResistanceGear.factor(player, 0.70)` (additive with weight, Wind Resistant gear, and grounded Anchoring boots). Shelter, headwind drain, and grit blindness are unchanged.
- **Sandstorm warning (`AtumStormWarning`, `AtumWarningRamp`, `AtumWeatherSync`):** over the `Storm Warning Seconds` before a storm the outdoor dust blends from the clear-weather values toward the storm values at `Warning Dust Fraction` of full strength (spawn attempts, wind speed, fleck alpha, lifetime, band size), eased, and the storm then ramps on from that level with no jump. The level comes from the client Atum provider's own `stormTime`. Atum counts that down on both sides but sends `PacketWeather` only when the state flips, never on join or dimension change, so a client that joined mid-countdown redrew a random value of its own. `AtumWeatherSync` (server, registered with the Atum handler) sends Atum's own `PacketWeather(isStorming, stormTime)` only to players in Atum: on login there, on entering it, on respawning there, and every `Warning Resync Seconds` (`aqtweaks_atum.cfg`, default 30, 0 = events only) while no storm is active; never while vanilla clean-weather time pins the countdown at 1 or 2 (the ramp also ignores those values). The wind heading stays the calm one during the warning; fog, sound and gameplay are untouched. **`/atumweather <sandstorm|clear|status> [seconds] [skipWarning]`** (OP level 2, replaces Atum's own command through `MixinAtumWeatherCommand`; `storm` stays an alias): `sandstorm` clears vanilla clean-weather time, then starts the configured `Storm Warning Seconds` countdown (the provider is set to clear with `stormTime` = the warning, the storm strength is reset and `PacketWeather` and `PacketStormStrength` go out at once so clients start the ramp), and when it runs out the storm starts with the duration given in `[seconds]` (carried on the provider so a singleplayer world change cannot leak it) or the usual looming duration; `skipWarning` (or `Enable Storm Warning` off) starts it at once. `clear [seconds]` ends it and sends the new state at once so a warning on clients stops. `status` prints the phase, strength and seconds left. The command refuses when Atum's own `SANDSTORM_ENABLED` is false.
- **Looming Sandstorm Frequency (`MixinWorldProviderAtum`, `aqtweaks_atum.cfg`):**
  - Overrides Atum 2's sparse hardcoded countdown ($12,000 + \text{rand}(168,000)$ ticks, up to 2.5 hours) with configurable, frequent storm intervals.
  - Injects into `WorldProviderAtum.updateWeather()` right before weather packet dispatch.
  - Configurable: `Enable Looming Sandstorms` (default `true`), `Storm Interval Min/Max Seconds` (default `840`–`2040`s clear, a full cycle of 18 to 40 minutes, one to two Minecraft days start to start), and `Storm Duration Min/Max Seconds` (default `240`–`360`s).

## Atum sandstorm sand accumulation

Server side, `atum/AtumSandHandler` (a `weather/LayerAccumulator`, the same build-up as the Aurorian snow, see [aurorian.md](aurorian.md)). While `stormStrength` is 0.25 or more, exposed ground in the loaded chunks within `Sand Accumulation Radius` (128) of any non-spectator player gains `atum:sand_layer` (Atum's Strange Sand layers): attempts per tick are chunks x `Sand Layers Per Chunk Per Second` (6) x strength squared / 20, capped at `Sand Max Changes Per Tick` (120). It replaces Atum's own weather sand, which walked every persistent chunk every tick even in clear weather (the `SANDSTORM_SAND_RARITY` guard comes first) and placed one layer per roll up to three layers and only above strength 0.9; `MixinWorldProviderAtum` redirects both reads of that setting to 0 while `Enable Sand Accumulation` is on.
* **Depth:** up to `Max Sand Layers` (default 6, 1 to 16; 8 layers per block, a full block is topped by a new layer-1 block). Atum's collision is `(layers - 2) x 0.125`: layers 1 and 2 have none, 6 layers is 0.5 (still walked over), 7 is 0.625 and 8 is 0.75 (a jump; mobs treat them as walls).
* **Smooth and shadowed:** a layer goes to the shallowest orthogonal neighbour when that one is `Sand Max Step` (2) or more layers shallower (up to 3 hops); the lee of walls, pyramids and dunes (upwind precipitation heights at 1 to 10 blocks against a flight line at `Sand Wind Shadow Angle`, 35 degrees, wind from `AtumWindHelper`) takes a layer about 35 percent of the time. Only air takes sand (`isAir`, never `isReplaceable`, which `BlockSandLayers` always answers true), so shrubs, deadwood, crops and torches are never replaced.
* **Where:** oasis biomes (`Skip Oasis Biome`) and `limestone_cracked` (pyramid trap floors) never gain sand; Strange Sand and limestone gravel do (`Accumulate On Strange Sand`, Atum's rule skipped them). Entities are not skipped (no burial hazard; a player embedded at up to 0.75 collision steps or jumps out). Layers are set with flag 2.
* **Permanent:** no erosion and no tracking. Nothing removes sand after a storm, so natural gradients and dunes outside storms are never touched; sand has no light melt (`BlockSandLayers` has no random tick) and drops nothing (`Items.AIR`), so it goes only by digging or by losing its support. Storms build on each other up to the cap.

## Atum pyramid plateau

On a new Atum chunk, `MapGenPyramid$Start.create` still picks the rotation and still bails when its own 5×5 sample is below Y 53. Tweaks then samples a 5×5 grid across the rotated 32×32 template footprint and sets the apron to that average (at least Y 55). The template origin is `apron - 8 + Pyramid Door Lift` (default lift 3), so template Y=8, the door, sits that many blocks above the apron and the stairs stand on the sand. The maze stays below the apron. The primer is blended after that start exists and before the chunk is built: inside the footprint, dunes above the apron become air so the raised base is not buried. Outside, lows fill with limestone and the surface is Atum sand with two limestone blocks under it. A 4-block apron stays flat; the next 16 blocks follow a Hermite slope into the natural dune. The entrance is the template's north face (the stair gap), rotated with the pyramid. A 6-wide walk extends 14 blocks out from that face, floor sloping from the raised door down to the sand, with the blocks above the floor cleared.

`aqtweaks_atum.cfg`: `Enable Pyramid Terrain Smoothing` (true), `Pyramid Apron Blocks` (4), `Pyramid Falloff Blocks` (16), `Enable Pyramid Entrance Corridor` (true), `Pyramid Entrance Corridor Length` (14), `Pyramid Door Lift` (3, range 0..8). Already pasted pyramids keep the door they were given. A start saved before this lift still gets a slope on newly generated neighbour chunks; the apron for that start is the template's minimum Y + 8 − lift. A start just measured from the dune sample keeps that sample.

## Default world type

Vanilla `GuiCreateWorld` initializes with `selectedIndex = 0` (`WorldType.DEFAULT`). In RTG 1.12.2 (v9.0.3), the legacy `B:rtgWorldTypeByDefault=true` setting in `config/rtg/rtg.cfg` is orphaned and not read by any RTG class.

Tweaks adds a client mixin `MixinGuiCreateWorld` on `GuiCreateWorld.<init>` at `RETURN`. When `Default World Type` is set (default `"RTG"`), it searches `WorldType.WORLD_TYPES` for a matching `WorldType` by name (`canBeCreated() == true`, case-insensitive) and sets `this.selectedIndex = i;`.

Because injection occurs at constructor `RETURN`:
- Subsequent `initGui()` / `updateDisplayState()` calls and screen resize events naturally construct `btnMapType` and `btnCustomizeType` with the correct labels and visibility.
- Recreate World (`recreateFromExistingWorld`) executes after the constructor and writes the source world's `WorldType.getId()`, properly preserving the copied world's type without being overwritten.
- Empty string `""` or an unknown name leaves `selectedIndex = 0` (vanilla Default).

## Config key reference (keys not named elsewhere in this file)

Exact `@Config.Name` strings and code defaults, so the doc and the cfg cannot drift apart. The meaning of each group is in the sections above; the note is the first sentence of the in-game comment.

| Key | Default | Section | Note |
| --- | --- | --- | --- |
| Enable Sand Accumulation | `true` | Atum sand accumulation | Server: exposed ground gains Atum sand layers during a sandstorm; replaces Atum's own weather sand. |
| Sand Layers Per Chunk Per Second | `6.0` | Atum sand accumulation | Build-up rate per loaded chunk at full strength (scales with strength squared). |
| Sand Max Changes Per Tick | `120` | Atum sand accumulation | Hard cap on sand block changes per tick for the whole dimension. |
| Sand Accumulation Radius | `128` | Atum sand accumulation | Loaded chunks within this many blocks of a player get sand (16 to 256). |
| Max Sand Layers | `6` | Atum sand accumulation | Deepest a column gets (1 to 16); 6 layers is still walked over. |
| Sand Max Step | `2` | Atum sand accumulation | Neighbouring columns differ by at most this many layers. |
| Sand Wind Shadow | `true` | Atum sand accumulation | Sand builds more slowly in the lee of obstacles. |
| Sand Wind Shadow Angle | `35.0` | Atum sand accumulation | Slant of the blowing sand in degrees (10 to 80). |
| Accumulate On Strange Sand | `true` | Atum sand accumulation | Also build on Strange Sand and limestone gravel. |
| Skip Oasis Biome | `true` | Atum sand accumulation | Oases stay clear. |
| Enable Dynamic Wind Direction | `true` | AtumFog | If true, desert wind direction dynamically veers over time and all active particles follow the shifting wind in unison. |
| Enable Sandstorm Gameplay Effects | `true` | AtumModuleConfig | If true, players exposed to the wind path of an Atum sandstorm experience physical and survival gameplay mechanics. |
| Storm Interval Min Seconds | `840` | AtumModuleConfig | Minimum clear weather time between sandstorms in seconds. |
| Storm Interval Max Seconds | `2040` | AtumModuleConfig | Maximum clear weather time between sandstorms in seconds. |
| Storm Duration Min Seconds | `240` | AtumModuleConfig | Minimum active sandstorm duration in seconds. |
| Storm Duration Max Seconds | `360` | AtumModuleConfig | Maximum active sandstorm duration in seconds. |
| Enable Wind Push | `true` | AtumModuleConfig | If true, unsheltered players in a sandstorm are physically pushed by the gale (tailwind boost, headwind resistance). |
| Wind Push Force | `0.020` | AtumModuleConfig | Base physical velocity force applied to unsheltered players in a sandstorm. |
| Enable Headwind Stamina Drain | `true` | AtumModuleConfig | If true, sprinting or fighting movement directly against the howling wind drains stamina (feathers) and increases exhaustion. |
| Enable Stinging Grit Blindness | `true` | AtumModuleConfig | If true, looking directly into the blowing sandstorm without protective headgear causes stinging sand blindness and slowness. |
| Grit Exposure Seconds Before Blindness | `3.0` | AtumModuleConfig | Seconds of continuous headwind eye exposure before stinging sand blindness triggers. |
| Shelter Wind Shadow Distance | `6.5` | AtumModuleConfig | Distance in blocks upwind to raytrace for solid obstacles (dune crests, walls, cliffs) that block the wind and grant shelter. |
| Enable Abrasive Sand Scour | `true` | AtumModuleConfig | If true, unsheltered entities in a sandstorm take periodic sand scour damage every 4 seconds. |
| Enable Desert Storm Ambience | `true` | AtumModuleConfig | If true, an unsheltered player in a strong sandstorm occasionally hears a low cave-wind howl. |
| Enable Baubles Goggles Protection | `true` | AtumModuleConfig | If true, eye-protection gear equipped in the Baubles head slot protects against sandstorm blindness. |
| Scour Damage Exposed | `6.0` | AtumModuleConfig | Damage dealt every 4 seconds when unsheltered and missing any of the 4 armor slots (HEAD, CHEST, LEGS, FEET). |
| Scour Damage Full Armor | `4.0` | AtumModuleConfig | Damage dealt every 4 seconds when unsheltered and wearing full standard armor across all 4 slots. |
| Scour Damage Atum Armor | `2.0` | AtumModuleConfig | Damage dealt every 4 seconds when unsheltered and wearing full Atum or whitelisted resistant armor across all 4 slots. |
| Enable Sandstorm Mob Effects | `true` | AtumModuleConfig | If true, unsheltered mobs in an Atum sandstorm experience physical wind push, slowness, and scour damage. |
| Enable Armor Abrasion Wear | `true` | AtumModuleConfig | If true, sandstorms slowly degrade durability on unsheltered non-Atum, non-whitelisted armor. |
| Abrasion Wear Interval Seconds | `10` | AtumModuleConfig | Seconds between durability wear ticks on unsheltered non-Atum armor. |
| Eye Protection Gear Whitelist | `(list)` | HazardGearConfig (`eye_protection`) | Moved to `aqtweaks_hazard_gear.cfg`: item registry names (wildcards, optional `:meta`) protecting eyes in armor HEAD or Baubles HEAD slots against sandstorm blindness. |
| Sandstorm Resistant Armor Whitelist | `(list, incl. thaumicaugmentation:thaumium_robes_*, etc.)` | HazardGearConfig (`atum`) | Moved to `aqtweaks_hazard_gear.cfg`: item registry names (wildcards, optional `:meta`) immune to sandstorm abrasion wear and qualifying for Tier 2 scour mitigation. |

## Do not regress

- Feathers and thirst stay on the right.
- Toughness must not share the armor **row** (keep `left_height + 10` GET).
- Do not import Metallurgy types. Do not add toughness mixin to the required json.
- Durability line must not appear on unbreakable tools (`maxDamage <= 0`).

## Verify

Armor + toughness: leftmost toughness icon over the leftmost heart/armor; extra pips go right (half pip on the right of a partial row). One row above armor. Feathers above thirst. Flag off: stock right RTL. Iron pick: Vanilla Tools + stars + durability + efficiency. Axe efficiency is log speed, not 1.0. Metallurgy pick: no second Tweaks harvest line. Remove toughnessbar jar: mixin skipped; tooltips still work. Create New World: World Type shows RTG by default; Recreate World preserves source world type.