# Build and release (1.8)

Last updated: 2026-09-20.

How to compile and deploy `aqtweaks`. Parent jar contract and **CurseForge relations**: [compatibility-matrix.md](compatibility-matrix.md). In-game smoke: [verification.md](verification.md).

There is **no CI**. The practical harness is the CurseForge **Arcana Quest DEVBOX** instance. `gradlew build` / `check` runs `verifyReleaseJar` (class major **65**, `VillagePlate.class` present, mixin json + shared refmap in the remapped jar).

## Prerequisites

- **JDK 25** (Gradle toolchain `JavaLanguageVersion.of(25)`). Compile emits **Java 21** class files (`options.release = 21`, class major **65**). The deploy script assumes `C:\Program Files\Zulu\zulu-25`. The game still **runs** on Zulu 25.
- **Do not** compile `--release 8`. Source uses records / `var` / `Set.of`; mixin json is `JAVA_21`. Mixin/Fugue refuse class version **66+** (Java 22), not 65. Legacy `build.ps1` still passes `--release 8` and must not ship.
- Repo root `C:\dev\ArcanaQuestTweaks` (or a clone with `gradlew.bat`).
- `libs/` containing the compile parents (gitignored; `build_gradle.ps1` copies them from DEVBOX). Gradle is `modCompileOnly files(each jar in libs/)`. Missing Depths / RTG / BC / Bewitchment / Thaumcraft / … will fail compile or produce a jar that crashes on mixin apply. Do not commit those jars.
- Pack mods folder for deploy (script only): `c:\Users\hughe\curseforge\minecraft\Instances\Arcana Quest DEVBOX\mods`

No environment-variable overrides exist today. `JAVA_HOME` and the DEVBOX path are **hardcoded** in `build_gradle.ps1`.

## Build only (portable)

From the repo root:

```text
.\gradlew.bat build
```

Does **not** copy from DEVBOX, does **not** set Zulu, does **not** deploy, does **not** delete old Tweaks jars.

Output:

| File | Use |
| --- | --- |
| `build/libs/ArcanaQuestTweaks-1.8.jar` | **Ship this** (remapped, `defaultRemapJar = true`) |
| `build/libs/ArcanaQuestTweaks-1.8-dev.jar` | MCP/dev classifier — **do not** drop in `mods/` |

Skip `*sources*` / `*javadoc*` if present.

If `libs/` is empty or incomplete, this still “works” only insofar as Gradle compiles what it finds. That is the unpinned-libs risk.

## Build and deploy (this machine)

```text
.\build_gradle.ps1
```

What it does:

1. Ensures `libs/`.
2. Deletes known-stale jars (`ElenaiDodge2-1.12.2-1.1.0`, `RecurrentComplexVolts-1.12.2-2.0.0.7`, `BaublesEX-1.12.2-2.3.5`, `WearableBackpacks-RLCraft-1.12.2-3.2.7`, `RoguelikeDungeons-Arcana-1.12.2-2.5.0`).
3. For each name in `$deps`, copies DEVBOX `mods\<name>` → `libs\` **only if the file exists** (silent skip). Then extracts nested McJtyTools from InControl into `libs/mcjtytools-1.12-0.0.21.jar`.
4. Sets `JAVA_HOME` to Zulu 25.
5. Runs `.\gradlew.bat build`.
6. Picks the newest `ArcanaQuestTweaks-*.jar` in `build/libs` whose name does **not** match `sources|javadoc|dev`.
7. **Deletes** every `ArcanaQuestTweaks-*.jar` in workspace `mods\` and DEVBOX `mods\`.
8. Copies that jar to both folders.

This **will overwrite** the instance Tweaks jar. Close the game first.

Copy-list holes and the Roguelike 2.5.0 vs 2.5.3 filename mismatch: [compatibility-matrix.md](compatibility-matrix.md).

## DEVBOX JVM (Cleanroom relauncher)

Instance file: `config/relauncher.json` (not shipped in the Tweaks jar). Boot crash on TC6 Aspects / Thaumcraft recipe-tag scan needs:

```text
-XX:-UseCompactObjectHeaders -XX:CompileCommand=exclude,thaumcraft.common.lib.crafting.ThaumcraftCraftingManager::generateTagsFromCraftingRecipes
```

Keep Zulu 25. Do **not** use `-XX:+UseCompactObjectHeaders`. `build_gradle.ps1` does not patch this file.

## Config on update

Forge `@Config` files under `config/arcanaquesttweaks/` **keep saved values** when Java defaults change. Comfort is two JSON files (`aqtweaks_comfort_settings.json`, `aqtweaks_comfort_blocks.json`) loaded in preInit. After a default change (example: RTG coast buffer 32 → 16), edit or delete the old key in the instance cfg.

In-game cfg change: `ConfigChangedEvent` → `ConfigManager.sync` + `normalizePinned()` + `DssSkillCosts.invalidate()` + spawn-type / structure-spawn / party / tier JSON reload + spawn-rules and group-size invalidate + Reskillable attribute restamp if loaded.

## Agent / human workflow

1. Plan in `.cursor/plans/YYYYMMDD-HHmm-<kebab-task>.md` (never repo-root `implementation_plan.md`), wait for `proceed`. Delete that file when the work is finished.
2. Implement.
3. Default: run `.\build_gradle.ps1` (deploy). Skip only if told not to rebuild (docs-only, etc.).
4. Worldgen: test **new chunks**.

Version stays **1.8** unless a plan bumps `ArcanaQuestTweaks.VERSION` and `build.gradle` `version`.
