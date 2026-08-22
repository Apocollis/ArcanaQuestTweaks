# Build and release (1.6)

Last updated: 2026-08-22.

How to compile and deploy `aqtweaks`. Parent jar contract: [compatibility-matrix.md](compatibility-matrix.md). In-game smoke: [verification.md](verification.md).

There is **no CI**. The practical harness is the CurseForge **Arcana Quest DEVBOX** instance.

## Prerequisites

- **JDK 25** (Gradle toolchain `JavaLanguageVersion.of(25)`). Compile emits **Java 21** class files (`options.release = 21`). The deploy script assumes `C:\Program Files\Zulu\zulu-25`. The game still **runs** on Zulu 25.
- Repo root `C:\dev\ArcanaQuestTweaks` (or a clone with `gradlew.bat`).
- `libs/` containing the compile parents. Gradle is `modCompileOnly files(each jar in libs/)`. Missing Depths / RTG / BC / Bewitchment / … will fail compile or produce a jar that crashes on mixin apply.
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
| `build/libs/ArcanaQuestTweaks-1.6.jar` | **Ship this** (remapped, `defaultRemapJar = true`) |
| `build/libs/ArcanaQuestTweaks-1.6-dev.jar` | MCP/dev classifier — **do not** drop in `mods/` |

Skip `*sources*` / `*javadoc*` if present.

If `libs/` is empty or incomplete, this still “works” only insofar as Gradle compiles what it finds. That is the unpinned-libs risk.

## Build and deploy (this machine)

```text
.\build_gradle.ps1
```

What it does:

1. Ensures `libs/`.
2. Deletes known-stale jars (`ElenaiDodge2-1.12.2-1.1.0`, `RecurrentComplexVolts-1.12.2-2.0.0.7`).
3. For each name in `$deps`, copies DEVBOX `mods\<name>` → `libs\` **only if the file exists** (silent skip).
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

Forge `@Config` files under `config/arcanaquesttweaks/` **keep saved values** when Java defaults change. Comfort is JSON (`aqtweaks_comfort.json`) loaded in preInit. After a default change (example: RTG coast buffer 32 → 16), edit or delete the old key in the instance cfg.

In-game cfg change: `ConfigChangedEvent` → `ConfigManager.sync` + `DssSkillCosts.invalidate()`.

## Agent / human workflow

1. Plan in `implementation_plan.md`, wait for `proceed`.
2. Implement.
3. Default: run `.\build_gradle.ps1` (deploy). Skip only if told not to rebuild (docs-only, etc.).
4. Worldgen: test **new chunks**.

Version stays **1.6** unless a plan bumps `ArcanaQuestTweaks.VERSION` and `build.gradle` `version`.
