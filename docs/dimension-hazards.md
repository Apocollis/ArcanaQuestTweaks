# Dimension environmental hazards (overview)

Last updated: 2026-11-03. This is the roster of how each pack dimension makes the environment itself a hazard, which system owns each one, and what is planned. Tweaks owns some of them; others are parent-mod or pack config and are listed here so the whole picture is in one place. Worldgen is not involved; these are runtime effects.

| Dimension | Hazard | Owner | State |
| --- | --- | --- | --- |
| Atum | Sandstorm: wind push, headwind drain, grit blindness, choking, three-tier scour damage, armor wear | Tweaks ([client.md](client.md), `atum/AtumStormGameplayHandler`) | Built |
| Aurorian | Moonfrost storm: frostbite, rime, whiteout, regeneration pause | Tweaks | Planned (`.plans/20261103-0130-aurorian-moonfrost-storm.md`) |
| Nether | Clustered lava spout events (30 s, 3 to 6 staggered eruptions per player, temporary lava splashes that clear by themselves) and withering ash clouds (gas-like, flow through openings, not walls) on separate timers | Tweaks | Planned (`.plans/20261103-0200-nether-lava-spouts-and-ash-clouds.md`) |
| Aether | Very short, very fast, telegraphed wind gusts that can push players off island edges (grappling hook, crouching at the edge or shelter to resist), plus telegraphed lightning strikes on a separate timer | Tweaks | Planned (`.plans/20261103-0210-aether-wind-bursts.md`) |
| Twilight Forest | Ley-Rupture: telegraphed (30 s) arcane surges rising from the ground, strongest at the forest floor and fading with height, sheltered by tall canopy, ancient structures and Comfort rooms; using any magic item while exposed backfires | Tweaks | Planned (`.plans/20261103-0220-twilightforest-wild-magic.md`) |
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
The pack's SetBonus mod (`config/setbonus/1.12.2.009+.cfg`) gives a fully worn set a potion bonus, so protection often already exists as an effect. Examples: Aurorian cerulean, knight, spectral and steel sets, and Twilight Forest arctic and yeti sets give `simpledifficulty:cold_resist`; Atum wanderer, mummy, desert and Ra sets give `simpledifficulty:heat_resist` (mummy also `rustic:wither_ward`); NetherEx Black Salamander Hide gives fire resistance, heat resist and wither ward, Orange Salamander Hide fire resistance and heat resist, Wither Bone wither ward and soulsight; Twilight Forest fiery, Aether phoenix and Embers ashen give fire resistance; Aether gravitite, Twilight Forest phantom, Aurorian spectral, Botania elementium and Ee endfall give `potioncore:slow_fall`. Rustic's Wither Ward reduces damage from the Wither effect. The Nether hazard plan uses these effects as its gear tiers (Fire Resistance for spouts, Wither Ward for ash); the other plans can do the same instead of keeping name whitelists.

## Design notes shared by the Tweaks hazards
* Hazards are scheduled or local events with a warning, a shelter rule and a gear scale, not permanent debuffs (the permanent pressure, such as the Aurorian cold or the Nether heat, already comes from Simple Difficulty).
* Schedules are computed from `World.getTotalWorldTime()` and the world seed, so the server and every client agree with no packet and `/time set` does not move them.
* Armor tiers follow Atum's whole-set rule (exposed, ordinary armor, proper gear), with a per-dimension "proper gear" list.
* Shared pieces planned in `weather/`: `StormSchedule` (the deterministic schedule) and `StormShelter` (the roof and upwind-wall test, extracted from the Atum handler).
