# Grimoire of Gaia module (1.8)

Last updated: 2026-09-17.

Config: `config/arcanaquesttweaks/aqtweaks_grimoireofgaia.cfg` (master switch) and `config/arcanaquesttweaks/gaia_mob_damage.json` (per-mob **base** + type multipliers). Soft dependency (`after:grimoireofgaia`). Optional `mixins.aqtweaks.gaia.json` (`required: false`). Parent jar: `GrimoireOfGaia3-1.12.2-1.7.2`.

## Locked intent

Tweaks owns **outgoing base amount** and **damage type** for stock Gaia mobs, plus the Tweaks-owned **Deep Dwarf** clone (`aqtweaks:deep_dwarf`). Gaia still owns stock dwarf AI, models, who swings, held weapons, and Strength-style buffs. **Deathword** is the exception: Tweaks replaces its melee with ranged Gaia magic.

Deep Dwarf is a hostile `EntityMobHostileBase` copy of dwarf combat (axe / bow / miner). Flesh on the face/neck is hue-shifted blue-purple; head hair and the beard overlay on that baked sheet are recast to white / light gray; gear stays Gaia’s palette; eyes are a red additive glow on every class. Attack base is `Deep Dwarf Attack Damage` in `aqtweaks_grimoireofgaia.cfg` (default 10). Tweaks lists the id in pack `mob_overworldspawntype.json` `underground` (layer filter) and `mob_tier.json` rare. **InControl `spawn.json` / `potentialspawn.json` are externally managed** — Tweaks does not write them; natural spawn only after those lists include `aqtweaks:deep_dwarf`. `/summon` works without that.

`gaia_mob_damage.json` `mobs[id]` is `ATTACK_DAMAGE` **base** (`setBaseValue`). `health` / `armor` are max-health and armor **bases** (pack spawn seeds: T1 30/4, T2 60/8, T3 120/12, plus Gaia half-HP exceptions). Orc `4` plus an iron sword / Strength still hits harder than 4 — raise or lower JSON to compensate. Spell = `getAttributeValue()` × `spellMultiplier` (includes gear/buffs); bomb same with `bombMultiplier`.

**One extra HP packet must not run.** Drop Gaia extras (melee instant-damage potion, archer harming tip, bomb thrown 2.0). **Do not** convert pierce into +6. Do **not** strip weapon or potion attack modifiers.

**Types**

| Attack | Packet Tweaks keeps | Type | Attribution |
| --- | --- | --- | --- |
| Melee | Vanilla `"mob"` from `super.attackEntityAsMob` | Physical: armor + Protection. Vanilla shield. | Mob is `trueSource` (untouched) |
| Archer | Vanilla tipped arrow | Projectile physical. Vanilla shield. | Shooter on the arrow |
| Magic / bubble / poison / web / magic-random | Gaia’s only HP packet, recast | `causeIndirectMagicDamage` (cultist-orb): armor/shields skip; Bewitchment Magic Protection yes; vanilla Protection still applies | Projectile immediate, caster `trueSource` |
| Bomb | Gaia’s MAGIC packet, recast | Explosion: armor + Blast Protection. Facing shield **zeroes** the hit (vanilla 1.12). | Bomb immediate, thrower `trueSource` |

Death messages and thorns / “hurt the attacker” read `trueSource`. The dropped MAGIC 6 had none.

Partial blast-shield absorption is **out of scope** (possible later module).

JSON load **once** in preInit. Edit JSON, **restart** (no Tweaks rebuild). Mixins are the hook; JSON is the table. HP/armor keys merge into existing instance files; they do not rewrite tuned `"mobs"` damage.

## How the parent mod works (1.7.2)

**Tier amounts:** `GaiaConfig.ATTRIBUTES.tierNattackDamage` (percent). `EntityAttributes` static init: `ATTACK_DAMAGE_1/2/3 = 4/8/12 × percent/100`. Each mob class sets vanilla `ATTACK_DAMAGE` from one of those three in `applyEntityAttributes`. Tweaks JSON **replaces the attribute base** for listed ids (instance `%` unused for those ids). Swords and Strength still add on top.

**`GaiaConfig.DAMAGE` is on/off, not an amount:** `baseDamage` (melee extra potion), `shieldsBlockPiercing`, `baseDamageArchers` (Hard-mode arrow tip).

**Melee** (`EntityMobHostileBase` / `EntityMobAssistBase.func_70652_k`):

1. `super.attackEntityAsMob` → vanilla `"mob"`, `trueSource` = Gaia mob, physical, armored.
2. If that succeeded and `baseDamage`: `addPotionEffect(INSTANT_DAMAGE, duration 2, amplifier 0)` → harming I = **6** `DamageSource.MAGIC` (unblockable, no attacker). Shield + `shieldsBlockPiercing` can skip (2). Tweaks **drops (2)**; does not recast it.

