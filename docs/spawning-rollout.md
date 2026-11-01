# Spawning rollout: testing and enabling, step by step

In Control stays installed and keeps making every spawn decision throughout. Each stage has its own switch, off by default, and each can be turned off again without a restart (config screen) to fall back to In Control. Do the stages in order, one per play session, so a problem points at one stage.

Switches (`config/arcanaquesttweaks/aqtweaks_spawning.cfg`, section General): `Use Potential Spawn Table`, `Shadow Spawn Conditions`, `Spawn Rules Decide`, `Potential Spawn Table Keeps In Control Rules`, `Potential Spawn Table Directory` (default `arcanaquest`, where the pack files and the rule files are read from).

## Stage 0: prepare

1. Build: `./gradlew build`. The 43 unit tests run as part of it. If anything fails, send me the output.
2. Back up `config/incontrol/`, `config/arcanaquest/` and `config/arcanaquesttweaks/`.
3. Make sure Spark is ready for stage 3.

## Stage 1: boot with every switch off (about 10 minutes)

1. Start the game and load a world.
2. Check `latest.log` for lines containing `AQTweaks`. Expected: `Potential spawn table: built 159 potential-spawn rows from .../config/arcanaquest`.
3. Run `/aqspawntable`. Expected: `table off, 159 rows, 0 queries cached; In Control has 159 potential-spawn rules loaded`.
4. Play normally for a few minutes. Nothing should feel different.

Pass: no crash, no warnings from `AQTweaks-Spawning`. Fail: send `latest.log`.

## Stage 2: verify the table against your potentialspawn.json (switches still off)

1. `/aqspawntable verify`. Expected last line: `IDENTICAL`, with 159 rules on both sides and no unsupported fields.
2. If it says `DIFFERENT`, open `config/arcanaquesttweaks/potentialspawn_verify.txt` and send it to me. A common cause is a `config/incontrol/potentialspawn.json` that is older than the pack files.
3. Optional: `/aqspawntable dump`, then compare `config/arcanaquesttweaks/potentialspawn_from_table.json` with your generated file.

Pass: IDENTICAL.

## Stage 3: turn the potential spawn table on

1. In the config screen set `Use Potential Spawn Table` to true. Leave `Index In Control Potential Spawn Rules` as it is.
2. Play 30 minutes across: Overworld day and night, caves at several depths, a Mineshaft, an ocean, the Beneath, the Nether and the End.
3. Check that mobs look right for each place and `/aqspawntable` shows a growing "queries cached" (a few dozen is normal).
4. Check `latest.log` for `cannot find mob` warnings (In Control logs the same ones) and any exception from `AQTweaks`.
5. Profile with Spark, as you did before, with the table on and `Shadow Spawn Conditions` off. Send me the profile, and one from the same flight with the table off if you can: I want `onPotentialSpawns`, `GenericRuleEvaluator` and `World.getBiome`.
6. If spawns look wrong, switch it off (takes effect at once) and tell me what looked wrong, where.

Pass: normal spawns, no errors, the profile shows the rule loop gone.

## Stage 4: convert and check the rule files (nothing runs yet)

1. `/aqspawntable convert`. Read `config/arcanaquesttweaks/converted/convert_report.txt`: it lists what moved or was dropped (expect the three cultist rows moved, and 23 results left out of rules with stats).
2. Copy `spawn_conditions.json`, `spawn_modifiers.json` and `spawn_gear.json` from `converted/` to `config/arcanaquest/`.
3. `/aqspawntable check`. Expected: 48, 34 and 9 rules ok, and `All rule files are valid.`

Pass: all three valid.

## Stage 5: compare the rule files with In Control (shadow)

1. `/aqspawntable reload`. Expected: `Spawn conditions: loaded 48 rules` and `Spawn modifiers and gear: modifiers: 34 rows; gear: 9 rows`.
2. Set `Shadow Spawn Conditions` to true. (Independent of stage 3: the table can be on or off.)
3. Play 30 to 60 minutes covering: surface by day and night, caves at several depths (including below Y -15), a village, a Mineshaft, spawner cages, Atum, the Nether, the End and the Beneath. Do part of it with a character that has the stages elite, deadly, dragon and crimsoncult, and part without.
4. `/aqspawntable shadow`. It reports the conditions, then the modifiers and gear. Target for conditions: `0 different`. Modifiers and gear will list some differences by design (below); everything else should be 0. The file `config/arcanaquesttweaks/spawn_shadow.txt` has the full list; each kind of difference is also in the log the first time it happens.
5. Use `/aqspawntable shadow reset` between scenarios to see which one a difference came from.
6. Send me `spawn_shadow.txt` and `latest.log`. We decide together for each difference whether the converter or evaluator is wrong, or whether the rule itself is the surprise.
7. Keep this off while profiling: it runs the rules a second time.

Pass: several sessions with no differences, or only differences we have agreed to accept.

## Stage 6: Tweaks takes over everything in one step (built, not yet tried in game)

Conditions, modifiers and gear switch over together with one switch, `Spawn Rules Decide` (off by default, applies at once, no restart).

