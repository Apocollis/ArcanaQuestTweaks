# Changelog (1.8)

Stay on version **1.8** until a plan bumps `ArcanaQuestTweaks.VERSION`.

## 2026-09-25 — Thermia cap kill and slow recovery

- Hypothermia and hyperthermia still cannot push the Elenai max below 1. The next penalty step at the config base drains the last half-feather and kills with that Simple Difficulty source. After the potion ends, the cap returns one half-feather every 10 ticks. Spec: [stamina.md](stamina.md).

## 2026-09-25 — Rancher clock throttle and perk lookup

- Rancher still doubles Animania clocks, applied as 20 steps once per second. Gestation stops at 1 so birth still happens. Armor weight, perk checks, gold feathers, and Simple Difficulty thirst use cached or compile-hard calls. Climb jump packets send on change. Spec: [reskillable.md](reskillable.md), [stamina.md](stamina.md).

## 2026-09-24 — Dark Vision flat mix and sight

- Dark Vision holds the lightmap at 0.4 everywhere the perk is on. Dynamic Stealth treats that player as having night vision. Spec: [reskillable.md](reskillable.md).

## 2026-09-24 — Prospecting pick outline, Dark Vision curve

- Prospector outlines ores a Prospectus pick counted, for 7 seconds, and raises that pick's accuracy by 25. Every pick uses the Tweaks base chances. Dark Vision mixes at 0.8 in pitch dark and eases over 5 ticks. Spec: [reskillable.md](reskillable.md).

## 2026-09-23 — Benevolent, Prospector outline, Dark Vision mix

- Benevolent doubles a Heal focus on any other living target. Prospector draws ore boxes for the mining player. Dark Vision mixes the client lightmap up to 0.7 instead of applying Night Vision. Spec: [reskillable.md](reskillable.md).

## 2026-09-23 — Reskillable perk wishlist

- New perks for mining, gathering, farming, attack, defense, and agility, plus the cost pass on the existing trees. Magic schools require Magic 20 and restamp their `not|trait|` locks after every school is registered. Spec: [reskillable.md](reskillable.md).

## 2026-09-23 — TF/Aether portal sits on grass

- RandomPortals vertical dest frames were one block in the surface (bottom row replaced grass). Tweaks now raises them so the bottom row rests on the grass/island. See [twilightforest.md](twilightforest.md) and [aether.md](aether.md).

## 2026-09-23 — GUI close once, DSS unbound key

- Mouse re-grab runs only on the outermost `displayGuiScreen` return, so Reskillable, BetterQuesting, and Hwyla config close with the pointer on the crosshair. DSS ignores key 0, so an unbound skills bind does not open on space. Assigned DSS and Baubles keys no longer call `Keyboard.isKeyDown`. See [minemenu.md](minemenu.md) and [stamina.md](stamina.md).

## 2026-09-23 — GUI mouse recenter and Baubles key

- Closing a screen forces the cursor free, centers it, then grabs again, and skips look for two camera frames so the warp is not yaw. MineMenu’s Baubles entry polls `isPressed()` and sends `PacketOpen(EXPANSION)` when the hardware key is up. See [minemenu.md](minemenu.md).

## 2026-09-23 — DSS skills GUI from MineMenu

- Client tick polls DSS Skills GUI `KeyBinding.isPressed()` and sends `OpenGuiPacket(0)` when the hardware key is not down. Client `/dssgui` sends the same packet. Real key presses stay on DSS `KeyInputEvent`. See [stamina.md](stamina.md).

## 2026-09-23 — MineMenu mouse grab

- Client mixins skip `EntityPlayerSP.turn` while any screen is open, and re-grab plus drain the LWJGL recenter delta when `displayGuiScreen` returns to play. `Minecraft` inject is in the early json client array. Spec: [minemenu.md](minemenu.md).

## 2026-09-21 — Reskillable Magic schools

- Four mutex Magic schools (Druid, Witch, Astromancer, Artificer) plus two follow-ups each. Thrift ×0.70 on the school perk. Spec: [reskillable.md](reskillable.md). Parent mixins stay in existing json where those modules already exist; Botania/Embers get optional json under this module.

## 2026-09-21 — Aether portal island landing

- RandomPortals dest dim 4 snaps onto aether grass/dirt/holystone before search/build (200 then 400). Mixin rejects void pads. Linked return portals unchanged. See [aether.md](aether.md).

## 2026-09-21 — Dawnstone same-tier reroll

- Common/Uncommon/Rare/Legendary also reroll yellow/green/blue/gold when the piece is already that tier. Ladder upgrades unchanged. Duplicate `cfg` string compare no longer gates Uncommon+.

## 2026-09-21 — Dawnstone rune registry names

- Crafting Runes 1.1 ids are `sccraftingrunes:itemcommonmat` (and uncommon/rare/legendary). Tweaks defaults and lookup now use those; old `*_mat` cfg values still resolve. Place the tool first, then the rune.

## 2026-09-20 — Quality Tools wear/break skips

- Do not cache `isQualityItem` before QT types load (crafted tools were stuck `not_quality`). Never cache a false miss; a live Quality tag counts as eligible. Armor `attemptDamageItem` with a null player still stamps wear/Broken in-slot.

## 2026-09-20 — Quality Tools wear chance

- Wear `gray` is no longer a flat 100% on the first eligible 40-tick hit. p = used × (250 / (max/2)), clamped to 0.05–0.50 (`wearChance` 0 still disables). Stone/iron sit on the ceiling; diamond stays below it.

## 2026-09-20 — Quality Tools Module

- Loot/drops stamp Quality Tools tags before pickup; crafted gear stays untagged (living-update first roll skipped). Wear `gray` at ≤20% remaining (not on Broken); break without Salvage drops `dark_gray` and keeps QualityBase. Dawnstone Anvil upgrades with Crafting Runes on a strict color ladder (rune consumed). Pack `tools.json` still needs a `dark_gray` Broken entry.

## 2026-09-20 — Game Stages / Chisel

- **Chisel** crafts (GUI `SlotChiselSelection.craft` and in-world `ItemChisel.canChisel`) refuse Recipe-Staged outputs unless `GameStageHelper.hasStage` is true. Pack allowlist: `apprentice_builder`, `experienced_builder`, `master_builder`. Red action bar `chat.aqtweaks.gamestages.chisel_locked`. Optional mixin json; AutoChisel and Chisels and Bits unchanged.

## 2026-09-20 — release hardening

- **Java target stays 21** (`options.release = 21`, class major 65). Do not `--release 8`. Mixin json is `JAVA_21`. Fugue refuses 66+ only.
- **Village unload:** `VillagePlate` is loaded when village Events register so Forge’s `EventSubscriptionTransformer` does not ClassReader empty bytes during `stopServer`.
- **Charm** is `@Mod required-after` (Curse / pack prerequisite). `mixins.aqtweaks.charm.json` is `required: true` on jar `MixinConfigs` with the early json; compile-hard `ASMHooks`. Isolated boot without Charm is expected to fail.
- Shared `mixins.aqtweaks.refmap.json` is packaged; mixin configs point at it (silences missing per-json refmap warnings).
- **Evasion:** drop unresolvable CAD `trait|` rows such as instance `trait|elenaidodge2:dodge`. Requirement is agility 16.
- Gradle `verifyReleaseJar` (via `check`): remapped jar has `VillagePlate`, mixin json, refmap, and every class major 65.
