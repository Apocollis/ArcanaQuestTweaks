# Grimoire of Gaia module (1.6)

Last updated: 2026-08-20.

Config: `config/arcanaquesttweaks/aqtweaks_grimoireofgaia.cfg`. Soft dependency (`after:grimoireofgaia`). `GrimoireOfGaiaModule` **always** registers; it no-ops unless `gaia.*` classes fire. No Gaia mixin.

## Background and locked intent

In Grimoire of Gaia 1.12.2, mob attacks often ignore player defenses. On the same tick as a physical hit, Gaia melee applies instant-damage potion (`Potion` id 6 / `INSTANT_DAMAGE`). Gaia projectiles apply MAGIC with no reliable attacker. Instant-damage and absolute/unblockable magic skip armor and Protection, so endgame Void / Fortress / Draconic gear still got 1–2 shot.

**Do not** nerf Gaia base attributes or delete the second hit. **Re-route** that piercing packet through armor-respecting `DamageSource`s that still report `isMagicDamage()` so Bewitchment Magic Protection, Resistance, and similar mods still see magic.

## How the parent mod works

1. **Melee:** Same world tick as the physical attack, instant-damage potion → vanilla fires a second `LivingHurtEvent` with MAGIC (often no `trueSource`).
2. **Projectiles:** `ProjectileImpactEvent`, then MAGIC hurt with a weak attacker reference.

Vanilla grants `hurtResistantTime = 20` after the physical hit. The second packet on the same tick would usually die on i-frames.

## Design plan (same-tick correlation)

Forge events only, **`EventPriority.HIGH`**, **players only** (`EntityPlayer`). Other living entities are ignored.

Class test: `getClass().getName().startsWith("gaia.")` on the mob or the projectile entity.

Map: `UUID → GaiaDamageInfo(tick, attacker, projectile)` (`ConcurrentHashMap`). One slot per player; a new hit overwrites. Stale ticks are ignored by comparing `world.getTotalWorldTime()`, not by expiry.

1. Physical hurt whose `trueSource` is `gaia.*` → record `(uuid, tick, attacker, projectile=null)` and **return** (do not modify the physical hit).
2. `ProjectileImpactEvent` whose projectile is `gaia.*` and whose hit entity is a player → record `(uuid, tick, shooter, projectile)`.
3. Later on that tick, if hurt is MAGIC **or** unblockable **or** absolute, and the map entry’s tick matches:
   - Cancel the event.
   - Build `GaiaDamageSources.Melee` (`EntityDamageSource` `"mob"`) or `Projectile` (`EntityDamageSourceIndirect` `"indirectMagic"`, projectile + shooter).
   - Both override `isMagicDamage()` → true. They do **not** set unblockable/absolute.
   - Save `hurtResistantTime`, set it to 0, `attackEntityFrom(custom, event.getAmount())`, **restore** the saved i-frame immediately.

Guard: if `source instanceof GaiaDamageSources.Melee || Projectile`, return. Prevents recursion when the re-routed hit fires `LivingHurtEvent` again.

Config `Disable Piercing Damage` (default **true**) is the master switch. Despite the name, it **converts** piercing to armored magic; it does not delete the second hit. When false, Tweaks does nothing.

### Ordering assumption (maintenance)

The MAGIC `LivingHurtEvent` must run **after** the physical record or the projectile impact record on that same tick. HIGH priority makes Tweaks see the physical hit early; the MAGIC event is a later call. If a Gaia version applies MAGIC **before** the physical event on the same tick, correlation misses and the pierce stays vanilla (armor bypass). Do not widen the tracker across ticks — that would rewrite unrelated magic (witches, Thaumcraft, etc.).

## How Tweaks hooks in

No Gaia mixin. `GrimoireOfGaiaModule` as above.

Custom sources:

- `GaiaDamageSources.Melee` — `"mob"`, `isMagicDamage() == true`
- `GaiaDamageSources.Projectile` — `"indirectMagic"`, `isMagicDamage() == true`

Effects of that pair:

1. Physical armor, toughness, Protection, Projectile Protection apply.
2. Magic mitigation still applies because `isMagicDamage()` is true.

## Config (`aqtweaks_grimoireofgaia.cfg`)

| Name | Default | Live? | Meaning |
| --- | --- | --- | --- |
| Disable Piercing Damage | true | yes | Convert Gaia same-tick MAGIC/absolute/unblockable into custom magic-but-armored sources |

## Files

- `gaia/GrimoireOfGaiaModule.java`
- `gaia/GaiaDamageSources.java`

## Do not regress

- Correlation is **same tick only**.
- Keep `isMagicDamage()` true.
- Always restore `hurtResistantTime` immediately after `attackEntityFrom`.
- Recursion guard must stay on the custom source classes.
- Player-only; `gaia.` prefix (not a Gaia API interface).
- Do not keep tracker entries across ticks to “catch” out-of-order MAGIC.

## Out of scope unless asked

- Rewriting Gaia AI or potion application inside Gaia classes
- Applying the conversion to non-player victims
- Mixing in Gaia projectile classes
