# Spawning rollout: handoff (state as of 2026-11-02)

Resume prompt to paste into a new chat is at the end of this file.

## What this project is

Arcana Quest Tweaks (`/home/user/arcanaquesttweaks`, GitHub `Apocollis/ArcanaQuestTweaks`, working branch `experimental`) is a Minecraft 1.12.2 Cleanroom mod for the Arcana Quest modpack. The spawning rollout replaces In Control's `spawn.json` rules (and the `potentialspawn.json` loop) with three rule files and a potential-spawn table that Tweaks reads. In Control stays installed (loot, experience, summon aid, and as the fallback). The user is the pack author, plays a DEVBOX instance on Windows, and sends `latest.log`, `spawn_trace.log` and screenshots from test runs. The user is not in this sandbox: they build and run the game themselves, so a change only reaches them after they rebuild from `experimental`.

Read first: `docs/spawning-rollout.md` (the staged runbook, with the spawn-trace section), `docs/spawn-tables-format.md` (file formats, "Differences from In Control on purpose", "Decisions recorded"), `docs/changelog.md` (top entries), `docs/spawning.md` (older spawning features).

## Where things stand

| Stage | State |
| --- | --- |
| 0 to 2: boot with switches off, `/aqspawntable verify` | done, IDENTICAL (159 rows, 4120 queries) |
| 3: `Use Potential Spawn Table` on | done, on in the user's config. Spark profile showed the rule loop gone (spawn-list lookup about 0.5% of tick) |
| 4: convert and check the rule files | done (48 conditions, 34 modifiers, 9 gear rows, all valid) |
| 5: shadow comparison (`Shadow Spawn Conditions`) | done on the latest build: modifiers and gear 1749 same / 0 different, conditions only the 14 animal differences explained below |
| 6: `Spawn Rules Decide` | built, one in-game session run (22:09 to 22:56 on 2026-11-01, trace `a935d917-spawn_trace.log`), results below. Not yet accepted |

Last commits on `experimental` (newest first): `6ae8d25` trace registered after the decider, `7d6de1b` trace labels and `apply` lines in decide mode, `f8742a1` Animania decision docs, `537895a` shadow attributes results to In Control exactly via the mixin, then the spawn trace commits, `20c3c7f` the Stage 6 build (modifier and gear evaluators, `RuleShadow`, `SpawnRuleDecider`, `SpawnRuleApplier`, `MixinInControlSpawnRules`), `5f08df2` the random-flag fix.

## Code map (all under `src/main/java/com/apocollis/aqtweaks/`)

- `spawning/rules/` plain Java, unit tested: `RuleFileLoader`, `RuleSchema`, `SpawnJsonConverter`, `ConditionTable` + `ConditionRow` (the match tests; `rollDecides` flags a spawn as random-dependent only when the roll could decide the row), `ModifierTable` + `ModifierPlan`, `GearTable`, `SpawnQuery`.
- `spawning/` game-facing: `ConditionShadow` (conditions comparison; hooks called from the mixin around In Control's handlers), `RuleShadow` (modifiers and gear comparison, snapshot just before In Control's join handler), `SpawnRuleDecider` (decide mode: sets the spawn-check result, cancels denied joins, applies modifiers and gear once, tags `aqtweaks:spawnrules`), `SpawnRuleApplier` (attributes, potions, equipment, angry), `SpawnRuleTables` (loads modifiers and gear files), `PotentialSpawnTable`, `SpawnPackFiller`, `SpawnParties`, `SpawnTrace` + `SpawnTraceHandler` (the trace log), `CommandAqSpawnTable` (`/aqspawntable ...`), `LiveQuery`.
- `mixin/incontrol/MixinInControlSpawnRules`: skips In Control's `onEntityJoinWorld` and `onEntitySpawnEvent` while `SpawnRuleDecider.deciding()`, and marks the moments just before and after those handlers for the shadow.
- Config: `ArcanaQuestTweaksConfig.SpawningModuleConfig.general`: `usePotentialSpawnTable`, `shadowSpawnConditions`, `spawnRulesDecide`, `spawnTrace`, `spawnTraceMobs`, `spawnTraceAllMobs`, `spawnTraceSummarySeconds`.
- Tests: `src/test/java/com/apocollis/aqtweaks/spawning/rules/` (`ConditionDifferentialTest`, `ActionDifferentialTest`, `ModifierGearTableTest`, `ConditionTableTest`, ...). 40 tests passed at last run. The differential tests run the pack's original `spawn.json` (copy in `src/test/resources/spawntable/`) the way In Control runs it beside the converted tables.

