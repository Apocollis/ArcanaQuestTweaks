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
| 4 | lightning | The Aether (its warning window is the strike telegraph) | [aether.md](aether.md) |
| 5 | rupture | The Twilight Forest | [twilightforest.md](twilightforest.md) |

Packet ids after the hazard event (9): 10 `PacketSpoutTelegraph`, 11 `PacketAshCloud`, 12 `PacketAetherBoltTelegraph`, 13 `PacketLeyExposure`. The next free channel id is 6 and the next free packet id is 14.

Tests: `HazardEventTest`, `HazardTimelineTest`.
