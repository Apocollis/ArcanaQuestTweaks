# Thaumcraft Additions

New Thaumcraft content owned by this pack. Package `com.apocollis.aqtweaks.thaumadditions`, config `config/arcanaquesttweaks/aqtweaks_thaumadditions.cfg` (`ArcanaQuestTweaksConfig.ThaumAdditionsConfig`). Loaded from `CommonProxy.init` when Thaumcraft is present (`ThaumAdditionsModule.init`). Thaumcraft is a compile parent (`libs/`).

## Shock focus effect

Thaumcraft has no lightning effect (Bolt is only a medium). Shock is a Potentia (`Aspect.ENERGY`) effect, element key `focus.aqtweaks.shock`, research `AQ_FOCUS_SHOCK`, registered with `FocusEngine.registerElement` (icon `assets/aqtweaks/textures/foci/shock.png`, a white 32x32 silhouette tinted in game by the element colour `0xFFE150`).

| Setting | Range | Effect |
| --- | --- | --- |
| `power` | 1 to 5 | Damage, as Frost |
| `duration` | 1 to 5 | Stun length |

Complexity is `duration + power * 2` (Frost's shape).

* **Damage:** `(Shock Base Damage Offset + power) * finalPower`, offset 3 as Frost, so 4 to 8 at power 1.0. Magic damage from the caster (`aqtweaks_shock`, marked magic through `ThaumcraftFocusHooks.markMagic`; no caster gives `aqtweaks_shock_wild`).
* **Wet:** `Entity.isWet()` (water or rain) or, with `Shock Rain Counts As Wet` off, `isInWater()`. Wet targets take `Shock Wet Damage Multiplier` (1.5). A `Water` effect earlier in the same package makes the target stand in water.
* **Chain:** after the first target, up to `Shock Chain Jumps` (2) more living targets, nearest first, within `Shock Chain Radius` (4), or `Shock Wet Chain Radius` (8) when both the current and the next target are wet. Each jump deals `Shock Chain Falloff` (0.6, compounding) of the previous hit before the wet bonus. One hit per target per cast. Valid: alive, not the caster, not creative or spectator, not friendly by `EntityUtils.isFriendly(caster, e)` (so with PvP off, players stay safe from other casters; a Battlemage still reaches them through its friendliness mixin, see the Battlemage plan). The first target is whoever the delivery medium hit.
* **Stun:** the target gets the shared **Stun** effect (`potion/PotionStun`, see below) for `Shock Stun Base Ticks + Shock Stun Ticks Per Level * (duration - 1)` ticks (5, 10, 15, 20, 25 at the defaults). The first target is always stunned; wet chained targets too (`Shock Stuns Wet Chained Targets`, default on); dry chained targets are not.
* **Creepers:** a creeper Shock hits becomes powered (`MixinEntityCreeperPowered` sets its synced `POWERED` flag; no lightning, no extra damage). `Shock Charges Creepers`, default on. Already-powered creepers are left alone.
* **FX and sound:** `PacketFXFocusPartImpact` at each hit (client `renderParticleFX` draws two short `arcLightning` sparks), `PacketFXZap` arcs between chained targets (colour `0xFFE150`). Sounds are Thaumcraft's own: `SoundsTC.zap` at the caster on cast (`onCast`, like Frost's), `SoundsTC.shock` at every struck target. `Shock Sound Volume` (0 to 2, default 1) scales both.
* **Research:** `assets/aqtweaks/research/thaumadditions.json`, registered by `ThaumAdditionsModule` (`ThaumcraftApi.registerResearchLocation`), Auromancy tab cell `[1, 2]`, parent `FOCUSELEMENTAL`. Stage 1 needs `THEORY;AUROMANCY;1` and the `potentia` aspect scan. Free cells were read from every research JSON in the pack at the time; only Thaumcraft puts entries in the Auromancy tab.
* **Toggle:** `Enable Shock Focus` (restart). Off registers neither the effect nor the research.

### Files

`thaumadditions/ThaumAdditionsModule`, `ShockMath` (plain-Java rules, `ShockMathTest`), `ShockTargets`, `PoweredCreeper` (+ `mixin/MixinEntityCreeperPowered`, early json), `foci/FocusEffectShock`; resources `textures/foci/shock.png`, `research/thaumadditions.json`, lang keys `focus.aqtweaks.shock.*`, `research.AQ_FOCUS_SHOCK.*`, `death.attack.aqtweaks_shock*`.

## Aegis focus effect

