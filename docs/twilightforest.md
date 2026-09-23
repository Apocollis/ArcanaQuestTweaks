# Twilight Forest portal landing (1.8)

Last updated: 2026-09-16.

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

- `isValidPortalPosition` RETURN: parent true still fails unless every platform cell at `y-1` is `Material.GRASS`.
- `findTopLeft` RETURN: if the returned pad is not grass, search `RPOConfig.NetherPortals.portalGenerationLocationSearchRadius` for a grass pad that passes `isValidPortalPosition`, convert to RP `topLeft`. If none, keep the parent return.

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
2. First Overworld→TF portal whose 1:1 column is a listed unsafe biome: dest in a safe biome, **on grass**, not in a landmark, **not on tall trees**.
3. Second trip through the same sending portal: same dest.
4. Nether RandomPortals travel unchanged.
5. Strip RandomPortals or TF: Tweaks still loads; mixin json skipped / handler not registered.