What it does when on:
- At the spawn check the conditions table sets the result, as In Control did.
- At join it cancels a denied mob; a mob that stays gets its modifiers (stats, potions, angry) and its gear, once. A tag (`aqtweaks:spawnrules`) stops a chunk reload applying them again, and a mob In Control already actioned (`incontrol:onjoinaction`) is left alone.
- In Control's own `spawn.json` handlers (spawn check and join) do nothing, through a mixin. Its potential-spawn, loot, experience and summon-aid rules are untouched.
- If any of the three files did not load cleanly, nothing changes: In Control keeps deciding. `/aqspawntable reload` says why.
- The comparison is not available while it is on (there is nothing of In Control's to compare with). Switch it off to compare again.

Order:
1. Stage 4, then Stage 5 with `Shadow Spawn Conditions` on: conditions 0 different, and the modifier and gear differences understood (next list).
2. Turn `Shadow Spawn Conditions` off, `Spawn Rules Decide` on. Play the same scenarios as stage 5 (surface by day and night, caves incl. below Y -15, a village, a Mineshaft, spawner cages, Atum, the Nether, the End, the Beneath; with and without the stages elite, deadly, dragon, crimsoncult). Look at mob health, damage, potions and gear (a cursed armor in a cave, a Crimson cultist, a Vindicator, a Stray).
3. If anything looks wrong, switch it off: In Control takes over again for new spawns. Mobs already spawned keep what they were given.
4. Only after a clean session, delete the rules that Tweaks has taken over from `spawn.json`. `loot.json` and In Control itself stay.

Differences from In Control that are on purpose (the modifier and gear comparison will list these):
- In Control ends its walk at the first matching rule, even a rule with no stats or no gear. A gear-only rule that matches first (Cursed Armor, Vindicator, Deep Dwarf) means In Control gives that mob no stats at all; the tables still give it the depth and dimension modifier rows below. The same the other way round for gear. In the test run this is about a quarter of generated joins.
- The Crimson cultist and archer buffs are applied at join, not at the spawn check.
- `angry` is not compared.
- Vanilla animals that Animania denies stay denied underground. In Control's late tier rules reset that denial by accident; the comparison lists these as "In Control DEFAULT, Tweaks DENY (no row)" on pigs, sheep, chickens, cows and horses.

What the tests establish: wherever In Control's first match gives modifiers or gear, the converted tables give exactly the same (stats, potions, gear), about 167,000 cases per run with random rows forced to pass and forced to fail. What no test covers: the code that reads and sets attributes, potions and equipment in the game, and the mixin into In Control. That is what the first session shows.

## Finding out why a mob did or did not spawn: the spawn trace

For a report like "seven Vindicators", "a squid outside the ocean" or "no hunting party", switch the trace on and send the file it writes.

1. `/aqspawntable trace on` (or the config option `Spawn Trace`). By default it watches the squid, Vindicator, feral goblin and the three Crimson cultists; change them with `/aqspawntable trace mobs minecraft:squid,...` or the config option `Spawn Trace Mobs`. `/aqspawntable trace all` watches every mob (a lot of output).
2. Optional: `/ctrldebug` as well, so In Control's own log lines are in `latest.log` for the same moments.
3. Play until it happens. While the trace is on it also writes a summary every 60 seconds (config `Spawn Trace Summary Seconds`, 0 turns it off): the `/aqspawntable shadow` counts for conditions, modifiers and gear, the list of differences whenever it has changed, the table state, and for each watched mob and world how many are really alive against what In Control's count says. `/aqspawntable trace now` writes that summary immediately.
4. Send `config/arcanaquesttweaks/spawn_trace.log` (and `latest.log`, and `/aqspawntable shadow`). The file rolls over to `spawn_trace.old.log` at 8 MB.

Every other mob is covered too (config `Spawn Trace All Mobs`, on by default): its spawn checks, joins and potential-spawn offers are counted per mob in a `tally` block with each summary. A mob you did not name is only followed line by line once the shadow comparison finds a mismatch for it (conditions, modifiers or gear, or the table disagreeing at a check or join): a `-- shadow mismatch for <mob>; now watching it in full` line is written, and from then on that mob gets every line a named mob does (at most 50 such mobs per session).

What the file holds, one line each: every spawn check and join of a watched mob (position, biome, sky, time, spawner or not, how it came about, In Control's result, the live counts, what the conditions table says); each time the potential spawn table offered or withheld it (live count, limit times players, structure and height needs; at most one line per 2 s per result); the pack filler's group size and how many candidate spots each check refused; and each party's leader, chance and companions.

## If something goes wrong

| Symptom | Do |
| --- | --- |
| Crash at start | Restore the previous jar, send `latest.log` and the crash report |
| Odd spawns after stage 3 | `Use Potential Spawn Table` off (applies at once), note where and what |
| Lag during stage 5 | `Shadow Spawn Conditions` off; it is a test tool |
| Odd health, damage, potions or gear after stage 6 | `Spawn Rules Decide` off (applies at once), note the mob, where and what |
| `verify` or `check` reports problems | Send the report file; nothing is used while a file has problems |
| Nothing seems to change after editing a pack file | `/aqspawntable reload` |
