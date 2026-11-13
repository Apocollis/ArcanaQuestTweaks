# Aurorian: fixed moonlit lighting and the Moonfrost storm

Last updated: 2026-11-08. Parent: **The Aurorian** (`theaurorian` 1.2, dimension provider `AurorianWorldProvider`). Tweaks cfg: `config/arcanaquesttweaks/aqtweaks_aurorian.cfg`. The jar is placed in `libs/` by hand (like Atum) and compiled against; the mixin json is queued only when `theaurorian` is loaded.

Two features, both for the same dimension. Overview of all dimension hazards: [dimension-hazards.md](dimension-hazards.md).

## Fixed moonlit lighting

**Problem.** The Aurorian has no day or night cycle, but its provider drifts the celestial angle (`0.5 + 0.1 * sin(time * 0.0005)`) and cycles the moon phase every 3200 ticks, both from world time. The Darkness mod (`darkDefault=true`) scales the lightmap by the moon phase and angle, so the screen brightness swung with time, and `/time set` shifted it.

**Fix.**
* `MixinAurorianWorldProvider` (late json `mixins.aqtweaks.aurorian.json`, `required: false`, gated on `theaurorian`) injects at HEAD of `getMoonPhase` (`func_76559_b`) and `calculateCelestialAngle` (`func_76563_a`) and returns the configured constants. This is the look: a permanent full-moon night (`Moon Phase` 0, `Celestial Angle` 0.5).
* `MixinWorldFixedSkylight` (early `mixins.aqtweaks.early.json`, no Aurorian import: the provider is recognised by class name, cached per class) injects at HEAD of `World.calculateSkylightSubtracted` and returns `15 - Sky Light Level`. This is the gameplay: open sky light 9, so crops and saplings (need 9) grow and outdoor hostile spawning stops (threshold 7). Under canopy the raw sky light is lower, so shade stays dark.
* The lightmap is built from the sun brightness (celestial angle), not from the subtraction, so the two are independent: the look stays at the moon-phase value while the gameplay light is 9.
* A permanent full moon maxes regional difficulty (`getCurrentMoonPhaseFactor` 1.0). Change `Moon Phase` to darken the Darkness look and lower the difficulty.

## Moonfrost storm

A scheduled cold storm, the cold twin of the Atum sandstorm ([client.md](client.md)). The baseline cold of the dimension is Simple Difficulty and is not touched.

