# Spawning rollout: testing and enabling, step by step

In Control stays installed and keeps making every spawn decision throughout. Each stage has its own switch, off by default, and each can be turned off again without a restart (config screen) to fall back to In Control. Do the stages in order, one per play session, so a problem points at one stage.

Switches (`config/arcanaquesttweaks/aqtweaks_spawning.cfg`, section General): `Use Potential Spawn Table`, `Shadow Spawn Conditions`, `Potential Spawn Table Keeps In Control Rules`, `Potential Spawn Table Directory` (default `arcanaquest`, where the pack files and the rule files are read from).

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

## Stage 5: compare the conditions with In Control (shadow)

1. `/aqspawntable reload`. Expected: `Spawn conditions: loaded 48 rules`.
2. Set `Shadow Spawn Conditions` to true. (Independent of stage 3: the table can be on or off.)
3. Play 30 to 60 minutes covering: surface by day and night, caves at several depths (including below Y -15), a village, a Mineshaft, spawner cages, Atum, the Nether, the End and the Beneath. Do part of it with a character that has the stages elite, deadly, dragon and crimsoncult, and part without.
4. `/aqspawntable shadow`. Target: `0 different`. The file `config/arcanaquesttweaks/spawn_shadow.txt` has the full list; each kind of difference is also in the log the first time it happens.
5. Use `/aqspawntable shadow reset` between scenarios to see which one a difference came from.
6. Send me `spawn_shadow.txt` and `latest.log`. We decide together for each difference whether the converter or evaluator is wrong, or whether the rule itself is the surprise.
7. Keep this off while profiling: it runs the rules a second time.

Pass: several sessions with no differences, or only differences we have agreed to accept.

## Stage 6: Tweaks takes over everything in one step (not built yet)

Decided: conditions, modifiers and gear are built together and switched over together, not one file per session.

1. Build: the modifiers and gear evaluators, the same shadow comparison for both, and the switch that lets Tweaks decide.
2. One switch, `Spawn Rules Decide` (off by default, applies without a restart). When on, Tweaks decides conditions at the spawn check and join, then applies modifiers and gear once per mob. In Control's `spawn.json` rules are skipped; `loot.json` and In Control itself stay.
3. Shadow keeps working while the switch is on: it records In Control's outcome before Tweaks overwrites it, so differences are still counted (otherwise it would compare Tweaks with itself).
4. Before the switch is turned on, stage 5 must pass for conditions, and the new shadow must pass for modifiers and gear, in the same sessions.
5. One play session with the switch on, then delete the taken-over rules from `spawn.json`. Turning the switch off is the way back until then.

## If something goes wrong

| Symptom | Do |
| --- | --- |
| Crash at start | Restore the previous jar, send `latest.log` and the crash report |
| Odd spawns after stage 3 | `Use Potential Spawn Table` off (applies at once), note where and what |
| Lag during stage 5 | `Shadow Spawn Conditions` off; it is a test tool |
| `verify` or `check` reports problems | Send the report file; nothing is used while a file has problems |
| Nothing seems to change after editing a pack file | `/aqspawntable reload` |
