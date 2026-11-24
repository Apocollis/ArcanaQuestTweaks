# Dimension environmental hazards (overview)

Last updated: 2026-11-14. This is the roster of how each pack dimension makes the environment itself a hazard, which system owns each one, and what is planned. Tweaks owns some of them; others are parent-mod or pack config and are listed here so the whole picture is in one place. Worldgen is not involved; these are runtime effects.

| Dimension | Hazard | Owner | State |
| --- | --- | --- | --- |
| Atum | Sandstorm: wind push, headwind drain, grit blindness, three-tier scour damage, armor wear, sand that piles up on the ground | Tweaks ([client.md](client.md), `atum/AtumStormGameplayHandler`) | Built |
| Aurorian | Moonfrost storm: frostbite, rime, blizzard snow that piles up (burial hazard), regeneration pause | Tweaks ([aurorian.md](aurorian.md)) | Built |
| Nether | Clustered lava spout events (45 s, 10 to 16 staggered eruptions per player, temporary lava splashes with a landing warning that clear by themselves) and withering ash storms (30 s warning, then rounded swirling clouds that flow through openings, not walls) on separate timers | Tweaks ([nether.md](nether.md)) | Built (not yet tested in game) |
| Aether | Very short, very fast, telegraphed wind gusts that can push players off island edges (grappling hook, crouching, or wind gear/weight to resist), plus lightning storms (10 s warning, then 5 to 6 telegraphed salvos over 18 to 24 s that lock onto exposed players by gear class; primary bolts inflict Slowness II + `20`-tick shared `Stun`, bypassed by full channeling sets) on a separate timer | Tweaks ([aether.md](aether.md)) | Built (not yet tested in game) |
| Twilight Forest | Ley-Rupture: telegraphed (30 s) arcane surges of 150 to 210 s every 12 to 20 minutes rising from the ground, strongest at the forest floor and fading by about Y50, sheltered by tall canopy, ancient structures and Comfort rooms; charges worn/held vis items & Vis Batteries while blocking runic shield recharge; exposure builds `WarpMeter` (`1` temp warp per 4 cycles, `1` normal per 40; decays 1 per sheltered cycle); casting/using magic items overchannels (2× focus power + backlash + temp warp, or double pool with warp-only cost for Aegis); 16 s continuous outdoor exposure rolls a weighted updraft (none, Flight + Slowfall, or Levitation + Slowfall) | Tweaks ([twilightforest.md](twilightforest.md)) | Built (not yet tested in game) |
| Betweenlands | Decay (and tool corrosion) | Betweenlands (parent mod), pack config | Built into the parent; see below |
| Beneath | Darkness effects, plus a timed hunt when you stay in the dark | Lycanites Mobs spawner config, pack config | Pack config; see below |

## Betweenlands: Decay and corrosion (parent mod, not Tweaks)
Betweenlands 3.9.6 has no mechanic called corruption. Its hazards are:
* **Decay:** a hunger-style bar, level 0 to 20, with saturation and an acceleration meter. It fills with time in the dimension at a rate set by difficulty (0.0025 easy, 0.0033 normal, 0.005 hard per tick base) and speeds up near stagnant water, sludge jets and wall lampreys, and when hit by smelly or decayed mobs; Betweenlands armor reduces it. Each level lowers maximum health down to a floor (`decay_min_health`, pack value 6.0 = 3 hearts, absolute, `decay_percentage=false`), and decay 10 or above applies Slowness that strengthens at 13 and 16. Decay food, the sap ball, weeping blue petals in a censer and the Elixir of Ripening heal it; the Elixir of Decay and stagnant-water censers worsen it.
* **Tool corrosion:** tools and armor corrode through six states (shown in the tooltip) and can be protected with coatings.
* A Corrupt Betweenstone block and a corrupted gecko exist in the jar, and the in-game manual has a page titled Corruption; they are dungeon content, not a player-wide mechanic. Not read in detail.
* Pack config: `config/thebetweenlands/config.cfg` (`use_decay=true`, `use_corrosion=true`, empty `decay_dimension_list`).

