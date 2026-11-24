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

Last updated: 2026-11-14. The Aether (`aether_legacy`) provider is `AetherWorldProvider` (compile-hard, jar in `libs/`); the handlers register only when the mod is loaded (`CommonProxy.init`). Tweaks cfg: `config/arcanaquesttweaks/aqtweaks_aether.cfg`, category **Silver Squalls** (`AetherModuleConfig.squall`; the portal landing keeps its own category). Scheduling and the event packet: [hazards.md](hazards.md) (channels `gust` id 3 and `lightning` id 4). Overview: [dimension-hazards.md](dimension-hazards.md).

* **Gusts:** 2 to 5 s (`Gust Duration Min/Max Seconds`) every 45 to 120 s (`Gust Interval Min/Max Seconds`), with a 5 s warning (`Warning Seconds`), a 6 tick ramp up and a 10 tick ramp down (`Ramp Up/Down Ticks`) so the ramps fit a 2 s gust. The heading comes from the event seed: by default (`Crosswind Shift Interval Ticks` 0) **one fixed heading per gust**, from the start of the warning to the end of the squall, so the direction the warning blows is the direction the gust hits; with a positive value a new heading every that many ticks, blended over 10 ticks (`AetherWindHelper.headingAt`, pure, tested). The server and every client agree with no heading packet.
* **Push (`client/aether/AetherSquallClient`):** the local client adds `Crosswind Push Force * strength * WindResistanceGear.factor(player, 1.0 - Sneak Anchor Factor)` (0.05) to the player's horizontal motion, like the Atum sandstorm, so it also moves gliding, flying and parachuting players with no special case (the existing glider, parachute and grappling mechanics run as they are). Sneaking anchors by `1.0 - Sneak Anchor Factor` (75% reduction), added linearly with `aqtweaks_hazard_gear.cfg` `[wind]` reductions (live iron-feather weight, Wind Resistant gear, and grounded Anchoring boots, clamped to `[0.0, 1.0]` up to 100% immunity); a horizontal speed cap `Max Gust Speed` stops compounding. Shelter is the shared `StormShelter.isSheltered` (no sky, or an upwind wall blocking feet and chest). Conductive/insulated/channeling lightning classes do not directly alter push (any push mitigation comes from weight and `[wind]` gear). The cloud wisps move at `(0.5 + 0.9 * warning scale) * (1 + strength)` blocks a tick, so the active squall doubles the warning speed at full strength. The server adds the sprint-into-the-wind cost (`Enable Headwind Stamina Drain`: exhaustion and a feather).
* **Warning:** cloud wisps stream along the coming heading and the wind loop (`aqtweaks:sandstorm.wind`, higher pitch) rises through the warning; no chat line.
* **Lightning (own timer):** a lightning **storm** every 90 to 180 s (`Lightning Interval Min/Max Seconds`) lasting 18 to 24 s (`Lightning Duration Min/Max Seconds`), after a **storm warning** of `Lightning Warning Seconds` (10) that is the channel's warning window. During the warning (`client/aether/AetherStormWarning`, only for a player who can see the sky) distant thunder builds (every ~4 s down to ~1.5 s), static sparks lift off the player (x2 for conductive gear, x3 for a channeling set), a faint crackle begins, and one action-bar line "The air crackles with static..." (`Lightning Warning Message`) is shown. The storm is 5 to 6 strike **salvos** (`Lightning Salvos Min/Max`) spread over the storm by `AetherLightningSchedule` (the first at the end of the warning, the last at least 60 ticks before the end, so a 24 s storm is about 4 s apart and an 18 s storm about 3 s). A salvo (`AetherSquallHandler`) picks a primary target weighted by gear, then locks every exposed survival player within 32 blocks of them, conductive and channeling players first. Gear classes (`AetherSquallGear.classify`, read from `aqtweaks_hazard_gear.cfg` category `aether`, in this order): **channeling** full set (`Channeling Gear Whitelist`): locked like conductive gear but takes no damage, no Slowness, and no Stun from any bolt; **insulated** full set (`Insulated Gear Whitelist`): not locked, the strike lands 3 to 8 blocks away, reduced shock damage; **conductive** piece worn or held (`Conductive Gear List`, excluding items matching Insulated or Channeling): locked first, weight 4 for the primary pick, and retargeted by `Lightning Conductive Extra Bolts` (1) extra full-damage bolts per salvo that re-read the player's position when their glow opens; everyone else: locked at their exact spot. Every bolt glows for `Lightning Telegraph Ticks` (35) on its marked block (`PacketAetherBoltTelegraph`, id 12) with a spark ring and a rising column, then lands as a non-destructive visual (`EntityLightningBolt` with `effectOnly`). Each primary spot is followed 1 to 3 s later by `Lightning Cluster Bolts Min/Max` (5 to 6) secondary bolts 2 to `Lightning Cluster Radius` (7) blocks around it. Every living entity within 2.5 blocks that can see the sky takes `Lightning Strike Damage` (6.0, `Lightning Insulated Gear Damage` 2.0 with a full insulated set; `aqtweaks_aether_lightning`, armor-bypassing magic damage), Slowness II for 2 s, and `Lightning Stun Ticks` (`20` ticks) of the shared `Stun` effect ([thaumcraft-additions.md](thaumcraft-additions.md)) on primary bolts; secondary bolts do `Lightning Secondary Damage Factor` (0.5) of that with no Slowness or Stun. Leaving the circle during a glow means no damage. About 36 to 42 bolts per player over 18 to 24 s, roughly two a second.
* **Test command:** `/aethersquall <gust|lightning|clear|status> [seconds] [skipWarning]` (OP, alias `/aetherwind`): `gust` and `lightning` start an event through the hazard engine (they warn like natural ones unless `skipWarning`), `clear` ends both and cancels a pending strike.
* Files: `aether/AetherChannels.java`, `AetherSquallHandler.java`, `AetherSquallGear.java`, `AetherLightningSchedule.java`, `StormWarningMath.java`, `AetherWindHelper.java`, `ClientBolts.java`, `CommandAetherSquall.java`; `client/aether/AetherSquallClient.java`, `AetherBoltEffects.java`, `AetherStormWarning.java`; `network/PacketAetherBoltTelegraph.java`; tests `AetherWindHelperTest`, `AetherSquallGearTest`, `AetherLightningScheduleTest`, `StormWarningMathTest`.
* Still to decide in play: whether Gravitite (slow fall) should stay conductive, and whether Aercloud blocks should count as a roof for the sky test.

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
| Crosswind Shift Interval Ticks | `0` | Ticks between heading changes during a gust; 0 keeps one fixed heading. |
| Crosswind Push Force | `0.05` |  |
| Max Gust Speed | `0.9` | Horizontal speed cap in blocks per tick while pushing. |
| Sneak Anchor Factor | `0.25` |  |
| Enable Headwind Stamina Drain | `true` |  |
| Shelter Wind Shadow Distance | `6.5` |  |
| Enable Electrostatic Lightning | `true` |  |
| Lightning Interval Min Seconds | `90` |  |
| Lightning Interval Max Seconds | `180` |  |
| Lightning Telegraph Ticks | `35` | Glow length of one bolt. |
| Lightning Warning Seconds | `10` | Warning before a lightning storm: building thunder and static on exposed players. |
| Lightning Warning Message | `true` | Client: one action-bar line when the warning starts. |
| Lightning Duration Min Seconds | `18` |  |
| Lightning Duration Max Seconds | `24` |  |
| Lightning Salvos Min | `5` | Strike salvos across a storm. |
| Lightning Salvos Max | `6` |  |
| Lightning Cluster Bolts Min | `5` | Secondary bolts around each marked player spot, per salvo. |
| Lightning Cluster Bolts Max | `6` |  |
| Lightning Cluster Radius | `7` | Secondary bolts land 2 to this many blocks from the marked spot. |
| Lightning Secondary Damage Factor | `0.5` | Secondary bolts deal this fraction of the damage and apply no slowness or stun. |
| Lightning Stun Ticks | `20` | A primary bolt also applies the shared Stun effect ([thaumcraft-additions.md](thaumcraft-additions.md): no attacks or item use, about Slowness I speed) for this long, on top of the Slowness II. Same exemptions as the damage (creative, sheltered, channeling gear). 0 turns it off. |
| Lightning Conductive Extra Bolts | `1` | Extra full-damage bolts per salvo that re-aim at each player in conductive or channeling gear. |
| Lightning Strike Damage | `6.0` |  |
| Lightning Insulated Gear Damage | `2.0` |  |
| Insulated Gear Whitelist | `(list)` | Moved to `aqtweaks_hazard_gear.cfg` (`aether`): item registry names (wildcards, optional `:meta`) worn in all four armor slots: no lightning lock-on (strikes land 3 to 8 blocks away), reduced shock damage. |
| Conductive Gear List | `(list)` | Moved to `aqtweaks_hazard_gear.cfg` (`aether`): item registry names (wildcards, optional `:meta`) worn or held that attract lightning (Insulated and Channeling items automatically excluded). |
| Channeling Gear Whitelist | `(list)` | Moved to `aqtweaks_hazard_gear.cfg` (`aether`): item registry names (wildcards, optional `:meta`) worn in all four slots that channel lightning (locked onto, no damage, no slowness). |
| Enable Squall Particles | `true` | Client: cloud wisps along the gust and the lightning telegraph effects. |
| Enable Squall Sound | `true` | Client: rising wind before and during a gust, and the telegraph hum. |
