# Crimson Battlemage (stages 1 and 2, entity split and gear: plan sections 17 to 19, part A)

A family of Crimson Cultist caster minibosses that fight with real Thaumcraft focus packages. Design and later stages: `.plans/archive/20261113-2124-crimson-cultist-battlemages.md`. Package `com.apocollis.aqtweaks.thaumcraft.battlemage`, config `config/arcanaquesttweaks/aqtweaks_battlemage.cfg` (`ArcanaQuestTweaksConfig.BattlemageConfig`), data `config/arcanaquesttweaks/cultist_battlemages.json`. Needs Thaumcraft.

**Four entities (plan 18.8), one shared brain.** `EntityCultistBattlemage` is the shared base and is itself the Tier 1 mage; the others are thin subclasses that only change the tier and the library variant. There is no tier or archetype NBT: the entity id is the identity.

| Entity | Class | Numeric id | Tier | Loadout |
| --- | --- | --- | --- | --- |
| `aqtweaks:cultist_battlemage` (Crimson Battlemage) | `EntityCultistBattlemage` | 4 | 1 | rolled (Tier 1 `loadout`) |
| `aqtweaks:cultist_master_battlemage` (Crimson Master Battlemage) | `EntityCultistMasterBattlemage` | 7 | 2 | rolled (Tier 2 `loadout`) |
| `aqtweaks:cultist_swarm_mage` (Crimson Swarm Mage) | `EntityCultistSwarmMage` | 8 | 1 | variant `swarm_mage`: fixed list, roller skipped |
| `aqtweaks:cultist_bombardier_mage` (Crimson Bombardier Mage) | `EntityCultistBombardierMage` | 9 | 2 | variant `bombardier_mage`: fixed list, roller skipped |

Ids 5 and 6 belong to the Nameless entities (`additions`), id 10 is the cosmetic escort portal (`aqtweaks:escort_portal`, see "Swarm Mage"). All four mages share `RenderCultistBattlemage`, one spawn egg each, and the lang keys `entity.<name>.name`. All four entities, the Swarm Mage's escort, portal, ally heal, and aura guard, and the Bombardier's five-focus volley and close sequence are built (only the separate Cultist Hunt spawn event is not built). **Pack lines now needed for four ids instead of one:** the Dynamic Stealth `Natural Soul Sight` list, any `mob_conditions.json` / `mob_modifiers.json` rules, spawn-party entries and In Control rules. In Control matches on the entity id, so each mage can carry its own rules (stage, dimension, structure, max count). Caution for the Swarm Mage: its escort is added by the mage itself, so In Control spawn rules do not see it, but rules that fire when an entity joins the world (`onjoin`) do and can remove or change a Crimson Knight, Cleric or Archer; check those first if an escort comes up short.

## At a glance (state on 2026-11-20; every number is in `cultist_battlemages.json` or `aqtweaks_battlemage.cfg`)

| | Tier 1 (Battlemage, Swarm Mage) | Tier 2 (Master Battlemage, Bombardier) |
| --- | --- | --- |
| Health / base armor | 80 / 1 | 120 / 3 |
| Worn armor | Crimson Paladin helmet, chestplate, leggings (Mindful, Elite, Elite), Crimson Boots (Nimble): about 16 armor plus about 2 from the Quality modifiers | same |
| Weapon / caster | Crimson Battlemage's Blade (Serrated, five enchantments), Mystical `caster_basic` | same |
| Cast power (`focusPower`) | x1.25 | x1.5 |
| Faked runic shield | 16 | 28 |
| Loadout | 4 fixed (Heal Self, Knock Back, Blink, Rift) + 4 slots (bolt, projectile, summon, area), budget 160 | the same + 2 wildcards, budget 250 |
| Cast pacing | gap 40 ticks, budget regen 5.5/s (hold at 25, resume at 65) | gap 30 ticks, regen 8/s |
| Movement | ground speed capped at 4.3 blocks per second (`Battlemage Max Move Speed`) | same |

The Swarm Mage and Bombardier take their tier's body, gear, drops and tuning; the Swarm Mage adds escorts, a Spell Bat swarm and a guarded Healing Aura, the Bombardier a five-focus area volley (every 64 ticks, 180 ticks between volleys, no cast budget) and a close sequence.

**Fight pattern, in priority order each AI tick** (`AIBattlemageCombat`, `BattlemageDefense`): Stun gate (a Stun that broke a strike combo is answered with Knock Back and an escape blink afterwards) → defence (sidestep, strict escape blink below 30% health, no-firing-line blink, offensive rift, Knock Back peel at 3 blocks, heal in place) → retreat to heal at 50% (not the Bombardier) → target re-pick → Spell Bat upkeep → focus cast within the budget (opener, follow-ups, burst of about 3 then a hold) → melee burst of 2 to 3 blows when a player is within 3 blocks → movement (band 8 to 14, strafe, hazard avoidance, no faster than the cap). Extras: during each cast hold the mage closes in for melee (60%, within 14 blocks) and half of those blink in next to the target (10 tick pause, burst, 10 tick pause, blink out); every 10 s a 40% chance of a reposition blink to a new spot on its band; casts at the heaviest foci have a wind-up.