**Schedule.** The Moonfrost storm is channel 0 of the shared hazard engine ([hazards.md](hazards.md)). The server draws each storm (clear gap, then storm, hashed lengths in the configured ranges; Atum's defaults: clear 360 to 1200 s, storm 240 to 420 s) and announces it once with `PacketHazardEvent`; both sides then read `strength` (0..1, eased over `Ramp Ticks`) and `warning` (0..1 over `Warning Ticks` before the storm) from `World.getTotalWorldTime()`, so `/time set` cannot move or skip a storm and there is no per-tick storm packet. `MoonfrostState.get(world)` returns the `HazardState` both sides use.

**Hazards** (`MoonfrostHandler`, a `LivingUpdate` handler registered when `theaurorian` is loaded). Active at `strength >= 0.15` for survival players who are not sheltered. The server work runs once a second per player.
* **Frostbite:** armor-bypassing `aurorian_frost` damage every 4 s, 6.0 / 4.0 / 2.0 by gear tier (Atum's numbers); doubled at the top rime stack.
* **Rime (freezing in place), non-deadlocking:** each second the horizontal distance moved is compared to the player's current movement speed attribute (`Rime Move Threshold Factor`, relative to speed, so Slowness cannot make thawing impossible); standing still adds rime by tier, moving removes 2, shelter removes 2, a warmth source 3. Stacks (cap 15) give Slowness I at 5, Slowness III and Mining Fatigue I at 10, Slowness V and Mining Fatigue III at 15 as short owned effects, so it never fully immobilizes. `RimeTracker` is pure and unit tested. The server sends the stack to the client (`PacketMoonfrostState`) for the frost overlay.
* **Wind:** the local client adds the wind push to its motion, the same as Atum's, so it also moves gliding and creative-flying players (no creative exemption); sneaking anchors at 30%; sprinting into the wind costs exhaustion and, now and then, a feather (`FeathersHelper.decreaseFeathers`). `AurorianWindHelper` veers the heading from `totalWorldTime`.
* **Stamina regeneration slowed while exposed:** Elenai counts regeneration on the client and resets its timer to `regenSpeed + regenModifier` per feather, so a positive modifier lengthens the interval. The handler raises the modifier by `base * (multiplier - 1)` with `FeathersHelper.increaseRegenModifier` (multiplier 20 exposed, 2 armored, 1.25 proper; base from Elenai's `feathers.rate`, fallback 30) and keeps the applied delta in entity data `AQTMoonfrostRegenDelta`, removed on shelter, storm end, leaving the dimension, logout and death, like Comfort's Vigor delta. Max stamina is never touched (never `syncMaxFeathers`; see [stamina.md](stamina.md)). `MixinCUpdateRegenHandler` (client, `mixins.aqtweaks.elenaidodge.json`) clamps the running timer to `regenSpeed + newModifier` whenever a modifier arrives, so regeneration resumes as soon as the slowdown ends instead of after the stalled interval.

**Gear tiers (`MoonfrostGear`, `MoonfrostTier`; Atum's whole-set rule):** EXPOSED (any armor slot empty), ARMORED (four ordinary pieces), PROPER. PROPER means the player has `Proper Gear Effect` active (`simpledifficulty:cold_resist`, looked up by registry name; the pack's SetBonus config gives it to full Aurorian, Arctic and Yeti sets, and a Cold Resistance potion also gives it) with four armor pieces worn, or all four slots are on `Proper Gear Whitelist` (the Thaumcraft and Bewitchment magic armors). Atum armor gives heat resist, so it is ordinary armor here.

**Shelter (`weather/StormShelter`, shared with Atum, which delegates to it):** the Atum rule: no open sky above the eyes, or an upwind block that blocks both the feet and the chest ray; also a Comfort room (Rested active) or a lit Simple Difficulty campfire within 4 blocks (`SimpleDifficultyHelper.isBurningCampfire`). For the Aurorian the sky test is `StormShelter.openSky`, which ignores leaves (every block above that is air, leaves or non-opaque still counts as open sky), so a tree canopy is not a roof; Atum keeps `World.canSeeSky`. The result is held by `ShelterHold` for `Shelter Change Delay Seconds` (2) before it changes, so a branch or doorway does not flip the effects (`MoonfrostHandler.isShelteredHeld`). `weather/Wildcard` is the registry-name wildcard matcher.

**Client (registered when `theaurorian` is loaded):**
* **No fog.** The Moonfrost storm applies no fog at any point, so stepping under cover never jumps between fog and no fog. Visibility is limited by snow particles: a dense veil on a perimeter around the player (below) and the wind-aligned band. The Nether ash fog call in `MixinEntityRendererFog` is unchanged.
* `AurorianSnowEmitter`, `ParticleMoonSnow`, `ParticleMoonClump` (client, pure shape in `MoonSnowShape`): blizzard snow that falls **diagonally** at `Snow Fall Angle` (58 degrees below horizontal, jittered about 15 percent per flake and clamped to 4..82 degrees) leaning with the shared wind. Horizontal speed is 0.7 blocks per tick at the start of the warning to about 1.6 at full strength (times `Snow Speed Multiplier`), so vertical speed reaches roughly 1.1 to 2.6 blocks per tick; a slow gust pulse multiplies both by 0.85 to 1.25. Flakes live 20 to 28 ticks (puffs 26 to 36) and the spawn volume is raised by half the fall distance and spans about -5 to +40 blocks of height, so the air around the player stays filled.
  * **Seven classes** with their own vanilla `particles.png` sprites (index = row x 16 + column): specks (113), small checkered flakes (160 to 163), medium diamond and cross stars (164 to 167), solid pellets (176 to 180), soft puffs (speckled 5 to 7, low alpha, the only class drawn with the depth test off), heavy clumps (a `ParticleDigging` subclass with the snow block texture: chunky, lit by the world light, spawned 3 to 8 blocks from the player and moved by our wind instead of gravity) and streaks (sprite 114, rolled to the screen direction of motion, `Snow Streaks`). Default weights 25 / 25 / 15 / 10 / 15 / 7 / 3, shifting toward puffs and clumps as the storm strengthens. Powder puffs (`SNOW_SHOVEL`) rise where the snow lands near the player.
  * **Perimeter veil:** a pass run before the band, on a sphere around the eye (flattened vertically) whose radius is drawn from a triangular distribution peaking at `Visibility Perimeter Radius` (10; between `max(3, 0.7 x radius)` and radius + 8), so there is a dense ring at about 10 blocks and nothing spawns within 3 blocks of the camera. Mostly puffs with some flakes, shorter lived, capped at 35 percent of `Max Storm Snow Particles`, independent of the indoor ease, gated by sky exposure.
  * **Band and indoors:** the wind-aligned band (50 blocks upwind to 16 downwind, 35 each side), up to `scale * (900 + 1200 * strength)` attempts a tick; a spawn cell must be air and see the sky (leaves ignored), as must the cell 6.5 blocks upwind and up along the flight line, with no block between. A particle-only exposure ease (about one second) blends the band into the look-cone mode used under cover, so a doorway fades the snow over a second instead of popping. During the warning the density rises to `Warning Particle Fraction` (0.6).
  * **Cleanup:** a flake dies on a solid block (end of the step, plus a third and two thirds of the way for fast steps, so it cannot pass through a thin roof) and under a roof (`Cull Snow Under Roofs`): each column's cover height is the top of the highest blocking non-leaf block, memoised per second in `MoonSnowRoof`, so roofs, overhangs and cave ceilings kill snow beneath them and canopies do not. `MoonSnowCounters` is reset on disconnect, world unload and when leaving The Aurorian (the particle manager clears particles without calling `setExpired`).
  * The emitter also starts and stops the wind loop `AurorianStormSound` (the existing `aqtweaks:sandstorm.wind` event at a lower pitch, rising through the warning). `AurorianSoundFilter` suppresses Dynamic Surroundings and AmbientSounds wind and animal loops during a storm.
* `AurorianFrostOverlay`: a gradient frost vignette on the HUD that grows with the rime stack.

**Warning.** No chat or action bar line: over the 30 s before a storm the snow rises to `Warning Particle Fraction` of full density and the wind loop fades in (there is no fog). The gameplay hazards start only at strength 0.15. `Hazard Debug` (client) shows a HUD line per hazard channel with the phase, warning, strength and seconds to start and end as this client sees them; "no event received" means the event packet did not arrive.

**Snow accumulation (server, `MoonfrostSnowHandler`, `Enable Snow Accumulation`).** While a storm is ACTIVE at strength 0.25 or more, ground exposed to the sky gains vanilla snow layers (`Blocks.SNOW_LAYER`), more slowly at first and faster as the storm strengthens. It samples the loaded chunks within `Snow Accumulation Radius` (128, in practice the view distance) of any non-spectator player, rebuilt every 20 ticks (a chunk near two players counts once). Attempts per tick are chunks x `Snow Layers Per Chunk Per Second` (8) x strength squared / 20 with the fraction carried between ticks (about 4 to 6 layers on average over a storm), capped at `Snow Max Changes Per Tick` (120) changes for the whole dimension (attempts at 3x that). The column's top is `getPrecipitationHeight` (vanilla rule: a roof, a canopy or any block that blocks movement or is liquid stops it, so snow sits on top of canopies and the ground under trees stays bare). Only air takes snow (`isAir` and `canPlaceBlockAt`): tall grass, flowers, crops, water and other blocks are never replaced. The block is set with flag 2.
* **Depth:** a column holds up to `Max Snow Layers` layers (default 16, two blocks; 8 layers per block, no conversion to a snow block; a full 8 layer block is topped by a new layer-1 block). Collision is `(layers - 1) x 0.125` per block: from 6 layers a column is a step players must jump and mobs treat as a wall, and 16 layers (1.875 blocks) cannot be jumped. Storms build on each other up to the cap (a few storms reach 16 at the default rate).
* **Smooth (`SnowLeveling`):** a layer meant for a column goes to the shallowest orthogonal neighbour when that neighbour is `Snow Max Step` (2) or more layers shallower, walking downhill up to 3 hops until it reaches a safe column, so orthogonal neighbours stay at most the step apart (diagonals at most twice). Neighbours that cannot hold snow (roofs, water, unloaded) do not constrain. No attempt is wasted on a rejection.
* **Wind shadow (`SnowShadow`, `Snow Wind Shadow`):** the server samples the precipitation height of the columns 1 to 10 blocks upwind along the slanted flight line at `Snow Fall Angle`; a column behind a wall, hill or tree is in shadow and takes a layer only about 35 percent of the time, so drifts pile up on the windward side and thin in the lee, and the smoothing rule bounds the contrast. Leaves count as blockers.
* **Entities are not skipped.** Snow lands on and around players and mobs, so a player who stands still in a storm can be buried. Vanilla gives no help: snow layers are not opaque (no suffocation, no push-out), and an entity inside a partial block is not clamped.
* **Vanilla afterwards.** No state, no tracking, no melt of our own. `BlockSnow` random-ticks and turns a layer to air when the block light there is above 11 (torches, lava, fire), with no drops; a layer also vanishes if the block under it is removed. A shovel drops `layers + 1` snowballs per block, as vanilla.
* **Burial hazard (`SnowBurial`, `MoonfrostSnowHandler.burialSecond`, `Enable Snow Burial`):** once a second per survival player in the Aurorian it reads the snow stack in their column. With the chest (`posY + 0.8`) inside the snow: Slowness II for 2 seconds (`Snow Burial Slowness`). With the eye inside it: the air debt grows by `Snow Burial Air Drain Per Second` (40 of 300, about 7 seconds), the air bar is held at the burial level every tick (vanilla refills it each tick), and once the air is gone `Snow Burial Damage` (2.0, bypasses armor, `aqtweaks_snow_burial`) is dealt every second **but never below 1 heart** (`min(damage, health - 1)`): burial is a trap, not a killer. The debt refills at 160 per second when the head is free. Escape is digging: snow layers have hardness 0.1, so a hand or shovel clears a block (all its layers) almost at once. Mobs and items are only embedded. Shelter, rime, frostbite and wind keep their own rules (snow layers do not block the sky).

**Test command.** `/aurorianweather <moonfrost|clear|status> [seconds] [skipWarning]` (OP, like `/atumweather`): `moonfrost` schedules a storm through the hazard engine, so it warns like a natural one (the warning lead is `Warning Ticks`) unless `skipWarning` is given; `[seconds]` sets its length. `clear` ramps an active storm down, or cancels an upcoming one. Clients get the new event by the normal packet.

## Files

| Piece | Role |
| --- | --- |
| `mixin/aurorian/MixinAurorianWorldProvider.java` | Fixed moon phase and celestial angle (late json `mixins.aqtweaks.aurorian.json`) |
| `mixin/MixinWorldFixedSkylight.java` | Fixed gameplay sky light, Aurorian provider recognised by name (early json) |
| `mixin/MixinEntityRendererFog.java` | Nether ash fog after every fog event handler (client, main json); nothing Aurorian since the Moonfrost storm has no fog |
| `mixin/elenaidodge/MixinCUpdateRegenHandler.java` | Clamps Elenai's running regeneration timer when the regen modifier changes (client) |
| `weather/StormSchedule.java`, `weather/StormShelter.java`, `weather/ShelterHold.java`, `weather/Wildcard.java` | Pure storm timetable, shared wind shelter test (with the leaf-ignoring `openSky`), shelter hold, wildcard matcher |
| `aurorian/StormVisuals.java`, `client/HazardDebugHud.java` | Continuous visual scale through warning, start and end; optional debug HUD |
| `aurorian/MoonfrostState.java`, `ClientMoonfrost.java` | Moonfrost hazard channel and state wrapper; client copy of the rime stack |
| `aurorian/MoonfrostHandler.java`, `MoonfrostGear.java`, `MoonfrostTier.java`, `RimeTracker.java`, `AurorianWindHelper.java` | Hazards, gear tiers, rime, wind heading |
| `aurorian/CommandAurorianWeather.java` | `/aurorianweather` |
| `network/PacketMoonfrostState.java` | Rime stack to the player (storm events use `PacketHazardEvent`, see [hazards.md](hazards.md)) |
| `client/aurorian/AurorianSnowEmitter.java`, `ParticleMoonSnow.java`, `ParticleMoonClump.java`, `MoonSnowCounters.java`, `MoonSnowRoof.java`, `AurorianStormSound.java`, `AurorianSoundFilter.java`, `AurorianFrostOverlay.java` | Blizzard snow, clumps, counters, roof cover heights, wind sound, sound filter, frost vignette |
| `aurorian/MoonSnowShape.java`, `SnowRate.java`, `SnowShadow.java`, `SnowStack.java`, `SnowLeveling.java`, `SnowBurial.java` | Pure shape, rate, wind shadow, stack, leveling and burial rules (unit tested) |
| `aurorian/MoonfrostSnowHandler.java` | Server snow accumulation and the burial hazard |

## Do not regress

* The lighting mixins stay independent: the look comes from the provider overrides, the gameplay light from `calculateSkylightSubtracted`. No Aurorian import in an early mixin.
* No per-tick storm packet. The server announces whole events; the state is a pure function of `getTotalWorldTime()` and the event.
* Never change max stamina for the regeneration slowdown; always remove the applied delta on every exit path (shelter, storm end, dimension change, logout, death).
* No fog is applied in the Aurorian. The Nether ash fog call in `MixinEntityRendererFog` stays. Snow accumulation is server side, vanilla snow layers only, into air only, with no tracking or melt of our own (vanilla melts it near bright light), and entities are not skipped (burial is intentional, and non-lethal).
* Rime movement is relative to the current speed attribute; do not replace it with a fixed distance.

## Verify

See [verification.md](verification.md) under *Aurorian*.

## Config key reference

Exact `@Config.Name` strings and code defaults.

| Key | Default | Note |
| --- | --- | --- |
| Enable Fixed Lighting | `true` | Pin the Aurorian to a permanent moonlit look (fixed moon phase and celestial angle) and a fixed gameplay sky light, so the time-driven brightness swing stops. |
| Sky Light Level | `9` | Gameplay sky light of open sky in the Aurorian (crops and saplings need 9, hostile spawns stop above 7). |
| Moon Phase | `0` | Fixed moon phase for the Aurorian. |
| Celestial Angle | `0.5` | Fixed celestial angle for the Aurorian. |
| Enable Moonfrost Storms | `true` | Scheduled cold storms in the Aurorian: frostbite, rime, blizzard snow, snow that piles up on the ground, wind and slowed stamina regeneration while exposed. |
| Storm Interval Min Seconds | `360` | Minimum clear time between Moonfrost storms in seconds. |
| Storm Interval Max Seconds | `1200` | Maximum clear time between Moonfrost storms in seconds. |
| Storm Duration Min Seconds | `240` | Minimum Moonfrost storm duration in seconds. |
| Storm Duration Max Seconds | `420` | Maximum Moonfrost storm duration in seconds. |
| Warning Ticks | `600` | Ticks before a storm during which the snow thickens and the wind rises. |
| Ramp Ticks | `600` | Ticks a storm takes to ramp up to full strength, and to fade out again. |
| Enable Frostbite | `true` | If true, unsheltered players in a Moonfrost storm take periodic armor-bypassing frostbite damage every 4 seconds. |
| Frostbite Damage Exposed | `6.0` | Damage every 4 seconds when unsheltered with any armor slot empty. |
| Frostbite Damage Armored | `4.0` | Damage every 4 seconds when unsheltered in four slots of ordinary armor. |
| Frostbite Damage Proper | `2.0` | Damage every 4 seconds when unsheltered with proper cold gear (Cold Resistance effect or a full Proper Gear Whitelist set). |
| Enable Rime | `true` | If true, standing still unsheltered in a Moonfrost storm builds rime (up to 15), which slows and weakens the player. |
| Rime Move Threshold Factor | `0.5` | A player counts as moving if they covered more than this factor times their current movement speed in the last second. |
| Rime Gain Exposed | `1.0` | Rime gained per second while standing still, no full armor. |
| Rime Gain Armored | `0.6` | Rime gained per second while standing still in ordinary full armor. |
| Rime Gain Proper | `0.25` | Rime gained per second while standing still in proper cold gear. |
| Enable Regen Pause | `true` | If true, the Elenai Dodge stamina regeneration interval is multiplied while exposed in a storm (Regen Interval Multiplier settings). |
| Regen Interval Multiplier Exposed | `20.0` | Multiplier on the interval between regenerated feathers while exposed with no full armor. |
| Regen Interval Multiplier Armored | `2.0` | Multiplier on the feather interval while exposed in ordinary full armor. |
| Regen Interval Multiplier Proper | `1.25` | Multiplier on the feather interval while exposed in proper cold gear. |
| Enable Wind Push | `true` | If true, unsheltered players in a Moonfrost storm are pushed by the wind (tailwind boost, headwind resistance). |
| Wind Push Force | `0.020` | Base velocity force applied to unsheltered players in a Moonfrost storm. |
| Enable Headwind Stamina Drain | `true` | If true, sprinting directly into the Moonfrost wind drains feathers and adds exhaustion. |
| Shelter Wind Shadow Distance | `6.5` | Distance in blocks upwind to trace for solid blocks that shield the player from the wind. |
| Proper Gear Effect | `simpledifficulty:cold_resist` | Potion registry name that marks proper cold gear. |
| Proper Gear Whitelist | `{ thaumcraft:goggles, thaumcraft:thaumium_robes_*, thaumcraft:fortress_*, bewitchment:besmirched_* }` | Item registry names (wildcards supported). |
| Enable Storm Particles | `true` | Client: spawn wind-blown moon snow around the player during a Moonfrost storm. |
| Max Storm Snow Particles | `12000` | Client: cap on live moon snow particles at full strength (the veil may use 35 percent of it). |
| Warning Particle Fraction | `0.6` | Client: fraction of full snow density reached at the end of the warning. |
| Snow Fall Angle | `58.0` | Client: degrees below horizontal the blizzard snow falls (5 to 80). |
| Snow Speed Multiplier | `1.0` | Client: multiplier on the snow speed (0.5 to 2). |
| Snow Streaks | `true` | Client: draw the fastest flakes as streaks aligned with their motion. |
| Cull Snow Under Roofs | `true` | Client: snow particles vanish under a roof or overhang (canopies do not count). |
| Visibility Perimeter Radius | `10` | Client: radius of the dense snow veil around the player (0 turns it off, max 32). |
| Perimeter Density | `1.0` | Client: multiplier on the veil (0 to 4). |
| Enable Snow Accumulation | `true` | Server: ground exposed to the sky gains vanilla snow layers during a storm. |
| Snow Layers Per Chunk Per Second | `8.0` | Server: build-up rate at full strength, per loaded chunk (scales with strength squared). |
| Snow Max Changes Per Tick | `120` | Server: hard cap on snow block changes per tick for the whole dimension. |
| Max Snow Layers | `16` | Server: deepest a column gets (8 per block, 1 to 16); storms build on each other up to it. |
| Snow Max Step | `2` | Server: neighbouring columns differ by at most this many layers. |
| Snow Accumulation Radius | `128` | Server: loaded chunks within this many blocks of a player get snow (16 to 256). |
| Snow Wind Shadow | `true` | Server: snow builds more slowly in the lee of walls, hills and trees. |
| Enable Snow Burial | `true` | Server: slowness at chest depth, air loss and damage (never below 1 heart) with the head under snow. |
| Snow Burial Slowness | `true` | Slowness II at chest depth or deeper. |
| Snow Burial Air Drain Per Second | `40` | Air lost per second with the head under (of 300; 0 turns suffocation off). |
| Snow Burial Damage | `2.0` | Damage per second once the air is gone; never takes a player below 1 heart. |
| Shelter Change Delay Seconds | `2.0` | The sheltered or exposed state must hold this long before it changes (0 = off). |
| Hazard Debug | `false` | Client: HUD line per hazard channel. |
| Enable Storm Sound | `true` | Client: looping wind that rises through the warning and follows storm strength. |
| Storm Wind Volume | `1.0` | Client: volume of the Moonfrost wind loop. |
| Suppress External Wind in Aurorian | `true` | Client: during a storm, suppress Dynamic Surroundings and AmbientSounds wind and animal loops in the Aurorian. |
| Enable Frost Overlay | `true` | Client: draw a frost vignette on the HUD that grows with rime. |