A shield held up while the caster keeps casting. Aspect `Aspect.PROTECT` (`praemunio`, colour `0x00C0C0`), element key `focus.aqtweaks.aegis`, research `AQ_FOCUS_AEGIS`, icon `textures/foci/aegis.png` (white 32x32 shield with two runic glyphs, tinted by the element colour). One setting, `power` 1 to 5. Complexity is `Aegis Vis Per Interval * 5` (Thaumcraft's cast cost is complexity / 5 vis), so the raise itself pays one interval's bill (2 vis at the defaults).

Thaumcraft's focus API has no per-tick or hold hook (`FocusEffect` has only `execute` and `onCast`, and `ItemCaster` never starts a "use" even though it declares a bow use action), so the channel is built from three hooks in `MixinItemCaster` plus `thaumadditions/AegisChannelHandler`:

* **Raise:** after a successful stock cast (`onItemRightClick`, `func_77659_a`, result SUCCESS) with an Aegis focus selected, `setActiveHand` starts the held state on both sides and the server opens a channel with pool `min(power * Aegis Pool Per Level, Aegis Max Pool)` (5, 10, 15, 20, 25 at the defaults). `getItemUseAction` (`func_77661_b`) returns `BLOCK` for an Aegis caster so the player gets the vanilla shield-raise pose; with that, vanilla's `isActiveItemStackBlocking()` is true after its raise delay (the pack's Responsive Shields mod sets it to 0), so perks that read it (Fortify) work unchanged. Other effects in the same package fire once at the raise.
* **Hold:** every `Aegis Interval Ticks` (20) held, the handler bills `Aegis Vis Per Interval` (2.0) through the caster's `consumeVis` (Vis Thrift and Full Font apply); the first interval is the raise. A failed bill drops the shield like a release. Releasing, a Stun, death, a changed focus or a logout ends the channel with no lockout, and the pool is discarded: a recast starts full and pays again.
* **Block:** `LivingAttackEvent` (HIGH): once held for `Aegis Raise Delay Ticks` (0), a hit whose source (`getDamageLocation`, else the attacker's position) is in the front 180 degrees (vanilla's test, `AegisMath.isFrontal`) is cancelled and its damage taken from the pool. Arrows, melee, explosions, thrown potions and spell damage are blocked; sources with no position (fall, fire, drowning, void, starvation) and the player's own hits are not. Repeated hits inside vanilla's 10-tick hurt window count only for what they add (`AegisMath.effectiveDamage`). A hit at or above the remaining pool breaks the shield: **the whole hit is absorbed, nothing carries over.**
* **Break:** channel ends, the hand is reset, `Shield Broken` (`potion/PotionAegisBroken`, `aqtweaks:aegis_broken`, icon `textures/gui/aegis_broken.png`) applies for `Aegis Break Lockout Ticks` (100). While it is active a cast of any focus containing Aegis is refused at the `onItemRightClick` HEAD hook (no vis spent, a short denied sound); other foci cast normally. The lockout is a potion, not `CasterManager.setCooldown`, because that cooldown is per caster and would block every focus.
* **Perks:** `ShieldState.isBlocking` and `ShieldState.ticksRaised(player)` read one value for vanilla shields and Aegis; `AegisBlockEvent` is posted on the Forge bus for every absorbed hit (player, attacker, damage, pool left, ticks raised, whether it broke). `AegisChannels.isRaised` is the plain server-side flag.
* **Stamina:** the stamina shield-hold billing skips a raised Aegis (it already pays vis) unless `Aegis Uses Stamina Shield Cost` is on.
* **Only players channel.** A mob casting an Aegis-bearing package gets nothing from the effect.
* **Ley-Rupture overchannel:** in a Twilight Forest surge (exposure at or above `Charge Min Exposure`) the pool and the cap both double (`FocusEffectAegis.pool(scale)`; 25 becomes 50), and the player pays 1 temporary warp at the raise and every 20 to 30 ticks while it stays raised, no damage. See [twilightforest.md](twilightforest.md).
* **Visuals** (`thaumadditions/client`): `PacketAegisShield` (discriminator 15, server to client, sent on raise, hit, end, break and every 10 ticks as a keepalive, 64 blocks) feeds `AegisClientState`; `AegisShieldRenderer` (`RenderWorldLastEvent`) draws a translucent blue panel (`textures/entity/aegis_shield.png`, 1.2 x 1.8 blocks, 0.9 in front of the head's facing) for every shielding player including the local one, with a crack overlay (`aegis_shield_cracks.png`) below half pool, a brief flash on a hit, and runic glyphs from Thaumcraft's particle atlas (the cells `RunicShieldHud` draws), one per 5 pool, flashing and fading as the pool drops. Sounds: `SoundsTC.runicShieldCharge` on raise, `runicShieldEffect` on a hit and (lower pitch) on a break.
* **Research:** same `thaumadditions.json`, Auromancy cell `[-1, 7]` beside Heal `[0, 7]`, parents `FOCUSHEAL` and `RUNICSHIELDING` (a cross-tab requirement, no line drawn). Stage 1 needs `THEORY;AUROMANCY;1` and the `praemunio` aspect scan.
* **Compared with Thaumic Augmentation's Void Shield** (an instant cast that spawns a persistent decoy entity, Impetus-gated, pool 5 to 75, optional reflection): Aegis blocks only the front, must be held, costs vis every second, locks out after a break, discards its pool when lowered, and tops out at 25. Its minimum equals Void Shield's, so it is "weaker" by those limits, not by the number alone; `Aegis Pool Per Level` is the lever.
* **Toggle:** `Enable Aegis Focus` (restart). The research file loads if either Shock or Aegis is on.

### Settings (`aqtweaks_thaumadditions.cfg`)

| Setting | Default | Notes |
| --- | --- | --- |
| Enable Aegis Focus | true | Restart |
| Aegis Pool Per Level | 5 | 1 to 5 |
| Aegis Max Pool | 25 | 5 to 25 |
| Aegis Vis Per Interval | 2.0 | Also sets complexity (value x 5) |
| Aegis Interval Ticks | 20 | 5 to 200 |
| Aegis Raise Delay Ticks | 0 | 0 to 20; vanilla is 5 |
| Aegis Break Lockout Ticks | 100 | 5 seconds |
| Aegis Sound Volume | 1.0 | 0 to 2 |
| Aegis Uses Stamina Shield Cost | false | |

### Files

`thaumadditions/AegisMath` (plain-Java rules, `AegisMathTest`), `AegisFocus` (finds Aegis in a caster's selected focus, cached), `AegisChannels` (server state), `AegisChannelHandler`, `ShieldState`, `AegisBlockEvent`, `foci/FocusEffectAegis`, `client/AegisClientState`, `client/AegisShieldRenderer`; `potion/PotionAegisBroken`; `network/PacketAegisShield`; `mixin/thaumcraft/MixinItemCaster`; resources `textures/foci/aegis.png`, `textures/entity/aegis_shield*.png`, `textures/gui/aegis_broken.png`, lang keys `focus.aqtweaks.aegis.*`, `research.AQ_FOCUS_AEGIS.*`, `effect.aqtweaks.aegis_broken`.

## Stun (shared effect)

`potion/PotionStun` (`aqtweaks:stun`, "Stunned", icon `textures/gui/stun.png`), applied through `potion/Stun.apply(entity, ticks)` and read with `Stun.isStunned(entity)`. Registered for every game, not behind the Shock toggle, so other systems can use it (the Aether lightning storm does, see [aether.md](aether.md)).

* **Movement:** the potion carries a movement-speed modifier of -15% (operation 2), about Slowness I. It stacks with a real Slowness (Frost).
* **While stunned** (`potion/StunHandler`, registered on both sides): item use is cancelled (`LivingEntityUseItemEvent.Start`, `RightClickItem`, `EntityInteract` with an item; `RightClickBlock` keeps block use but denies the item); a player cannot attack an entity (`AttackEntityEvent`); a **direct melee hit** by the stunned entity is cancelled (`LivingAttackEvent`, not thorns, not indirect); a new projectile owned by it never spawns (arrows, throwables including witch potions, fireballs, llama spit; checked at `ticksExisted == 0`); a witch's drink is cut off each tick so its potion never applies.
* **Applying it** also ends a use in progress (`resetActiveHand`) and, for mobs, clears the path. Creative and spectator players are skipped.
* **Unaffected:** projectiles already in flight, block breaking, block use (doors, chests), a creeper's lit fuse. **Not covered:** a mod's custom special ability that is not a melee hit, a projectile, an item use or a witch drink (for example a teleport, a summon or an area pulse); such mobs can check `Stun.isStunned` themselves. The Battlemage AI (`AIBattlemageCombat`) checks it before every action.

Files: `potion/PotionStun`, `Stun`, `StunHandler`; registered in `CommonProxy.init`.

## Tabula Rasa research

`AQ_TABULA_RASA` (Alchemy, [-3, -6]) and its Infusion recipe are registered from `ThaumAdditionsModule.init` through `StarterThaumcraftHelper`, independent of the focus toggles. See [starter-classes.md](starter-classes.md#tabula-rasa-class-reset-item).