**Defence and healing.** Heal Self (its strength comes only from the tier's `focusPower`) from a budget of 4 charges, one back per 100 ticks, used below 65% health; the shield is refilled to full at each phase change; Knock Back every 80 ticks, sidestep 30, blink 80, rift 15 to 20 s; Stun lasts half as long and cannot be repeated for 4 s; knockback resistance about 0.5 from the chestplate.

**Phases and boss bar.** Below 66% Empowered, below 33% Desperate (details in "Miniboss profile" below); a boss bar for each mage; a reset to full when no player is within 48 blocks for 30 s.

**Drops.** Player kills only: the tier loot table, one worn gear piece, one runic bauble, two distinct weighted foci; Looting adds chance, count and a second gear piece or bauble ("Miniboss profile, drops and the surge").

**In a Twilight Forest surge** a mage in the open casts at double power for 3 damage per cast, its shield stops refilling, and below 10% health it only heals, blinks, rifts and melees ([twilightforest.md](twilightforest.md)).

**Stage 1 scope.** Entity, the five Thaumcraft target mixins, the focus library, per-focus cooldowns, and a brain with `ENGAGE` / `CAST` / `REPOSITION`.

**Stage 2 scope (loadout and combat core; historical milestone — Stage 3 defence/Blink/Rift/shield/heal budget/drops and Stage 4 phases/boss bar/variants are now built below, leaving only the Hunt spawn event unbuilt).** The loadout roller, per-tier cast power and body numbers, the cast budget and burst rhythm, openers, Spell Bat upkeep, the cloud refire rule, the melee burst, Stun resistance, the Soul Sight entry, a versioned library migration, and the six-slot drop-chance fix. Spawn them with `/summon aqtweaks:cultist_battlemage`, `/summon aqtweaks:cultist_master_battlemage`, `/summon aqtweaks:cultist_swarm_mage` or `/summon aqtweaks:cultist_bombardier_mage`; there is a spawn egg for each.

## Entity

The four entities above extend `thaumcraft EntityCultist`, so the body, texture, armor layer and team rules are the stock Crimson Cultist. **Never despawns** (plan 21): persistent, `canDespawn()` is false and `BattlemageDespawnGuard` denies Forge's `AllowDespawn` for all four entities (so a mod's despawn rule cannot take one). The entity class fixes the tier (`getTier()`) and the library variant (`getVariantId()`, null for the plain mages); the tier decides health, armor, cast power, equipment and how the loadout is rolled, a variant may override body numbers, tuning and the loadout. The loadout, health and equipment are built on the first server tick, so `/summon` with NBT works. NBT keys: `BattlemageInit`, `FociIds` (the loadout, saved) and `Foci` (a list of focus ids given at spawn; it replaces the roll or the variant's fixed list, unknown or unresolved ids are dropped). The old `Tier` key is gone (a Stage 1 or 2 mage saved with it simply loads as a Tier 1 `aqtweaks:cultist_battlemage`).

**Loadout source, in order:** explicit `Foci` NBT, then the variant's `fixedLoadout` (the roller is skipped), then the variant's own `loadout`, then the tier's `loadout` roll, then the tier's `stage1Foci` fallback.

**Variant hooks** (all no-ops in the base; the variant subclasses override them): `onInitialised(lib, tier, variant)` (fresh spawn only), `onVariantTick()`, `writeVariantNbt` / `readVariantNbt`, `variantStep(target, dist, los)` (called every AI tick before melee and movement; true owns the tick), `variantPreCast(...)` (decision tick, after Spell Bat upkeep, before the normal cast; true = acted), `handlesCasting()` + `variantCast(...)` (replace the normal cast choice), plus `getAi()` (this mage's resolved tuning), `getRoleWeight(role)`, `indexOfFocus(id)`, `castFocus(index, target, ai)` and `setNextCastTick(tick)`.

* **Hands:** a melee weapon in the main hand (its attributes give melee damage; `Battlemage Melee Damage Scale` multiplies it) and a Thaumcraft caster in the off hand (`thaumcraft:caster_basic`; an off-hand entry that is not an `ItemCaster` falls back to it). Drop chance of **every** equipment slot is 0.
* **Gear (plan 14, 15, 15a).** Both tiers wear the Crimson Paladin set from the tier's `equipment` block: `crimsonrevelations:crimson_paladin_helmet` / `_chestplate` / `_leggings` (dyed 8399402; Mindful, Elite, Elite), `thaumcraft:crimson_boots` (Nimble), `thaumcraft:crimson_blade` named "Crimson Battlemage's Blade" (Serrated, RepairCost 31, five enchantments) and the Mystical `thaumcraft:caster_basic`. Each slot is `{item, nbt, enchantments}`: `nbt` is Minecraft SNBT (`123L` long suffixes, not ZenScript `as long`), parsed with `JsonToNBT`; `enchantments` are registry ids with levels, resolved with `Enchantment.getEnchantmentByLocation` at spawn because Forge hands out numeric enchantment ids per world. The blade's enchantments are Scorn I (`msmlegacy:scorn`), Spectral Edge III (`spectral_edge:spectral_edge`), Decay I (`msmlegacy:decay`), Frozen Aspect I (`msmlegacy:frozen_aspect`) and Keen Edge III (`msmlegacy:keen_edge`): the numeric ids from `/ct hand` (39, 72, 52, 44, 38) were resolved against the BALANCETEST1 save's registry (the DEVBOX save from 2026-11-17; TEST52 numbers them differently).
* **Quality Tools on a mob.** Quality Tools only applies its attribute modifiers to players, so a mob would get the bare items. `GearAttributes.applyQuality` reads each stack's `Quality.AttributeModifiers` and applies them straight to the mob's attributes (armor, knockback resistance, movement speed, attack damage); an attribute the mob does not have (attack speed, and PotionCore's magic shielding, jump height, step height and magic damage unless PotionCore registers them on mobs) is skipped and logged once. The same UUID is never applied twice.
* **No vis.** The AI never calls `ItemCaster.consumeVis`. Bytecode check (BETA26): `FocusEngine.castFocusPackage(caster, pkg, bypassCopy)` copies the package unless asked not to, calls `initialize(caster)`, `onCast` on every effect, then `runFocusPackage`; none of these spend vis, so a mob casts for free.
* **Orb colour.** Thaumcraft's `ColorHandler` colours the gauntlet from the `focus` compound in its own NBT (`ItemFocus.getFocusColor` reads the focus stack's `color` int). On each focus switch the mage writes a light display focus `{id, Count 1, tag {color}}` into the off-hand gauntlet with `ItemCaster.setFocus`; the full package stays in the entity. Vanilla compares equipment NBT each tick and sends the changed slot to tracking clients, so no packet of ours is needed.
* **Renderer.** `RenderCultistBattlemage extends RenderCultist` (stock body, armor layer, `LayerHeldItem` draws both hands).

## Casting

`EntityCultistBattlemage.castFocus` copies the focus package (`FocusPackage.copy(mage)`), multiplies its power by the tier's `focusPower` (Tier 1 1.25, Tier 2 1.5) times `Battlemage Focus Damage Scale` (a global dev multiplier, default 1.0; power scales potency, so heals, clouds and summons are stronger too), replaces its ROOT medium with a `FocusMediumRoot` holding an eye-to-target trajectory (with lead for projectile roles) and the target, then `FocusEngine.castFocusPackage(mage, pkg, true)`. It swings the off hand. Nothing casts while the mage is Stunned.

**Natural cooldown.** `ItemFocus.getActivationTime` is `max(5, (complexity / 5) * (complexity / 4))` ticks (and `CasterManager` stores it per caster entity, which a mob bypasses). Each focus has its own ready timer: that value, or the JSON `cooldownTicks`, times `Battlemage Focus Cooldown Scale`. The selector only considers foci that are off cooldown, so the mage switches foci to keep casting. `globalCastGapTicks` spaces two casts (Tier 1 24 ticks, Tier 2 16).

| Complexity | Cooldown |
| --- | --- |
| 17 | 12 ticks |
| 20 | 20 |
| 25 | 30 |
| 35 | 56 |
| 50 | 120 |

## Brain (`AIBattlemageCombat`)

One task (mutex bits 3, move and look). Each decision tick (every `decisionIntervalTicks`, 2, once the global cast gap has passed) it runs, in this order:

1. **Stun gate.** Stunned: no casts and no melee, it only stops moving.
   *(The full Stage 3 brain runs right after the Stun gate and before everything below: the defence layer `BattlemageDefense`, then target scoring via `TargetScorer`.)*
2. **Summon upkeep (plan rule 6).** `OwnedSummonTracker` counts the Spell Bats the mage owns (public `EntitySpellBat.getOwner()`, a read-only box query every `upkeep.intervalTicks` (20) within `upkeep.radius` (48), refreshed at once after a summon or cloud cast). The wanted count is `max(1, ceil(castSize / 2))` (`SummonUpkeep`; Curse Bat spawns 1 so it recasts at zero, the Swarm 5 so it recasts at 2 or fewer; `upkeep.desiredActiveSpellBats` overrides it). The ready `summon_upkeep` focus is cast at once, with no range, line of sight or budget test; only its own cooldown applies.
3. **Cast (rule 9).** `FocusSelector` takes foci off cooldown, inside `[minRange, maxRange]`, with line of sight where the focus needs it, scored by role (bolt 4.5, projectile 4, long-range cloud 6 beyond `aoeMinRange` 14, debuff 5 on a fresh target, heavy foci 2 plus 4 when the target is Stunned, slowed, wet or burning for that focus's `followUp`, openers +1 until a follow-up state exists and **+3 more on the first cast of the fight**, -1.5 for repeating the last focus, times `baseWeight`), then a weighted pick among those within 1 point of the best. A cloud focus (`refireOnExpire`, default for `aoe`) scores 0 while the mage still owns a cloud, so a long-range cloud is recast only once the last one has dissipated. Roles cast: `ranged_primary`, `ranged_heavy`, `bolt`, `bolt_heavy`, `debuff`, `aoe`, `area_denial`. Heal, Knock Back, Blink and Rift are used by the defence layer (see "Defence, shield and mobility" below), not by the selector.
4. **Melee burst (rule 10).** If nothing was cast this tick and a player is within `melee.triggerRange` (3), the mage closes to `melee.reach` (2.5) and makes `melee.burstMin` to `burstMax` (2 to 3) main-hand attacks, one per `attackIntervalTicks` (14), then retreats for `retreatTicks` (30). A burst ends after `burstMaxTicks` (60) at the latest and the next one waits `burstCooldownTicks` (100). Casts swing the off hand, melee swings the main hand. Pure timing in `MeleeBurst`.
5. **Move (rule 11).** `BandSteering`: inside `idealRange.min` (8) it backs away, outside `max` (14) or without line of sight it approaches, otherwise it strafes with a randomised flip timer. Targeting is the stock Cultist pair (`AICultistHurtByTarget`, nearest player).

**Cast budget (`CastBudget`).** Cooldowns alone would make a mob that pays no vis a metronome. The pool holds `castBudget.max` (100) energy, regenerates `regenPerSecond` (Tier 1 5.5, Tier 2 8; slowed 2026-11-19, twice), and each budgeted cast spends `complexity * costPerComplexity` (1.0). When it falls to `holdBelowFraction` (25%) the mage may not cast again until it refills to `resumeFraction` (60%): about three casts, then a 3 to 5 second opening. Summon upkeep is exempt. A cost above the maximum is capped, so a complexity-50 focus can always be cast from a full pool. `castBudget.enabled: false` (Bombardier Mage) turns the pool off; casting is then limited by the cast gap, each focus's cooldown and the variant's own rhythm.

**Stun resistance.** A Stun lasts `stun.durationScale` (0.5) as long and the mage cannot be Stunned again for `stun.immunityTicks` (80) after it ends (`StunGate`, reached through `Stun.apply` and the small `IStunResistant` interface).

**Soul Sight.** The mage always knows where players are. All four entity ids (`aqtweaks:cultist_battlemage`, `cultist_master_battlemage`, `cultist_swarm_mage`, `cultist_bombardier_mage`) are added to `Natural Soul Sight` in `config/dynamicstealth/1.12.2.116+.cfg` (pack config, set on the DEVBOX; add the same line on any other copy of the pack). Fallback: `Battlemage Soul Sight Potion Fallback` gives each mage the `dynamicstealth:soulsight` potion at spawn.

## Swarm Mage (`aqtweaks:cultist_swarm_mage`, plan 18.7)

A Tier 1 variant with a fixed loadout (Heal Self, Knock Back, Blink, Rift, Curse Bat, Spell Bat Swarm, Healing Aura, Flux Bolt, Prismatic Bolt; no area foci, no roller, bolts x2 selection weight). It brings an escort and keeps it alive.

* **Escort.** On a fresh spawn the mage queues the library's `escort` list (`variants[swarm_mage].escort`: 2 `thaumcraft:cultistknight`, 1 `thaumcraft:cultistcleric`, 1 `crimsonrevelations:cultist_archer`), created by registry id through `EntityList` (no compile dependency on Crimson Revelations; add or change entries in the JSON). Once only: `EscortSpawned` NBT. `/summon aqtweaks:cultist_swarm_mage ~ ~ ~ {Escort:0b}` skips it. The escort uuids are saved in `EscortIds` and an unfinished arrival in `EscortArrival` (a reload mid-arrival continues where it stopped). Escorts are ordinary mobs: not persistent, vanilla drops. Escorts without a target take the mage's; with no target of its own the mage takes the player an escort was hurt by (`EscortSpawner`, `syncEscortTargets`, every 10 ticks).
* **Portal arrival.** A lesser crimson portal opens on a floor spot 4 to 6 blocks away (3 blocks of headroom; none found means the escort drops beside the mage with no portal), the first escort drops 15 ticks later and then one every 10, and the portal closes with a portal puff at 70 ticks. The mage does not cast until the last one has dropped. `EntityEscortPortal` (`aqtweaks:escort_portal`, id 10, no egg, never saved) is a subclass of Thaumcraft's `EntityCultistPortalLesser` drawn by `RenderCultistPortalLesser`. It does not call `super.onUpdate()`: the stock one spawns cultists on a timer, and Crimson Revelations mixes `EntityCultistPortalLesserMixin` (a replacement cultist spawner with its own FX packet) into that same method, so skipping it bypasses both. It cannot be hit (except by the void), collided with or pushed, does not hurt players, has no loot, and removes itself after its life. Hostile-mob mods that count every `EntityMob` will see it for 3.5 seconds.
* **Ally heal (`AllyHealPlanner`, pure).** On a decision tick (after Spell Bat upkeep, before the normal cast) the mage takes the living escorts within `healAllyRange` (16) below `healAllyHpFraction` (0.6) and serves the lowest health fraction (ties: the nearer). Within `healAllyCastDistance` (2.5) it casts Healing Aura (its own cooldown `healAllyCooldownTicks`, 60, and the focus's natural cooldown). Farther: it **blinks next to the ally** if Blink is ready, otherwise **walks** toward it unless a player is inside the melee trigger range, otherwise waits. Stun stops all of it. Healing Aura has role `heal_ally`, which the normal selector never picks, so this rule is the only thing that casts it. The aura is a radius 2 cloud centred on the caster, which is why the mage has to be close.
* **Blink (`BattlemageBlink`, `BlinkPicker`, plan 3.10).** Three modes: `TOWARD_ALLY` (Swarm Mage), `REPOSITION_BAND` (a ring around the target: the Bombardier's close sequence, the return to the firing band after melee, the no-line-of-sight blink, the timed reposition blink, and the strike blink in with a tight 1 to 2 block band) and `AWAY` (the strict escape and the post-Stun escape, a far ring around the target). All modes share one cooldown (`blink.cooldownTicks`, 80); the strike combo waives it for its exit blink. `BlinkPicker` (pure) samples `blink.samples` (16) points on a 1 to 2 block ring around the ally (`blink.allyMinDistance`/`allyMaxDistance`), drops any farther than `blink.maxTravel` (24) from the mage and scores them (near the middle of the ring, short hop). `BattlemageBlink` takes the best three that pass the world checks (chunk loaded, solid ground within 4 blocks, the mage's box fits with no collision or liquid, no fire, lava, magma or cactus) and performs it like Thaumic Wonders' Blink: `SoundsTC.hhoff` at the origin, an off-hand swing, Forge's `EnderTeleportEvent` (cancelable), dismount, `attemptTeleport`, Blink's particle burst by key `focus.thaumicwonders.blink` at both ends (a vanilla portal puff when that element is not registered), `SoundsTC.hhon` and the Ender teleport sound at both ends. One shared cooldown `blink.cooldownTicks` (100) kept on the entity (`blinkReady()`); a failed blink retries after 20 ticks. Never while Stunned. A mode is a `BlinkPicker.Mode` plus a `BlinkPicker.Penalty` list (cloud avoidance, line of sight) built in `BattlemageBlink.blinkToAlly` / `blinkToBand` / `blinkToRange` / `blinkAway`.
* **Aura guard (`CloudGuard`, pure, plan 20).** While its own Healing Aura is out (an owned `EntityFocusCloud` within 12 blocks, the aura being the only cloud focus the mage carries) or it cast one within `cloudGuardCastTicks` (20), a survival player within `cloudGuardRange` (3.5) of the cloud centre **and** within `cloudGuardNearRange` (6) of the mage sets the guard off. Player within melee reach (2.5): 1 to 2 melee strikes (`cloudGuardMeleeMin`/`Max`), then Knock Back if it is ready and in Touch reach (3.0). Between melee and Touch reach: Knock Back at once. A player stepping toward the cloud who is already in Touch reach gets Knock Back with no melee first. Beyond Touch reach (or Knock Back not ready): the mage walks toward the player for at most `cloudGuardCloseTicks` (20), then fires Knock Back, or melees if only melee reach was gained. `cloudGuardCooldownTicks` (30) separates two sequences; Stun ends one with no cooldown. It runs from `variantStep` (every AI tick); on decision ticks `variantPreCast` keeps the normal casts and the normal melee burst off while it owns the mage. Casting the aura (ally heal) and Spell Bat upkeep outrank it; no blink. Knock Back from the guard does not move the next normal cast. All keys are in the `swarm_mage` variant `ai` block.
* **Heal on players.** Thaumcraft's HEAL effect (`thaumcraft.HEAL`) heals any living non-undead entity the cloud touches and damages undead; it does not check friendship. A player standing in the aura beside an escort is healed too.

## Bombardier Mage (`aqtweaks:cultist_bombardier_mage`, id 9, plan 19)

A Tier 2 variant with a fixed loadout: Heal Self, Knock Back, Blink, Rift plus all five area foci (Entropic Cloud, Fire and Ice, Tempest, Sands of Atum, Aurorian Winds). No roller, no escort, the Tier 2 body (120 HP, armor 3, power 1.5), the energy pool off (`castBudget.enabled: false`), `aoeMinRange` 6. Drops use the Tier 2 table with focus drops from its own loadout (plan 18.9, built with the drops stage).

* **Volley (`VolleyPlanner`, pure).** It replaces the normal cast choice (`handlesCasting()` / `variantCast`). One area cast every `volleyGapTicks` (64), each of the five once in a random order, then a `volleyPauseTicks` (80, 4 s) pause. The first focus of a new volley is never the last of the previous one. A focus is skipped (it keeps its place, the next ready one goes first) while its own cooldown runs (natural 120 ticks for complexity 50), while a cloud from its previous cast is still alive, or while the target is outside its band (closer than `aoeMinRange` 6, farther than the focus's `maxRange` 30, or out of sight for a focus that needs it). A volley ends at the latest `volleyMaxTicks` (200) after it started, so it cannot stall. The clock is wall time: the gap, the pause and the limit keep running while the mage is busy with the close response. The selector's old rule "an area focus waits while the mage owns any cloud" (`refireOnExpire`) is not used by this variant, so all five can be out at once.
* **One live cast per focus (`CloudLedger`).** Thaumcraft's cloud entity only knows its owner, not the focus, so clouds are attributed by observation: a scan of the owned `EntityFocusCloud`s just before a cast, the cast, a scan just after, and a 10 tick window for a cloud that spawns late. All clouds from one cast count as one. Scans run every 4 ticks while the mage has a target or a live cast. After a reload the ledger starts empty (existing clouds are unattributed; only cooldowns hold the foci back until the clouds expire). The generic `OwnedSummonTracker` cloud count is unchanged for the other mages.
* **Close response (`CloseSequence`, pure, driven from `variantStep`).** A target inside `aoeMinRange` (6): within `meleeTriggerRange` (3) it makes 1 to 2 melee attacks (`close.meleeMin`/`meleeMax`, a `MeleeBurst` with no cooldown of its own), then casts Knock Back if it is ready and the target is inside the touch reach (`Battlemage Touch Reach`, 3; the cast does not delay the volley's next shot), then blinks. A target 3 to 6 blocks away (out of both reaches) goes straight to the blink. With Blink on cooldown (or after a failed blink) it steps back on foot and tries again each tick. The sequence ends when the mage blinks, when the target is outside 6 blocks, or after `close.maxTicks` (160), when it starts over.
* **Reposition blink (`BattlemageBlink.blinkToBand`, `BlinkPicker.Mode.REPOSITION_BAND`).** Samples a ring around the **target** at `blink.bandMinDistance`..`bandMaxDistance` (8 to 14, never closer than `aoeMinRange` + 1), at most `blink.maxTravel` (24) from the mage, preferring the middle of the band and a short hop. Penalties (`CloudAvoidance`, pure): a spot inside one of **its own** clouds (radius + `blink.cloudAvoidMargin` 0.75, with a block of vertical slack) is rejected; a spot inside an **enemy** cloud (any other owner that is not a cultist) or a mine costs `blink.enemyCloudCost` (6) per cloud, so when every candidate is covered the least bad one is still taken; a spot with no line of sight to the target costs 4. Mines have no readable radius or owner, so every mine counts as an enemy zone of radius 2. Same world checks, sounds, particles, `EnderTeleportEvent` and shared 100 tick cooldown as the ally blink.
* **Tuning keys** (`ai` block of the variant, all inherit the tier): `aoeMinRange`, `castBudget.enabled`, `globalCastGapTicks` (60), `volleyGapTicks`, `volleyPauseTicks`, `volleyMaxTicks`, `close.meleeMin`, `close.meleeMax`, `close.maxTicks`, `blink.bandMinDistance`, `blink.bandMaxDistance`, `blink.cloudAvoidMargin`, `blink.enemyCloudCost`.

## Focus library (`cultist_battlemages.json`)

A default is shipped in the jar (`assets/aqtweaks/battlemage/cultist_battlemages.json`) and copied into the config folder on first run; edit the copy. Parsed by the pure `LibraryParser` (bad entries are dropped and logged, never fatal). Sections:

* `schemaVersion` (4; schema 4 corrected the Thaumcraft escort ids to `thaumcraft:cultistknight` / `thaumcraft:cultistcleric`, so a schema 3 copy is replaced). A config copy with no `schemaVersion` (stage 1) or an older one (including schemas 2 and 3) is **backed up to `cultist_battlemages.json.bak`** (`.bak2`, `.bak3` ... never overwritten) and replaced by the bundled default on the next start, with a warning in the log; port any edits from the backup. A copy at or above the current version is never touched (`LibraryMigration`).
* `ai`: the global tuning (`decisionIntervalTicks`, `idealRange`, `globalCastGapTicks`, `strafe`, `leadShots`, `repositionIntervalTicks`, `giveUpTicks`, `aoeMinRange`, `followRange`, `moveSpeed`, `startupDelayTicks`, `projectileSpeed`, `maxLeadTicks`) plus the blocks `castBudget` (now with `enabled`), `upkeep`, `melee` and `stun`, and the flat keys read by the variants: `healAllyHpFraction` (0.6), `healAllyRange` (16), `healAllyCastDistance` (2.5), `healAllyCooldownTicks` (60) for the Swarm Mage and `volleyGapTicks` (30), `volleyPauseTicks` (80), `volleyMaxTicks` (200) for the Bombardier. **Every tier and every variant may carry its own `ai` object** with any of these keys: the tuning a mage reads (`getAi()`) is the global block, then its tier's overrides, then its variant's. Shipped pacing: Tier 1 cast gap 24, refill 10/s, startup delay 40; Tier 2 cast gap 16, refill 14/s, startup delay 30; hold below 25%, resume at 60%, pool 100.
* `tiers`: per tier `health`, `armor`, `focusPower`, `shieldMax` (stored, used from stage 3), `ai` (overrides), `equipment` (slot to an item id string, or `{item, nbt, enchantments}`), `loadout` and `stage1Foci` (only a fallback if the roll fails). Tier 1: health 80, armor 1, power 1.25, shield 16. Tier 2: health 120, armor 3, power 1.5, shield 28.
* `variants` (plan 18.8): each `{id, tier, fixedLoadout | loadout, ai, weights, health, armor, focusPower, shieldMax, equipment, escort}`. A variant inherits its tier's body, equipment and drop table and replaces what it names. `fixedLoadout` is carried as listed (the roller is skipped; `healing_aura` may be listed although it is in no slot). `weights` multiplies the selector weight of a role (the Swarm Mage's bolts x2). `escort` is `[{entity, count}]`, read by the Swarm Mage once built. Shipped: `swarm_mage` (Tier 1; Heal Self, Knock Back, Blink, Rift, Curse Bat, Spell Bat Swarm, Healing Aura, Flux Bolt, Prismatic Bolt; escort 2 `thaumcraft:cultistknight`, 1 `thaumcraft:cultistcleric`, 1 `crimsonrevelations:cultist_archer`) and `bombardier_mage` (Tier 2; the fixed four plus Entropic Cloud, Fire and Ice, Tempest, Sands of Atum, Aurorian Winds; `aoeMinRange` 6, `castBudget.enabled` false, cast gap 30).
* `loadout` (per tier or variant): `fixed` (carried by every mage: Heal Self, Knock Back, Blink, Rift), `slots` (one focus rolled per slot, in order: `bolt`, `projectile`, `summon`, `aoe`), `wildcards` (extra foci from any rollable slot: 0 for Tier 1, 2 for Tier 2), `complexityBudget` (summed complexity of the rolled foci: 160 and 250; each pick reserves the cheapest possible rest, so a heavy early pick pushes later ones cheaper), `complexityBias` (0 is flat, positive favours heavy foci as the tier rises), `maxPerRole` (2), `dimensionWeightBonus` (x3 for a focus whose `dimensions` contains the mage's dimension), `fastFillerComplexity` (25: at least one of the direct slots is at or below this). Foci need `minTier` at or below the mage's tier, so Tier 1 never rolls Enervate, Aether's Bolt, Nether's Wrath, Sands of Atum or Aurorian Winds. Tier 1 carries 8 foci, Tier 2 carries 10. **Heavy foci (plan 17):** a focus at `heavyComplexity` (50) or more is heavy (Prismatic Bolt, Aether's Bolt, Nether's Wrath, Spell Bat Swarm and every area focus). `maxHeavy` caps them per mage (Tier 1 2, Tier 2 3). The area slot always keeps a heavy seat held back, so a lighter slot rolled earlier cannot take the last one; every mage therefore carries one area focus. Tier 2 also has `minHeavyArea` 1 and `minHeavyOther` 1: one heavy non-area focus is forced (picked by focus, not by slot, among Prismatic Bolt, Aether's Bolt and Nether's Wrath, weighted up for the current dimension) and placed in its own slot; the third heavy, if any, is a random wildcard. **Swarm:** Tier 1 treats Spell Bat Swarm as an ordinary summon-slot option (about 25% of mages; it counts as a heavy; a Tier 1 mage carries one bat focus only). Tier 2 has `swarmChance` 0.20: Swarm leaves the normal pools and on a hit is added on top of Curse Bat, using one wildcard seat and one heavy seat (it does not satisfy the forced non-area heavy), so a Tier 2 mage always carries Curse Bat and a fifth of them also carry the Swarm.
* `foci`: the 20 authored foci with `role`, `slot`, `minTier`, `complexity`, `baseWeight`, `dimensions`, ranges, `requireLos`, `tags` (`opener`, `followUp`), `cooldownTicks`, `windupTicks`, `summonCount` (bats one cast spawns, Spell Bat Swarm 5), `refireOnExpire` (default true for `aoe` and `area_denial`), `performed` (Blink and Rift are performed by the AI, never cast) and `nbt`, the CraftTweaker bracket line as exported (`<thaumcraft:focus_2>.withTag({...})`, with `as float` casts, parsed by `CtFocusLine` and `JsonToNBT`).

At first use `FocusLibrary` resolves each castable focus: unknown item, bad NBT, or a node key that `FocusEngine.getElement` does not know (a missing Crimson Revelations, Thaumic Augmentation or Thaumic Wonders effect) drops that focus with one log line and the mob keeps working.

## Thaumcraft mixins (`mixins.aqtweaks.thaumcraft.json`)

| Mixin | Target | What it does |
| --- | --- | --- |
| `MixinFocusMediumTouch` | `FocusMediumTouch.supplyTrajectories` / `supplyTargets` (Bolt inherits both) | For a non-player caster runs a copy of the stock ray-trace with a mob reach: 16 blocks for Bolt (the stock constant), `Battlemage Touch Reach` (3) for Touch. Stock casts the caster to `EntityPlayer` for reach (a `ClassCastException` on a mob) and returns no targets for any non-player. Players run the stock code. Bolt's own `execute` ray-traces without these, so a plain Bolt already worked; the split-target and Touch effects need targets. |
| `MixinEntityUtilsFriendly` | `EntityUtils.isFriendly` | A valid player (survival or adventure, alive) is never friendly to a Battlemage, whatever the PvP flag says. |
| `MixinEntityFocusProjectileSeek` | `EntityFocusProjectile.onUpdate` seek scan | A Battlemage-owned Seek Hostiles projectile (`special == 2`) only considers valid players, the mob's attack target first. View cone, line of sight, steering and the 5-tick scan are stock. |
| `MixinEntitySpellBatTarget` | `EntitySpellBat.findTargetToAttack` | A Battlemage's hostile bat hunts players only (attack target first, within the stock 12 blocks). Heal bats stay stock. |
| `MixinEntityFocusCloud` | `EntityFocusCloud.onUpdate` target scan | A Battlemage cloud with no Heal effect affects valid players only; a Heal-only cloud affects the owner and friends, never players; a mixed package counts as hostile. Classified once per cloud. Other clouds stay in the list. |
| `MixinFocusEffectBlockPlacement` | `FocusEffectFire.execute` / `FocusEffectFrost.execute` `World.setBlockState` calls | Redirects the block placement: a non-player caster places nothing unless `Allow Mob Focus Block Placement` is on. Players unaffected; the damage and status effects stay. |

Every mixin acts only on a non-player caster or a Battlemage owner; player casts and other mobs run stock code. `Battlemage Seek Players Only` off restores stock behavior for the three target mixins. Conflict check (2026-11-17, DEVBOX jars): Thaumcraft Fix and Thaumic Tweaker mix into `EntityUtils` (drops and runic shielding) and Tweaker into `FocusMediumBolt.execute` (sound), none on `isFriendly`, the seek scan, `EntityFocusCloud`, `EntitySpellBat` or the two `supply*` methods.

## Config (`aqtweaks_battlemage.cfg`)

| Setting | Default | Effect |
| --- | --- | --- |
| Enable Cultist Battlemage | true (restart) | Registers the four entities (Battlemage, Master Battlemage, Swarm Mage, Bombardier Mage) |
| Battlemage Focus Damage Scale | 1.0 | Global dev multiplier on every cast's power, on top of each tier's `focusPower` (1.25 / 1.5) |
| Battlemage Melee Damage Scale | 1.0 | Multiplies the weapon's melee damage (unsaved attribute modifier) |
| Battlemage Soul Sight Potion Fallback | false | Gives each mage the `dynamicstealth:soulsight` potion at spawn; leave off when the `Natural Soul Sight` list carries the mage |
| Battlemage Focus Cooldown Scale | 1.0 | Multiplies every focus cooldown |
| Battlemage Seek Players Only | true | The player-only target mixins above |
| Battlemage Seek Priority | ATTACK_TARGET | `ATTACK_TARGET`, `NEAREST` or `LOWEST_HEALTH` |
| Battlemage Touch Reach | 3.0 | Reach of a mob-cast Touch focus |
| Battlemage Shield Enabled | true | The faked runic shield (16 / 28 absorption) and its hit cue |
| Battlemage Shield Drains Aura | false | Each recharged point costs 1 vis from the chunk aura |
| Battlemage Blink Enabled | true | Every Battlemage teleport except the stuck-in-a-wall escape |
| Battlemage Rift Enabled | true | Tunnelling through walls |
| Allow Real Rift Holes | true | The Rift opens real temporary Thaumcraft holes; off: it only teleports |
| Allow Mob Focus Block Placement | false | Fire and Frost cast by a mob place fire, ice or snow; off keeps the damage but places nothing |
| Battlemage Max Move Speed | 4.3 | The most a Battlemage can cover on the ground, in blocks per second (4.3 walking, 5.6 sprinting player); blinks, knockback and dodge hops are exempt; 0 turns it off |
| Battlemage Boss Bar | true | A boss bar (name, health, phase colour) for each Battlemage near a player |
| Battlemage Cast Damage Cap Fraction | 0 (off) | One hit from a Battlemage cannot take more than this share of the target's maximum health |
| Battlemage Drops | true | The loot table, gear, bauble and focus drops of a player kill |
| Table Looting Chance Per Level | 0.05 | Looting adds this to each table chance and 1 to each maximum count per level |
| Gear Extra Chance Per Looting | 0.15 | Chance per level for a second worn piece (at most 2) |
| Bauble Extra Chance Per Looting | 0.15 |  |
| Fixed Focus Drop Weight | 1 | Weight of a fixed-slot focus (Heal Self, Knock Back, Blink, Rift) in the two-focus drop pick |
| Rolled Focus Drop Weight | 3 | Weight of a rolled focus |
| High Tier Focus Drop Weight | 5 | Weight of a Tier 2 only focus |
| Battlemage Debug Log | false | Logs the loadout and each cast |

## Verify (in game)

1. `/summon aqtweaks:cultist_battlemage`: a Crimson Cultist body with a blade in the main hand and the caster in the off hand, no log errors from `AQTweaks-Battlemage` (`[battlemage library] N foci ready, 2 tiers, 2 variants`).
2. It engages a player, holds 8 to 14 blocks, strafes, and its gauntlet orb changes colour as it switches foci (Frost, Flux Bolt, Entropic Cloud beyond 14 blocks).
3. A Flux Bolt reaches a player 16 blocks away; Tier 2 Prismatic Bolt and Enervate split-target bolts apply their effects.
4. With `pvp=false` its Seek Hostiles projectiles and clouds still hit a player and ignore a nearby villager or animal.
5. A stunned mage (Shock) does not cast.
6. A player's own Touch and Bolt casts, and other mods' mob casts, behave as before.

### Stage 2 checks

7. **First start after the update:** the log warns that `cultist_battlemages.json` was saved as `.bak` and replaced; `/summon aqtweaks:cultist_battlemage` then shows 80 health and `/summon aqtweaks:cultist_master_battlemage` 120 (use `Battlemage Debug Log` to print the loadout). The file now has `"schemaVersion": 3`.
8. **Loadouts:** summon 10 Tier 1 mages: each carries 8 foci (Heal Self, Knock Back, Blink, Rift plus a Bolt, a Projectile, a summon, an AOE), they differ between mages, none carries Enervate, Aether's Bolt, Nether's Wrath, Sands of Atum or Aurorian Winds, none carries both bats, and none has more than 2 heavy foci (one is always an area focus). Master Battlemages carry 10, with one area focus and one heavy bolt or projectile each, at most 3 heavy foci in all, Curse Bat always, and Spell Bat Swarm on about one in five. In the Nether a Master Battlemage carries Nether's Wrath noticeably more often. `/summon aqtweaks:cultist_battlemage ~ ~ ~ {Foci:["flame","frost"]}` carries exactly those.
8a. **Gear:** a summoned mage wears the Crimson Paladin helmet, chestplate and leggings (dark red), Crimson boots, holds the named "Crimson Battlemage's Blade" with five enchantments and a Mystical caster; armor shows about 19 (tier 1) or 21 (tier 2) with the Quality bonuses. Hover or `/data get` the stacks to check the Quality tags.
8b. **Variants:** `/summon aqtweaks:cultist_swarm_mage` carries exactly Heal Self, Knock Back, Blink, Rift, Curse Bat, Spell Bat Swarm, Healing Aura, Flux Bolt, Prismatic Bolt (no escort yet); `/summon aqtweaks:cultist_bombardier_mage` carries the fixed four plus the five area foci, casts without the energy pool and starts casting areas from 6 blocks.
9. **Bursts:** it opens with about three casts, then holds back for a few seconds (it strafes or melees) before the next; the gap between two casts is 40 ticks (Tier 1) or 30 (Tier 2); a Tier 2 cast hits harder than Tier 1 (power 1.5 vs 1.25).
10. **Spell Bats:** a mage with a Curse Bat recasts within a second of its bat dying; a Swarm mage recasts once 2 or fewer bats are left, even mid-strafe or while out of line of sight. A long-range cloud is not stacked: it is recast only after the last cloud has gone.
11. **Melee burst:** stand next to it while its foci cool down: it swings its weapon 2 to 3 times, then backs off and casts; it does not melee while Stunned.
12. **Stun resistance:** Shock stuns it for about half the normal time and a second Shock within about 4 seconds of the first ending does nothing.
13. **Soul Sight:** with the line in the Dynamic Stealth config it keeps targeting a hidden (sneaking, dark) player at range.
14. **No armor drops:** kill one with Looting: no worn armor or weapon drops.

### Escort ids and despawning checks (plan 21)

19. **All four escorts:** `/summon aqtweaks:cultist_swarm_mage`: the portal drops 2 Crimson Knights, 1 Crimson Cleric and 1 Crimson Archer. The Thaumcraft ids are `thaumcraft:cultistknight` and `thaumcraft:cultistcleric` (no underscore); an id that is not registered is skipped with one WARN per id (`Swarm Mage escort entity '...' is not a registered mob`), checked at the first Swarm Mage spawn. The first start after the update logs the schema 3 to 4 replacement (the old copy is saved as `.bak`).
20. **Never despawn:** leave a mage and its escorts far from every player (past 128 blocks, or wait in a quiet spot) and come back: all are still there. Kill the Swarm Mage, wait or move away: its surviving escorts now despawn like any mob (the `aqtweaks_escort` tag is removed from loaded ones; escorts that were unloaded when it died are released when they next load, from the `aqtweaks_released_escort_owners` data, which forgets an owner after about 5 in-game days). An escort is tagged, not persistent, because vanilla's persistence flag cannot be cleared again.

### Swarm Mage aura guard checks

18. **Guard:** `/summon aqtweaks:cultist_swarm_mage`, let an escort get hurt (below 60%) so the mage casts Healing Aura, then walk into the cloud: the mage hits you once or twice and casts Knock Back (a player already inside reach who steps toward the cloud gets Knock Back with no melee). From 4 to 6 blocks it walks in first, for at most a second, then pushes. A second sequence cannot start for 30 ticks. Stunned, it does nothing. It does not blink for this.

### Bombardier checks

15. **Volley:** `/summon aqtweaks:cultist_bombardier_mage` and stand 10 to 25 blocks away in the open: after its startup delay it casts one area focus about every 1.5 s (30 ticks), all five different, then goes quiet for about 4 s, then a new volley whose first focus is not the last one of the previous (`Battlemage Debug Log` prints each cast). Up to five clouds are on the ground at once; a focus is not recast while its own clouds are still out.
16. **Close response:** walk up to it mid-volley: it hits you once or twice, casts Knock Back, then blinks 8 to 14 blocks away (with the Blink sounds and burst) and the volley carries on. At 4 to 5 blocks it blinks at once without hitting. It never lands inside one of its own clouds, and prefers spots outside your clouds. With Blink on cooldown it backs away on foot.
17. **No budget stall:** it keeps the volley rhythm with no long holds; a Stunned mage casts nothing and resumes its volley afterwards.

## Defence, shield and mobility (plan 22, stage 3)

All of this lives in the shared base, so the Battlemage, Master Battlemage, Swarm Mage and Bombardier Mage all have it unless noted. Tuning: the `ai` blocks `shield`, `healBudget`, `defense`, `rift`, `unstick` (global, overridable per tier or variant).

**Priority.** After the Stun gate: (1) the strict defensive escape, (2) the "no firing line" blink, (3) the offensive rift, (4) peel, (5) retreat and heal; then the normal upkeep, cast, melee and move. Sidestepping runs alongside and never takes the tick. Stun still blocks every action; only the stuck-in-a-wall escape ignores it.

- **Runic shield (`BattlemageShield`, `ShieldModel`).** Vanilla absorption, as a player's stock runic shield: 16 (Tier 1) / 28 (Tier 2) points, full at spawn, +1 point per 40 ticks, 40 ticks of wait once it reaches 0. Only damage lowers it. Whenever it is up at impact the real `PacketFXShield` rune burst and the runic charge sound go out within 32 blocks (`LivingHurtEvent`, effects only, no damage change). A broken or still-rebuilding shield (below half) moves the preferred band out by 3 blocks and halves the peel cooldown. A big hit (15% of max health) or a break staggers casting for 10 ticks.
- **Heal budget (`HealBudget`).** Three charges, one back per 200 ticks. Heal Self at or below 60% health when a charge and the focus are ready and no player is within melee pressure (4 blocks); each heal spends a charge. Swarm Mage ally heals (Healing Aura on escorts) are outside the budget.
- **Retreat.** At or below 50% health with a charge available the mage withdraws (the Swarm Mage toward its nearest escort), breaks sight where it can, casts no attacks and heals; it returns at 75% health, with no charges, or after 160 ticks, then cannot retreat again for 200. The Bombardier does not retreat.
- **Peel.** A player within 3 blocks and Knock Back ready: cast it (own cooldown 100 ticks so melee bursts still happen), then step back for 30 ticks. The Bombardier keeps its own close sequence.
- **Sidestep.** A projectile on a collision course within 12 blocks, or a bow drawn at the mage, gets a sideways hop (40 tick cooldown).
- **Strict defensive escape.** All four: health below 25%, a player within 4 blocks, no escape on foot (a player that close for 30 ticks, or the mage is cornered), Knock Back spent or used within 100 ticks. It blinks 10 to 18 blocks from the player, hidden if possible, never into its own clouds; cornered against a wall it prefers the Rift; if the blink fails it falls back to the Rift.
- **Blink after a melee burst.** With Blink ready, half the time the mage blinks back to its 8 to 14 block firing band. With no line of sight for 60 ticks it blinks to a spot that has one.
- **Rift (`RiftPicker`, `BattlemageRift`, `ReachabilityProbe`).** A straight two-block-high tunnel through a wall of at most 12 blocks (24 holes), opened as real `BlocksTC.hole` blocks (closing after 120 ticks; a player still inside is stuck in the wall) and the mage teleports through in the same tick. Anything a real Rift refuses (bedrock, hardness -1, blacklisted blocks, tile entities) invalidates that line. Defensive: lands out of the player's sight and farther away. Offensive: used when the player is behind a wall with no sane walking route (no path, a path that stops short, or a detour over 2.5 times the distance, probed once per 20 ticks, at most 4 probes per tick pack-wide) and sight has been lost for 20 ticks; lands on the player's side. Own cooldown of 300 to 400 ticks (rolled per use), separate from Blink.
- **Stuck in a wall.** Inside an opaque block for 3 ticks: teleports to the nearest safe standing spot within 16 blocks (up preferred), ignoring cooldowns, health, Stun and the rule; if another mod cancels the teleport event it moves directly.
- **Hazards.** On fire: heads for water within 6 blocks. Inside a player's cloud or a mine: steps out of it.
- **Target scoring.** Every 20 ticks, with two or more players around: the last attacker, low health, a player casting, line of sight and distance, with a stickiness bonus for the current target.
- **Opponent reading.** Against a raised vanilla shield facing the mage, bolts and projectiles weigh 0.35 and clouds, mines and debuffs 1.6.

### Stage 3 checks

21. **Shield:** a fresh mage shows the rune burst and hears the charge sound when hit; absorption hearts drain from 16 / 28; after it breaks, nothing comes back for 2 s, then +1 per 2 s; while broken it keeps farther away.
22. **Heal and retreat:** hurt it to 60%: it heals itself up to three times, then not again for 10 s; at 50% it withdraws and heals instead of attacking; the Swarm Mage withdraws toward its escorts.
23. **Peel:** walk up to it: Knock Back, then it steps back. The Bombardier still does its melee, Knock Back, blink sequence.
24. **Escape:** below 25% health, with a player hugging it for 1.5 s and Knock Back spent, it blinks (or rifts, if cornered) out of sight.
25. **Rift:** stand behind a 3 to 8 block wall with no way round: it opens a tunnel and lands on your side; the tunnel closes after 6 s and restores the blocks. Bedrock or a chest in the way: it does not rift. Two rifts are at least 15 s apart. With `Allow Real Rift Holes` off no blocks change.
26. **Stuck:** `/tp` it inside a block: it is out within a few ticks, even stunned.
27. **Grief:** a mage casting Fire or Frost leaves no fire, ice or snow blocks (damage still applies); with `Allow Mob Focus Block Placement` on it does.
28. **Reading:** a player behind a raised shield sees clouds more than bolts; a player with two players near gets re-targeted when one hits it.

## Miniboss profile, drops and the surge (plan 3.13 and 13, stage 4)

Built 2026-11-19, not verified in game.

* **Pacing (slowed 2026-11-19).** Tier 1: `globalCastGapTicks` 40, `castBudget.regenPerSecond` 5.5, `resumeFraction` 0.65. Tier 2: 30, 8, 0.65. Bombardier: `globalCastGapTicks` 60, `volleyGapTicks` 64, `volleyPauseTicks` 180, `volleyMaxTicks` 300 (cut again on 2026-11-20 after testing: area shots 3.2 s apart, 9 s between volleys). `focusPower` (1.25 / 1.5) is unchanged; it is the next knob if damage is still high.
* **Ground speed limit (2026-11-20).** `EntityCultistBattlemage.capGroundSpeed` shortens any ground step longer than `Battlemage Max Move Speed` (4.3 blocks per second, a walking player) along its own path and scales the velocity. Done on the real step because the speed attribute (0.3 base, +10% from the boots) times the navigator multipliers (1.2 closing in, 1.3 fleeing, 1.4 out of hazards) does not map directly to a player's speed. Steps over 2 blocks (blink, rift), hurt ticks, air and water are left alone.
* **Heals and escapes (2026-11-20).** `ai.healBudget`: 4 charges, one per 100 ticks (changed from 160 on 2026-11-20), `healHpFraction` 0.65, `phases.desperateHealRegenFactor` 0.6. The Swarm Mage's Healing Aura has its own pool: `healAllyBudgetMax` 5 charges, one back per `healAllyBudgetRegenTicks` 100, separate from the self-heal charges; `healAllyCooldownTicks` (60) stays as the shortest gap between two aura casts. `ai.defense`: `peelCooldownTicks` 80, `sidestepCooldownTicks` 30, `awayHpFraction` 0.30; `ai.blink.cooldownTicks` 80. The shield is refilled to full on every phase change (Empowered and Desperate). Casting power, melee damage, health and shield size are unchanged.
* **Boss bar.** `BossInfoServer` per mage (`Battlemage Boss Bar`), shown to the players tracking it, name from the entity, percent from health; the bar colour is red, purple (Empowered) and white (Desperate).
* **Phases (`BattlemagePhase`, `ai.phases`).** Below 66% health "Empowered": cooldowns x0.75, cast-budget regeneration x1.25, +1 desired Spell Bat, and the faked shield is refilled once. Below 33% "Desperate": cooldowns x0.6, regeneration x1.5, +1 Spell Bat, Blink cooldown 60 ticks, heal charges regenerate in 75% of the time, and the first cast after every blink is the opener choice. A phase only rises (a heal never lowers it); a change plays a growl, a particle ring and an action-bar line to players within 48 blocks (`aqtweaks.battlemage.phase.N`).
* **Close-in melee (2026-11-19).** When the cast budget starts holding the mage back it rolls once (`ai.melee.closeInChance` 0.6): on success, and with the target within `closeInMaxRange` (14) and a melee burst available, it walks in (APPROACH overrides the band while it has line of sight) for the usual 2 to 3 blow burst, after which the existing blink-back or retreat takes it out again. Ends when the hold ends, the burst starts or finishes, or sight is lost. Not used by the Bombardier (no cast budget).
* **Strike blink (the offensive blink of plan 3.10, built 2026-11-20).** Half of the close-in rolls (`ai.melee.strikeBlinkChance` 0.5, target within `strikeBlinkMaxRange` 24, Blink ready) blink to 1 to 2 blocks from the target instead of walking, start the 2 to 3 blow burst at once, and when the burst ends blink back to the firing band with the entry blink's cooldown waived (windows added 2026-11-20: 10 ticks standing still after the blink in before the first blow, and 10 ticks after the last blow before the blink out; `strikeInWindowTicks`, `strikeOutWindowTicks`). A Stun that cuts the combo short makes the mage, once the Stun is over, cast Knock Back (if ready and the player is close) and then escape-blink (the blink cooldown waived, a rift as the fallback) (`BattlemageDefense.afterMelee(..., strike)`), then the normal 100 tick cooldown runs. It was in the plan and stage 3 text but had not been built; only the return-to-band blink after melee existed.
* **Timed reposition blink (2026-11-20, plan 3.10).** Every `ai.blink.repositionIntervalTicks` (200) a mage with Blink ready, not in a burst, strike combo, close-in or volley (so not the Bombardier), rolls `repositionChance` (0.4) and on success blinks to a new spot on its firing band around the target (out of a crowding player's reach, in on one who kites, around one who holds still). A failed pick retries after 40 ticks. Shares the blink cooldown.
* **Reset rule.** With no player within `ai.phases.resetRange` (48) for `resetAfterTicks` (600) a worn mage (hurt, in a phase, or shield down) returns to full health, full shield, phase 0, full cast budget and heal charges, and drops its target.
* **Per-hit damage cap** (`Battlemage Cast Damage Cap Fraction`, default 0 = off): a `LivingHurtEvent` hook on hits whose true source is a Battlemage caps the amount at that share of the victim's maximum health.
* **Drops (`BattlemageDrops`, `BattlemageDropEvents`, JSON `drops` and `baubleDrops` per tier).** Only on a player kill (`isRecentlyHit`). The tier table (coin, crimson fabric and plate, void seed, rare and legendary runes materials, the loot crate `lootchests:loot_crate:3`, the spatial rift tear; Tier 2 larger) with Looting adding chance and max count; one worn gear piece by weight (sword 1, chestplate 1, caster 2, helmet 2, leggings 2, boots 2) and one faked runic bauble (ring 5, amulet 3, girdle 2 for Tier 2), a second of each with Looting (`+15%` per level, cap 2); and **two distinct foci** cloned from the mage's own loadout (authored item and tag, performed foci included), weighted fixed 1, rolled 3, Tier 2 only 5, or a focus's `dropWeight`. Variants use their tier's table and their own loadout (plan 18.9). Vanilla equipment drop chances stay 0. The JSON file has no schema bump: an existing config copy needs the new keys copied in (the DEVBOX copy was replaced by the bundled file).
* **Gear and Quality Tools.** The paladin armor, boots, blade and Mystical caster with their Quality modifiers were built with stage 2.1; the modifiers are applied directly by `GearAttributes.applyQuality` (see "Entity" above), because Quality Tools itself only handles players. `potioncore.*` attributes only apply if PotionCore registers them on the mob.
* **Surge (Ley-Rupture) interaction.** In an active Twilight Forest surge a mage standing outdoors is overchanneled: its casts are doubled in power and each costs it backlash damage (never warp); its shield stops refilling. Below `ai.defense.surgeLowHealthFraction` (10%) health it may only cast healing foci, blink and rift, and melee (`SurgeCastRules`): ranged, bolt, debuff, area, summon upkeep and the Knock Back peel are held back until it is healed above the threshold or out of the surge. See [twilightforest.md](twilightforest.md).
* **Not built from the plan.** The spawn announcement belongs to the Cultist Hunt event (not built), so there is no chat line on spawn; no per-cast (rather than per-hit) cap.
* Tests: `Stage4Test`, `SurgeCastRulesTest`; files `BattlemagePhase`, `DropEntry`, `DropRules`, `BattlemageDrops`, `BattlemageDropEvents`, `SurgeCastRules`.

### Stage 4 checks
29. **Boss bar and phases:** `/summon aqtweaks:cultist_battlemage`, hurt it below 66%: a growl, a ring, an action-bar line, the bar turns purple and the shield is full again; below 33% it turns white and the mage blinks more often.
30. **Drops:** kill a Tier 1 and a Tier 2 mage yourself: you get the table items (coin, fabric, plate, rift tear, crate), one worn piece, one ring or amulet (or girdle), and two different foci; a kill by lava or a mob drops none of it. With Looting III extra pieces appear sometimes.
31. **Reset:** hurt a mage, walk more than 48 blocks away for 30 s, return: full health, phase 0.
32. **Surge:** in a Twilight Forest surge, a mage in the open takes 3 damage per cast and its shield does not recharge; hurt it below 10% and it only heals, blinks and melees.
33. **Strike blink combo:** stay 10 to 25 blocks away during a cast hold: sometimes the mage walks in, sometimes it blinks next to you, stands still about half a second, hits 2 to 3 times, waits about half a second and blinks back out. Shock it during the combo: after the Stun it casts Knock Back and blinks away.
34. **Reposition blink:** over a minute of fighting at range the mage now and then (about one 10 second check in three) blinks to a new spot on its band without attacking.
35. **Speed:** walk away from a mage in the open: it never gains on a walking player, and it does not run faster while closing in, fleeing or leaving a hazard; blinks, knockback and dodge hops are not limited.
36. **Heals and escapes:** hurt a mage below 65% health: it heals up to four times at a charge per 8 seconds; at each phase change its shield is full again; below 30% health and crowded with Knock Back spent it escape-blinks.

## Tuning key reference (`cultist_battlemages.json`)

Values are the shipped defaults (2026-11-20). Paths are inside the `ai` block (global, then each tier, then each variant may override any key; an override only needs the keys it changes). Times are ticks (20 per second), distances blocks.

### Pacing and movement
| Key | Default | Use |
| --- | --- | --- |
| `decisionIntervalTicks` | 2 | How often the mage decides what to cast. |
| `globalCastGapTicks` | 24 global; Tier 1 **40**, Tier 2 **30**, Bombardier **60** | Minimum ticks between two casts. |
| `startupDelayTicks` | 40 global; Tier 2 30 | Quiet time after spawn before the first cast. |
| `idealRange.min` / `.max` | 8 / 14 | The firing band the mage tries to hold. |
| `strafe.flipMinTicks` / `.flipMaxTicks` / `.step` | 20 / 50 / 3 | How often it flips strafe direction and how far each step goes. |
| `repositionIntervalTicks` | 10 | Re-path interval while moving. |
| `giveUpTicks` | 200 | Ticks without a target before the fight ends. |
| `moveSpeed` | 1.0 | Navigator speed multiplier (the actual ground speed is also capped by `Battlemage Max Move Speed`, 4.3 blocks per second). |
| `followRange` | 40 | Target detection range. |
| `leadShots`, `projectileSpeed`, `maxLeadTicks` | true, 1.5, 8 | Aim lead for projectiles. |
| `aoeMinRange` | 14 (Bombardier 6) | Nearest distance an area focus is used. |

### Cast budget and upkeep
| Key | Default | Use |
| --- | --- | --- |
| `castBudget.enabled` | true (Bombardier false) | The energy pool; off uses the volley rhythm instead. |
| `castBudget.max` | 100 | Pool size. |
| `castBudget.regenPerSecond` | 10 global; Tier 1 **5.5**, Tier 2 **8** | Energy regained per second. |
| `castBudget.costPerComplexity` | 1.0 | Energy spent per point of focus complexity. |
| `castBudget.holdBelowFraction` / `.resumeFraction` | 0.25 / 0.6 (tiers 0.65) | Hold casting at or below the first, resume at the second: the burst rhythm. |
| `upkeep.intervalTicks` / `.radius` / `.desiredActiveSpellBats` | 20 / 48 / 0 (auto) | Spell Bat upkeep: check interval, search radius, how many bats to keep up (phases add 1). |

### Melee and the strike combo
| Key | Default | Use |
| --- | --- | --- |
| `melee.triggerRange` / `.reach` | 3 / 2.5 | Melee starts inside the first; blows land inside the second. |
| `melee.attackIntervalTicks` | 14 | Ticks between blows. |
| `melee.burstMin` / `.burstMax` / `.burstMaxTicks` | 2 / 3 / 60 | Blows per burst and the burst's time cap. |
| `melee.burstCooldownTicks` | 100 | Minimum ticks between bursts. |
| `melee.retreatTicks` | 30 | Walk-back time after a burst. |
| `melee.closeInChance` / `.closeInMaxRange` | 0.6 / 14 | Chance, rolled when a cast hold starts, that the mage closes in for melee; the longest distance it will. |
| `melee.strikeBlinkChance` / `.strikeBlinkMaxRange` | 0.5 / 24 | Share of close-ins that blink next to you instead of walking, and the longest blink. |
| `melee.strikeInWindowTicks` / `.strikeOutWindowTicks` | 10 / 10 | Standing still after the blink in before the first blow, and after the last blow before the blink out. |
| `stun.durationScale` / `.immunityTicks` | 0.5 / 80 | Stun lasts half as long; no new Stun for 4 s after one ends. |

### Blink and rift
| Key | Default | Use |
| --- | --- | --- |
| `blink.cooldownTicks` | 80 | The one cooldown shared by every blink mode. |
| `blink.repositionIntervalTicks` / `.repositionChance` | 200 / 0.4 | Timed reposition blink: how often it is considered and the chance it happens. |
| `blink.samples` / `.maxTravel` | 16 / 24 | Candidate spots sampled and the longest blink. |
| `blink.bandMinDistance` / `.bandMaxDistance` | 8 / 14 | Landing ring around the target for band blinks. |
| `blink.allyMinDistance` / `.allyMaxDistance` | 1 / 2 | Landing ring around an ally (Swarm Mage). |
| `blink.cloudAvoidMargin` / `.enemyCloudCost` | 0.75 / 6 | Keep-out margin around clouds and the score penalty for landing in an enemy cloud. |
| `rift.enabled` | true | Turns the rift off. |
| `rift.cooldownMinTicks` / `.cooldownMaxTicks` | 300 / 400 | Rift cooldown, rolled between the two. |
| `rift.maxThickness` / `.maxHolesPerCast` | 12 / 24 | Longest wall it will dig and most holes opened at once. |
| `rift.detourFactor` / `.pathProbeIntervalTicks` | 2.5 / 20 | How much longer the walking route must be before the offensive rift; how often reachability is probed. |
| `rift.holeDurationTicks` | 120 | How long the holes stay open (about 6 s). |
| `unstick.suffocationTicks` / `.emergencyRadius` | 3 / 16 | Ticks inside a block before the emergency blink and how far it searches. |

### Defence, healing and shield
| Key | Default | Use |
| --- | --- | --- |
| `healBudget.max` / `.regenTicks` | **4** / **100** | Heal charges and ticks per charge back. |
| `healBudget.healHpFraction` | **0.65** | Heal in place below this health. |
| `healBudget.retreatHpFraction` / `.retreatResumeFraction` | 0.5 / 0.75 | Withdraw to heal at the first, return at the second. |
| `healBudget.retreatMaxTicks` / `.retreatCooldownTicks` | 160 / 200 | Longest withdrawal and the wait before the next. |
| `defense.peelRange` / `.peelCooldownTicks` | 3 / **80** | Knock Back when a player is this close; its cooldown. |
| `defense.sidestepCooldownTicks` / `.sidestepRange` / `.sidestepImpulse` | **30** / 12 / 0.55 | Dodging projectiles and drawn bows. |
| `defense.awayHpFraction` | **0.30** | Strict escape blink below this health (with the other three conditions). |
| `defense.awayCloseRange` / `.awayPressureTicks` / `.awayKnockBackWindowTicks` | 4 / 30 / 100 | The escape conditions: player within 4, 30 ticks of pressure, Knock Back spent or used in the last 100. |
| `defense.awayBandMinDistance` / `.awayBandMaxDistance` | 10 / 18 | Where the escape blink lands. |
| `defense.blinkAfterMeleeChance` | 0.5 | Chance to blink back to the band after a melee burst. |
| `defense.noLosBlinkTicks` | 60 | Blink to a spot with sight after this long without it. |
| `defense.targetRepickTicks` | 20 | Re-score targets this often with two or more players near. |
| `defense.surgeLowHealthFraction` | 0.10 | Surge rule: below this health only heal, blink, rift, melee. |
| `shield.rechargeIntervalTicks` / `.breakWaitTicks` | 40 / 40 | One shield point per interval; wait after a break. |
| `shield.startFull` / `.brokenBandBonus` / `.resumeFraction` | true / 3 / 0.5 | Starts full; a broken shield pushes the band out by 3 until half is back. |
| `shield.staggerTicks` / `.staggerDamageFraction` | 10 / 0.15 | Brief stagger after a hit of 15% of max health or one that breaks the shield. |

### Phases, reset and drops
| Key | Default | Use |
| --- | --- | --- |
| `phases.empoweredBelow` / `.desperateBelow` | 0.66 / 0.33 | Health fractions for the two phases. |
| `phases.empoweredCooldownScale` / `.desperateCooldownScale` | 0.75 / 0.6 | Focus cooldown multipliers. |
| `phases.empoweredRegenScale` / `.desperateRegenScale` | 1.25 / 1.5 | Cast budget regeneration multipliers. |
| `phases.empoweredExtraBats` / `.desperateExtraBats` | 1 / 1 | Extra Spell Bats kept up. |
| `phases.desperateBlinkCooldownTicks` | 60 | Blink cooldown cap in Desperate. |
| `phases.desperateHealRegenFactor` | 0.6 | Heal charge interval multiplier in Desperate (100 becomes 60). |
| `phases.resetRange` / `.resetAfterTicks` | 48 / 600 | No player within the range for this long resets a hurt mage; 0 range turns it off. |

### Bombardier and Swarm Mage (variant `ai` blocks)
| Key | Default | Use |
| --- | --- | --- |
| `volleyGapTicks` / `volleyPauseTicks` / `volleyMaxTicks` | 64 / 180 / 300 | Ticks between area casts in a volley, between volleys, and the volley's time cap. |
| `close.meleeMin` / `.meleeMax` / `.maxTicks` | 1 / 2 / 160 | Close-range response: melee hits, then Knock Back, then blink; it restarts after the time cap. |
| `healAllyHpFraction` / `Range` / `CastDistance` / `CooldownTicks` | 0.6 / 16 / 2.5 / 60 | Swarm Mage heals an escort below this health, within this range, casting the aura when this close, at most this often. |
| `healAllyBudgetMax` / `healAllyBudgetRegenTicks` | 5 / 100 | The Swarm Mage's own Healing Aura charges: a pool of 5, one back every 100 ticks (separate from the self-heal budget); the cooldown above stays as the minimum gap between casts. |
| `cloudGuardRange` / `NearRange` / `CastTicks` / `MeleeMin` / `MeleeMax` / `CooldownTicks` / `CloseTicks` | 3.5 / 6 / 20 / 1 / 2 / 30 / 20 | The Swarm Mage's guard of its Healing Aura cloud. |

### Tier, variant and focus rows
| Where | Keys | Use |
| --- | --- | --- |
| `tiers[]` | `health` 80 / 120, `armor` 1 / 3, `focusPower` 1.25 / 1.5, `shieldMax` 16 / 28 | Body and the cast power multiplier (also scales Heal Self). |
| `tiers[].loadout` | `fixed`, `slots`, `wildcards` (0 / 2), `complexityBudget` (160 / 250), `complexityBias`, `maxPerRole`, `dimensionWeightBonus`, `heavyComplexity`, `maxHeavy` | How the roller builds the focus set. |
| `tiers[].equipment`, `drops`, `baubleDrops` | item, `nbt`, enchantments; `item`, `meta`, `nbt`, `chance`, `min`, `max`, `weight` | Worn gear and the loot tables. |
| `variants[]` | `tier`, `fixedLoadout` (or its own `loadout`), `weights` (role weight multipliers), `escort`, `ai`, optional `equipment`, `health`, `armor`, `focusPower`, `shieldMax` | A variant names its tier and overrides only what differs. |
| `foci[]` | `id`, `minTier`, `role`, `slot`, `complexity`, `baseWeight`, `dimensions`, `minRange`, `maxRange`, `requireLos`, `cooldownTicks` (null = natural), `windupTicks`, `summonCount`, `refireOnExpire`, `performed`, `dropWeight`, `tags.followUp`, `opener`, `nbt` (CraftTweaker line) | One castable focus and its selection data. |

Global settings live in `aqtweaks_battlemage.cfg` (see "Config" above): the switches (`Enable Cultist Battlemage`, shield, blink, rift, real rift holes, mob block placement, debug), the scales (`Battlemage Focus Damage Scale`, `Focus Cooldown Scale`, `Melee Damage Scale`, both 1.0 and meant only as global dev multipliers), `Battlemage Max Move Speed`, the boss bar, the per-hit damage cap, and the drop and Looting settings.

## Files by role (package `thaumcraft/battlemage`)

* **Entities and registry:** `EntityCultistBattlemage` (the shared body and AI hooks), `EntityCultistMasterBattlemage`, `EntityCultistSwarmMage`, `EntityCultistBombardierMage`, `BattlemageRegistry`, `BattlemageEntityEvents`, `BattlemageDespawnGuard`, `client/BattlemageClient`.
* **Library and data:** `FocusLibrary`, `LibraryParser`, `LibraryMigration`, `LibraryData`, `TierDef`, `VariantDef`, `FocusDef`, `EquipmentDef`, `EscortDef`, `DropEntry`, `AiTuning` (every `ai` key), `BattlemageFocus`, `CtFocusLine`.
* **Casting:** `FocusSelector`, `FocusCooldowns`, `CastBudget`, `VolleyPlanner`, `CloseSequence`, `LoadoutRoller`, `BattlemageAim`, `OpponentReading`, `SummonUpkeep`, `OwnedSummonTracker`, `CloudLedger`, `CloudAvoidance`, `CloudGuard`.
* **Targeting and Thaumcraft hooks:** `BattlemageSeekHooks`, `BattlemageSeekPicker`, `BattlemageTouchHooks`, `BattlemageGrief`, `TargetScorer`.
* **Movement and defence:** `AIBattlemageCombat` (the brain), `BattlemageDefense`, `DefenseRules`, `DodgePlanner`, `BandSteering`, `MeleeBurst`, `HealBudget`, `ShieldModel`, `BattlemageShield`, `BattlemageHurtEvents` / `BattlemageHurtRouter` (shield cue, stagger, damage cap), `StunGate`, `BattlemagePhase`, `SurgeCastRules`.
* **Blink and rift:** `BattlemageBlink`, `BlinkPicker`, `BattlemageRift`, `RiftPicker`, `ReachabilityRule`, `ReachabilityProbe`, `UnstickSearch`.
* **Escorts and the Swarm Mage:** `EscortSpawner`, `EscortRelease`, `EscortReleasedData`, `EntityEscortPortal`, `AllyHealPlanner`.
* **Drops and gear:** `BattlemageDrops`, `BattlemageDropEvents`, `DropRules`, `GearAttributes`.
