# Portal module (1.8)

Last updated: 2026-09-10.

Config: `config/arcanaquesttweaks/aqtweaks_portal.cfg`. Tweaks-owned. No parent portal mod.

## Locked intent

Two items open the same **two-way 60-second** `EntityArcaneRift` pair. **Arcane Tunnel** (`spatial_rift_tear`) **activates** (binds) on first air-use and can open a rift to a **bound dimension**; **Unstable Arcane Tunnel** lands like spreadplayers on solid land in the **current dimension**. Standing owned pets and leashed mobs follow the player; sitting pets stay.

Do **not** instant-teleport the player. Do **not** use vanilla end-portal TESR. Do **not** persist rifts across logout. Do **not** use vanilla nether `Teleporter` (`ITeleporter.isVanilla() == true`).

## How it works

### Items

| Id | Display | Use |
| --- | --- | --- |
| `aqtweaks:spatial_rift_tear` | Arcane Tunnel; **Linked Arcane Tunnel** + glint when bound | Unbound air-use binds feet. Bound air-use opens source here and dest at bound XYZ in `BoundDim`. **Sneak-use unbinds** (air or block). Unbound sneak does nothing. Bind is item NBT (`Bound`, `BoundX/Y/Z`, `BoundDim`). Creative does not consume. Tooltip always shows the use line. |
| `aqtweaks:spatial_rift_wild` | Unstable Arcane Tunnel | Air-use searches random XZ in min–max range, surface Y, reject liquid / leaves / ocean. Same-dimension rift pair. |

Wild `findStandPos` starts at `getHeight` and walks **down** through air, plants, and **leaves** (cap 48) onto **solid + 1** whose two body cells are air or plants **only** (not leaves, not a log in the canopy). Source spawn is player XZ + horizontal look × **Spawn Offset** (cfg, default 1.5), snapped with `snapStand`: skip plants at that Y, do **not** fall through air. A failed **source** snap is not fatal — the source rift takes the player’s own Y.

Dest snap is `snapStand` first, then `findStandFromY` (down 48, then climb up to 8) if that returns null; the open fails only when **both** return null. `spawnLinkedRifts` is shared, so that fallback runs on the wild path too, but it only changes anything for a **bound** destination: wild’s `destStand` already came from `findStandPos`, which is `findStandFromY` plus liquid / ocean rejection.

If `MinecraftServer.getWorld(BoundDim)` is null, open fails (`missing_dim`); item kept. Other failures are status messages; item kept. A **successful** open consumes one from the stack (`stack.shrink(1)` in both `ItemSpatialRiftTear` and `ItemSpatialRiftWild`) unless the player is creative — kept on failure, spent on success. On a successful pair spawn the **opener** also gets `timeUntilPortal` set to Teleport Cooldown Ticks (default 80) immediately, even if they never enter the rift. Dest chunk is loaded via `getChunk`; both rifts hold `ForgeChunkManager` tickets until collapse.

### Entity `aqtweaks:arcane_rift`

Lifespan is read from cfg in `entityInit` into the synced `REMAINING` value (default 1200 ticks), so a cfg edit only reaches **newly opened** rifts — live ones keep the lifespan they were built with. `setSize(1.6, 2.4)`, noClip, not saved (`writeToNBTOptional` false). Linked by UUID **and** dest dimension. Synced `wild` flag (unstable pair). Ticket on **each** rift’s own world; released in `setDead()`. Killing one finds the other across loaded worlds and `setDead()`.

Block light **15** like glowstone: `MixinBlockRiftLight` on Forge `Block.getLightValue(state, world, pos)`, plus `MixinWorldRiftLight` on `World.getRawLight`. `RiftLighting` queues those cells and calls `World.checkLight` at **world-tick END** (client world always; dedicated `WorldServer` only — skip the integrated server world so client `updateClouds` is not mutated from the server thread). Never `checkLight` from the rift entity tick. Mixins stay in `mixins.aqtweaks.early.json`.

Teleport: AABB overlap. Skip other rifts and **sitting** tamed pets. Players still dismount, companion-pull (radius), remount, re-leash. Everything else in the box (`EntityItem`, villagers, hostiles, standing tames, XP orbs, etc.) `moveToExit`. Then `timeUntilPortal` = cooldown (default 80). Exit XZ is dest + horizontal look × **Exit Offset** (cfg, default 1.5), then `findStandFromY` (solid + 1, two body cells) — **not** wild `findStandPos`, which also rejects liquids and `Type.OCEAN`. If that heading is a wall/trunk/hole, try 8 headings at the same radius, then dest feet.

