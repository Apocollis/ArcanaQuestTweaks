# Thaumcraft module (1.8)

Last updated: 2026-09-25.

Config: `config/arcanaquesttweaks/aqtweaks_thaumcraft.cfg`. Event handler registers only if `thaumcraft` is loaded (`CommonProxy.init`). Warp API is reflection (`ThaumcraftHelper`, raw `Class`) so Comfort can call it without importing TC types. Focus mixins **compile-hard** TC **6.1 BETA26** (`libs/`); missing that jar fails compile. Optional `mixins.aqtweaks.thaumcraft.json` (`required: false`) skips at runtime if TC is absent.

Comfort homestead drain is a **different** NBT key and module ([comfort.md](comfort.md)). Bewitchment ritual warp is [bewitchment.md](bewitchment.md). Village `isInsideStructure` padding is [rtg.md](rtg.md) — not this dungeon check. Cultist cage fail delay is [spawning.md](spawning.md) (`MobSpawnerBaseLogic`), not a Thaumcraft entity mixin.

## Locked intent

Add pack-side warp **sources and sinks** Thaumcraft does not have: first visit to a dimension, lingering in configured dimensions / underground Y bands / Roguelike dungeons, and reducing **sticky** warp after a successful night sleep. Temporary warp during sleep is TC decay + Comfort Homestead (Somnia ticks those). Do **not** mixin `handleWarp`. Quiet Mind may mixin `checkWarpEvent` after visor, before `PacketMiscEvent` — do **not** cancel HEAD (that would skip −1 temp). Parent still **−1 TEMPORARY / 2000 ticks** while online, not Warp Ward, not wussMode.

Stamp `setMagicDamage()` on stock caster **focus HP** so Reskillable Magic drip classifies them without a `thrown` prefix (snowballs stay physical). Scale Heal-focus `heal(float)` by `ReskillableBonuses.scaleOutgoingMagic` (drip, Blood Pact, Full Font stamp). Full Font multiplies `ItemCaster.consumeVis` amount ×1.5 when chunk vis ≥ aura base ×0.9, then Vis Thrift; success stamps. Quiet Mind mixins `checkWarpEvent` after visor (not HEAD). Vis Thrift mixins `CasterManager.getTotalVisDiscount`. See [reskillable.md](reskillable.md).

## How the parent mod works

Thaumcraft 6 stores warp on capability `IPlayerWarp`:

| `EnumWarpType` | Tweaks index | Role |
| --- | --- | --- |
| `NORMAL` | 0 | Sticky |
| `TEMPORARY` | 1 | Decays; most Tweaks grants |
| `PERMANENT` | 2 | Research/eldritch; this module does not grant it (ritual wrapper can) |

API (reflection):

- `ThaumcraftCapabilities.getWarp(player)`
- `IPlayerWarp.get / add / reduce(EnumWarpType, int)`
- `IPlayerWarp.sync(EntityPlayerMP)` — **required** after server-side changes or the client HUD is stale

Sounds such as `thaumcraft:whispers` are TC registry names played through vanilla `SoundEvent`.

### Caster foci (BETA26)

`FocusEffect.execute` is the only HP/heal call site. Media (bolt / projectile / touch / spellbat) do not call `attackEntityFrom` / `heal` themselves.

| Effect | Living | Undead |
| --- | --- | --- |
| Fire | `fireball` + `setFireDamage` + `attackEntityFrom` | — |
| Frost / Air / Earth | `causeThrownDamage` + `attackEntityFrom` | — |
| Flux / Curse | `causeIndirectMagicDamage` + `attackEntityFrom` | already magic |
| Heal | `heal(power × potency)` | magic `attackEntityFrom` (`× 1.5`) |

Break / rift / exchange do not hit HP. Addon `FocusEffect` subclasses are not covered.

## Design plan (hooks)

Warp: `ThaumcraftModule` uses Forge events. Foci: optional mixins below.

| Event | Behavior |
| --- | --- |
| `PlayerLoggedInEvent` | If persisted `VisitedDimensions` is missing, set it to **current dim** so login is not “first visit” |
| `PlayerChangedDimensionEvent` | If `enableDimensionWarp` and dim not in the list: append, then a **worker thread sleeps 2s**, then `server.addScheduledTask` awards warp |
| `PlayerWakeUpEvent` | If `enableWarpCleansing`, not `wakeImmediately`, world is daytime: reduce **sticky** if `clearNormalWarp` (temp off by default) |
| `PlayerTickEvent` END every **tickSeconds × 20** (default 600 / 30s) | Exposure: highest-G match only; pause banks otherwise |

### Dimension first visit

Persisted int array `VisitedDimensions`. Amounts: `dimensionNormalWarp` (**5**) + `dimensionTempWarp` (**5**). Sync, play `dimensionEntrySound` at `dimensionEntrySoundVolume` (2), send `dimensionChatMessageText`. Skip award/sound/chat if both amounts are 0.

After the 2s delay, abort if the player is dead, world is null, or the player is no longer in `playerEntities` (teleport/logout). **Do not call the TC API on the worker thread.**

### Sleep cleanse

