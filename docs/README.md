# Arcana Quest Tweaks — Architecture & Module Documentation

This directory contains technical design notes, architecture decisions, integration hooks, and "do not regress" rules for all modules in Arcana Quest Tweaks (`aqtweaks`).


Mod: `aqtweaks` 1.6. Minecraft 1.12.2 / CleanroomMC / Forge. Stay on **1.6** unless asked to bump.

`aqtweaks` is a **tweak layer**. Parent mods still own their systems. Tweaks listens to Forge events, calls public APIs (`FeathersHelper`, Thaumcraft warp caps, Bewitchment `Ritual`), or mixins parent methods when events are not enough.

## Module docs

Each file covers: what Tweaks changes, how the **parent mod** implements the feature, and how Tweaks hooks it.

| Module | Parent mod(s) | Doc |
| --- | --- | --- |
| Stamina | Elenai Dodge 2 Extended (`elenaidodge2`), Grappling Hook, Dynamic Sword Skills, Embers, Reskillable, Simple Difficulty, Spartan Weaponry | [stamina.md](stamina.md) |
| Grimoire of Gaia | Grimoire of Gaia (`gaia`) | [grimoire-of-gaia.md](grimoire-of-gaia.md) |
| Thaumcraft | Thaumcraft 6 | [thaumcraft.md](thaumcraft.md) |
| Bewitchment | Bewitchment + Thaumcraft | [bewitchment.md](bewitchment.md) |
| Comfort | Vanilla + optional Thaumcraft, Simple Difficulty, Biomes O' Plenty | [comfort.md](comfort.md) |
| Depths | Depths Update, YUNG's Better Caves, RTG, CoFH World, Recurrent Complex | [depths.md](depths.md) |
| RTG | Realistic Terrain Generation + vanilla `MapGenVillage` + Recurrent Complex | [rtg.md](rtg.md) |
| Recipes | Forge `CraftingHelper` (Metallurgy / Spartan JSON) | [recipes.md](recipes.md) |

## How modules load

- **Hard dependency:** `required-after:elenaidodge2` in `@Mod`. Stamina always loads.
- **Soft:** `after:grimoireofgaia;after:thaumcraft;after:bewitchment;after:grapplemod;after:embers`. Event handlers register only if the mod is present where noted.
- **Late mixins** (`AQTweaksLateMixinLoader` / MixinBooter): `mixins.aqtweaks.json` (required); `mixins.aqtweaks.grapple.json`, `mixins.aqtweaks.dss.json`, `mixins.aqtweaks.astral.json`, `mixins.aqtweaks.bewitchment.json`, and `mixins.aqtweaks.mysticalworld.json` (`required: false` so missing parent mods do not crash).

`CommonProxy.init` registers stamina, Gaia, comfort always; Thaumcraft and Bewitchment only if loaded. Client also registers stamina HUD and Depths fog.

## Workflow (always)

1. Investigate read-only.
2. Write `implementation_plan.md`, also put the plan in chat.
3. Wait for explicit `proceed`.
4. Implement, then `.\build_gradle.ps1` (Java 25; copies the remapped jar to workspace `mods` and CurseForge Arcana Quest DEVBOX). Skip rebuild only if told not to.

Worldgen changes apply to **new chunks only**.
