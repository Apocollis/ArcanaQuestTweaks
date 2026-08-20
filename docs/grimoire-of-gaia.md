# Grimoire of Gaia module

Config: `config/arcanaquesttweaks/aqtweaks_grimoireofgaia.cfg`. Soft dependency (`after:grimoireofgaia`). Handler always registers; it no-ops unless the Gaia classes fire.

## Background & Design Intent

In Grimoire of Gaia 1.12.2, mob attacks are notorious for completely ignoring player defenses. On every melee hit, Gaia mobs apply an instant-damage potion effect (`Potion.getPotionById(6)`) on the same tick, and Gaia projectiles apply unblockable indirect magic. Because instant-damage potion effects and absolute magic bypass standard armor calculations, endgame players wearing top-tier modded gear (Thaumcraft Void Robes/Fortress armor, Draconic, etc.) with full Protection IV enchantments were regularly 1- or 2-shot as if completely naked.

The goal of this module is **not** to nerf Gaia mobs' base attributes or remove their secondary damage entirely, but to **re-route** the piercing damage through armor-respecting damage sources while preserving full compatibility with magic mitigation defenses.

## What Tweaks does

Gaia mobs apply a second **magic / absolute / unblockable** hit on the same tick as a physical hit (melee potion `INSTANT_DAMAGE`, or projectile MAGIC with no entity). Armor and Protection do not reduce that second hit.

Tweaks intercepts and cancels that piercing packet and re-applies the exact same damage amount through custom `DamageSource`s:
- `GaiaDamageSources.Melee` (`"mob"`)
- `GaiaDamageSources.Projectile` (`"indirectMagic"`)

Both custom sources override `isMagicDamage()` to return `true`, but omit the vanilla `isUnblockable()` and `isDamageAbsolute()` bypass flags:
1. **Physical Armor & Protection Apply**: Physical armor defense points, toughness, and vanilla Protection / Projectile Protection enchantments reduce the incoming damage.
2. **Magic Mitigation Remains Active**: Because `isMagicDamage()` remains `true`, Bewitchment Magic Protection enchantments, Potion of Resistance, Magic Resistance, and other modded magic defenses continue to detect and mitigate the attack.

## How the parent mod works

Grimoire of Gaia (`gaia.*` entity classes):

1. **Melee:** On the same world tick as the mob’s physical attack, it applies instant damage as a potion. Vanilla then fires a second `LivingHurtEvent` with MAGIC (often no `trueSource`).
2. **Projectiles:** On `ProjectileImpactEvent`, a Gaia projectile damages the player with MAGIC and no reliable attacker reference.

### I-Frame (Invulnerability) Handling

When the primary physical attack lands, vanilla Minecraft grants the player 20 ticks of invulnerability (`player.hurtResistantTime = 20`). Under vanilla rules, any secondary hit on the same tick would normally be completely discarded by the i-frame damage threshold check.

To ensure the re-routed second hit lands without removing the player's overall protection against external mobs:
1. Tweaks captures the player's active `hurtResistantTime` (`tempHurtResistant`).
2. Temporarily zeroes `hurtResistantTime = 0` exclusively for the `attackEntityFrom` call.
3. **Immediately restores** `hurtResistantTime` back to `tempHurtResistant`.
This allows the re-routed hit to register cleanly while preserving the player's post-hit invulnerability window against other attackers.

## How Tweaks hooks in

No Gaia mixin. Forge events only (`GrimoireOfGaiaModule`).

1. Physical hurt from `trueSource` class name `gaia.*` → record `(playerUUID → tick, attacker)`.
2. Gaia projectile impact on a player → record `(playerUUID → tick, shooter, projectile)`.
3. Same tick, MAGIC / absolute / unblockable hurt → cancel, temporarily zero `hurtResistantTime`, re-apply damage via `attackEntityFrom` using:
   - `GaiaDamageSources.Melee` (`EntityDamageSource` `"mob"`, `isMagicDamage() == true`)
   - `GaiaDamageSources.Projectile` (`EntityDamageSourceIndirect` `"indirectMagic"`, `isMagicDamage() == true`)
4. Restore `hurtResistantTime`.

Guard: if the source is already one of those custom classes, return immediately to prevent recursion.

Config `Disable Piercing Damage` (default true) is the master switch.

## Files

- `gaia/GrimoireOfGaiaModule.java`
- `gaia/GaiaDamageSources.java`

## Do not regress

- Correlation is **same tick only**. Do not keep the tracker across ticks or unrelated magic (witches, Thaumcraft) gets rewritten.
- Keep `isMagicDamage()` true so magic-protection mods (e.g. Bewitchment) still see it as magic.
- Always restore `hurtResistantTime` immediately after the re-routed attack.