Not a nap: `wakeImmediately` false **and** `world.isDaytime()`. Sticky reduce 1 if `clearNormalWarp`. `clearTempWarp` defaults **false** (Somnia already runs TC −1 temp / 100s and Comfort Homestead). Chat only if something was actually cleared and `enableChatMessage`.

### Exposure

The handler **returns immediately** unless `ticksExisted % (exposureTickSeconds × 20) == 0` (default **30s**). Dim parse, Y, `isInsideStructure`, NBT, grant, whisper only on that pass.

**Warp Ward:** if `PotionWarpWard.instance` is active, skip the pass (banks frozen). Same intent as TC `handleWarp`.

Sources this pass (G from config; skip G ≤ 0):

1. `exposureDimensionGrants` `dimId=G` if current dim matches.
2. If `enableUndergroundExposure`: floor Y **in [Y Min, Y Max]** → `under` G; **Y < Y Min** → `underDeep` G. Y > Y Max is not underground.
3. If `enableDungeonExposure` and `ChunkProviderServer.isInsideStructure(..., "RoguelikeDungeon", pos)` → `dungeon` G.

There is **no Tweaks mixin** on `isInsideStructure`. Roguelike Dungeons Arcana must register that name. Village detection ([rtg.md](rtg.md)) is a different inject.

**Winner:** highest G among matches. Tie-break: `underDeep` > `dungeon` > `under` > `dim:<id>`. Only that key’s bank in persisted `WarpExposureBySource` gets **+tickSeconds**. Losing matches get +0. No source / Warp Ward: **no decrement**.

When that key **≥ grantSeconds** (300): add **that G** temporary warp, sync, optional whispers, set **that** key to 0.

Old `WarpExposureProgress` int: on first pass with a winner, add it onto that key and remove the int.

Y knobs `@Config.RangeInt` **−256..256** (Depths).

Java G defaults: Nether 7, End 9, Aether 6, TF 5, Betweenlands 8, Atum 6, Beneath 7, Emptiness 10, Aurorian 6, upper underground (−20..30) 5, deep (Y < −20) 6, dungeon 4.

### Focus mixins

Optional `mixins.aqtweaks.thaumcraft.json`. Vanilla INVOKEs MCP + `remap = true` (Gaia rule). Helpers in `ThaumcraftFocusHooks` (no TC imports).

- `MixinFocusEffectExecute` — Fire / Frost / Air / Earth / Flux / Curse / Heal `execute` → `attackEntityFrom`: `setMagicDamage()`. Does **not** change the hurt float (Reskillable `LivingHurtEvent` does). Fire keeps `isFireDamage`.
- `MixinFocusEffectHeal` — Heal only, `heal(F)`: amount `×` caster Magic outgoing if the caster is a player. Not a global `LivingHealEvent`.
- `MixinCasterManager` — `getTotalVisDiscount` RETURN: Vis Thrift +0.30. `changeFocus` / `fetchFocusFromPouch` / `addFocusToPouch`: bauble pouch offset **4 → 100** so BaublesEX slots ≥ 4 stay negative (not `mainInventory`). Does **not** rewrite the armor `4` in `getTotalVisDiscount`. Those two pouch methods **skip** `IBaublesItemHandler.setChanged`: stock BaublesEX is a no-op, and the transformed method reads missing field `player` (`NoSuchFieldError`) after the focus was already taken out of the pouch. `markDirty` still runs.
- `MixinWarpEvents` — `checkWarpEvent`: Quiet Mind severity after visor, before `PacketMiscEvent`.

Do **not** add `thrown` to Reskillable allow-prefixes. Java default prefix `fireball` is ghast/Lich only (instance cfg may still be empty).

### Runic shielding HUD

Thaumic Tweaker with **Runic Shielding Overhaul** off stores the shield in vanilla absorption, so damage is absorbed before armor. `handleRunicArmor` fills `PlayerEvents.runicInfo` on the **server only** (`!isRemote`). The client cap is the same sum: `getRunicCharge` on the four armor slots and baubles, then `RunicShieldingCalculateEvent.fire` (Astral). Recharge fills up to that cap. A golden apple adds to the same number and stays when it sits above the cap.

Client mixins in `mixins.aqtweaks.thaumcraft.json`. `MixinGuiIngameForgeRunicShield` reports absorption 0 from `renderHealth` while that cap is above 0, then `RunicShieldHud` draws the rune overlay. `MixinRunicShieldingHudHandler` cancels Tweaker's ten-rune bar. Compile-hard Thaumcraft, Thaumic Tweaker, Baubles, and Astral.

- Shield points `min(absorption, cap)` are runes (`ParticleEngine.particleTexture`, `UtilsFX.drawTexturedQuad`). The first 10 sit on the red health hearts. Further points are rows above the health stack, each on an empty heart socket.
- Surplus `max(0, absorption - cap)` is gold absorption hearts above those rows (full offset 144, half 153), with the same socket. The cap is computed on the client; do not read `runicInfo` there.
- Extra rows add to `left_height` so the armor bar sits above them.
- Overhaul on: Tweaker's attribute and overlay stay. No cap in `runicInfo`: absorption stays gold hearts. An apple that only fills a partial shield stays runes.

