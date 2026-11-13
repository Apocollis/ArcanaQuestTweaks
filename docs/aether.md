# Aether portal landing (1.9)

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

## Silver Squalls (crosswind gusts and telegraphed lightning)

Last updated: 2026-11-09. The Aether (`aether_legacy`) provider is `AetherWorldProvider` (compile-hard, jar in `libs/`); the handlers register only when the mod is loaded (`CommonProxy.init`). Tweaks cfg: `config/arcanaquesttweaks/aqtweaks_aether.cfg`, category **Silver Squalls** (`AetherModuleConfig.squall`; the portal landing keeps its own category). Scheduling and the event packet: [hazards.md](hazards.md) (channels `gust` id 3 and `lightning` id 4). Overview: [dimension-hazards.md](dimension-hazards.md).

* **Gusts:** 2 to 5 s (`Gust Duration Min/Max Seconds`) every 45 to 120 s (`Gust Interval Min/Max Seconds`), with a 5 s warning (`Warning Seconds`), a 6 tick ramp up and a 10 tick ramp down (`Ramp Up/Down Ticks`) so the ramps fit a 2 s gust. The heading comes from the event seed: a new heading every `Crosswind Shift Interval Ticks` (50) inside a gust, blended over 10 ticks (`AetherWindHelper.headingAt`, pure, tested), so the server and every client agree with no heading packet.
* **Push (`client/aether/AetherSquallClient`):** the local client adds `Crosswind Push Force * strength * gear factor` to the player's horizontal motion, like the Atum sandstorm, so it also moves gliding, flying and parachuting players with no special case (the existing glider, parachute and grappling mechanics run as they are). Sneaking anchors to `Sneak Anchor Factor` (25%); a horizontal speed cap `Max Gust Speed` stops compounding. Shelter is the shared `StormShelter.isSheltered` (no sky, or an upwind wall blocking feet and chest). Gear factor (`AetherSquallGear`): a full `Insulated Gear Whitelist` set 0.6, conductive metal 0.75, otherwise 1. The server adds the sprint-into-the-wind cost (`Enable Headwind Stamina Drain`: exhaustion and a feather).
* **Warning:** cloud wisps stream along the coming heading and the wind loop (`aqtweaks:sandstorm.wind`, higher pitch) rises through the warning; no chat line.
* **Lightning (own timer):** a strike every 40 to 120 s (`Lightning Interval Min/Max Seconds`); the channel warning window is the telegraph (`Lightning Telegraph Ticks`, 35). When it opens, `AetherSquallHandler` picks one survival player who can see the sky, weighted toward conductive gear (`Conductive Gear List`; an insulated full set avoids lock-on and gets a bolt 6 to 14 blocks away on nearby high ground, a conductive player is hit within a couple of blocks), locks a point and sends `PacketAetherBoltTelegraph` (id 12) to players within 64 blocks. Clients draw a spark ring and rising column with a charge sound. When the event starts the bolt lands as a non-destructive visual (`EntityLightningBolt` with `effectOnly`), and every living entity within 2.5 blocks that can see the sky takes `Lightning Strike Damage` (6.0, `Lightning Insulated Gear Damage` 2.0 with a full insulated set; `aqtweaks_aether_lightning`, armor-bypassing magic damage) and Slowness II for 2 s. Leaving the circle during the telegraph means no damage.
* **Test command:** `/aethersquall <gust|lightning|clear|status> [seconds] [skipWarning]` (OP, alias `/aetherwind`): `gust` and `lightning` start an event through the hazard engine (they warn like natural ones unless `skipWarning`), `clear` ends both and cancels a pending strike.
* Files: `aether/AetherChannels.java`, `AetherSquallHandler.java`, `AetherSquallGear.java`, `AetherWindHelper.java`, `ClientBolts.java`, `CommandAetherSquall.java`; `client/aether/AetherSquallClient.java`, `AetherBoltEffects.java`; `network/PacketAetherBoltTelegraph.java`; tests `AetherWindHelperTest`, `AetherSquallGearTest`.
* Still to decide in play: whether Gravitite (slow fall) should stay conductive, whether Obsidian's `solid_core` should reduce the push, and whether Aercloud blocks should count as a roof for the sky test.

### Silver Squalls config key reference

| Key | Default | Note |
| --- | --- | --- |
| Enable Silver Squalls | `true` | Timed gusts of crosswind in the Aether that push unsheltered players. |
| Gust Interval Min Seconds | `45` |  |
| Gust Interval Max Seconds | `120` |  |
| Gust Duration Min Seconds | `2` |  |
| Gust Duration Max Seconds | `5` |  |
| Ramp Up Ticks | `6` |  |
| Ramp Down Ticks | `10` |  |
| Warning Seconds | `5` |  |
| Crosswind Shift Interval Ticks | `50` |  |
| Crosswind Push Force | `0.025` |  |
| Max Gust Speed | `0.9` | Horizontal speed cap in blocks per tick while pushing. |
| Sneak Anchor Factor | `0.25` |  |
| Enable Headwind Stamina Drain | `true` |  |
| Shelter Wind Shadow Distance | `6.5` |  |
| Enable Electrostatic Lightning | `true` |  |
| Lightning Interval Min Seconds | `40` |  |
| Lightning Interval Max Seconds | `120` |  |
| Lightning Telegraph Ticks | `35` |  |
| Lightning Strike Damage | `6.0` |  |
| Lightning Insulated Gear Damage | `2.0` |  |
| Insulated Gear Whitelist | `(list)` | Item registry names (wildcards supported) worn in all four armor slots: lower wind push, no lightning lock-on, reduced shock damage. |
| Conductive Gear List | `(list)` | Item registry names (wildcards supported) worn or held that attract lightning. |
| Enable Squall Particles | `true` | Client: cloud wisps along the gust and the lightning telegraph effects. |
| Enable Squall Sound | `true` | Client: rising wind before and during a gust, and the telegraph hum. |
