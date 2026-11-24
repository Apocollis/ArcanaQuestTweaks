# Structure titles (Traveler's Titles hook)

Last updated: 2026-11-17.

When a player enters a structure, Traveler's Titles (`travelerstitles`, `TravelersTitles-1.12.2-1.1.0.jar`) shows its name in the same large title it uses for biomes. Traveler's Titles has no structure support of its own, so the server finds the structure and tells the client. A player home shows as `<owner>'s Home`.

Plan: `.plans/archive/20261117-1600-travelers-titles-structure-hook.md`. Homes: [comfort.md](comfort.md#player-homes).

## What shows

* One line, no subtitle. A home shows its formatted name (`home_name_format`, default `%s's Home`); every other structure shows its name from `aqtweaks.structure.<name in lower case>` in `en_us.lang`, or a prettified name when there is no entry (`Desert_Pyramid` becomes "Desert Pyramid").
* A structure title **overrides the biome title**: one already on screen is cleared when the structure title starts, and none starts while it shows. A **player home** also covers the **Waystones title**: one on screen is cleared, and the closest waystone is marked as seen while the home title shows, so no Waystones title or sound follows when it fades. Other structures leave the Waystones title alone. The dimension title is not touched.
* The structure title uses Traveler's Titles' own `TitleRenderer` built from its **biome title** settings (fade in, display, fade out, colour, shadow, size, X offset, Y offset, centring), so it sits where the biome title would.

## Which structures

Checked in this order, first match wins:

1. **Home**: `HomeRegistry.structureAt(world, pos)`, the cached player home (see comfort.md).
2. **`RoguelikeDungeon`**: Roguelike Dungeons ships a mixin on vanilla's structure test that answers this name from its own saved data.
3. The vanilla names: `Village`, `Mineshaft`, `Mansion`, `Stronghold`, `Monument`, `Fortress`, `EndCity`, `Igloo`, `Desert_Pyramid`, `Jungle_Pyramid`, `Swamp_Hut`, `Temple`.
4. `Structure Titles Extra Names` (the names `/locate` takes).

Each name goes through `ChunkProviderServer.isInsideStructure(world, name, pos)`. Only that cheap test runs, never a nearest-structure search, so nothing is located or generated. A world generator that does not answer a name simply never matches it (vanilla 1.12 answers `Temple` for every scattered feature; whether it answers the four specific scattered names depends on the generator).

## Flow

1. `titles/StructureTitleTracker` (server `PlayerTickEvent` END) runs every `Structure Titles Interval Ticks` (default 20), staggered by `ticksExisted + entityId`. It keeps the last structure per player in `StructureTitles.Tracker` (pure, tested).
2. On a change it sends `network/PacketStructureTitle` (server to client, discriminator **16**): `key` (lower-case name or `home`), `title` (the formatted home name, or the structure name in its original case), `subtitle` (kept, unused) and `enter`. Leaving a structure sends the same packet with `enter=false`; the client ignores it.
3. Re-entering the same structure within `Structure Titles Repeat Cooldown Ticks` (default 200) is remembered but not announced, so walking along an edge does not repeat the title.
4. `titles/StructureTitleClient` (no Traveler's Titles types) forwards to `titles/TravelersTitlesStructureHook` only when `travelerstitles` is loaded. The hook creates a `TitleRenderer<String>`, calls `displayTitle(text, "")`, ticks it on `ClientTickEvent` START and draws it on `RenderGameOverlayEvent.Pre` (`ALL`, honouring F1). It clears `TravelersTitles.titleManager.biomeTitleRenderer` and, for a home only, `WaystonesCompat.reset()` when it starts.
5. `mixin/travelerstitles/MixinTitleRenderManagerStructure` (`mixins.aqtweaks.travelerstitles.json`, client, queued only when the mod is loaded, `require = 0`) cancels Traveler's Titles' private `updateBiomeTitle` at HEAD while a structure title is active. `MixinWaystonesCompat` (same json) injects at HEAD of the static `WaystonesCompat.updateWaystoneTitle`: while `TravelersTitlesStructureHook.isHomeActive()` it calls `waystoneTitleRenderer.addRecentEntry(closestWaystone)` and returns false (shadows the private statics `closestWaystone` and `waystoneTitleRenderer`; Waystones is compile-hard).

Without Traveler's Titles on the client the packet is received and ignored. On a dedicated server the tracker still runs and the packets are harmless.

## Settings

Server, `config/arcanaquesttweaks/aqtweaks_titles.cfg`:

| Setting | Default | Meaning |
| --- | --- | --- |
| Structure Titles Enabled | `true` | Run the tracker. |
| Structure Titles Interval Ticks | `20` | Check interval per player (5 to 200). |
| Structure Titles Repeat Cooldown Ticks | `200` | No second title for the same structure inside this window (0 to 12000). |
| Structure Titles Extra Names | empty | Extra structure names to ask the world. |

Client, `config/arcanaquesttweaks/aqtweaks_client.cfg`, category `Structure Titles`:

| Setting | Default | Meaning |
| --- | --- | --- |
| Structure Title Y Offset | `0.0` | Added to the biome title's Y offset. 0 puts it where the biome title it replaces would be. |
| Show Owner Name On Homes | `true` | Off shows plain "Home" (`aqtweaks.structure.home`). |

## Files

- `titles/StructureTitles.java` (pure: name list, keys, language keys, prettify, client title choice, change detection), `StructureTitleTracker.java` (server), `StructureTitleClient.java`, `TravelersTitlesStructureHook.java` (client, compile-hard)
- `network/PacketStructureTitle.java`
- `mixin/travelerstitles/MixinTitleRenderManagerStructure.java`, `mixins.aqtweaks.travelerstitles.json`, `AQTweaksLateMixinLoader`
- `ArcanaQuestTweaksConfig.StructureTitlesConfig`, `ClientModuleConfig.structureTitles`
- `build_gradle.ps1` copies the Traveler's Titles jar into `libs/`
- Test: `StructureTitlesTest` (under `src/test`)
