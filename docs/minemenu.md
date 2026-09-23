# MineMenu module (1.8)

Last updated: 2026-09-23.

Config: `config/arcanaquesttweaks/aqtweaks_minemenu.cfg`. Client-only vanilla mixins. No MineMenu jar, no MineMenu types, no `@Mod` dependency.

## Locked intent

Stop mouse look while any GUI is open, and re-grab the cursor when play resumes, so non-pausing screens (MineMenu radial, Reskillable, Hwyla) cannot yaw the player or leave a recenter snap. Do not mixin MineMenu, Reskillable, or Hwyla for this.

## How vanilla does it

`EntityRenderer.updateCameraAndRender` calls `EntityPlayerSP.turn` when `inGameHasFocus` and the display is active. It does not check `currentScreen`. A screen that leaves focus set still turns the head.

`Minecraft.displayGuiScreen(null)` calls `setIngameFocus`, which grabs the mouse once when focus was false. `MouseHelper.grabMouseCursor` recenters via LWJGL. The warp stays in `Mouse.getDX` / `getDY` until the next `mouseXYChange`, and that delta is applied as yaw. Mods that ungrab without clearing focus skip the vanilla grab.

## Design plan

`Fix GUI Mouse Grab` default true.

- `MixinEntityRendererMouse` (`mixins.aqtweaks.json` client, with `MixinRenderGlobal`): `@Redirect` both `EntityPlayerSP.turn(FF)V` invokes in `updateCameraAndRender`. Screen open → return. Screen closed → `player.turn`. That frame’s `mouseXYChange` delta is dropped with the skipped call.
- `MixinMinecraftMouseGrab` (`mixins.aqtweaks.early.json` **client** array): `@Inject` `displayGuiScreen` at RETURN. `Minecraft` is already loaded when late mixins prepare; a late inject fails boot. Dedicated server does not load the `client` array. When `currentScreen` is null, world and player exist, and `Display.isActive()`, call `mouseHelper.grabMouseCursor()`, then `Mouse.getDX()` and `Mouse.getDY()`. Do not call `setIngameFocus` (it calls `displayGuiScreen(null)` again).

Flag false: both mixins no-op (stock focus).

Overlays that never set `currentScreen` and never call `displayGuiScreen` are unchanged. Smooth-camera accumulators are not cleared.

## Files

| Piece | Role |
| --- | --- |
| `ArcanaQuestTweaksConfig.MineMenuModuleConfig` | `aqtweaks_minemenu.cfg` |
| `mixin/MixinEntityRendererMouse.java` | Skip `turn` while a screen is open. Late client json |
| `mixin/MixinMinecraftMouseGrab.java` | Re-grab and drain DX/DY on return to play. Early client json |

## Live config

| Knob | Default | Notes |
| --- | --- | --- |
| Fix GUI Mouse Grab | true | Skip look while a screen is open; re-grab and drop the recenter delta on close |

## Do not regress

- Do not late-mixin `Minecraft`. If boot says `EntityRenderer was loaded too early`, move `MixinEntityRendererMouse` into the early json `client` array.
- Do not call `setIngameFocus` from the close inject.
- Inventory and pause still ungrab through vanilla `displayGuiScreen`. This only adds a grab plus a delta drain on the return to a null screen.
- No MineMenu compile dependency.

## Verify

Hold MineMenu (and open a Reskillable screen): yaw and pitch stay put while the screen is up. After close, the cursor is grabbed and the camera does not jump. Inventory and Esc pause ungrab, then regrab on close with no snap. `fixGuiMouseGrab` false (restart): stock look. Boot log must not say `Minecraft was loaded too early` or `EntityRenderer was loaded too early`.
