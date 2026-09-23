# MineMenu module (1.8)

Last updated: 2026-09-23.

Config: `config/arcanaquesttweaks/aqtweaks_minemenu.cfg`. Client-only vanilla mixins. No MineMenu jar, no MineMenu types, no `@Mod` dependency.

## Locked intent

Stop mouse look while any GUI is open, and force the cursor back to the crosshair when play resumes, so non-pausing screens (MineMenu radial, Reskillable, BetterQuesting, Hwyla config) cannot yaw the player or leave the pointer offset. MineMenu keybind entries for DSS and Baubles open those menus. Do not mixin MineMenu, Reskillable, Hwyla, or BetterQuesting. The Baubles inject only marks a packet Baubles already sent.

## How vanilla does it

`EntityRenderer.updateCameraAndRender` calls `EntityPlayerSP.turn` when `inGameHasFocus` and the display is active. It does not check `currentScreen`. A screen that leaves focus set still turns the head.

`Minecraft.displayGuiScreen(null)` calls `setIngameFocus`, which grabs the mouse once when focus was false, then calls `displayGuiScreen(null)` again before the outer call returns. Cleanroom `Mouse.setGrabbed(true)` returns immediately when the Java grabbed flag is already true, and it does not move the cursor before `GLFW_CURSOR_DISABLED`. `Mouse.setCursorPosition` no-ops while grabbed. Running ungrab → center → grab on both returns leaves the pointer off the crosshair. Reskillable `GuiSkills`, BetterQuesting `GuiScreenCanvas.confirmClose`, and Hwyla `ScreenBase` then call `setIngameFocus` again; focus is already true, so that call does not grab. A container close does not make that extra call. The recenter warp can also arrive a frame later and become yaw.

Baubles `ClientEventHandler.onKeyInput` returns if a screen is open, then sends `PacketOpen(Option.EXPANSION)` when `KEY_BAUBLES.isKeyDown()` (the stuck `pressed` flag, not `isPressed()`). MineMenu sets that flag and `pressTime`, then posts `KeyInputEvent`, so an assigned bind is sent by Baubles. An unbound bind does not set the flag (`setKeyBindState(0)` returns immediately). `EXPANSION` closes the current screen and opens Baubles GUI id 0.

## Design plan

`Fix GUI Mouse Grab` default true.

- `MixinEntityRendererMouse` (`mixins.aqtweaks.json` client, with `MixinRenderGlobal`): `@Redirect` both `EntityPlayerSP.turn(FF)V` invokes in `updateCameraAndRender`. Screen open → return. `GuiMouseGrab.consumeLook()` → return for two camera frames after close (drops the warp). Otherwise `player.turn`.
- `MixinMinecraftMouseGrab` (`mixins.aqtweaks.early.json` **client** array): `@Inject` `displayGuiScreen` at HEAD and RETURN. `Minecraft` is already loaded when late mixins prepare; a late inject fails boot. Dedicated server does not load the `client` array. A depth count skips the nested `setIngameFocus` re-entry. Only the outermost return, when `currentScreen` is null, world and player exist, and `Display.isActive()` (`org.lwjglx`): `Mouse.setGrabbed(false)`, `setCursorPosition` to the window center, drain DX/DY, `mouseHelper.grabMouseCursor()`, drain again, `inGameHasFocus = true`, `GuiMouseGrab.armSuppress()`. Do not call `setIngameFocus`.
- `BaublesMenuClient` from `ClientProxy` init. `@Mod` `after:baubles`. Client tick END: `KEY_BAUBLES.isPressed()` and Baubles did not already send → `PacketHandler.INSTANCE.sendToServer(new PacketOpen(Option.EXPANSION))`. `MixinClientEventHandler` (`mixins.aqtweaks.baubles.json` client, `required: false`) sets that flag at `onKeyInput`'s `sendToServer`. Do not call `Keyboard.isKeyDown`.

Flag false: both mouse mixins no-op (stock focus). The Baubles tick still runs.

Overlays that never set `currentScreen` and never call `displayGuiScreen` are unchanged. Smooth-camera accumulators are not cleared.

## Files

| Piece | Role |
| --- | --- |
| `ArcanaQuestTweaksConfig.MineMenuModuleConfig` | `aqtweaks_minemenu.cfg` |
| `client/GuiMouseGrab.java` | Two-frame look suppress after close |
| `mixin/MixinEntityRendererMouse.java` | Skip `turn` while a screen is open, and during the suppress window. Late client json |
| `mixin/MixinMinecraftMouseGrab.java` | Ungrab, center, re-grab on the outermost return to play. Early client json |
| `client/BaublesMenuClient.java` | `isPressed()` → `PacketOpen(EXPANSION)` when Baubles did not already send. Registered from `ClientProxy` |
| `mixin/baubles/MixinClientEventHandler.java` | Marks `onKeyInput` when it sends `PacketOpen`. Optional client json |

## Live config

| Knob | Default | Notes |
| --- | --- | --- |
| Fix GUI Mouse Grab | true | Skip look while a screen is open; force-center and re-grab on close; drop two frames of look |

## Do not regress

- Do not late-mixin `Minecraft`. If boot says `EntityRenderer was loaded too early`, move `MixinEntityRendererMouse` into the early json `client` array.
- Do not call `setIngameFocus` from the close inject.
- Inventory and pause still ungrab through vanilla `displayGuiScreen`. The outermost close forces ungrab, center, then grab, and ignores look for two frames.
- No MineMenu compile dependency. BaublesEX is compile-hard (`KeyBindings`, `PacketHandler`, `PacketOpen`, `ClientEventHandler`).

## Verify

Hold MineMenu, then open Reskillable, BetterQuesting, or the Hwyla config: yaw and pitch stay put while the screen is up. Move the cursor to a corner, then close: the pointer is grabbed at center and the camera does not jump; the next mouse move yaws from that center. Inventory, Baubles, and Esc pause do the same. `fixGuiMouseGrab` false (restart): stock look. Boot log must not say `Minecraft was loaded too early` or `EntityRenderer was loaded too early`. MineMenu keybind for Baubles opens the expanded GUI once; the real Baubles key still opens it once. DSS Skills GUI: [stamina.md](stamina.md).
