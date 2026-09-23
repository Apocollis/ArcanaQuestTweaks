# Aether portal landing (1.8)

Last updated: 2026-09-23.

Optional bus handler if RandomPortals **and** The Aether (`aether_legacy`) are present. Shares `mixins.aqtweaks.randomportals.json` with [twilightforest.md](twilightforest.md). Tweaks cfg: `config/arcanaquesttweaks/aqtweaks_aether.cfg`.

Does **not** change Tweaks’ own rift items ([portal.md](portal.md)). Does **not** require Overworld `Material.GRASS`.

## Locked intent

RandomPortals destinations into The Aether (pack dim **4**) must generate on island ground (aether grass / dirt / holystone), not in the void. Walk down through air and skyroot canopy.

Do **not** cancel `SearchingForDestination`. Do **not** `@Mod required-after:aether_legacy`. Do **not** retarget already-linked return trips.

## How the parents work

Pack `config/randomportals/portal_types/aether_portal/0.json`: dest dim **4**, coordinate multiplier **1.0**, `generatePortalIfNotFound` true.

`RPOTeleporter.findTopLeft` last-resort clamps Y to ≥70 and builds a frame in air when no solid pad is in range. Vanilla `TeleporterAether.makePortal` does the same after only ±16 XZ. There is no public Aether `findSafeCoords`.

Island surface blocks (compile-hard `BlocksAether`): `aether_grass`, `enchanted_aether_grass`, `aether_dirt`, `holystone`, `mossy_holystone`.

## Design plan

`AetherPortalLandingHandler` on `MinecraftForge.EVENT_BUS` only when both mods are loaded. Keep Aether imports off `CommonProxy` and `ArcanaQuestTweaksConfig`.

On `SearchingForDestination` (server), cfg enabled, dest dim = cfg id (default 4), no receiving frame:

1. If the current column has island ground under canopy, snap Y to that surface + 1.
2. Else random search 200 then 400 (`range/8` samples).
3. Never cancel. If no island, leave the entity (RP may still float).

Mixin `MixinRPOTeleporter` when `dimensionID` is the Aether dest dim:

- `isValidPortalPosition` RETURN: every platform cell at `y-1` must be island ground. Vertical frames need one extra air cell above.
- `findTopLeft` RETURN: vertical dest frames shift **up 1** so the bottom row sits on the island, not in it. If the pad is not island ground, search generate-radius for an island pad. If none, keep the parent return.

TF grass rules do not run in dim 4. Nether unchanged.

## Files

| Piece | Role |
| --- | --- |
| `aether/AetherPortalLandingHandler.java` | Search-event island snap |
| `aether/AetherPortalIsland.java` | Surface / pad checks |
| `mixin/randomportals/MixinRPOTeleporter.java` | Shared with TF; Aether branch |
| `ArcanaQuestTweaksConfig.AetherModuleConfig` | `aqtweaks_aether.cfg` |

## Live config (`config/arcanaquesttweaks/aqtweaks_aether.cfg`)

| Category / Key | Type | Default | Description |
| --- | --- | --- | --- |
| `general` / Enable Aether Portal Safety | boolean | true | Master switch for handler + mixin island gate |
| `general` / Destination Dimension ID | int | 4 | Dim the hook applies to |

## Do not regress

- Do not cancel `SearchingForDestination`.
- Do not import Aether types from always-on bus classes or the `@Config` class.
- Do not `@Shadow` vanilla `Teleporter.world` on `RPOTeleporter`.
- Do not require TF grass in the Aether.
- Do not retarget already-linked receiving frames.
- Arcane Tunnel rifts stay [portal.md](portal.md).

## Verify

1. Build with `options.release = 21` via `build_gradle.ps1`.
2. New Overworld→Aether portal whose 1:1 column is open sky: dest on an island (grass/dirt/holystone), not floating at Y≥70.
3. Second trip through the same sending portal: same dest.
4. TF grass landings and Nether RP unchanged.
5. Strip Aether or RP: Tweaks still loads if the other absences already allowed; handler not registered. Mixin json still needs RP (and compile-hard Aether types if RP is present).