## Config (`aqtweaks_thaumcraft.cfg`)

| Name | Default | Live? | Meaning |
| --- | --- | --- | --- |
| Enable Sleep Warp Cleansing | true | yes | Master sleep sink |
| Clear Normal Warp | true | yes | Reduce sticky on sleep |
| Normal Warp Reduction | 1 | yes | Per successful sleep |
| Clear Temporary Warp | **false** | yes | Extra temp on wake; leave off |
| Temporary Warp Reduction | 2 | yes | Unused unless Clear Temporary is on |
| Enable Sleep Chat Message | true | yes | Chat if anything cleared |
| Sleep Chat Message Text | (purple “whispers grow quieter”) | yes | |
| Enable Dimension Entry Warp | true | yes | Master first-visit source |
| Dimension Entry Normal Warp | **5** | yes | Sticky |
| Dimension Entry Temporary Warp | 5 | yes | |
| Dimension Chat Message Text | (purple “ancient whispers”) | yes | |
| Dimension Entry Sound | `thaumcraft:whispers` | yes | Empty = no sound |
| Dimension Entry Sound Volume | 2.0 | yes | |
| Enable Warp Exposure | true | yes | Master tick source |
| Exposure Tick Seconds | 30 | yes | Pass cadence |
| Exposure Grant Seconds | 300 | yes | Bank to grant |
| Exposure Dimension Grants | `-1=7` `1=9` `4=6` `7=5` `20=8` `17=6` `10=7` `14676=10` `424=6` | yes | `id=G` not seconds |
| Enable Deep Underground Exposure | true | yes | Both Y bands |
| Underground Y Max | 30 | yes | Upper band inclusive |
| Underground Y Min | −20 | yes | Upper inclusive; below = deep |
| Underground Exposure Warp | 5 | yes | G for −20..30 |
| Deep Underground Exposure Warp | 6 | yes | G for Y < −20 |
| Enable Dungeon Exposure | true | yes | `RoguelikeDungeon` |
| Dungeon Exposure Warp | 4 | yes | |
| Enable Exposure Sound | true | yes | |
| Exposure Sound Effect | `thaumcraft:whispers` | yes | |
| Exposure Sound Volume | 2.0 | yes | |

## Files

- `thaumcraft/ThaumcraftModule.java`
- `thaumcraft/ThaumcraftHelper.java` — lazy `init()`, type index 0/1/2, `sync` only if `EntityPlayerMP`. Use raw `Class` (not `Class<?>`): Forge 1.12 `SideTransformer` throws on Java 21 generic Signature / LVT and the class then looks missing (`NoClassDefFoundError` from Comfort homestead cleanse).
- `thaumcraft/ThaumcraftFocusHooks.java` — `markMagic`, Heal scale (Reskillable via reflection)
- `thaumcraft/ThaumcraftPerkHooks.java` — Vis Thrift / Quiet Mind (no Reskillable import)
- `mixin/thaumcraft/MixinFocusEffectExecute.java`, `MixinFocusEffectHeal.java`, `MixinCasterManager.java`, `MixinWarpEvents.java`
- `thaumcraft/RunicShieldHud.java` — client gear cap, rune rows, and gold surplus. Not `@SideOnly`
- `mixin/thaumcraft/MixinGuiIngameForgeRunicShield.java`, `MixinRunicShieldingHudHandler.java`
- `mixins.aqtweaks.thaumcraft.json`

## Do not regress

- Always `syncWarp` after add/reduce on the server.
- Dimension warp is **first visit only** (persisted array). Login must seed the current dim.
- Sleep must be a real night sleep (`wakeImmediately` false, daytime). Default sleep sink is **sticky only**.
- Off-thread sleep then `addScheduledTask` — never TC API from the worker thread.
- Exposure banks **pause** when unmatched or Warp Ward; do not −seconds. Do not fill two banks in one pass.
- Old cfg `dimId=seconds` / interval keys are dead; grants are amounts.
- Comfort `WarpCleansingProgress` is a different counter.
- `ThaumcraftHelper` fields and `Class.forName` locals stay raw `Class`. Generics here crash SideTransformer on Java 21 class files.
- Focus mixin: stamp magic only on `attackEntityFrom`; do not double-scale hurt. Heal scale is Heal-only (other foci have no `heal` invoke).
- Snowballs stay non-magic (`thrown` is not an allow prefix).
- Vis Thrift still injects `getTotalVisDiscount` RETURN. Focus-pouch bauble offset is only the three pouch methods.
- Runic HUD does not change recharge, vis cost, or the overhaul attribute. Overhaul off keeps absorption for damage. The client cap is worn `TC.RUNIC` plus Astral's calculate event, not `runicInfo`. First 10 runes sit on the red hearts. Past 10 uses empty sockets above the health stack. Gold hearts are only the surplus above that cap.

## Out of scope unless asked

- Permanent warp from dimensions/sleep/exposure
- Mixin into Roguelike structure lookup
- Wiring `villageMinWellHeight` or village detection into dungeon warp
