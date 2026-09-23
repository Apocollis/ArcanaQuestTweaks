# MineMenu module (1.8)

Last updated: 2026-09-23.

Config: `config/arcanaquesttweaks/aqtweaks_minemenu.cfg`. Client-only vanilla mixins. No MineMenu jar, no MineMenu types, no `@Mod` dependency.

## Locked intent

Stop mouse look while any GUI is open, and force the cursor back to the crosshair when play resumes, so non-pausing screens (MineMenu radial, Reskillable, Hwyla) cannot yaw the player or leave the pointer offset. MineMenu keybind entries for DSS and Baubles open those menus. Do not mixin MineMenu, Reskillable, Hwyla, or Baubles input.

## How vanilla does it

`EntityRenderer.updateCameraAndRender` calls `EntityPlayerSP.turn` when `inGameHasFocus` and the display is active. It does not check `currentScreen`. A screen that leaves focus set still turns the head.

`Minecraft.displayGuiScreen(null)` calls `setIngameFocus`, which grabs the mouse once when focus was false. Cleanroom `Mouse.setGrabbed(true)` returns immediately when the Java grabbed flag is already true, and it does not move the cursor before `GLFW_CURSOR_DISABLED`. `Mouse.setCursorPosition` no-ops while grabbed. A second `grabMouseCursor()` after vanilla focus is often a no-op, so the menu cursor stays locked off the crosshair. The recenter warp can also arrive a frame later and become yaw.

Baubles `ClientEventHandler.onKeyInput` runs only on `KeyInputEvent`. It returns if a screen is open, then sends `PacketOpen(Option.EXPANSION)` when `KEY_BAUBLES.isKeyDown()` (the stuck `pressed` flag, not `isPressed()`). MineMenu sets key state and `pressTime` and does not post that event. `EXPANSION` closes the current screen and opens Baubles GUI id 0.

## Design plan

`Fix GUI Mouse Grab` default true.

- `MixinEntityRendererMouse` (`mixins.aqtweaks.json` client, with `MixinRenderGlobal`): `@Redirect` both `EntityPlayerSP.turn(FF)V` invokes in `updateCameraAndRender`. Screen open → return. `GuiMouseGrab.consumeLook()` → return for two camera frames after close (drops the warp). Otherwise `player.turn`.
- `MixinMinecraftMouseGrab` (`mixins.aqtweaks.early.json` **client** array): `@Inject` `displayGuiScreen` at RETURN. `Minecraft` is already loaded when late mixins prepare; a late inject fails boot. Dedicated server does not load the `client` array. When `currentScreen` is null, world and player exist, and `Display.isActive()` (`org.lwjglx`): `Mouse.setGrabbed(false)`, `setCursorPosition` to the window center, drain DX/DY, `mouseHelper.grabMouseCursor()`, drain again, `inGameHasFocus = true`, `GuiMouseGrab.armSuppress()`. Do not call `setIngameFocus` (it calls `displayGuiScreen(null)` again).
- `BaublesMenuClient` from `ClientProxy` init. `@Mod` `after:baubles`. Client tick END: `KEY_BAUBLES.isPressed()` and the hardware key is up → `PacketHandler.INSTANCE.sendToServer(new PacketOpen(Option.EXPANSION))`. A real key press stays on Baubles `onKeyInput`; `isPressed()` consumes `pressTime` so a second packet is not sent. Do not poll `isKeyDown()`.

Flag false: both mouse mixins no-op (stock focus). The Baubles tick still runs.

Overlays that never set `currentScreen` and never call `displayGuiScreen` are unchanged. Smooth-camera accumulators are not cleared.

## Files

| Piece | Role |
| --- | --- |
| `ArcanaQuestTweaksConfig.MineMenuModuleConfig` | `aqtweaks_minemenu.cfg` |
| `client/GuiMouseGrab.java` | Two-frame look suppress after close |
| `mixin/MixinEntityRendererMouse.java` | Skip `turn` while a screen is open, and during the suppress window. Late client json |
| `mixin/MixinMinecraftMouseGrab.java` | Ungrab, center, re-grab on return to play. Early client json |
| `client/BaublesMenuClient.java` | `isPressed()` → `PacketOpen(EXPANSION)`. Registered from `ClientProxy` |

## Live config

| Knob | Default | Notes |
| --- | --- | --- |
| Fix GUI Mouse Grab | true | Skip look while a screen is open; force-center and re-grab on close; drop two frames of look |

## Do not regress

- Do not late-mixin `Minecraft`. If boot says `EntityRenderer was loaded too early`, move `MixinEntityRendererMouse` into the early json `client` array.
- Do not call `setIngameFocus` from the close inject.
- Inventory and pause still ungrab through vanilla `displayGuiScreen`. Close forces ungrab, center, then grab, and ignores look for two frames.
- No MineMenu compile dependency. BaublesEX is compile-hard (`KeyBindings`, `PacketHandler`, `PacketOpen`).

## Verify

Hold MineMenu (and open a Reskillable screen): yaw and pitch stay put while the screen is up. Move the cursor to a corner, then close: the pointer is grabbed at center and the camera does not jump; the next mouse move yaws from that center. Inventory and Esc pause do the same. `fixGuiMouseGrab` false (restart): stock look. Boot log must not say `Minecraft was loaded too early` or `EntityRenderer was loaded too early`. MineMenu keybind for Baubles opens the expanded GUI once; the real Baubles key still opens it once. DSS Skills GUI: [stamina.md](stamina.md).
