# Patchouli (`patchouli`)

Pack parent: `Patchouli-1.0-28.jar` (compile-hard, in `libs/`; see [compatibility-matrix.md](compatibility-matrix.md)). `@Mod` has `after:patchouli`.

## Inventory sidebar tab

`patchouli/PatchouliInventoryTabHandler` (client, registered in `ClientProxy.init` after the Comfort tab, only if `patchouli` is loaded) replaces Patchouli's small inventory guidebook button with a **32x28 tab** on the left of the survival inventory.

- **What it swaps:** on `GuiScreenEvent.InitGuiEvent.Post` (LOWEST) it finds Patchouli's `GuiButtonInventoryBook` in the button list (the button Patchouli puts where the recipe book button was, when `Inventory Button Book` is set in `patchouli.cfg`) and replaces it, at the same list index and with the same button id, by a subclass. Patchouli's own `ActionPerformedEvent.Pre` handler matches any `GuiButtonInventoryBook`, so it still opens the book and cancels the vanilla recipe book click. If the button is absent, nothing changes (one log line).
- **Look:** `aqtweaks:textures/gui/tab_left.png` (the same texture as the Reskillable and Comfort tabs, so the Stoneborn override in `resources/aqtweaks/textures/gui/tab_left.png` restyles it too), the book item centered at `x + 11, y + 6`, and Patchouli's own unread/pending marker when the book has one. Hovering shows the book name, "New or unread entries" when the marker shows, and "Click to open". The tab hides while the vanilla recipe book drawer is open.
- **Slot:** Location 1 is the slot where Reskillable's Abilities tab would sit, between Skills and the Comfort tab (the pack never fills it). The Reskillable tab list is read directly, and a Reskillable tab only counts as occupying a slot when `TabType.shouldRender()` is true (`enabled` is not updated until the first draw). If another visible button overlaps it, the tab goes directly below the lowest tab in the left column (Location 2, below the Comfort tab, `ComfortToggleButton.BUTTON_ID`).
- **Not affected:** other screens; creative inventory; the in-book GUI.

## Config (`aqtweaks_patchouli.cfg`, `PatchouliModuleConfig`, category `Inventory Tab`)

| Key | Default | Use |
| --- | --- | --- |
| Move Inventory Button To Sidebar | `true` | False keeps Patchouli's own button |
| Inventory Tab Offset X | `0` | Pixels added to the tab X |
| Inventory Tab Offset Y | `0` | Pixels added to the tab Y |

## Verify

Open the inventory with Patchouli's `Inventory Button Book` set: the crafting-grid book button is gone, the tab sits between the Skills tab and the Comfort tab with the book icon, shows the pulsing marker when there are unread entries, the tooltip names the book, and a click opens it. Set `Move Inventory Button To Sidebar` false or clear `Inventory Button Book`: stock behavior.