## How to build and test in this sandbox (important)

- `./gradlew` does not work here: the lwjgl dependency 429s from Maven Central and there is no offline cache. Do not spend time on it. The user builds with Gradle on their side.
- Plain-Java tests (the `spawning/rules` package): compile with `javac` (JDK 21 is fine) the files in `spawning/rules/`, `spawning/table/` and the test dir, with gson (`~/.gradle/caches/modules-2/files-2.1/com.google.code.gson/gson/2.8.9/...`) and the JUnit console standalone jar (it was kept in the previous session's scratchpad `libs/`; re-download if gone), then run `java -jar junit-platform-console-standalone-*.jar execute -cp <out>:<gson> --scan-classpath <out>`. Copy `src/test/resources/*` into the output dir so the resource lookups work.
- Game-facing code can only be compile-checked: use the JDK 25 at `~/.gradle/jdks/eclipse_adoptium-25-amd64-linux.2/bin/javac` (the Cleanroom jars are class version 69), `-proc:none`, classpath from the cached Cleanroom 0.5.7 jars under `~/.gradle/caches/unimined/...` plus gson, and a `-sourcepath src/main/java` plus small stubs for In Control classes. Expected, ignorable errors: `SpawnPackFiller` complaining about `MIN_PLAYER_MONSTER_SPAWN_DISTANCE` and `MIN_PLAYER_MOB_SPAWN_DISTANCE` (a gap in the stubs). Anything else in `spawning/` or `mixin/incontrol/` is a real error. The user says the build now targets a newer CleanroomMC than 0.5.7; the used APIs are unchanged 1.12.2 ones.
- In Control Continuation 3.10.5 source is at `/home/user/fonnymunkey/incontrolcontinuation` (rules, `ForgeEventHandlers`, config). McJty Tools (the base of `RuleBase`, where stat, potion and equipment actions live) is NOT available as source, so `SpawnRuleApplier` follows the format doc, not In Control's code.

## Decisions made with the user

- Conditions, modifiers and gear match **independently** (two separate lookups on each mob; a gear match never stops the modifier search and vice versa). Conditions: first matching row wins; modifiers: each stat from the first matching row that sets it, potions from all, `stop` ends the search; gear: first matching row gives the whole loadout.
- Stage 6 is **one step**, not one file at a time, behind one switch, `Spawn Rules Decide` (off by default, applies without restart, falls back to In Control if any of the three files did not load). Shadow is not available while it is on.
- **Animania's denial of vanilla animals stays**: In Control's late tier rules (`dimension 0`, `maxheight` 55 / 15 / -15, `result: default`, stats, no `mob`) reset another mod's DENY, the converter drops `result` from stat rules, so Tweaks keeps the DENY. The 14 "In Control DEFAULT, Tweaks DENY (no row)" animal differences in shadow are expected.
- **Goblin hunting party** was just the pack size (3 to 5); nothing to change. The only entry in `mob_spawnparties.json` is the Crimson cult party led by the cultist cleric.
- **Vindicators:** the user disabled the roofed-forest spawning in Quark / Charm config. No cap row was added. The Mineshaft potential-spawn rule (5 per player) is the only limit.
- Open, not decided: cultist buffs are applied at **join** now, not at the spawn check as In Control does. An optional `phase: "spawn"` for modifier rows could restore the old timing; the user has not answered.
- Crimson cultists show up as an expected shadow difference (In Control buffs them at the spawn check).

## What the Stage 6 session showed (trace `a935d917`, 2026-11-01 22:09 to 22:56, Overworld only)

- Decide mode ran cleanly: 956 spawn checks decided by Tweaks, no exceptions in the trace. The goblin got the right tier stats at the right heights (y at or below 15: 1.75x, 7.0 damage and 24.5 health; y 16 to 55: 1.25x, 5.0 and 17.5), armour 4.0, follow range 40. Vindicators got the iron axe and no stats (their only row is gear). 150 goblin and 11 vindicator `apply` lines.
- Vindicators still arrive: 24 joins in BOP maple woods and sacred springs at y 68 to 98 with **no spawn check and no potential-spawn offer at all**, so they bypass In Control's checks regardless of Quark's roofed-forest setting. Source still unknown (another mod's spawn code, worldgen, or similar). Six of those joins showed "table DENY (row 3)" (hostile in a village by day); in the same run the trace was registered before the decider, so the line showed the state before the decider cancelled it (fixed in `6ae8d25`; those joins should have been cancelled, verify in the next trace that a vindicator with table DENY at join does not get an `apply` line and does not stay).
- 83 checks printed "decided=DEFAULT table=DENY" and 12 "decided=DEFAULT table=ALLOW": the same ordering artifact (trace line written before the decider set the result). With `6ae8d25` they should read decided = table. If they still differ after rebuilding, the decider is not setting the result and that is a real bug.
- Squid: never allowed outside ocean in any trace (219 denies outside ocean in an earlier run, all joins in ocean).
- Still unverified in game: potions applied by the decider (no watched mob had a potion row in the session), angry, enderman held blocks, gear on mobs with non-empty starting gear, and the mixin when the pack is loaded fresh (no failure was seen).

## Suggested next steps

1. Ask the user to rebuild from `experimental` (latest commit) and run one more Stage 6 session with the trace on, naming extra mobs in `Spawn Trace Mobs`: `eerieentities:cursed_armor`, `minecraft:stray`, `thaumcraft:cultistknight`, `crimsonrevelations:cultist_archer`, `grimoireofgaia:vampire`, `raids:pillager`, an Enderman, plus a mob with a potion row (the Mimic `artifacts:mimic`, the Crimson archer). Include Atum, the Nether, the End, a village by day, a Mineshaft, and a character with and without the stages elite, deadly, dragon and crimsoncult.
2. Check in that trace: "Tweaks decided X" equals "table X" on every check; no cancelled-at-join mobs stay; each watched mob's `apply` line shows the expected multipliers, potions and held items; no `WARN` lines about unknown items or potions.
3. Chase the vindicators with no spawn check (add `raids:pillager` and the other illagers to the watch list; look at the `context=` and `join` lines near each cluster).
4. When a session is clean: delete the taken-over rules from `spawn.json` (keep `loot.json` and In Control), and record it in `docs/changelog.md`.
5. Resolve the open cultist-timing question.

## Working notes for the next assistant

- Commit and push to `experimental` after each change; the user rebuilds from there. Commit messages end with the attribution lines the session's system reminder gives.
- Say plainly what is untested: the game-facing code compiles but only the plain-Java parts have tests. Do not claim a behaviour was verified in game unless a trace shows it.
- The scratchpad (`/tmp/claude-0/.../scratchpad`) is per session. Uploaded logs live under `/root/.claude/uploads/<session>/`; analyse large logs with grep and small scripts, not by reading them whole.
- `/ctrldebug` toggles In Control's own debug logging (`Join Rule N: ...`, `Rule N: ...`).

## Resume prompt

```
We are continuing the Arcana Quest Tweaks spawning rollout (repo /home/user/arcanaquesttweaks, branch experimental). Read docs/spawning-handoff.md first, then docs/spawning-rollout.md and docs/spawn-tables-format.md. Stages 0 to 5 are done. Stage 6 (Spawn Rules Decide, Tweaks decides conditions, modifiers and gear and In Control's spawn.json rules are skipped) has had one in-game session; the handoff lists what it showed and what is unverified. Today's job: read the newest spawn_trace.log and latest.log I attach from a Stage 6 session (rebuilt from the latest commit), check them against the "what to check" list in the handoff, fix anything that is actually wrong, and tell me plainly what is verified and what is not. Gradle does not work in the sandbox; use the javac workflow described in the handoff. Do not change my pack files; propose pack changes and ask first.
```
