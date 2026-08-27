# Portal module (1.7)

Last updated: 2026-08-26.

Config: `config/arcanaquesttweaks/aqtweaks_portal.cfg`. Tweaks-owned. No parent portal mod.

## Locked intent

Two items open the same **two-way 60-second** `EntityArcaneRift` pair. **Arcane Tunnel** (`spatial_rift_tear`) **activates** (binds) on first air-use and can open a rift to a **bound dimension**; **Unstable Arcane Tunnel** lands like spreadplayers on solid land in the **current dimension**. Standing owned pets and leashed mobs follow the player; sitting pets stay.

Do **not** instant-teleport the player. Do **not** use vanilla end-portal TESR. Do **not** persist rifts across logout. Do **not** use vanilla nether `Teleporter` (`ITeleporter.isVanilla() == true`).

## How it works

### Items

| Id | Display | Use |
| --- | --- | --- |
| `aqtweaks:spatial_rift_tear` | Arcane Tunnel; **Linked Arcane Tunnel** + glint when bound | Unbound air-use binds feet. Sneak+block binds Y+1. Sneak-air rebinds feet. Bound air-use opens source here and dest at bound XYZ in `BoundDim`. Bind is item NBT (`Bound`, `BoundX/Y/Z`, `BoundDim`). Creative does not consume. |
| `aqtweaks:spatial_rift_wild` | Unstable Arcane Tunnel | Air-use searches random XZ in min–max range, surface Y, reject liquid / leaves / ocean. Same-dimension rift pair. |

Wild `findStandPos` starts at `getHeight` and walks **down** through air, plants, and **leaves** (cap 48) onto **solid + 1** whose two body cells are air or plants **only** (not leaves, not a log in the canopy). Source / bound spawn uses `snapStand`: skip plants at that Y, do **not** fall through air. Failed dest snap does **not** use a solid `destStand` — open fails.

If `MinecraftServer.getWorld(BoundDim)` is null, open fails (`missing_dim`); item kept. Other failures are status messages; item kept. Dest chunk is loaded via `getChunk`; both rifts hold `ForgeChunkManager` tickets until collapse.

### Entity `aqtweaks:arcane_rift`

Lifespan from cfg (default 1200 ticks). `setSize(1.6, 2.4)`, noClip, not saved (`writeToNBTOptional` false). Linked by UUID **and** dest dimension. Synced `wild` flag (unstable pair). Ticket on **each** rift’s own world; released in `setDead()`. Killing one finds the other across loaded worlds and `setDead()`.

Block light **15** at the mid cell (`RiftLighting` + `MixinWorldRiftLight` on `World.getRawLight`); `checkLight` on spawn/move/death. That mixin is in `mixins.aqtweaks.early.json` (jar `MixinConfigs`), not the late Tweaks json — late prepare hits `World` after it is already loaded and crashes boot.

Teleport: AABB overlap. Skip other rifts and **sitting** tamed pets. Players still dismount, companion-pull (radius), remount, re-leash. Everything else in the box (`EntityItem`, villagers, hostiles, standing tames, XP orbs, etc.) `moveToExit`. Then `timeUntilPortal` = cooldown (default 80). Exit XZ is dest + horizontal look × **Exit Offset** (cfg, default 1.5), then the same stand search as wild (solid + 1, two body cells). If that heading is a wall/trunk/hole, try 8 headings at the same radius, then dest feet.

Do **not** `untrack`/`track` dest on arrival. That destroy packet plus Dynamic Stealth **Entity Specific Full Bypass** leaves dest with a server hitbox and light but **no cylinder**. Keep `aqtweaks:arcane_rift` on DS full bypass so dest is not sense-gated. If the client entity drops, baked block light can stay; that is expected. No Tweaks light packet.

Same dimension: `setPlayerLocation` / `setLocationAndAngles`. Cross-dimension: `entity.changeDimension(destDim, RiftTeleporter)` with `isVanilla() == false`. Sitting pets still stay in the origin dimension.