**Spell fireballs** (`EntityGaiaProjectileMagic`, `MagicRandom`, `Bubble`, `Poison`, `Web`): one HP packet, `attackEntityFrom(DamageSource.MAGIC, ATTACK_DAMAGE_2 / 2)`. Always the **global T2 constant**, not the shooter’s attribute. `MAGIC` is a singleton (`isMagicDamage` + `isUnblockable`, no `trueSource`). Status potions (slowness, poison, …) are not a second HP hit. All five use the same MAGIC source; type is the entity class.

**Bomb** (`EntityGaiaProjectileBomb`): (1) same MAGIC `ATTACK_DAMAGE_2 / 2`; (2) `causeThrownDamage` **2.0**. Neither sets `isExplosion()`.

**Archer** (`gaia.entity.ai.Ranged.rangedAttack`): vanilla `EntityTippedArrow`. On Hard + `baseDamageArchers`, tip INSTANT_DAMAGE → extra MAGIC **6**.

Golem/witch `INSTANT_DAMAGE` in bytecode is potion **immunity** (`func_70687_e`), not extra player pierce.

An **orc** that also casts still has T1 on the entity in Gaia; Gaia bolts ignore it and use T2/2. Tweaks uses the same JSON **base** for that orc’s melee; bolts use live `getAttributeValue()` (base + gear + buffs) × `spellMultiplier`.

**Deathword (Tweaks):** stock is melee + Levitation + summons/beacon. Tweaks drops melee/Levitation, adds `EntityAIAttackRanged` (Harpy Wizard cadence: 1.25 move, 20–60 interval, 15 range) and `Ranged.magic` (`EntityGaiaProjectileMagic`). Bolt HP is the same recast as other Gaia magic (orc caster). On a successful hit, Wither I duration is **remaining + 30 ticks** (so a group/repeat can cross the 40-tick wither pulse). Other casters’ magic does not wither. Summons and beacon stay.

## How Tweaks hooks in

No tick tracker. No `LivingHurtEvent` correlation.

| Attack | First / only packet | Extra (drop) | Tweaks |
| --- | --- | --- | --- |
| Melee | Vanilla `"mob"` | Instant-damage **6** MAGIC | Redirect `addPotionEffect` in `func_70652_k` (hostile + assist): skip INSTANT_DAMAGE on players. Backup: `PotionApplicableEvent` DENY if `INSTANT_DAMAGE` duration **2** amplifier **0** on a player. No replacement hit. |
| Archer | Vanilla tipped arrow | Instant-damage tip | Redirect `Ranged.rangedAttack` `addEffect`: skip INSTANT_DAMAGE. Do not add +6 to arrow damage. |
| Magic / bubble / poison / web / magic-random | Gaia MAGIC `ATTACK_DAMAGE_2/2` | Status potions stay; Deathword adds Wither +30 ticks on remaining | Redirect `attackEntityFrom`: if MAGIC and player, amount = shooter `getAttributeValue()` × `spellMultiplier`. Source = `causeIndirectMagicDamage(projectile, shooter)`. Deathword wither keyed by shooter id. |
| Bomb | Gaia MAGIC | Thrown **2.0** | MAGIC → `GaiaDamageSources.Bomb` (`setExplosion()`, bomb + thrower), amount = thrower `getAttributeValue()` × `bombMultiplier`. Skip thrown invoke for players. |

**Cleanroom INVOKE rule:** mixin **class** stays `remap = false` (Gaia methods are SRG: `func_70652_k`, `func_70227_a`, `func_70184_a`, `rangedAttack`). Vanilla **INVOKE** targets must be **MCP + `remap = true`** (`addPotionEffect`, `attackEntityFrom`, `addEffect`). SRG on those INVOKEs fails `InvalidInjectionPointException` and the json is `required: false`, so the game boots **with mixins off**. Same class of bug as `WorldGenLakes`. After deploy, `latest.log` must have **zero** `mixins.aqtweaks.gaia.json` injection failures before tuning JSON.

**Per-mob JSON:** keys `grimoireofgaia:<path>` (e.g. `orc`). Attack seeds are Gaia **100%** constants (T1 **4**, T2 **8**, T3 **12** by class). HP/armor seeds are **pack live spawn** (Gaia `tierNmaxHealth=75` → 30/60/120; armor 4/8/12; feral/butler/inquisitor 15/4; sporeling 15/2; Deep Dwarf 60/8). Unlisted ids keep Gaia’s attribute. `EntityJoinWorldEvent` (server) `setBaseValue` only — weapons and buffs stack. Missing keys on upgrade are merged from defaults. Attack JSON still requires pierce-on; HP/armor always apply when listed.

Master switch `Disable Piercing Damage` (default **true**): false → Gaia vanilla (no JSON overwrite, mixins call through).

Player-only for melee skip and bolt/bomb retype. Archer tip skip is at shoot time (all victims).

## Config

