# Grimoire of Gaia module (1.7)

Last updated: 2026-08-29.

Config: `config/arcanaquesttweaks/aqtweaks_grimoireofgaia.cfg` (master switch) and `config/arcanaquesttweaks/gaia_mob_damage.json` (per-mob bases + type multipliers). Soft dependency (`after:grimoireofgaia`). Optional `mixins.aqtweaks.gaia.json` (`required: false`). Parent jar: `GrimoireOfGaia3-1.12.2-1.7.2`.

## Background and locked intent

Gaia melee and arrows already deal a correctly typed **first** hit (physical mob melee / vanilla arrow) with a real attacker. A **second** packet (instant-damage potion, arrow tip, or bomb thrown 2.0) is vanilla `DamageSource.MAGIC` or extra damage: no shooter, armor bypass, endgame gear still 1–2 shot.

Spell bolts and the bomb’s main hit **are** that MAGIC packet — there is no separate physical first hit.

**Do not** convert the extra pierce into +6. **Drop** it. Keep the first (or only) HP packet. Per-mob attack **base** comes from Tweaks JSON, not Gaia’s three-tier `%`. Bolts use the typical spell-bolt pattern (`causeIndirectMagicDamage`, like Thaumcraft crimson cleric `EntityGolemOrb`): magic + armor bypass, attributed. Bombs use an explosion-typed source (armor + Blast Protection). Death messages name the Gaia mob.

## How the parent mod works (1.7.2)

**Tier amounts:** `GaiaConfig.ATTRIBUTES.tierNattackDamage` (percent). `EntityAttributes` static init: `ATTACK_DAMAGE_1/2/3 = 4/8/12 × percent/100`. Each mob class sets vanilla `ATTACK_DAMAGE` from one of those three in `applyEntityAttributes`. Tweaks JSON **replaces** that base for listed ids.

**`GaiaConfig.DAMAGE` is on/off, not an amount:** `baseDamage` (melee extra potion), `shieldsBlockPiercing`, `baseDamageArchers` (Hard-mode arrow tip).

**Melee** (`EntityMobHostileBase` / `EntityMobAssistBase.func_70652_k`):

1. `super.attackEntityAsMob` → vanilla `"mob"`, `trueSource` = Gaia mob, physical, armored, death messages work.
2. If that succeeded and `baseDamage`: `addPotionEffect(INSTANT_DAMAGE, duration 2, amplifier 0)` → harming I = **6** `DamageSource.MAGIC` (unblockable, no attacker). Shield + `shieldsBlockPiercing` can skip (2).

**Spell fireballs** (`EntityGaiaProjectileMagic`, `MagicRandom`, `Bubble`, `Poison`, `Web`): one HP packet, `attackEntityFrom(DamageSource.MAGIC, ATTACK_DAMAGE_2 / 2)`. Always the **global T2 constant**, not the shooter’s attribute. `MAGIC` is a singleton (`isMagicDamage` + `isUnblockable`, no `trueSource`). Status potions (slowness, poison, …) are not a second HP hit. All five use the same MAGIC source; type is the entity class.

**Bomb** (`EntityGaiaProjectileBomb`): (1) same MAGIC `ATTACK_DAMAGE_2 / 2`; (2) `causeThrownDamage` **2.0**. Neither sets `isExplosion()`.

**Archer** (`gaia.entity.ai.Ranged.rangedAttack`): vanilla `EntityTippedArrow` — projectile physical, shooter = mob. On Hard + `baseDamageArchers`, tip INSTANT_DAMAGE → extra MAGIC **6**.

Golem/witch `INSTANT_DAMAGE` in bytecode is potion **immunity** (`func_70687_e`), not extra player pierce.

An **orc** that also casts still has T1 on the entity; Gaia bolts ignore it and use T2/2. Tweaks uses the same JSON base for that orc’s melee and bolts.

## How Tweaks hooks in

No tick tracker. No `LivingHurtEvent` correlation.