Companion pull (player trips only, same tick, radius cfg 16): `EntityTameable` owned, **not** `isSitting()`; `EntityLiving` leashed to that player. Re-bind leash only for those that were leashed. Fence knots ignored.

### Look and audio

`RenderArcaneRift`: nether-portal texture on a **wobbly cylinder**, **V scrolls upward** (no yaw spin). Vertex alpha **0.75** healthy / **0.25** collapse. Wild pair: red vertex tint. Not End TESR, not a dest camera.

Particles (client `RiftParticles`, **4**/tick): **purple** `DRAGON_BREATH` column (wild **red**); no `PORTAL` motes; no enchantment-table glyphs; **6** `CLOUD` on collapse.

**Open:** `ENTITY_LIGHTNING_THUNDER` and `BLOCK_PORTAL_TRIGGER` at **origin and destination**. **While open:** `BLOCK_PORTAL_AMBIENT` every 40 ticks per rift. **Collapse:** `BLOCK_PORTAL_TRIGGER` on that rift.

## Files

| Piece | Role |
| --- | --- |
| `portal/PortalModule.java` | Registry, chunk callback, dual-world spawn pair, wild land search, companion list |
| `portal/ItemSpatialRiftTear.java` | Bind / open; bound display name |
| `portal/ItemSpatialRiftWild.java` | Random land open |
| `portal/EntityArcaneRift.java` | Lifetime, link, teleport, wild flag, lighting tick |
| `portal/RiftLighting.java` | Glowstone-level emission cells |
| `portal/RiftTeleporter.java` | Cross-dim place at exit pose |
| `mixin/MixinWorldRiftLight.java` | `World.getRawLight` BLOCK 15 at rift cells; `mixins.aqtweaks.early.json` |
| `portal/client/RenderArcaneRift.java` | Wobbly portal cylinder |
| `portal/client/RiftParticles.java` | Dragon-breath column |
| `portal/client/PortalClientEvents.java` | Item models |
| `ArcanaQuestTweaksConfig.PortalModuleConfig.general` | `aqtweaks_portal.cfg` |

## Live config

| Knob | Default |
| --- | --- |
| Enable Portal Module | true |
| Rift Lifespan Ticks | 1200 |
| Teleport Cooldown Ticks | 80 |
| Spawn / Exit Offset | 1.5 |
| Companion Radius | 16 |
| Wild Min / Max Distance | 4000 / 6000 |
| Wild Search Attempts | 48 |

Existing instance `aqtweaks_portal.cfg` keeps old wild distances until edited.

## Do not regress

- Renderer only from `ClientProxy.preInit`. No `RenderArcaneRift` on the server classpath path.
- `getRawLight` no-ops when no rifts are live. Stamina packets stay 0–2. Java 21 `--release`.
- Sitting pets must not companion-pull. Leash rebind must not attach unleashed pets.
- Particle counts stay capped. No vanilla nether portal math on rift travel. Tear rifts must not use the wild red flag.
- Do not `untrack`/`track` rifts. No portal light packet.
- Wild dest must not sit on canopy logs. Exit must snap at Exit Offset, not dest Y in a trunk.

## Verify

`/give @p aqtweaks:spatial_rift_tear` then `/give @p aqtweaks:spatial_rift_wild`. Unbound name Arcane Tunnel; first use binds (Linked Arcane Tunnel + glint). Second (elsewhere, including Nether) opens purple rifts both ends; wild opens red. Dark cave lights like glowstone. Walk through both ways; villager/zombie in the box also go; 60s collapse. Sitting wolf stays; standing follows. Lead follows. Wild tear lands on dirt/stone ~4000–6000 blocks away, not ocean, **not** on a tree limb. Walk through: stand on solid ~1.5 in front of dest. Far dest cylinder still draws with DS full bypass. Missing dest dim fails with item kept. Dedicated server boots.