| Name | Default | Live? | Meaning |
| --- | --- | --- | --- |
| Disable Piercing Damage (`aqtweaks_grimoireofgaia.cfg`) | true | yes (Forge sync) | Drop extras + retype bolts/bombs + apply JSON bases |
| Enable Deep Dwarf | true | no (register at load) | Register `aqtweaks:deep_dwarf` when Gaia is loaded |
| Deep Dwarf Attack Damage | 10 | new joins after Forge sync | `ATTACK_DAMAGE` base for Deep Dwarf; wins over JSON for this id |
| `gaia_mob_damage.json` | T1/T2/T3 attack seeds; HP/armor pack-live; Deep Dwarf attack cfg | no (preInit) | Per-id melee **base**; `health`/`armor` bases; spell/bomb multipliers |

## Files

- `gaia/GaiaPierce.java` — skip potion/tip; retype MAGIC; Deathword wither +30 on remaining
- `gaia/GaiaDamageSources.java` — `Bomb` only
- `gaia/GaiaDamageConfig.java` — JSON attack + health + armor
- `gaia/GaiaDamageHandler.java` — join-world `setBaseValue` (HP/armor always; attack pierce-gated except Deep Dwarf cfg); `PotionApplicableEvent` pierce backup
- `gaia/GaiaEntityEvents.java` — load-gated `EntityEntry` (no Gaia import)
- `gaia/GaiaDeepDwarfRegistry.java` / `gaia/EntityDeepDwarf.java` — hostile clone
- `gaia/client/RenderDeepDwarf.java`, `ModelDeepDwarf.java`, `DeepDwarfSkin.java` — purple flesh bake, white hair and beard, red eyes
- `mixin/gaia/MixinEntityMobHostileBase.java`, `MixinEntityMobAssistBase.java`, `MixinGaiaMagicProjectile.java`, `MixinEntityGaiaProjectileBomb.java`, `MixinRanged.java`, `MixinEntityGaiaDeathword.java`
- `mixins.aqtweaks.gaia.json`

## Do not regress

- Do not change `super.attackEntityAsMob` or arrow `setDamage` (only the extra potion/tip).
- Magic bolts: `causeIndirectMagicDamage` (magic + bypass armor), not the MAGIC singleton.
- Bomb: explosion, not magic-bypass; not `setDamageIsAbsolute`; do not null `getDamageLocation()` (shield must still be able to zero).
- Bolt status potions stay.
- Do not mixin vanilla `EntityTippedArrow` globally.
- Mixin json `required: false`. Vanilla INVOKEs: MCP + `remap = true`.
- JSON is **base** (`setBaseValue`). Do not strip weapon/Strength modifiers.
- HP JSON: fresh spawn at old max → `setHealth(newMax)`; wounded chunk-load does not snap to full.
- Mixin APPLY in the log does not prove pierce is skipped — diamond must not take a flat MAGIC 6.
- Deep Dwarf: do not subclass `EntityGaiaDwarf`; do not `GlStateManager.color` the whole mesh; do not tint helm lamp or gear. Cfg wins over JSON for Deep Dwarf **attack**. HP/armor JSON still apply when pierce is off. Do not edit InControl `spawn.json` / `potentialspawn.json` from Tweaks.
- Deathword: ranged `Ranged.magic` only; do not leave `EntityAIAttackMelee` on the task list; do not wither non-Deathword magic; wither **adds** 30 ticks (do not overwrite remaining). Do not import `EntityGaiaDeathword` from always-loaded `GaiaPierce`.

## Out of scope unless asked

- Partial shield absorption for blasts
- Gaia AI, beacon/cloud potions (**other** stock mobs; Deathword combat AI is Tweaks)
- Rewriting archer arrow damage from JSON
- Hot-reload JSON without restart
- Committing Gaia PNG copies

## Verify

- `latest.log`: no `Mixin apply for mod aqtweaks failed mixins.aqtweaks.gaia.json` / `InvalidInjectionException`.
- Orc JSON `4`: **no** MAGIC 6. Diamond: physical leftover only (not a flat ~3 hearts of magic). Unarmored physical may exceed 4 (sword/buffs).
- Bolt: attributed indirect magic; armor does not help; Magic Protection does; death names caster.
- Bomb: armor + Blast Protection; no extra 2.0; facing shield zeroes as vanilla; death names thrower.
- JSON edit + restart moves orc melee and orc bolts together. HP/armor JSON edit + restart moves max health / armor; wounded loads stay wounded.
- `/summon` orc: 30 HP, 4 armor at default seeds. Dwarf / Deep Dwarf: 60 HP, 8 armor. Feral goblin 15/4. Sporeling 15/2.
- `/summon grimoireofgaia:deathword`: shoots Gaia magic (no slap); diamond takes piercing magic like an orc bolt; wither +30 ticks per hit (two hits can pulse); summons/beacon still happen. Orc magic has no wither.
- `/summon aqtweaks:deep_dwarf`: attacks on sight; face/neck purple-blue; **white/gray hair and beard** on the purple bake; red glow eyes; armor/weapons stock; diamond no MAGIC 6. Cfg damage moves new summons. Gaia off: Tweaks still boots. Natural spawn is out of Tweaks (external InControl lists).