| Attack | First / only packet | Extra (drop) | Tweaks |
| --- | --- | --- | --- |
| Melee | Vanilla `"mob"` | Instant-damage **6** MAGIC | Redirect `func_70690_d` in `func_70652_k` (hostile + assist): skip INSTANT_DAMAGE on players. No replacement hit. |
| Archer | Vanilla tipped arrow | Instant-damage tip | Redirect `Ranged.rangedAttack` `addEffect`: skip INSTANT_DAMAGE. Do not add +6 to arrow damage. |
| Magic / bubble / poison / web / magic-random | Gaia MAGIC `ATTACK_DAMAGE_2/2` | Status potions stay | Redirect `func_70097_a`: if MAGIC and player, amount = shooter `ATTACK_DAMAGE` × `spellMultiplier`. Source = `causeIndirectMagicDamage(projectile, shooter)`. |
| Bomb | Gaia MAGIC | Thrown **2.0** | MAGIC → `GaiaDamageSources.Bomb` (`setExplosion()`, bomb + thrower), amount = thrower `ATTACK_DAMAGE` × `bombMultiplier`. Skip thrown invoke for players. |

**Per-mob JSON** (`gaia_mob_damage.json`): load **once** in preInit. Formula: melee = `mobs[id]`; spell = melee × `spellMultiplier`; bomb = melee × `bombMultiplier`. Keys are `grimoireofgaia:<path>` (e.g. `orc`). Shipped seeds are Gaia **100%** constants (T1 **4**, T2 **8**, T3 **12** by class). Instance `tierNattackDamage` is not copied. Unlisted ids keep Gaia’s attribute. `EntityJoinWorldEvent` (server) `setBaseValue` on `ATTACK_DAMAGE` for listed mobs. Edit JSON, **restart** (no Tweaks rebuild). Missing keys on upgrade are merged from defaults.

Master switch `Disable Piercing Damage` (default **true**): false → Gaia vanilla (no JSON overwrite, mixins call through).

Player-only for melee skip and bolt/bomb retype. Archer tip skip is at shoot time (all victims).

**Mitigation:** magic bolts = cultist-orb pattern (no armor/shields; Bewitchment Magic Protection yes; vanilla Protection still applies). Bombs = explosion (armor + Blast Protection; not Magic Protection).

## Config

| Name | Default | Live? | Meaning |
| --- | --- | --- | --- |
| Disable Piercing Damage (`aqtweaks_grimoireofgaia.cfg`) | true | yes (Forge sync) | Drop extras + retype bolts/bombs + apply JSON bases |
| `gaia_mob_damage.json` | T1/T2/T3 seeds, multipliers 1.0 | no (preInit) | Per-id melee base; spell/bomb multipliers |

## Files

- `gaia/GaiaPierce.java` — skip potion/tip; retype MAGIC
- `gaia/GaiaDamageSources.java` — `Bomb` only
- `gaia/GaiaDamageConfig.java` — JSON
- `gaia/GaiaDamageHandler.java` — join-world attribute
- `mixin/gaia/MixinEntityMobHostileBase.java`, `MixinEntityMobAssistBase.java`, `MixinGaiaMagicProjectile.java`, `MixinEntityGaiaProjectileBomb.java`, `MixinRanged.java`
- `mixins.aqtweaks.gaia.json`

## Do not regress

- Do not change `super.attackEntityAsMob` or arrow `setDamage` (only the extra potion/tip).
- Magic bolts: `causeIndirectMagicDamage` (magic + bypass armor), not the MAGIC singleton.
- Bomb: explosion, not magic-bypass; not `setDamageIsAbsolute`.
- Bolt status potions stay.
- Do not mixin vanilla `EntityTippedArrow` globally.
- Mixin json `required: false`.

## Out of scope unless asked

- Gaia AI, beacon/cloud potions
- Rewriting archer arrow damage from JSON
- Hot-reload JSON without restart
