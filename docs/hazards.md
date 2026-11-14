# Shared hazard timeline (`hazard/`)

One engine for every scheduled dimension hazard. The server decides each event, announces it once, and every client animates it locally from `World.getTotalWorldTime()`. Atum keeps its own storms (`AtumWeatherSync`); see [client.md](client.md) and [dimension-hazards.md](dimension-hazards.md).

## Concepts

* `HazardEvent` (pure): channel, id, `warningStart <= start < end` (absolute world ticks), `rampUp`, `rampDown`, `seed` (headings and visuals every client reproduces), `intensity`, `flags`. `phaseAt`, `warningAt` (eased 0..1 over the warning) and `strengthAt` (eased, separate ramps) are the only maths clients use.
* `HazardTiming` / `HazardChannel` / `HazardChannels`: a channel has an id, a name, a world test (provider class, never a dimension id), an enabled flag and timing ranges read from the owning module config each time an event is drawn. Register in `CommonProxy.init` only when the owning mod is loaded.
* `HazardTimeline` (pure, server): one event per dimension and channel, the upcoming one (warning included) or the active one. When it ends the next is drawn from its end tick; gap and duration come from `StormSchedule.clearLength / stormLength` of `(key, index)`. A config edit changes only the next draw. If an event ended more than 5 s before it is noticed (an absence), a fresh gap is drawn from now; missed events are not replayed.
* `HazardData`: per-dimension saved data `aqtweaks_hazards` (index, last end, current event per channel), so a restart continues the schedule.
* `Hazards`: `state(world, channel)` returns `HazardState` (event, phase, `warning`, `strength`, `storming`, `ticksToEnd`) on both sides; `trigger(world, channel, seconds, skipWarning)` and `clear(world, channel)` are for commands. `Hazards.Handler` (always registered) ticks the timelines on `WorldTickEvent`, broadcasts new events, and resends the current event on login, dimension change and respawn.
* `ClientHazards`: the client copy of the announced events, cleared on disconnect.

## Packet

`PacketHazardEvent` (id 9, server to client): dimension, channel, id, warningStart, start, end, rampUp, rampDown, seed, intensity, flags. A new event for a channel replaces the old one. A cleared storm is sent as the same event with an earlier end.

## Commands behave like natural events

`trigger` starts an event after the channel's normal warning lead (or at once with `skipWarning`), supersedes the current or upcoming event, and the schedule resumes from its end. `clear` ramps an active event down over its `rampDown`, or cancels an upcoming one and draws a new gap.

## Adding a channel

1. Make a `HazardChannel` (unique id; 0 is Moonfrost) with a provider-class test, an enabled flag and a timing supplier from your config.
2. Register it in `CommonProxy.init` under the mod `isModLoaded` check.
3. Read `Hazards.state(world, channel)` in server handlers and client renderers; do not keep your own timers.
4. Content inside the event (gust headings from `seed`, eruption points, strike points) stays with your module.

## Channels

| Id | Name | Dimension | Doc |
| --- | --- | --- | --- |
| 0 | moonfrost | The Aurorian | [aurorian.md](aurorian.md) |
| 1 | spout | The Nether | [nether.md](nether.md) |
| 2 | ash | The Nether | [nether.md](nether.md) |
| 3 | gust | The Aether | [aether.md](aether.md) |
| 4 | lightning | The Aether (its warning window is the storm warning; each bolt has its own glow) | [aether.md](aether.md) |
| 5 | rupture | The Twilight Forest | [twilightforest.md](twilightforest.md) |

Packet ids after the hazard event (9): 10 `PacketSpoutTelegraph`, 11 `PacketAshCloud`, 12 `PacketAetherBoltTelegraph`, 13 `PacketLeyExposure` (exposure and, when changed, the nearby sheltered structure boxes), 14 `PacketSplashTelegraph`. The next free channel id is 6 and the next free packet id is 15.

Tests: `HazardEventTest`, `HazardTimelineTest`, `WindResistanceGearTest`.

## Shared hazard gear & wind resistance (`aqtweaks_hazard_gear.cfg`)

All dimensional hazard gear whitelists and effect IDs are centralized in `config/arcanaquesttweaks/aqtweaks_hazard_gear.cfg` (`HazardGearConfig`) across seven categories: `wind`, `eye_protection`, `aether`, `atum`, `aurorian`, `nether`, and `twilightforest`. Patterns use glob wildcards (`weather/Wildcard.java`) with optional `:meta` suffixes (checked via `weather/GearTierRule.java`).

* **Startup pattern audit (`weather/HazardGearAudit.java`):** runs on `FMLLoadCompleteEvent` against `ForgeRegistries.ITEMS.getKeys()` and logs `[hazard-gear] Unmatched pattern in <list>: <pattern>` (plus any invalid configured potion IDs) so stale or misspelled entries are surfaced immediately on boot.
* **Shared wind-push resistance (`weather/WindResistanceGear.java`):** scales local client wind push additively across Aether Silver Squalls, Atum sandstorms, and Aurorian Moonfrost storms using live Elenai Dodge 2 iron-feather weight (before Armor Mastery, via `StaminaFeathers.getWeightBeforeMastery`, cached 10 ticks per player so weight-reducing enchantments still reduce wind resistance while Armor Mastery does not), **Wind Resistant** gear (proportional across the 4 armor slots, active both grounded and mid-air), **Anchoring Boots** (`FEET` slot, active only while `onGround`), and sneaking reduction (70% in Atum/Aurorian, `1.0 - Sneak Anchor Factor` = 75% in Aether), all **added together** and clamped to `[0.0, 1.0]` so combinations can reach 100% wind immunity. Config reloading invalidates the cache.

| Category / Key | Default | Note |
| --- | --- | --- |
| `wind` / Enable Wind Gear | `true` | Master switch for weight, Wind Resistant gear, and Anchoring boots wind-push reduction. |
| `wind` / Enable Weight Push Resistance | `true` | Scale wind-push resistance dynamically from Elenai Dodge 2 iron-feather weight before Armor Mastery. |
| `wind` / Weight Reduction Threshold | `0.0` | Weight (half-feathers) below which weight grants no wind resistance. |
| `wind` / Weight Full Reduction At | `20.0` | Weight (half-feathers) at which weight reaches Max Weight Push Reduction (20.0 = 10 iron feathers). |
| `wind` / Max Weight Push Reduction | `0.50` | Maximum wind-push reduction from armor weight. |
| `wind` / Wind Resistant Gear Whitelist | `(list)` | Aerodynamic armor across the 4 armor slots active both grounded and mid-air. |
| `wind` / Wind Resistant Full Set Push Reduction | `0.40` | Full 4-piece Wind Resistant reduction (1/4 per piece). |
| `wind` / Anchoring Boots Whitelist | `(list)` | Footwear in the FEET slot that resists wind push only while standing on solid ground. |
| `wind` / Anchoring Grounded Push Reduction | `0.25` | Extra wind-push reduction granted by Anchoring Boots while `onGround`. |
| `eye_protection` / Eye Protection Gear Whitelist | `(list)` | Goggles, eyewear, and sealed helmets checked across armor HEAD and Baubles HEAD slots against grit blindness. |