## Beneath: the darkness hunt (Lycanites Mobs spawner)
Implemented entirely in the pack's Lycanites config, `config/lycanitesmobs/spawners/darkness.json`; Tweaks has no code for it.
* **Scope:** the spawner's world condition is dimension id 10 (the Beneath). `ignoreDimensions`, `ignoreBiomes`, `ignoreLightLevel` and `ignoreCollision` are true, so it ignores the usual spawn restrictions.
* **Build-up:** a player trigger checks every 100 ticks (5 s). While the player's light level is 3 or lower, each check has a 75% chance to add one to the trigger count; `triggersRequired` is 3. A second trigger, while the light level is 4 or higher, sets the count back to 0. So about 15 to 20 seconds in the dark builds the count to 3, and any light at level 4 or above clears it.
* **Warnings:** `triggerCountMessages` shows `spawner.darkness.level1`, `level2`, `level3` as the count rises, and `spawner.darkness.level1.back` when it resets.
* **Payoff:** at the required count a Lycanites `grue` (`mobId lycanitesmobs:grue`, weight 8) spawns within 2 blocks of the player on air, tall grass or water. One mob per trigger (`mobCountMin` 1, location `limit` 1).
* Changing this behavior is a Lycanites config edit, not a Tweaks change. The count semantics above are read from the file and the message keys; they have not been tested in game.

## Pack set bonuses (what already protects)
The pack's SetBonus mod (`config/setbonus/1.12.2.009+.cfg`) gives a fully worn set a potion bonus, so protection often already exists as an effect. Examples: Aurorian cerulean, knight, spectral and steel sets, and Twilight Forest arctic and yeti sets give `simpledifficulty:cold_resist`; Atum wanderer, mummy, desert and Ra sets give `simpledifficulty:heat_resist` (mummy also `rustic:wither_ward`); NetherEx Black Salamander Hide gives fire resistance, heat resist and wither ward, Orange Salamander Hide fire resistance and heat resist, Wither Bone wither ward and soulsight; Twilight Forest fiery, Aether phoenix and Embers ashen give fire resistance; Aether gravitite, Twilight Forest phantom, Aurorian spectral, Botania elementium and Ee endfall give `potioncore:slow_fall`. Rustic's Wither Ward reduces damage from the Wither effect. All dimension gear whitelists, wind-push resistance (`[wind]`), eye protection (`[eye_protection]`), and effect IDs now live in `config/arcanaquesttweaks/aqtweaks_hazard_gear.cfg`.

## Design notes shared by the Tweaks hazards
* Hazards are scheduled or local events with a warning, a shelter rule and a gear scale, not permanent debuffs (the permanent pressure, such as the Aurorian cold or the Nether heat, already comes from Simple Difficulty).
* Scheduled hazards run on the shared hazard engine ([hazards.md](hazards.md)): the server owns each event and announces it once; clients animate it from `World.getTotalWorldTime()`, so `/time set` does not move it and command-started events warn like natural ones.
* Armor tiers follow Atum's whole-set rule (exposed, ordinary armor, proper gear), with per-dimension lists in `aqtweaks_hazard_gear.cfg`.
* Shared pieces in `weather/`: `LayerAccumulator`, `LayerRate`, `LayerShadow`, `LayerStack` and `LayerLeveling` (the smooth, wind-shadowed layer build-up shared by the Aurorian snow and the Atum sand), `StormSchedule` (the pure gap and duration generator used by the hazard engine), `StormShelter` (the roof and upwind-wall test, extracted from the Atum handler), `WindResistanceGear` (additive wind-push resistance across Aether, Atum, and Aurorian), and `HazardGearAudit` (startup check logging unmatched gear patterns).

