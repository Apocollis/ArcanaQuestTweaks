# Client module (1.9)

Last updated: 2026-10-19.

Config: `config/arcanaquesttweaks/aqtweaks_client.cfg`. Client-only. No `@Mod` parent. Optional mixin if Toughness Bar is present.

## Locked intent

HUD and tooltip presentation that is not stamina spend. Do **not** change Elenai feather or thirst icons. Do **not** mixin Metallurgy. Skip `metallurgy:` items so Reforged’s own shift stats are not doubled.

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
| `client/atum/AtumFogHandler.java` | Overrides Atum fog with uniform linear fog and smooth cave transition |
| `client/atum/AtumOutdoorDustEmitter.java` | Spawns volumetric 3D drifting sand particles outdoors, dynamically scaling during sandstorms |
| `client/atum/ParticleAtumDustSheet.java` | Custom 3D billboard dust particle with soft depth blending |
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
| Max Outdoor Dust Particles | 200 | Category "Atum Fog". Baseline cap on active simultaneous outdoor dust particles in clear weather |
| Max Sandstorm Dust Particles | 10000 | Category "Atum Fog". Cap during a sandstorm. Range 100..25000. A saved cfg value overrides this default |

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
  - **Openings:** A spawn still requires open sky, so sand does not appear inside a room. Once spawned, a particle keeps moving under a roof and through a doorway until it hits a solid block or its life ends. Storm life starts at 38 ticks.
  - Clear weather keeps the breeze (`max(6, 32 * breezeFactor)` attempts, speed `0.16 + 0.16 * breezeFactor`) and the clear-weather cap.
  - During sandstorms (`/atumweather sandstorm`), wind speed is `1.40 + stormStrength * 0.70` blocks/tick. Spawn attempts are `600 + stormStrength * 900`. The cap is `Max Sandstorm Dust Particles` (default 10000).
  - **Exposure** (`AtumStormGameplayHandler`, `aqtweaks_atum.cfg`): an unsheltered player with no protective headgear loses breath (about 4 air per tick, choking damage once the bar is empty). Missing chest, legs, or boots, or armor under 10, takes half a heart of abrasion every 4 seconds. A wanderer or mummy set on those three slots stops the abrasion. A strong storm occasionally plays a low howl. Shelter and the existing wind push, headwind drain, and grit blindness are unchanged.

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

## Do not regress

- Feathers and thirst stay on the right.
- Toughness must not share the armor **row** (keep `left_height + 10` GET).
- Do not import Metallurgy types. Do not add toughness mixin to the required json.
- Durability line must not appear on unbreakable tools (`maxDamage <= 0`).

## Verify

Armor + toughness: leftmost toughness icon over the leftmost heart/armor; extra pips go right (half pip on the right of a partial row). One row above armor. Feathers above thirst. Flag off: stock right RTL. Iron pick: Vanilla Tools + stars + durability + efficiency. Axe efficiency is log speed, not 1.0. Metallurgy pick: no second Tweaks harvest line. Remove toughnessbar jar: mixin skipped; tooltips still work. Create New World: World Type shows RTG by default; Recreate World preserves source world type.