Do **not** `untrack`/`track` dest on arrival. That destroy packet plus Dynamic Stealth **Entity Specific Full Bypass** leaves dest with a server hitbox and light but **no cylinder**. Keep `aqtweaks:arcane_rift` on DS full bypass so dest is not sense-gated. If the client entity drops, baked block light can stay; that is expected. No Tweaks light packet.

Same dimension: `setPlayerLocation` / `setLocationAndAngles`. Cross-dimension: `entity.changeDimension(destDim, RiftTeleporter)` with `isVanilla() == false`. Sitting pets still stay in the origin dimension.

Companion pull (player trips only, same tick, radius cfg 16): `EntityTameable` owned, **not** `isSitting()`; `EntityLiving` leashed to that player. Re-bind leash only for those that were leashed. Fence knots ignored.

### Look and audio

`RenderArcaneRift`: nether-portal texture on a **wobbly cylinder**, **V scrolls upward** (no yaw spin). Vertex alpha **0.75** healthy / **0.25** collapse. Wild pair: red vertex tint. Not End TESR, not a dest camera.

Particles (client `RiftParticles`, **2**/tick): **purple** `DRAGON_BREATH` inside the cylinder (radius ≤ 0.70, slight inward drift); wild **red**; no `PORTAL` motes; no enchantment-table glyphs; **4** `CLOUD` on collapse (same disc).

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
| `mixin/MixinWorldRiftLight.java` | `World.getRawLight` BLOCK 15 at rift cells |
| `mixin/MixinBlockRiftLight.java` | Forge `getLightValue` 15 at rift cells (glowstone-style emitter) |
| `portal/client/RenderArcaneRift.java` | Wobbly portal cylinder |
| `portal/client/RiftParticles.java` | Dragon-breath column |
| `portal/client/PortalClientEvents.java` | Item models |
| `ArcanaQuestTweaksConfig.PortalModuleConfig.general` | `aqtweaks_portal.cfg` |

## Live config

| Knob | Default |
| --- | --- |
| Enable Portal Module | true |
| Rift Lifespan Ticks (new rifts only) | 1200 |
| Teleport Cooldown Ticks | 80 |
| Spawn Offset (`spawnOffset`) | 1.5 |
| Exit Offset (`exitOffset`) | 1.5 |
| Companion Radius | 16 |
| Wild Min / Max Distance | 4000 / 6000 |
| Wild Search Attempts | 48 |

Existing instance `aqtweaks_portal.cfg` keeps old wild distances until edited.

## Do not regress

- Renderer only from `ClientProxy.preInit`. No `RenderArcaneRift` on the server classpath path.
- `getRawLight` no-ops when no rifts are live. Stamina packets stay 0–2. Java 21 `--release`.
- Sitting pets must not companion-pull. Leash rebind must not attach unleashed pets.
- Particle counts stay capped at 2/tick inside the cylinder. No vanilla nether portal math on rift travel. Tear rifts must not use the wild red flag.
- Do not `untrack`/`track` rifts. No portal light packet.
- Wild dest must not sit on canopy logs. Exit must snap at Exit Offset, not dest Y in a trunk.

## Verify

`/give @p aqtweaks:spatial_rift_tear` then `/give @p aqtweaks:spatial_rift_wild`. Unbound name Arcane Tunnel; tooltip has use line. First air-use binds (Linked + glint). Sneak-use unbinds. Second air-use (elsewhere, including Nether) opens purple rifts both ends; wild opens red. Breath stays inside the cylinder. Dark cave lights like glowstone. Walk through both ways; villager/zombie in the box also go; 60s collapse. Sitting wolf stays; standing follows. Lead follows. Wild tear lands on dirt/stone ~4000–6000 blocks away, not ocean, **not** on a tree limb. Walk through: stand on solid ~1.5 in front of dest. Far dest cylinder still draws with DS full bypass. Missing dest dim fails with item kept; a successful open spends one from the stack (creative keeps it). Dedicated server boots.
