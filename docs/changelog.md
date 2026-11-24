# Changelog (1.10)

Stay on version **1.10** until a plan bumps `ArcanaQuestTweaks.VERSION`.

## 2026-11-23 — Bed rest: one action-bar line

- **Plan `20261123-1745`:** resting on a bed while not tired showed Somnia's "You're not tired yet" and vanilla's "Press SHIFT to dismount" drawn over each other. It now shows one line, "You're not tired yet. Press SHIFT to get up.": new early client mixin `somnia.MixinNetHandlerPlayClientBedSeat` (replaces the vanilla hint on `EntityBedSeat` only) and `somnia/client/RestMessageHandler` (cancels Somnia's own action-bar line while `Rest on Beds When Not Tired` is on, except when armour blocks sleep). [somnia.md](somnia.md).

## 2026-11-23 — Botania mana burst damage credits the shooter

- **Plan `20261123-1705`:** shooting villagers, golems or mobs with a Damage-lens mana blaster now provokes them and credits kills to the player. Botania's Damage lens used `DamageSource.MAGIC` (no attacker); new `mixin/botania/MixinLensDamage` uses `causeIndirectMagicDamage(burst, shooter)`, and new early `mixin/explosion/MixinExplosionBurstOwner` credits the shooter for Explosive and Storm lens blasts. Damage amounts are unchanged. First build did nothing because the blaster's burst has no thrower; new `MixinEntityManaBurstShooter` records the player. Not Dynamic Stealth. [reskillable.md](reskillable.md).

## 2026-11-23 — Smart HUD Stoneborn frames

- **Plan `20261123-1830`:** Smart HUD's slot frames were unthemed because no Stoneborn pack had a `smarthud` texture. `scripts/generate_smarthud_stoneborn.py` builds the sheet from Stoneborn's hotbar and adds it to `Stoneborn-ArcanaQuest-Additions.zip`. [client.md](client.md#hotbar-side).

## 2026-11-23 — Player home covers the Waystones title

- **Plan `20261123-1000`:** the Waystones title no longer appears after a player home's title; only homes cover it, other structures leave it alone. New `mixin/travelerstitles/MixinWaystonesCompat` marks the closest waystone as seen while a home title shows; the old `updateWaystoneTitle` inject on `TitleRenderManager` is gone. [titles.md](titles.md).

## 2026-11-23 — Smart HUD second row

- **Plan `20261123-1700`:** when the offhand slot and quiver leave too little room on the right, Smart HUD's extra slots draw as a second row one frame height above, right-aligned to the first row (`client/SmartHudRows`, an `@Inject` in `MixinSmartHudSlotRenderer`). New `Smart HUD Wrap` setting in `Hotbar Side` (default on) and `Smart HUD Max Slots` (default 8, replaces Smart HUD's limit of 5 while wrapping).

## 2026-11-23 — Hotbar side: offhand, quiver and Smart HUD on the right

- **Plan `20261123-1420`:** the offhand slot, Spartan Weaponry's quiver and Smart HUD's item slots now sit right of the hotbar in that order, each taking room only while on screen, so the left strip is free for the Rested panel. New `Hotbar Side` category in `aqtweaks_client.cfg` (`Offhand Slot Side = left` restores vanilla). New client mixins `hud.MixinGuiIngameOffhandSide` (early json), `spartanweaponry.MixinHudQuiverAmmo` and `smarthud.MixinSmartHudSlotRenderer` (new late json `mixins.aqtweaks.spartanweaponry.json` and `mixins.aqtweaks.smarthud.json`). The quiver is also level with the hotbar. The Spartan Weaponry and Smart HUD jars are now copied into `libs/` by `build_gradle.ps1`.

## 2026-11-22 — State-based stamina regen, Marathon Runner and Cardio Master

- **Plan `20261122-0610`:** stamina refill now follows what the player is doing: Resting 10, Moving 20, Active (sprint, climb, glide, swim, grapple, bow hold, mining, melee) 30 ticks per half-feather. Costs unchanged. Rested Vigor (25 / 20 / 15) now caps every state instead of applying a flat modifier. Melee keeps the player Active for 20 plus the weapon's attack period after a swing (rank excluded). New `Regen Pace` category in `aqtweaks_stamina.cfg`.
- **Replenishment (Elenai potion):** its own modifier (which refilled one half-feather per tick) is cancelled by `MixinReplenishmentPotion`. It now caps only the Active state: 15 / 10 / 5 ticks for amplifier 0 / I / II, floor 5. Astromancer's Star Powered gets the same new meaning.
- **Marathon Runner** (`aqtweaks:marathon_runner`, the old Cardio Master renamed, Agility 16, cost 3): sprinting and jumping refill as Moving, plain walking refills at the Resting rate. It no longer reduces sprint or jump cost, so net sprint stamina is 0. **Cardio Master** (`aqtweaks:cardio_master`, new, Agility 24, cost 4, cell 4,3, needs Marathon Runner): Active counts as Moving. A save that held the old `cardio_master` now holds the new perk. The new perks use the old Cardio icon until the art is approved.

## 2026-11-22 — DSS weapon speed, combo balance, Weapon Tempo

- **Plan `20261122-0327`:** Dynamic Sword Skills lock-on swings now wait for the weapon's own attack cooldown (`MixinDSSClientEvents`); the combo window is DSS's grace plus the weapon's attack period (`MixinCombo`); DSS's flat combo bonus is scaled by stamina weapon category, light 1.0 / medium 1.6 / heavy 3.0 (`MixinDSSCombatEvents`); heavy melee hits ignore 25% of target armor reduction (medium 10%) (`HeavyArmorBypass`). New `aqtweaks_dss.cfg`.
- **Stamina costs:** `Light Weapons Cost` 1 to 2, `Medium Weapons Cost` 2 to 3 (heavy stays 4). Melee Efficiency stays a flat -1.
- **Weapon Tempo perk (`aqtweaks:weapon_tempo`):** Attack tree (3,2), cost 4, needs Attack 24 and Melee Efficiency. While locked on in a DSS combo, hits 3, 6, 9 and 12 cost no stamina, and every other hit from the 3rd on has `5% x Basic Technique level` (1 to 10) to be free too. A free hit also skips Power Attack and the empty-stamina penalty. It replaced a short-lived Heavy Efficiency perk (heavy-only, flat -1) that overshot. Melee Efficiency icon rebuilt (vanilla iron sword + feather); Weapon Tempo icon is the iron sword with five gold tick marks. [stamina.md](stamina.md), [reskillable.md](reskillable.md).

## 2026-11-22 — Tabula Rasa

- **Tabula Rasa class reset (plan `20261121-1416`):** drinkable `aqtweaks:tabula_rasa` resets Reskillable skills and perks in place, refunds XP for purchased levels only, and reopens the class menu with `resetIntro` / `resetPrompt` and no second kit (`AQTStarterClassNoKit`; a new Scholar still gets `FIRSTSTEPS`). Also clears perk toggle flags. Thaumcraft Infusion recipe and `AQ_TABULA_RASA` research. [starter-classes.md](starter-classes.md).

## 2026-11-21 — Patchouli inventory tab and perk toggling

- **Patchouli inventory tab (plan `20261121-1534`):** the inventory guidebook button is now a 32x28 left-sidebar tab (Stoneborn-styled via `tab_left.png`) between the Skills and Comfort tabs; `aqtweaks_patchouli.cfg` (`Move Inventory Button To Sidebar`, tab offsets). Patchouli is now a compile-hard parent. [patchouli.md](patchouli.md).
- **Reskillable perk toggling (plan `20261121-1542`):** left-click an unlocked perk in the skill screen to switch it off or on (persisted, synced, refund resets it); `Toggleable Perks` defaults to Dark Vision, Tunnel Sense, Power Attack, Respite, Low Profile, Taunt, Full Font and Sower, whose descriptions now say so. Packets 21 and 22, `mixins.aqtweaks.reskillable.json`. [reskillable.md](reskillable.md).

## 2026-11-21 — Stamina hold billing tuning

- **Plan `20261120-1700` section 21:** `Mine Hold Interval Ticks` default 10 → **15** (about 1.33 half-feathers per second); Mining and Gathering Efficiency stay at 20 ticks (25% less). Both perks now require level **8** (Mining 8, Gathering 8; costs 3 and 2) and their descriptions say "25% less stamina".

## 2026-11-20 — Perk description cleanup

- **Reskillable perk descriptions (plan `20261120-1855`):** cleaned up **38** perk descriptions in [en_us.lang](file:///C:/dev/ArcanaQuestTweaks/src/main/resources/assets/aqtweaks/lang/en_us.lang) to drop internal jargon (`half-feather`, hidden temperature numbers `9`/`15`, `"Classified magic"`, `"deep and shallow warp"`, `"type by type, rounding down"`, `"after Magic skill bonuses"`, `"outside the block arc"`, `"feather replenishment"`, mod-name trivia), add missing accuracy/target details (`Prospector` `+25%`, `Orchard` vine tomatoes, `Tunnel Sense` `10` blocks / `5s` cooldown, `Full Font` `50%`), and make the 4 Magic school keystones name their mutually exclusive schools while their 8 dependent perks state `Requires <School>`. (`Mining Efficiency` and `Gathering Efficiency` stay reserved for plan `20261120-1700`.)

## 2026-11-19 — Ley-Rupture overhaul, Battlemage stage 4, HUD, starter class follow-ups

- **Ley-Rupture surge (plans `20261119-1530`):** scour 5.0 / 3.0 every 4 s (a charge cycle, anchored to exposure, first hit at 4 s); runic shield and absorption soak it and **do not refill while exposed** (`MixinPlayerEventsRunicRecharge`); the escape roll has no "nothing" outcome and repeats every 16 s; a warp buildup meter (1 temporary per 4 exposed cycles, 1 normal per 40, 1 back per cycle out) replaces the old hit and charge warp; charging is flat per cycle (mana 5,500, embers 50, vis 8, Vis Batteries 0.25 level, held batteries included); **overchannel** doubles a surge-exposed caster's power for backlash damage and temporary warp (Aegis: warp only, pool and cap doubled; Battlemages overchannel too, damage only); a Battlemage below 10% health only heals, blinks, rifts and melees. Structure shelter now accepts the whole structure footprint and the default list covers every built structure; `/leyrupture status` prints the shelter test.
- **Battlemage stage 4 (plan `20261113-2124`):** boss bar, three phases (Empowered, Desperate), reset rule, optional per-hit damage cap, and the drop tables (loot table, gear, bauble, two weighted foci). Casting slowed again (Tier 1 gap 40 and regen 5.5, Tier 2 30 and 8, Bombardier volley 48 and 130 after a second 20% cut).
- **Battlemage melee:** during each cast hold a mage now closes in for a melee burst 60% of the time (`ai.melee.closeInChance`, `closeInMaxRange` 14) instead of standing in its band.
- **Battlemage strike blink:** half of the melee close-ins now blink next to the target, hit, and blink back out when the burst ends (`ai.melee.strikeBlinkChance`). It now waits 10 ticks after landing before the first blow and 10 ticks after the last before blinking out. A Stun that breaks the combo is answered with Knock Back and an escape blink once it ends.
- **Battlemage speed:** ground movement is capped at `Battlemage Max Move Speed` (4.3 blocks per second, a walking player).
- **Bombardier:** area shots every 64 ticks (was 48), 180 ticks between volleys (was 130), volley cap 300.
- **Battlemage heals and escapes:** 4 heal charges every 160 ticks, healing from 65% health, Desperate heal charges every 96 ticks; Knock Back peel every 80 ticks, sidestep every 30, blink every 80, escape blink below 30% health; the shield refills fully at every phase change. Casting power unchanged. Heal charges return every 100 ticks (was 160); the Swarm Mage's Healing Aura has its own 5-charge pool, one back per 100 ticks.
- **Battlemage reposition blink:** every 200 ticks a 40% chance to blink to a new spot on its firing band (`ai.blink.repositionIntervalTicks`, `repositionChance`).
- **Starter class emblems redrawn (plan 20261120-1900):** the six icons are now native 32x32 pixel art in the perk-icon palette (`scripts/generate_starter_icons_32.py` — heater shield & sword, bow & arrow, crossed daggers, socketed pickaxe, cottage, open tome with ribbon) instead of flat polygons.
- **Stamina overhaul (plan 20261120-1700):** mining and woodcutting stamina is billed by how long you dig (1 half-feather at the start and every 10 ticks, `MiningHold`, `MiningBilling`, a mixin on `PlayerInteractionManager.updateBlockRemoving`) instead of per break; plants (hardness 0) cost a flat 1; Mining and Gathering Efficiency slow the drain to every 20 ticks; Mining Fatigue starts at 3 usable half-feathers and now respects the armor weight floor; new perks Forager (plants free) and Heartwood (the extra-log roll), Lumberjack now makes logs break twice as fast; the axe-click swing charge no longer applies to blocks.
- **Runic HUD:** the Overloaded Armor Bar now clears two or more extra rune and gold rows (`absorptionForArmorBar`, `armorBarShift`).
- **Starter classes:** Hearthkeeper moved to the Building skill (cell 1,1); icons redrawn (archer bow and arrow, miner pickaxe, scout dagger, scholar star, warrior shield without the cross); a new home takes its Hearthkeeper flag from the owner's perk at once.
- **Docs audit (2026-11-20):** `scripts/doc_audit.py` is clean (0 hard findings); added the Nameless config table (weapon.md), the starter perk key table (reskillable.md), the Battlemage "At a glance", tuning key reference and files-by-role sections, a Quality on mobs section (qualitytools.md), a files list for the starter module, and refreshed the Ley-Rupture rows of verification.md. Scratch checkers `scripts/_defaults_check.py` (config defaults vs doc tables) and `scripts/_method_check.py` (documented members that do not exist) are read-only helpers.
- **Tests:** `SurgeCycleTest`, `SurgeCastRulesTest`, `Stage4Test`; `LibraryParserTest` updated for the new pacing. Docs: [twilightforest.md](twilightforest.md), [cultist-battlemage.md](cultist-battlemage.md), [thaumcraft.md](thaumcraft.md), [thaumcraft-additions.md](thaumcraft-additions.md), [reskillable.md](reskillable.md), [comfort.md](comfort.md).

## 2026-11-18 — Crimson Battlemage stage 3: shield, heal budget, escape, Rift (plan 22)

- **Shield:** faked runic shield in vanilla absorption (16 / 28), recharge +1 per 40 ticks, 40 tick wait after a break, real `PacketFXShield` hit cue; a broken shield widens the band and halves the peel cooldown; heavy hits stagger casting.
- **Defence:** heal budget (3 charges, 200 ticks each), retreat to heal at 50% (Swarm Mage toward its escorts, not the Bombardier), Knock Back peel, projectile and bow sidestep, strict four-condition defensive blink or rift, blink back to the band after melee and when there is no firing line, stuck-in-a-wall escape, hazard step-out, target re-scoring, raised-shield reading.
- **Rift:** real temporary `BlocksTC.hole` tunnels (`RiftPicker`, `BattlemageRift`, `ReachabilityProbe`), defensive and offensive, own 300 to 400 tick cooldown.
- **Mixin:** `MixinFocusEffectBlockPlacement` stops a mob's Fire and Frost foci placing blocks (`Allow Mob Focus Block Placement`, default false).
- **Config:** `Battlemage Shield Enabled`, `Battlemage Shield Drains Aura`, `Battlemage Blink Enabled`, `Battlemage Rift Enabled`, `Allow Real Rift Holes`, `Allow Mob Focus Block Placement`. New `ai` blocks `shield`, `healBudget`, `defense`, `rift`, `unstick`; no schema change (absent blocks use the defaults).
- **Tests:** `Stage3Test`, `RiftPickerTest`. Docs: [cultist-battlemage.md](cultist-battlemage.md) (Defence, shield and mobility; checks 21 to 28).

## 2026-11-18 — Battlemages never despawn, escort id fix (plan 21)

- **Fix:** the Swarm Mage escort listed `thaumcraft:cultist_knight` / `thaumcraft:cultist_cleric`, which are not registered (Thaumcraft's ids are `thaumcraft:cultistknight` / `thaumcraft:cultistcleric`), so only the Crimson Archer spawned. Ids corrected; library `schemaVersion` 4 so a stale config copy is backed up to `.bak` and replaced. An escort id that does not resolve now logs one WARN per id.
- **Never despawn:** all four battlemage entities have `canDespawn()` false and `BattlemageDespawnGuard` denies Forge's `AllowDespawn` for them. Swarm Mage escorts carry an `aqtweaks_escort` tag (owner UUID) and are denied despawning while the mage lives; when it dies, loaded escorts lose the tag and unloaded ones are released from `EscortReleasedData` (`aqtweaks_released_escort_owners`), so they despawn normally again.
- **Tests:** `EscortReleaseTest`, escort id tests in `EscortSpawnerTest`, schema 4 in `LibraryMigrationTest`. Docs: [cultist-battlemage.md](cultist-battlemage.md) (checks 19 and 20).

## 2026-11-18 — Crimson Swarm Mage: Healing Aura guard (plan 20)

- **Guard:** when a player closes on the Swarm Mage's own Healing Aura cloud (within 3.5 of the centre, mage within 6), it hits 1 to 2 times in melee and casts Knock Back, or Knock Back alone when the player is in reach and stepping toward the cloud; beyond Touch reach it walks in for up to 20 ticks first. 30 tick cooldown between sequences. New `ai` keys `cloudGuardRange`, `cloudGuardNearRange`, `cloudGuardCastTicks`, `cloudGuardMeleeMin`/`Max`, `cloudGuardCooldownTicks`, `cloudGuardCloseTicks` (in the `swarm_mage` variant).
- **Tests:** `CloudGuardTest`, guard keys in `LibraryParserTest`. Docs: [cultist-battlemage.md](cultist-battlemage.md) (Swarm Mage).

## 2026-11-18 — Crimson Bombardier Mage: volley, close response, reposition blink (plan 19, part C)

- **Volley:** `aqtweaks:cultist_bombardier_mage` fires one area focus every 30 ticks, each of its five once in a random order, then pauses 80 ticks; a focus waits while its own cooldown runs or its previous clouds are alive (`CloudLedger`, one live cast per focus, so up to five casts' clouds can be out). The energy pool is off for this variant. New `ai` keys `volleyGapTicks`, `volleyPauseTicks`, `volleyMaxTicks`.
- **Close response:** a target inside 6 blocks gets 1 to 2 melee attacks, Knock Back, then a blink (3 to 6 blocks: straight to the blink; Blink on cooldown: it steps back on foot). New `close` block (`meleeMin`, `meleeMax`, `maxTicks`).
- **Reposition blink:** `BlinkPicker.Mode.REPOSITION_BAND` lands 8 to 14 blocks from the target with line of sight preferred, never inside its own clouds, avoiding enemy clouds and mines (`CloudAvoidance`). New `blink` keys `bandMinDistance`, `bandMaxDistance`, `cloudAvoidMargin`, `enemyCloudCost`.
- **Tests:** `VolleyPlannerTest`, `CloseSequenceTest`, `CloudAvoidanceTest`, `CloudLedgerTest`, band cases in `BlinkPickerTest`, keys in `LibraryParserTest`. Docs: [cultist-battlemage.md](cultist-battlemage.md) (Bombardier Mage).

## 2026-11-18 — Crimson Swarm Mage: escort, portal arrival and ally heal (plan 18.7, part B)

- **Escort:** the Swarm Mage spawns its escort (2 Crimson Knights, a Cleric, a Crimson Archer, from the library `escort` list) once, staggered out of a cosmetic lesser crimson portal (`aqtweaks:escort_portal`, id 10, drawn by Thaumcraft's portal renderer, a neutralised `EntityCultistPortalLesser`), `Escort:0b` NBT skips it. Escorts share the mage's target and the mage takes the player an escort was hurt by. Escort uuids and an unfinished arrival are saved.
- **Ally heal:** an escort within 16 blocks below 60% health gets a Healing Aura; the mage blinks next to it first when it is farther than 2.5 blocks (walks if Blink is on cooldown and no player is in melee range). `AllyHealPlanner`, `BattlemageBlink`, `BlinkPicker` (`TOWARD_ALLY` only), shared 100 tick blink cooldown; new `ai.blink` keys.
- **Tests:** `AllyHealPlannerTest`, `BlinkPickerTest`, `EscortSpawnerTest`, blink keys in `LibraryParserTest`. Docs: [cultist-battlemage.md](cultist-battlemage.md) (Swarm Mage).

## 2026-11-18 — Crimson Battlemage: four entities, real gear, slower casting (plan 17 to 19, part A)

- **Entity split:** `aqtweaks:cultist_battlemage` (Tier 1, id 4), `cultist_master_battlemage` (Tier 2, id 7), `cultist_swarm_mage` (Tier 1 variant, id 8) and `cultist_bombardier_mage` (Tier 2 variant, id 9), one shared renderer, one egg each. The `Tier` NBT is gone; the entity id is the identity. Ids 5 and 6 belong to the Nameless entities; id 10 is reserved for the escort portal. The variants are fixed-loadout stubs with hooks (escort, ally heal and volley are a later build part).
- **Library schema 3:** per-tier and per-variant `ai` overrides, `variants` (fixed loadout, role weights, escort), `castBudget.enabled`, ally-heal and volley keys, equipment as `{item, nbt, enchantments}`. A schema 2 copy is backed up to `.bak` and replaced.
- **Gear:** the Crimson Paladin set, Crimson boots, the named Serrated blade (five enchantments resolved to registry ids) and the Mystical caster replace the stock robes, with the Quality Tools modifiers applied straight to the mob (`GearAttributes`; Quality Tools only does it for players).
- **Slower casting:** cast gap 24 / 16 ticks, pool refill 10 / 14 per second, resume at 60%, startup delay 40 / 30 ticks (Tier 1 / Tier 2).
- **Loadout rules:** at most 2 heavy foci (complexity 50 or more) on Tier 1 and 3 on Tier 2; the area slot keeps a heavy seat; Tier 2 forces a heavy bolt or projectile; Spell Bat Swarm is an ordinary Tier 1 summon option and a 20% add-on at Tier 2 (always with Curse Bat).
- **Tests:** `LoadoutRollerTest` (heavy caps, forced heavy, Swarm rates), `LibraryParserTest` (variants, resolved pacing, gear, SNBT sanity), `LibraryMigrationTest` (schema 3). Docs: [cultist-battlemage.md](cultist-battlemage.md).

## 2026-11-18 — Starter class perks: Studious and Hearthkeeper

- **Studious (Scholar / Magic):** three Scholar-only theorycrafting cards (Eureka, Cross-Reference, Second Opinion) delivered by a virtual research aid that `MixinTileResearchTable` adds to a Scholar's theory (also +1 starting inspiration and 3 extra-choice rounds). **Steady Focus:** `MixinCasterManager` shortens the Scholar's focus cooldowns by 25% (`CasterManager.setCooldown`). Marginalia dropped.
- **Hearthkeeper (Homesteader / Building):** Open Hearth (+8 comfort for others within 16 blocks of the Homesteader, diminishing company bonus for the Homesteader, cap 15) and the home box (+15% cozy score and a Rested that lingers 50% longer for anyone resting in the Homesteader's home, owner online or not). Hooks in `ComfortSystemHandler`; the home record's `hearthkeeper` flag follows the owner's perk.
- **Tests:** `StudiousMathTest`, `HearthkeeperMathTest`. Docs: [reskillable.md](reskillable.md), [comfort.md](comfort.md). Config: `aqtweaks_reskillable.cfg` Starter Scholar Perks, Starter Home Perks, Perks cells (Magic 1,3 and Building 1,1).

## 2026-11-18 — Starter classes: server module, lockdown and commands

- **Module** (`starter/`): a new character is shown the class menu, locked down until they choose, then gets the class skills, the free class perk, the CSG kit and the class commands (Scholar: `tc research %player% add FIRSTSTEPS`). Config `aqtweaks_starter_classes.json` (written on first run, validated at load), failsafe when no class is valid.
- **Lockdown:** immune to damage, effects and targeting, held in place, no block, item or container actions; guards in the Comfort, Stamina, Simple Difficulty, Somnia and Thaumcraft player ticks.
- **Packets 17-20** (open, select, result, reveal done). **`/aqclass`** reload, skip, reset, regrant, info. Login re-grants a missing class perk.
- **CSG** is now a compile parent (`libs/`, copied by `build_gradle.ps1`).
- **Tests:** `StarterClassValidatorTest`, `StarterSettingsIOTest`, `StarterKitDiffTest`, `StarterRulesTest`. Docs: [starter-classes.md](starter-classes.md).

## 2026-11-18 — Starter class perks: Riposte, Steady Draw, Fleetfoot, Vein Rhythm

- **Class-perk support:** `ClassPerkSupport` registers free (cost 0) Reskillable traits whose only requirement is the impossible `adv|aqtweaks:class_only`, and cancels `LockUnlockableEvent.Pre` so a class perk is never bought or given back. `AqtweaksTrait` gained a package-private constructor that takes forced requirement rows.
- **Riposte (Defense):** a shield raised within 10 ticks of a hit staggers a melee attacker with Stun for 10 ticks and restarts the shield stamina hold timer. Vanilla/modded shields via `LivingAttackEvent`, Aegis via `AegisBlockEvent`.
- **Steady Draw (Attack):** bows cost no draw stamina and the hold cost starts at full draw; 75% of arrows that hit are returned at the target's feet (pickup-allowed vanilla arrows only).
- **Fleetfoot (Agility):** sneak speed gives back half the vanilla slowdown, +10% sprint with a LIGHT weapon or empty hand, first 3 seconds of a sprint free (re-arms after 40 idle ticks).
- **Vein Rhythm (Mining):** consecutive pickaxe breaks of stone/ore within 2 s stack up to 5 times for +4% break speed each.
- **Tests:** `StarterPerkMathTest`. Docs: `reskillable.md` Starter class perks. Config: `aqtweaks_reskillable.cfg` Perks cells and Starter Combat Perks.

## 2026-11-18 - Starter class screens and art (client)

- **Screens:** `GuiStarterClassSelection` (intro, question, scrolling class rows, confirm line and Select) and `GuiStarterReveal` (what you gained: levels, perk, gear, with a cross-fade, Continue and a 12 s hold), entered through `StarterClientHooks.open`, `showResult` and `closeAll`. Escape does not close the selection. Details in [starter-classes.md](starter-classes.md).
- **Art:** Stoneborn-style nine-slice panel, option, button and divider textures plus six 32x32 class emblems under `textures/gui/starter/`, generated by `scripts/generate_starter_textures.ps1`. They live in the jar, so the screens look right with Stoneborn on or off.
- **Tests:** `StarterClientMathTest`, `RevealTimelineTest`.

## 2026-11-17 — Crimson Battlemage stage 2 (loadout and combat core)

- **Rolled loadouts:** `LoadoutRoller` (pure, tested) gives each mage its fixed Heal Self, Knock Back, Blink and Rift plus one Bolt, Projectile, summon and AOE focus. Tier 1 carries 8 foci (budget 160), Tier 2 carries 10 (two wildcards, budget 250). Complexity-weighted, gated by `minTier`, a dimension-named focus is x3 in its dimension, a fast filler (complexity 25 or less) is always among the direct foci, at most 2 of one role. Persisted on the entity; spawn NBT `Foci` overrides the roll.
- **Stats:** Tier 1 health 80, armor 1, focus power 1.25, shield 16 (stored for stage 3); Tier 2 120, 3, 1.5, 28. Power is applied per cast on top of `Battlemage Focus Damage Scale`. A config copy written by stage 1 is backed up to `.bak` and replaced (`schemaVersion` 2).
- **Combat:** cast budget with burst hold-back; the first cast of a fight prefers an opener; Spell Bat upkeep (`OwnedSummonTracker`, recast at once when bats are short); a long-range cloud is not recast while one is alive; melee burst (2 to 3 weapon attacks, then a short retreat); Stun resistance (half length, no re-stun for 4 s after); Soul Sight (`aqtweaks:cultist_battlemage` in Dynamic Stealth's Natural Soul Sight list). Details: [cultist-battlemage.md](cultist-battlemage.md).
- **Fix:** all six equipment slots now have a 0 drop chance (armor was leaking at the vanilla 8.5%).

## 2026-11-17 — Structure titles for Traveler's Titles

- **Titles:** entering a player home, a Roguelike dungeon or a vanilla structure (village, mineshaft, mansion, stronghold, monument, fortress, End city, the scattered temples) shows its name through Traveler's Titles' own renderer, styled from its biome title settings. A home shows `<owner>'s Home` (option to show plain "Home"). The structure title overrides the biome title and the Waystones title. Spec: [titles.md](titles.md).
- **Server:** `StructureTitleTracker` checks each player every 20 ticks (staggered) with vanilla's `isInsideStructure` only, and sends `PacketStructureTitle` (discriminator 16) on a change. Settings in `aqtweaks_titles.cfg`; client Y offset and owner-name option in `aqtweaks_client.cfg`.
- **Build:** `build_gradle.ps1` copies `TravelersTitles-1.12.2-1.1.0.jar` into `libs/` (compile-hard); `MixinTitleRenderManagerStructure` is queued only when the mod is loaded.

## 2026-11-17 — Player homes: door test, live-scan merge, stairs

- **Doors:** every door, fence gate and trapdoor is a portal, open or closed. The fill judges the cell beyond it in layers: open sky (rain-exposed, glass and leaves count as cover), then 26-direction rays (the forward-facing ones, 16 blocks), then a speculative sub-fill with a budget of 800 cells that must close to count as interior. Outside doors are walls, so a door left open no longer leaks the home into a box fallback and a closed interior door no longer splits the house. `home_door_*` and `home_max_doors` settings; `/aqhome info` prints the verdicts.
- **Live-scan merge:** inside a home the score is the cached home plus the live 25x5x25 scan, de-duplicated by block position, so a garden or farm beside the house still counts. Caps and decay apply to the merged list.
- **Stairs:** any stairs block not listed under `categories.structure` counts as structure at `structure_stairs_weight` (default 1.0), so a stair-roofed starter house scores a roof. Spec: [comfort.md](comfort.md#player-homes).

## 2026-11-17 — Player homes (cached whole-build Comfort scoring)

- **Homes:** sleeping in a bed or setting spawn at one registers the player's home. The connected interior of the build (any number of floors, through open doors, gates, trapdoors and ladders) is found with a sliced flood fill capped at 4000 cells, cached, and scored as a whole by Comfort; category caps, decay and thresholds are unchanged. Open-air or oversized builds fall back to a box around the bed (12 wide, 6 down, 8 up). Outside any home the old 25x5x25 scan runs as before. Only the bed anchor is saved; the region rebuilds when a player comes near after a restart, after a block change, and every 10 minutes while someone is inside.
- **Structure record and `structure:Home`:** `HomeRegistry.structureAt` returns a structure-shaped record (type home, display name "<owner>'s Home", owner UUID and name, bounds, hearthkeeper flag) for a later title display and spawn rules. `MixinChunkProviderServerHome` answers the vanilla structure test (Music Triggers) and `MixinStructureCacheHome` answers In Control's `structure` rule for the name Home.
- **Commands and settings:** `/aqhome info|rescan|list` (OP 2); `home_*` keys in `aqtweaks_comfort_settings.json`. Spec: [comfort.md](comfort.md#player-homes).

## 2026-11-17 — Crimson Battlemage, stage 1

- **Crimson Battlemage (new, `aqtweaks:cultist_battlemage`):** a Crimson Cultist caster miniboss that casts real Thaumcraft focus packages for free, switching between foci as each one's natural cooldown (`max(5, (complexity/5)*(complexity/4))` ticks) runs. Stage 1: Tier 1 and Tier 2, a fixed loadout from the new `cultist_battlemages.json` focus library (20 foci), `ENGAGE`/`CAST`/`REPOSITION` brain, gauntlet orb colour follows the active focus, `/summon` and spawn egg only. Config `aqtweaks_battlemage.cfg`. See [cultist-battlemage.md](cultist-battlemage.md).
- **Thaumcraft mixins:** `MixinFocusMediumTouch` makes Touch and Bolt work for a non-player caster (stock cast the caster to a player and returned no targets); `MixinEntityUtilsFriendly`, `MixinEntityFocusProjectileSeek`, `MixinEntitySpellBatTarget` and `MixinEntityFocusCloud` make a Battlemage's seek projectiles, hostile Spell Bats and clouds affect players only, whatever the PvP flag says. Player casts and other mobs run stock code.

## 2026-11-16 — Armor bar row under runic shielding, underground warp needs darkness

- **Armor bar one row too high:** Overloaded Armor Bar places its bar from the raw absorption amount, so runic charge (stored as absorption, drawn on the health row) left an empty row. `MixinOverloadedArmorBarRunic` and `MixinOverloadedLavaCharmRunic` feed it `RunicShieldHud.absorptionForLayout`. See [thaumcraft.md](thaumcraft.md).
- **Underground warp exposure only in the dark:** the `under` and `underDeep` sources now also need combined light ≤ `Underground Exposure Max Light` (default 6, 15 = off) at the player's head, so lit underground bases no longer bank warp. Y bands, grants, dimension and dungeon sources are unchanged. `ExposureRules` + test.

## 2026-11-16 — Aegis focus effect

- **Aegis (new focus effect, Thaumcraft Additions):** a shield held up while you keep casting. Blocks hits from the front (vanilla's 180 degree test) and spends a damage pool of 5 per power level (5 to 25); the hit that empties the pool is absorbed in full and breaks the shield, which locks every focus containing Aegis for 5 seconds (`Shield Broken`, `aqtweaks:aegis_broken`). Costs 2 vis at the raise and every 20 ticks held (the raise is the first bill, so every raise costs 2). Lowering early discards the pool. Raised Aegis counts as a real shield (`BLOCK` use action, so `isActiveItemStackBlocking()` and Fortify work) and is skipped by the stamina shield-hold cost (`Aegis Uses Stamina Shield Cost`). New hooks for shield perks: `ShieldState`, `AegisBlockEvent`. Client draws a blue panel with runic glyphs that fade as the pool drops (`PacketAegisShield`, discriminator 15). Research `AQ_FOCUS_AEGIS` at Auromancy `[-1, 7]` beside Heal, requiring Heal and Runic Shielding. `MixinItemCaster` gains the raise, lockout and use-action hooks. See [thaumcraft-additions.md](thaumcraft-additions.md).

## 2026-11-16 — Shock focus effect, runic shield HUD under Mantle, Stun, bow hold billing

- **Runic shield HUD:** the rune overlay never showed because Mantle's `ExtraHeartRenderHandler` cancels the health overlay and draws its own hearts, so neither our hook in `GuiIngameForge.renderHealth` nor Thaumic Tweaker's overlay ran. New client mixin `MixinMantleExtraHeartRunicShield` hooks Mantle's `renderHealthbar` the same way. Diagnostic `Runic HUD Debug` (`aqtweaks_client.cfg`, `runic hud`, default off). See [thaumcraft.md](thaumcraft.md).
- **Thaumcraft Additions module (new):** `thaumadditions` package, `aqtweaks_thaumadditions.cfg` (`ThaumAdditionsConfig`). First addition is the **Shock** focus effect (Potentia lightning): Frost's damage formula `(3 + power) * finalPower`, 1.5x against wet targets, chains to 2 more targets (4 blocks, 8 between two wet targets) at 0.6 damage per jump, Slowness II stun 10 ticks at duration 1 up to 50 at duration 5, hits players and mobs by Thaumcraft's `isFriendly` rule. Auromancy research `AQ_FOCUS_SHOCK` at cell `[1, 2]` next to Elemental Foci. See [thaumcraft-additions.md](thaumcraft-additions.md).
- **Stun (new shared effect, `aqtweaks:stun`):** the target cannot attack or use items while it lasts (item use, melee hits, new projectiles including witch potions, a witch's drink) and moves at about Slowness I speed; arrows already in flight still fly. `potion/PotionStun`, `Stun`, `StunHandler`. See [thaumcraft-additions.md](thaumcraft-additions.md).
- **Shock revised:** the Slowness stun is replaced by Stun (5 ticks at duration 1, +5 per level, so 5 to 25 ticks; `Shock Stun Base Ticks`, `Shock Stun Ticks Per Level`; the amplifier key is removed). Creepers it hits become powered with no extra damage (`Shock Charges Creepers`, `MixinEntityCreeperPowered` in the early json). Cast zap and hit crackle (Thaumcraft's `zap` and `shock` sounds, `Shock Sound Volume`).
- **Aether lightning storm:** a primary bolt also stuns for `Lightning Stun Ticks` (default 20; 0 off), on top of the Slowness. See [aether.md](aether.md).
- **Bow hold stamina fixed:** a held bow was billed from two places and paid twice per interval; it is now billed once (`UseItem.Tick`), which halves the effective hold cost for everyone. Throwing-weapon hold is unchanged. See [stamina.md](stamina.md).

## 2026-11-14 — Hazard gear centralization, Atum choking removal, TF surge updraft

- **Centralized hazard gear (`aqtweaks_hazard_gear.cfg`, `HazardGearConfig`, `HazardGearAudit`, `WindResistanceGear`):** moved all dimension hazard armor/gear whitelists and effect IDs into seven categories (`wind`, `eye_protection`, `aether`, `atum`, `aurorian`, `nether`, `twilightforest`) with `:meta` matching and startup audit logging for unmatched patterns. Added shared additive wind-push resistance across Aether gusts, Atum sandstorms, and Aurorian Moonfrost storms from live Elenai Dodge 2 iron-feather weight (before Armor Mastery), Wind Resistant armor, grounded Anchoring boots, and additive sneaking (clamped up to 100%). Renamed Aether `Channelled` → `Channeling`, excluded Insulated and Channeling gear from Conductive, fixed `thaumicaugmentation:thaumium_robes_*` and `atum:eyes_of_atum`, and replaced hardcoded eye-protection shortcuts with `Eye Protection Gear Whitelist`.
- **Atum choking removed:** removed Sand Inhalation Choking, air-bar drain, `Enable Sand Inhalation Choking`, and `recoverBreath` from `AtumStormGameplayHandler` (cleans leftover `"AQTweaks_AtumAirDebt"` NBT).
- **Twilight Forest Ley-Rupture updraft (`SurgeExposureRoll`):** after 16 s of continuous outdoor surge exposure, rolls once per continuous exposure (`1:2:2` weights) for no effect, 16 s Flight + 26 s Slowfall, or 16 s Levitation + 26 s Slowfall across all exposed players equally (refresh if shorter).

## 2026-11-02 — Comfort score breakdown in inventory popup HUD

- **Comfort score breakdown in inventory popup HUD.** Hovering the Comfort tab in the inventory menu now displays the full Comfort score breakdown:
  - Total effective score and tier status (`Comfort Score: X.X (Tier I)`), with the `Cozy + Bonus - Penalty` equation.
  - Next tier goal, point gap, and promotion countdown (`Promotes in M:SS`).
  - Gating warnings for missing required furniture (hearth, bedding, seating) or combat cooldowns (hurt/attack).
  - Detailed cozy furnishings breakdown by category sorted descending by score with item counts (`Bedding: 8 (1/1)`, `Hearth: 6 (1/1)`, `Seating: 4 (1/1)`, etc.).
  - Active bonuses breakdown (`Resting: +2`, active potion bonuses such as Farmer's Delight comfort).
  - Active penalties breakdown (`Hunger: -2`, `Cold: -1.5`, negative status effects such as Somnia fatigue).
  - Resting prompt (`Sit, sneak, or lie down to rest`) when in an eligible comfort area but standing.
  - Clear indicator when away from home.
- **Hearthside tooltip rate.** Updated the Hearthside line on the hover box to `* Hearthside: At Home (Cleansing warp - <rate>)` (1 warp per 3 min for Tier I, 1 warp per 2 min for Tier II, 1 warp per min for Tier III).
- Serialized category capped scores in `PacketComfortStatus` and mapped active potion bonuses (kind 7) to sync full room telemetry to the client.

## 2026-11-14 — Hazard timers, ash storm, Ley-Rupture, Aether lightning storm, Moonfrost plants

- **Timers:** Atum and Aurorian storms are now 4 to 6 minutes (240 to 360 s) with 14 to 34 minutes (840 to 2040 s) clear; the Nether ash storm lasts 3 to 5 minutes; the Ley-Rupture surge lasts 150 to 210 s every 12 to 20 minutes (charge, hunger and mob pulses re-paced; scour every 8 s for 3.0 unprotected / 1.0 proper, new `Scour Interval Seconds`). The DEVBOX cfg values were updated to match.
- **Nether lava spouts:** 45 s events with 10 to 16 eruptions per player; spouts prefer lava near land; the thrown lava is chosen when the spout starts, warned with embers and a hiss for `Splash Warning Ticks` (25) after the burst (`PacketSplashTelegraph`, id 14) and then placed per cell on the local surface (no more all-or-nothing rejection), so it reliably lands on the bank (and on players who do not move).
- **Nether ash storm:** a 30 s warning (wisps, cinders, rising wind), clouds created and filled in the last 2 s so they arrive on the first active tick, and a rounded, turning cloud of `ParticleAshSwirl` motes (dense core, feathered rim) with far higher particle caps (4000 per cloud, 8000 total).
- **Ley-Rupture:** the warning is visible from the first second (columns, hum, growing vignette); columns start 24 blocks below the player (never above Y30), rise through ordinary blocks to Y50-55 and dissolve there, about 30 percent runes and about half the spawns within 18 blocks; they stop only in Shelter Features structures (boxes sent in `PacketLeyExposure`); the effect reaches about Y50 (`Fade Height Blocks` 22); surge charging now covers Thaumcraft rechargeable items, Thaumic Augmentation caster vis batteries, worn armor and Baubles.
- **Aether:** gusts keep one heading from the warning to the end and push twice as hard with 2x active wisps; lightning is a storm: 10 s warning, 18 to 24 s of 5 to 6 salvos that lock onto every exposed player (insulated sets are diverted 3 to 8 blocks; conductive gear is locked first and retargeted; a channelled set, list empty for now, takes no damage), each bolt glowing on its block; clear interval 90 to 180 s. Conductive and insulated gear no longer change the wind push.
- **Moonfrost snow** overwrites wild Aurorian lavender and tall grass (`Snow Replaces Plants`, `Snow Replaceable Plants`).

## 2026-11-13 — Atum: sandstorm warning from `/atumweather`, and sand accumulation

- **`/atumweather <sandstorm|clear|status> [seconds] [skipWarning]`** now runs through Tweaks (`MixinAtumWeatherCommand`): a forced sandstorm builds its `Storm Warning Seconds` of warning dust first, `[seconds]` sets the storm's length after the warning, `skipWarning` starts at once, `clear` and the storm state reach clients immediately, and `status` is new. The forced duration is stored on the provider, so it cannot leak into another world.
- **Sand accumulation:** during a sandstorm exposed ground gains smooth, wind-shadowed Atum sand layers (default up to 6 layers, 1 to 16), shared with the Aurorian snow through the new `weather/LayerAccumulator` and `Layer*` classes (the Snow* pure classes moved there). Atum's own weather sand loop (every chunk every tick, three layers) is switched off while this is on. Nothing erodes afterwards. New keys: `Enable Sand Accumulation`, `Sand Layers Per Chunk Per Second`, `Sand Max Changes Per Tick`, `Sand Accumulation Radius`, `Max Sand Layers`, `Sand Max Step`, `Sand Wind Shadow`, `Sand Wind Shadow Angle`, `Accumulate On Strange Sand`, `Skip Oasis Biome`. Design: [client.md](client.md).

## 2026-11-13 — Moonfrost blizzard and Ley-Rupture eruption particles

- **Moonfrost storm:** the fog is gone (no fog at any point, so no jump at a doorway); `Whiteout Fog End Fraction` and `Warning Fog Fraction` were removed. Snow now falls diagonally with the wind (`Snow Fall Angle` 58, much faster, gust pulses) in seven classes using vanilla particle sprites (specks, flakes, stars, pellets, soft puffs, chunky `ParticleDigging` clumps, streaks), with a dense veil about 10 blocks around the player (`Visibility Perimeter Radius`), a one-second doorway ease, a stronger solid check for fast flakes and a roof kill (`Cull Snow Under Roofs`; canopies do not count). New keys: `Snow Fall Angle`, `Snow Speed Multiplier`, `Snow Streaks`, `Cull Snow Under Roofs`, `Visibility Perimeter Radius`, `Perimeter Density`.
- **Snow accumulation (server):** during a storm exposed ground gains vanilla snow layers across the loaded area, up to 16 layers (two blocks, `Max Snow Layers`), smoothly (neighbouring columns at most `Snow Max Step` layers apart), with a wind shadow in the lee of walls and trees; vanilla snow afterwards (melts only near bright light, snowballs as vanilla). **Burial hazard:** entities are not skipped, so a player who stands still can be buried: Slowness II at chest depth, air loss and damage (never below 1 heart) with the head under; dig out by hand. New keys: `Enable Snow Accumulation`, `Snow Layers Per Chunk Per Second`, `Snow Max Changes Per Tick`, `Max Snow Layers`, `Snow Max Step`, `Snow Accumulation Radius`, `Snow Wind Shadow`, `Enable Snow Burial`, `Snow Burial Slowness`, `Snow Burial Air Drain Per Second`, `Snow Burial Damage`. Design: [aurorian.md](aurorian.md).
- **Ley-Rupture particles:** warning glyphs rise, zigzag and fade after about 2 s instead of being pulled into the ground; the surge is a heavy eruption (glyphs, sparks, dots and vent plumes, violet 70 percent and emerald 30 percent, full-bright) spread over a 48 block radius with the density per square block held at the old level. New keys: `Ley Particle Radius`, `Max Ley Particles` (9000), `Ley Particle Density`. Design: [twilightforest.md](twilightforest.md).

## 2026-11-13 — Performance and stability fixes

- `Wildcard` is a regex-free glob matcher (Atum headgear, gear tiers, gas blocking). Regexes were compiled on every call, per tick in a storm.
- Ash clouds share one `Gas Cells Per Tick` budget for the whole Nether (it was per cloud), and an unchanged cloud is not rebroadcast (the client extrapolates the drift).
- Carminite phlogiston and Void Breach rituals are dropped when their world unloads, so they cannot tick in a later singleplayer world.
- Unyielding no longer re-applies its knockback modifier every tick (it resent attribute packets); Tunnel Sense checks its throttle first; Star Powered runs every 10 ticks; potion lookups are cached.
- Atum server shelter test is cached per player for 10 ticks or a block change.
- Loot quality stamping uses a per-tile-entity depth instead of a thread-local stack; the rarity base probe is cached per subtype.

## 2026-11-12 — Carminite Reactor Void Breach ritual

- **Void Breach ritual.** Right-click an inactive Carminite Reactor with Salis Mundus while it is encased in the configured casing (default Void Metal, optional separate cap block) and, with the `voidmastery` GameStage, a charge ends in a permanent pair of Thaumic Augmentation fractures to The Emptiness (one at the reactor, one beside the nearest Eldritch Spire). Works anywhere except inside The Emptiness. The fractures are TA's own entities, so they spawn guardians and behave as natural fractures. A missing Emptiness or no standable destination refunds the Salis Mundus and keeps the shell. New category `Void Breach Ritual` in `aqtweaks_twilightforest.cfg`. Compile-hard against Thaumic Augmentation 2.1.14, `after:thaumicaugmentation`. Design: [twilightforest.md](twilightforest.md).

## 2026-11-12 — Infinity Blade

- **Infinity Blade.** New `aqtweaks:infinity_blade` sword with the animated texture, replacing the ContentTweaker placeholder that never loaded. Configurable attack damage (default 18 total), attack speed (1.6), durability (6000) and anvil repair item (Nether Star, `none` disables). The placeholder recipe (8 Nether Stars around a Diamond Sword) is **off by default** (`Enable Default Recipe`); the crafting method is undecided. Rarity comes from the rarity overhaul's `Custom Item Rarities`. Design: [weapon.md](weapon.md).
- The old DEVBOX `infinity_sword` CraftTweaker and ContentTweaker scripts were removed; no remap (no old swords existed).

## 2026-11-10 — Rarity overhaul

- **New rarity ladder.** Common white, Uncommon green, Rare dark aqua, Epic light purple, Legendary gold, Mythic rainbow. Uncommon, Rare and Epic recolor the vanilla `EnumRarity` constants (Forge `EnumHelper` adds `LEGENDARY` and `MYTHIC`).
- **Enchantments and Quality Tools lift rarity.** An item shows the highest of its base tier, its best non-curse enchantment (common/uncommon to Uncommon, rare to Rare, very rare to Epic) and its Quality Tools quality color (green and yellow to Uncommon, dark_aqua to Rare, gold to Legendary). Nothing lowers a tier; a Sharpness-only sword is Uncommon instead of vanilla's flat Rare.
- **Overrides.** `Custom Item Rarities` (empty by default) sets any item, meta or `modid:*` to a tier and always wins. Two early mixins (`ItemStack.getRarity`, `Item.getForgeRarity`) carry it, because tooltips and JEI read `getForgeRarity`.
- **Tooltip.** Mythic names are a per-character rainbow (animated by default); an optional rarity subtitle line is off by default. The hotbar name stays white.
- New `config/arcanaquesttweaks/aqtweaks_rarity.cfg`. Design: [rarity.md](rarity.md). Six unit-test classes (25 tests).

## 2026-11-09 — Mana-Weave infusion (Thaumcraft + Botania)

- New Thaumcraft infusion "Mana-Weaving": any damageable item can be infused with Botania's Manasteel self-repair. It needs the item's Manasteel cousin (or a Manasteel Block when there is none), a full-size Mana Tablet (500,000 mana, consumed), a Rune of Mana, Mana Pearl, Mana Diamond, Salis Mundus and two Thaumium Ingots, plus essentia (30 / 30 / 60 / 30) at instability 9.
- Infused items repair one point per tick from the carrier's Botania mana (120 per point for tools and weapons, 140 for armor; armor only while worn) and let mana pay durability damage (60 / 70 per point), as Manasteel gear does. Botania's own mana items, Manasteel gear and Spartan Manasteel items are not accepted.
- Config: `Enable Mana Repair`, `Enable Mana Repair Damage Mitigation`, `Mana Repair Cost Multiplier`. Docs: [thaumcraft.md](thaumcraft.md).
## 2026-11-09 — Tartarite Paragon absorbs other metals

- Metallurgy's Tartarite tools and armor can take a Paragon from Aurorian Steel, Fiery, Octine, Knightmetal, Zanite and Gravitite (four metal blocks around the item, Metallurgy's own pattern). New recipe type `aqtweaks:paragon_shaped` (36 recipe files, shown in JEI), NBT key `arcana_paragon`, one Paragon per item, Metallurgy's own Paragon wins a tie. Per-item effects: Aurorian Steel leveling, Fiery and Octine burn, Knightmetal armor-piercing bonus, Zanite wear scaling, Gravitite launch and block lifting. Full-set perk: Gravitite's high jump needs all four worn pieces. Config `aqtweaks_paragon.cfg`; per-trait switch `Disabled Traits`. Docs: [paragon.md](paragon.md).

## 2026-11-08 — Twilight Forest: Carminite Reactor Phlogiston Matrix Synthesis Ritual

- Right-clicking a Carminite Reactor (Inactive) (`twilightforest:tower_device:12`) with Thaumcraft Salis Mundus surrounded by 6 Betweenlands Octine Blocks initiates an in-world 200-tick (10-second) synthesis ritual. An incomplete shell does not cancel the click, so a normal Twilight Forest reactor still activates.
- Requires GameStage `adamantine`, which the pack grants (Tweaks does not). The refusal is sent only when the Octine shell is complete, and Salis Mundus is not consumed. Lang: `chat.aqtweaks.twilightforest.reactor_unlearned`.
- Features a rising audio ramp (`BLOCK_PORTAL_TRIGGER`, pitch 0.5 toward 2.0) and inward flame, lava, and portal particles (directed velocity uses particle count 0).
- On tick 200, plays an explosion sound and particles with no entity or block damage, dissolves the core and 6 Octine blocks, and yields 5–7 `contenttweaker:phlogiston_matrix` alongside crystalline chime / triumph sounds. If that item is not registered, the ritual fizzles and the shell stays. A chunk or dimension unload pauses the ritual instead of dropping it. A server restart forgets a ritual that was still charging.
- Handler registers only when Twilight Forest, Thaumcraft, Game Stages, Betweenlands, and ContentTweaker are all present. Docs: [twilightforest.md](twilightforest.md).

## 2026-11-09 — Twilight Forest Ley-Rupture

- Timed arcane surges rising from the ground ([twilightforest.md](twilightforest.md)): a 30 s telegraph (silence, a rising hum, glyph wisps), then 25 to 30 s of armor-bypassing magic scour (4.0, or 1.5 in proper arcane gear) scaled by a surface-relative height gradient (full on the forest floor and underground, none about 14 blocks above the ground, so the canopy is the escape), zeroed inside Lich Tower-class structure bounds and Comfort rooms. Hunger burn, capped temporary warp, biome intensity and extras.
- Spell backlash for any magic item used while exposed (Thaumcraft casters, Botania mana items, a pattern list, brooms); surge charging of carried Embers and Botania items and placed Vis Batteries (at a warp or flux price); impermanent ley mobs with no drops.
- New `Ley-Rupture` category in `aqtweaks_twilightforest.cfg`, `/leyrupture`, channel 5, packet 13, death message. Not built: fissures, the caster Vis cost multiplier, Thaumic Augmentation charging.

## 2026-11-09 — Nether lava spouts and ash clouds; Aether Silver Squalls

- **Nether** ([nether.md](nether.md)): clustered lava spout events (30 s, 3 to 6 staggered eruptions per player, 3 s telegraph, a 6 s tremor warning, temporary flowing lava tracked and cleared by a saved queue) and withering ash clouds that flood-fill open space (through doorways, never walls) and build a Slowness, Wither and ash-lung dose by gear tier. New `aqtweaks_nether.cfg`, `/netherhazard`, channels 1 and 2, packets 10 and 11, `LoadedBlocks` (loaded-chunk reads), `weather/GearTierRule`.
- **Aether** ([aether.md](aether.md)): Silver Squalls, 2 to 5 s crosswind gusts every 45 to 120 s with a 5 s warning (heading from the event seed, pushes gliders and flyers like Atum, anchored by sneaking) and telegraphed lightning on its own timer. New `Silver Squalls` category in `aqtweaks_aether.cfg`, `/aethersquall` (alias `/aetherwind`), channels 3 and 4, packet 12.
- Death messages for the spout, ash and Aether lightning. `fastutil` added to the test classpath for the flood-fill test.

## 2026-11-08 — Ladders and vines: Space climbs, Shift holds; no drain on the ground

- Space now climbs ladders, vines and ropes (with or without a wall behind them), and Shift holds position (even with W pressed into the wall), both at the existing climb and cling costs. Standing on the ground in a ladder, vine or rope cell costs nothing and the depleted slide/input clearing is skipped there. Empty stamina still breaks the grip and slides. Docs: [stamina.md](stamina.md).

## 2026-11-08 — Moonfrost follows the Atum system

- Particles at Atum scale (up to 12000 live, thousands of attempts a tick) with Atum's spawn rules; fog end 0.12 (was 0.06); the warning is stronger (fog to 50%, snow to 60% of full) and continuous into the storm.
- Shelter is the Atum rule, but leaves do not count as a roof (`StormShelter.openSky`) and the state is held for 2 s (`ShelterHold`, `Shelter Change Delay Seconds`), so passing under a tree or doorway does not flip the effects. Atum is unchanged.
- New `Hazard Debug` HUD line. New cfg keys `Warning Particle Fraction`, `Warning Fog Fraction`, `Shelter Change Delay Seconds`, `Hazard Debug`; defaults of `Max Storm Snow Particles` (12000) and `Whiteout Fog End Fraction` (0.12) changed. Docs: [aurorian.md](aurorian.md).

## 2026-11-08 — Thaumcraft: portable Vis Battery

- A broken Vis Battery keeps its charge as item damage 0 to 10 (`MixinBlockVisBattery`); placing it restores the charge, and item subtypes are turned on so different charges never merge. Client tooltip `Stored vis: N/10`, optional charged item model and creative entry. New `aqtweaks_thaumcraft.cfg` rows `Portable Vis Battery Charge`, `Vis Battery Charge Tooltip`, `Charged Battery Model`, `Creative Charged Batteries`. Docs: [thaumcraft.md](thaumcraft.md).

## 2026-11-08 — Shared hazard timeline

- New hazard engine (`hazard/`, [hazards.md](hazards.md)): the server owns each scheduled event per dimension and channel and announces it once with `PacketHazardEvent` (replaces `PacketMoonfrostOffset`, id 9); clients animate it from the world clock. The Moonfrost storm is channel 0.
- `/aurorianweather moonfrost` now warns like a natural storm; `skipWarning` restores the old instant start. Removed `MoonfrostWorldData`, `MoonfrostNetwork` and the key/offset sync.

## 2026-11-08 — Atum sandstorm warning

- Over the 30 s before an Atum sandstorm the outdoor dust blends toward 60% of storm intensity (faster, denser flecks), then the storm ramps on from there. Particles only. New client settings `Enable Storm Warning`, `Storm Warning Seconds`, `Warning Dust Fraction`.
- `AtumWeatherSync` (server) sends Atum's own `PacketWeather` on login, entering Atum and respawn, and every `Warning Resync Seconds` (default 30) while no storm is active, so a joining client's storm countdown matches the server (Atum never sent it on join). This also fixes briefly wrong storm visuals after joining. Docs: [client.md](client.md).

## 2026-11-08 — Aurorian: fixed moonlit lighting and the Moonfrost storm

- **Fixed lighting.** `MixinAurorianWorldProvider` pins the moon phase and celestial angle (the Darkness mod's brightness swing stops); `MixinWorldFixedSkylight` fixes the gameplay sky light at 9 (crops and saplings grow, outdoor hostile spawns stop). New cfg `aqtweaks_aurorian.cfg`; `theaurorian` jar in `libs/`.
- **Moonfrost storm.** A deterministic schedule (`weather/StormSchedule`, Atum's timings) from the world time: frostbite by gear tier, non-deadlocking rime, wind push, whiteout fog applied after every fog handler (`MixinEntityRendererFog`), moon snow, wind sound, frost vignette, a 30 s fog-and-wind warning, and Elenai stamina regeneration slowed while exposed (regen modifier, never max stamina) with `MixinCUpdateRegenHandler` clamping the running timer when it ends. Proper cold gear is detected by the `simpledifficulty:cold_resist` effect (the pack's SetBonus sets) or a magic-armor whitelist. `/aurorianweather` forces and clears storms.
- `AtumStormGameplayHandler.isSheltered` now delegates to the shared `weather/StormShelter`. Docs: [aurorian.md](aurorian.md).

## 2026-11-03 — Dimension hazards overview doc

- New `docs/dimension-hazards.md`: roster of environmental hazards per dimension and their owners, Betweenlands decay and corrosion (parent mod), and the Beneath darkness hunt (`config/lycanitesmobs/spawners/darkness.json`, a Lycanites config, not Tweaks).

## 2026-11-03 — No stamina drain standing on the ground in a ladder cell

- `StaminaModule.handleServerClimbing`: clinging and ascending need `!onGround`, so idling, sneaking or jumping while standing in a ladder, vine or rope cell no longer spends stamina. The depleted pull (server `motionY = -0.15`, client slide and jump/sneak clearing) is skipped when grounded and not pushing into the climbable.

## 2026-11-02 — Docs drift audit

- Docs refreshed against the tree: `rtg.md` config row names (`Enable Village Well Chunk Cache`, `Village Well Chunk Negative TTL Ticks`), `client.md` dust particle default (250), README now links the three spawning-rollout docs, `villagegen_info.md` names the bridge prune, `portal.md` lists `PortalLang`.
- File rows added for the classes no doc named (Atum pyramid classes and mixins, Deep Dwarf client layers, perk helpers, Fast Revive and Lithomancy mixins, RTG village plate snapshot/Chunk Steward helpers, `VillageExpander`, `VillagePathLinker`, spawning rule types and index mixins, Simple Difficulty helpers).
- `Config key reference` tables added to `stamina.md`, `reskillable.md`, `client.md`, `comfort.md`, `bewitchment.md`, `rtg.md` and `depths.md`: every `@Config.Name` is now documented by exact name and default.
- `compatibility-matrix.md` gains `atum`, `dsurround` and `orelib` rows (jars that sit in `libs/` but were not in the matrix).
- New `scripts/doc_audit.py` (read-only) and *Doc drift check* in `verification.md`.

## 2026-11-02 — Village bridges join structure plates only, no dip

- **Wet roads are kept only as bridges between two village plates that each hold a structure and are reachable from the well** (`rtg/VillageBridgeRule`, `VillageBridgePrune`). Ocean arms, bridges that start off the pad, end on nothing or join a road-only plate are dropped with pieces joined only through them. Dry roads, pads and layout are unchanged. Island plates joined only by a bridge stay. New chunks only.
- **No dip before the deck.** `VillageBridges` reverts the plank over 1-deep water, levels columns the deck did not cover, and the approach width follows `Village Water Bank`.
- Unit test `VillageBridgeRuleTest`. No `/aqvillage bridges` command: diagnostics are the `bridge keep/drop` lines in `villagepatch.log`.

## 2026-11-02 — SolCarrot book cleanup, immediate milestone stamina fill, bed/hammock resting when not tired, Vigor seconds tooltip

- **Immediate SolCarrot milestone feather fill.** When achieving a SolCarrot food milestone, the newly unlocked stamina feather socket is immediately filled (`FeathersHelper.increaseFeathers`) in `StaminaModule.onFoodFinished` so the feather starts full rather than empty. Config `Minimum Food Value = 0` in DEVBOX `solcarrot.cfg` so low-nutrition foods and drinks contribute to food diversity progress.
- **SolCarrot Food Book cleanliness.** Late client mixins in `mixins.aqtweaks.solcarrot.json`:
  - `MixinGuiFoodBook`: removes `ConfigInfoPage` at RETURN of `initPages` so foods are not partitioned by nutrition tiers and the book flows directly from statistics to the food lists.
  - `MixinFoodItemStacks`: filters JEI-blacklisted items from `FoodItemStacks.getAllFoods()` using `mezz.jei.Internal.getHelpers().getIngredientBlacklist()`, hiding disabled pack items from the book.
- **Bed & hammock resting when not tired.** When right-clicking a bed or hammock and Somnia refuses sleep due to low fatigue (`OTHER_PROBLEM`) or daytime (`NOT_POSSIBLE_NOW`), players are now mounted on an invisible `EntityBedSeat` (`aqtweaks:bed_seat`) at the head block with custom height offsets (beds `+0.52`, hammocks `+0.35`, sleeping bags `+0.20`).
  - Intercepted via `SomniaSleepHandler.onPlayerSleepInBed` at `EventPriority.LOWEST` after Somnia's own lowest listener.
  - The player sits on the bed without triggering sleeping, fast-forwarding, or chat spam. Since `isRiding()` is true, Comfort homestead resting bonuses and Hearthside warp cleansing continue while seated.
  - Dismounts cleanly on sneak (Shift), and despawns if the block is broken or the rider leaves. Respects Somnia's armor sleeping rule. Configurable via `restOnBedsWhenNotTired = true` in `aqtweaks_somnia.cfg`.
- **Comfort Vigor tooltip in seconds.** Updated `ComfortTooltips.tabLines` to display Vigor regen as total seconds required to restore 10 full feathers: `Vigor: restores 10 feathers in Xs (base 30s)` (25s for Tier I, 20s for Tier II, 15s for Tier III).

## 2026-11-02 — Rested stamina sync without the full config packet, SolCarrot milestones, armor-weight HUD

- **Disconnect on Rested tier change.** `RestedBonuses.reconcile` no longer calls `FeathersHelper.syncMaxFeathers` (it ends with Elenai's full `CUpdateConfigMessage` including the armor weight list, blank at join but full mid-game). It uses the new `stamina/StaminaMaxSync`, which lowers current feathers if above the maximum and sends `PacketTemperatureMax`. The temperature module's non-lethal paths use the same helper. Confirmed in game: no more disconnects on a Rested change.
- **Logging.** `network/ChannelErrorLogger` is now installed on every registered network channel (both sides) from `FMLLoadCompleteEvent`, and names the channel. `RestedBonuses` writes temporary `[AQ-STAMINA]` lines on a Vigor or Fortitude change.
- **SolCarrot milestones** now raise the stamina maximum when reached (`StaminaModule.onFoodFinished`, entity data `AQTSolCarrotMilestones`), and login and respawn record the current count so earlier milestones have a stored value. An unreadable SolCarrot read no longer counts as 0; the stored count is used and the first failure is logged.
- **Stamina HUD with armor weight.** Bonus stamina above 20 draws as green over the weight bar from the left, and a green/iron split feather where it meets iron. See `stamina.md`.
- **Armor HUD at full stamina.** Extended's `getBlockedBaseLayerAmount` subtracted current bonus stamina from the weight, so a full bar with light armor reported nothing blocked, took the layered path, and lost both the iron and the green (and at one unit spent the iron slot turned blue). `MixinDodgeGui` now makes the blocked part the armor weight itself, so the weight bar, iron, and green stay at every stamina level. At odd weights (1, 3, ...) the boundary slot stays iron instead of drawing blue. At odd weights (1, 3, ...) the boundary slot stays iron instead of drawing blue.
- **Comfort tab shows the Rested breakdown.** The inventory hover boxes on the effect column are removed (`ComfortTooltips.onDraw`): they assumed Rested was on the top row, so they showed on whatever effect or tab was there. `MixinInventoryEffectRenderer` and `mixins.aqtweaks.comfort.json` are deleted (that class is loaded by other mods first, so the mixin never applied). The Comfort tab hover box now lists Rested tier and time, Insight, Vigor, Fortitude, and the Hearthside line; with no Rested it shows the panel's `Next:` and `Held back by:` lines. The Hearthside warp line shows only while the player is also resting at home.

## 2026-11-02 — Comfort panel: compact, bottom-left, hidden in chat

- The panel is now 37 px tall and defaults to bottom-left, below the lowest chat line and left of the hearts. "Held back by" is the last line. It is hidden while the chat input is open. The toggle tab icon is centered (`x + 10`). The jar ships a neutral tab sprite; the Stoneborn tab is a Resource Loader file in the pack.

## 2026-11-01 — Fix inventory open crash and limit comfort info to resting areas

- **Inventory Crash**: Removed invalid mixin accessor/invoker interfaces (`AccessorGuiContainer`, `InvokerGuiScreen`) from `mixins.aqtweaks.comfort.json` that triggered `IllegalClassLoadError` on pressing the inventory key; `ComfortToggleButton` and `ComfortTooltips` now call Forge/Minecraft's public `getGuiLeft()`, `getGuiTop()`, and `drawHoveringText(...)` methods directly.
- **Resting Area Comfort Info**: `isHome` in `ComfortSystemHandler` and `ComfortFeedback.send` no longer treat lingering `PotionHomestead` (Rested) outside as an active home/resting context. `ComfortClientState.present` and `panelVisible()` now strictly require `message.state != 0` (resting or in an area with comfort furniture), ensuring the comfort HUD panel only shows in areas identified as resting and fades out when roaming the wilderness or other dimensions. Version stays 1.10.

## 2026-11-01 — Fix MDECore `/mde tpx` cross-dimension teleport crash

- `MixinTeleportHelper`: replaces MDECore's broken legacy manual packet and respawn sequence in `TeleportHelper.transferPlayerToDimension` with standard Forge `Entity.changeDimension(dimension)`, eliminating client handshake terminations caused by desynced custom mod network packets (e.g. OpenGlider, veining). Cancels `sendPlayerToSpawnInCurrentWorld` when teleporting into the Nether (`dimension == -1`) to keep the player safely at the destination Nether portal rather than displacing into bedrock at `(0, 0, 0)`. Gated on `mdecore` in `AQTweaksLateMixinLoader`. Version stays 1.10.

## 2026-11-02 — Network error logging and a deploy guard

- `network/ChannelErrorLogger` is added to the end of the `aqtweaks` channel pipeline on both sides. It logs any exception raised while decoding or handling one of our packets to the `AQTweaks` logger and passes it on unchanged. Forge reports such a failure to the player as "A fatal error has occurred, this connection is terminated" but the pack never wrote the matching error line.
- `build_gradle.ps1` no longer removes old jars from, or copies to, the DEVBOX `mods` folder while a game started from that instance is running (found by command line). It still builds and writes the workspace copy, and prints how to retry; `AQ_FORCE_DEPLOY=1` copies anyway.

## 2026-11-01 — Comfort HUD, names and tooltips

- The panel defaults to top-center beneath Hwyla (chat is lower left); `Panel Anchor` takes `top-center`, `top-left`, `top-right`, `center-right`, `bottom-left`, `bottom-right`, and the old `top`/`center`/`bottom` still read. Panel lines are clipped to the panel width.
- The effect shows as **Rested** (key `effect.aqtweaks.rested`; a ContentTweaker lang had kept the old "Homestead" text). The at-home effect (`aqtweaks:clarity`) shows as **Hearthside**.
- Hovering the Rested or Hearthside row in the inventory now works away from home (the server sends the Rested values with the cleared packet) and shows the buffs, time left, warp rate and room breakdown. The pin mixin now redirects `Ordering.sortedCopy`, because vanilla re-sorted the effects after the old redirect.
- The comfort toggle is a tab on the left inventory frame under the last Reskillable tab.
- An earned Rested tier is no longer lowered by the ladder while resting; it keeps topping up at its own strength. Moving up a tier heals the new hearts.

## 2026-11-01 — Rested review fixes

- Vigor and Fortitude are cleaned up when Rested expires on its own (`PotionExpiryEvent` plus a once-per-second catch-all). Re-entering home keeps a lingering Rested tier, capped by the score. Entry is checked on the 2s pass, not only the 30s scan.

## 2026-11-01 — Village preparer sits out worlds with no RTG generator

- `VillagePlateJobPreparer.prepare` returns null (sits the job out) when the world has no RTG generator, so worker chunks in the Nether, End, Twilight Forest and Atum install instead of being discarded as "result no longer valid". Dimension 0 also looks for the live RTG generator, and the village generator falls back to `findVillageGenerator` when the stash is still empty. `unavailable` stays the discard for a failed snapshot on an RTG world.

## 2026-11-01 — Rested homestead

- Comfort's effect is **Rested** (`aqtweaks:homestead`). It lingers after the player leaves. Hurt, attack, lost furniture and a score below I end the at-home state only. Clarity (`aqtweaks:clarity`) is the at-home warp indicator and is removed at once. Insight, Vigor and Fortitude replace the borrowed Learning, Soot and Elenai potions unless a `rested.potions_*` row names one. Family decay, a shell tag, and a sitting/lying bonus are in the 30s scan. The 2s pass refreshes penalties and the ladder. Client panel, tooltips, toast, particles and the inventory toggle are in. Not built or deployed in this pass. Version stays 1.10.

## 2026-11-01 — Shadow comparison attributes results to In Control exactly

- `MixinInControlSpawnRules` now also marks the moments just before and just after In Control's `onEntitySpawnEvent` and `onEntityJoinWorld`. The conditions, modifiers and gear comparisons (and the trace's "In Control left ..." lines) use those, so a mob or event result changed by another mod (Animania replacing vanilla pigs, sheep, chickens and cows; Metallurgy and Gaia equipping and buffing mobs at join) is no longer counted as In Control's. The one remaining expected difference is the Crimson cultists (In Control buffs them at the spawn check).

## 2026-11-01 — Spawn trace

- **`SpawnTrace` / `SpawnTraceHandler`**: opt-in log (`config/arcanaquesttweaks/spawn_trace.log`) of what happened to watched mobs: spawn checks and joins with In Control's result, the conditions table's decision and the live counts; potential-spawn table offers and withholds with count, limit and needs; pack-filler group sizes and refused spots; party leader checks. Config `Spawn Trace`, `Spawn Trace Mobs`, `Spawn Trace Summary Seconds`; `/aqspawntable trace on|off|all|mobs <ids>|now|file`. With `Spawn Trace All Mobs` (default on) every mob is tallied per summary, and a mob that is not named is watched in full from the first shadow mismatch for it on. While on, every 60 s (configurable) it also writes the shadow comparison (counts each time, differences when changed) and the live counts against In Control's.
- **Modifier and gear comparison**: the snapshot is taken at join, not at the spawn check, which had blamed In Control for gear and stats the game and other mods give at initial spawn.

## 2026-11-01 — Spawn rules: modifiers and gear evaluators, comparison, and the `Spawn Rules Decide` switch

- **Shadow comparison fix** (`ConditionTable`, `ConditionRow.rollDecides`): a spawn is flagged "random" only when a random row's other tests passed, so its roll could decide the row. Before, any spawn that walked past the Atum throttle row was skipped (53% of spawns in a live run); now only Atum hostile spawns are.
- **`ModifierTable` / `ModifierPlan` / `GearTable`** (`spawning/rules/`, plain Java): modifiers combine as documented (each stat from the first matching row that sets it, potions from every matching row, `stop`), gear is the first matching row's whole loadout.
- **`RuleShadow`**: under `Shadow Spawn Conditions`, snapshots a mob before In Control acts and compares what In Control gave it (eight stats, potions, six slots) with the tables applied on paper. `/aqspawntable shadow` and `spawn_shadow.txt` list both comparisons.
- **`Spawn Rules Decide`** (`aqtweaks_spawning.cfg`, off by default): `SpawnRuleDecider` sets the spawn-check result, cancels denied joins and applies modifiers and gear once per mob (`SpawnRuleApplier`); `MixinInControlSpawnRules` makes In Control's `onEntityJoinWorld` and `onEntitySpawnEvent` do nothing while it is on. Falls back to In Control if any file is not loaded.
- **Tests**: `ModifierGearTableTest`, and `ActionDifferentialTest` runs the pack's `spawn.json` as In Control gives stats, potions and gear beside the converted tables over 300,000 joins per run (about 167,000 comparisons, none different). The game-facing code (attributes, potions, equipment, the mixin) compiled but has not been run in a game.

## 2026-10-29 — Potential spawn table (replaces In Control's `potentialspawn.json` loop, off by default)

- **`PotentialSpawnTable`** (`spawning/`, plain-Java builder in `spawning/table/`): builds the rows `regenerate_potentialspawn.py` writes from the pack's classification files at load and on reload, and answers a spawn-list query with one lookup on (dimension, biome, creature type) plus the height, water, structure and per-player count tests. Hooked at the same `ForgeEventHandlers.onPotentialSpawns` HEAD as the rule index; In Control's rules run when the table is off, cannot be built, or throws. Keys: `Use Potential Spawn Table` (default false), `Potential Spawn Table Directory`, `Potential Spawn Table Keeps In Control Rules`. No new mixin.
- **`/aqspawntable verify`** compares the table with `config/incontrol/potentialspawn.json` for every dimension, biome and creature type and writes `config/arcanaquesttweaks/potentialspawn_verify.txt`; `reload` and `dump` as well. Details in `spawning.md`.
- **Conditions evaluator and comparison with In Control** (`spawning/rules/ConditionTable`, `spawning/ConditionShadow`, `spawning/LiveQuery`): decides a spawn from `spawn_conditions.json` and, with `Shadow Spawn Conditions` on, compares that with In Control's outcome at every spawn check and join without changing anything (`/aqspawntable shadow`). Differential tests run the pack's original `spawn.json` against the converted conditions over 600,000 generated situations with no difference; two rules in `spawn.json` can never fire (see `spawn-tables-format.md`). Registered in postInit after In Control, no mixin.
- **Spawn rule files: loader, checker, converter** (`spawning/rules/`, format in `docs/spawn-tables-format.md`): reads `spawn_conditions.json`, `spawn_modifiers.json` and `spawn_gear.json`, reports every mistake as `file:line: message`, and converts In Control's `spawn.json` into them (`/aqspawntable check`, `/aqspawntable convert`). Nothing evaluates the rules yet and In Control still makes every decision. Tests convert the pack's real `spawn.json` and load the result.
- **`biome_dimensions.json`** (optional, same folder): puts a biome category in other dimensions than the Overworld, so a mob listed under that category gets rows there (`{"atum": 17}`). Rows are made per mob and dimension; with no file the output is the script's.
- **Tests** (`src/test`, JUnit 5, `build.gradle` `test` task): the builder reproduces the pack's 159-rule `potentialspawn.json` exactly from the pack files (copies in `src/test/resources/spawntable`); the verifier finds a changed weight, a missing rule, an unsupported field and a changed removal order.

## 2026-10-28 — Light recording for Chunk Steward staged populate (API v4)

- **`PopulateScopeBridge`** (`rtg/`, registered once from mod init through `ChunkStewardAPI.registerPopulateScope`, loaded only when the runtime API version is 4 or higher): Chunk Steward's staged populate applies RTG's decorations, the rest of the populate and the mod world generators after the populate call returned. The bridge opens `PopulateRelight`'s recording scope for that part (`PopulateRelight.beginResume`, which accepts a chunk that is already marked terrain populated), so those block writes and light checks are recorded and replayed in slices like an unstaged populate. A chunk with a record that is still queued keeps it; `take` is keyed by chunk. Compiled against `ChunkSteward-1.1.jar` (API version 4). Nothing changes without Chunk Steward's `features.enabled`.

## 2026-10-28 — Village plate on a Chunk Steward worker (API v3)

- **Flatten and seal can run on a terrain worker** (`rtg/PipelineContext`, `rtg/VillagePlateJobPreparer`, `rtg/VillagePlateSnapshots`; compiled against `ChunkSteward-1.1.jar`, API version 3). `PipelineContext.active()` now answers `ChunkStewardAPI.isPipelineContext()`, so the guards that were inert are live: a worker generator is never stashed (`MixinChunkGeneratorRTGVillage`, `MixinMapGenVillageWorld`), `registerVillages` does not lay out on a worker, and `MixinMapGenVillageStart` does not write the village registry from a worker. Every Chunk Steward reference sits in a nested class that loads only after the runtime `API_VERSION` (read by reflection, since the constant is inlined by javac) is 3 or higher; on a version 2 jar, or without Chunk Steward, nothing changes.
- **`VillagePlateJobPreparer`** (registered once from mod init): `prepare` runs the same village layout and builds the same snapshot the inline hooks build, one list for flatten (recovered, flatten reach) and one for seal (registry as it is, seal reach), plus the start chunks of every village near the chunk. A chunk with no village gets an empty snapshot. `isStillValid` is read-only and false only when the set of villages near the chunk changed since `prepare`; a failed `prepare` returns an unavailable snapshot, so that worker chunk is discarded and generated inline.
- **Worker passes read the job snapshot**, never the live registry; an empty snapshot writes nothing and a missing or unavailable context skips the pass. On the server thread and on verify the inline path is unchanged (`VillagePlateSnapshots` is that code moved out of the mixin). Landscape reads on a worker use the worker's own generator (pushed for the whole `generateChunk`); with none, `sampleLandscape` returns null instead of falling back to the stash.
- Not covered, and still live reads on a worker: `world.getBiome` of loaded chunks, `VillageShoreMask` and the village-water checks. No version bump.

## 2026-10-28 — Populate column relight reaches RTG chunks

- **RTG-Plus populate is now recorded** (`mixin/rtg/MixinRtgLatePopulate`, late json): RTG's `LatePopulatingChunk.doPopulate` calls `checkLight()`, `IChunkGenerator.populate`, `GameRegistry.generateWorld` and `markDirty()` itself and never reaches `Chunk.populate(IChunkGenerator)`, so the populate relight did not apply to RTG overworld chunks (flight: `relightBlock` in `placeLeavesBlock` did not collapse). The hook begins the same scope before `doPopulate` and ends it on return. Key: `Populate Relight RTG` (default true, needs `Populate Relight`). Checked against RTG-1.12.2-9.0.3.

## 2026-10-27 — Atum sandstorm gameplay expansion: 3-tier scour, baubles goggles, mob physics & scour

- **3-Tier Sand Scour Damage** (`AtumStormGameplayHandler`, `aqtweaks_atum.cfg`): Reworked unsheltered sandstorm abrasion damage (evaluated every 80 ticks / 4 seconds, bypassing standard armor absorption):
  - *Tier 0 (Exposed)*: Missing any armor slot (`HEAD`, `CHEST`, `LEGS`, `FEET`) deals **6.0 damage** (3 hearts).
  - *Tier 1 (Fully Armored)*: All 4 armor slots filled with standard armor deals **4.0 damage** (2 hearts).
  - *Tier 2 (Full Atum / Whitelisted Magic Armor)*: All 4 armor slots filled with Atum-native armor or whitelisted sets (`sandstormResistantArmorWhitelist`) deals **2.0 damage** (1 heart).
- **Protective Headgear & Baubles Goggles** (`BaublesGogglesHelper`): Stinging grit blindness and sand inhalation choking now strictly require Atum-specific headgear (`desert_helmet_*`, `wanderer_helmet`, `mummy_helmet`, `eyes_of_atem`, `halo_of_ra`, `mask_of_nuit`) or goggles. Non-Atum helmets (iron, diamond, etc.) no longer protect eyes/airways. Goggles (such as Thaumcraft Goggles of Revealing) worn in the **Baubles head accessory slot** provide full protection while leaving the primary armor helmet slot free.
- **Sandblasting Armor Durability Wear** (`enableArmorAbrasionWear`, `abrasionWearIntervalSeconds`): Unsheltered non-Atum armor pieces take 1 point of durability wear every 10 seconds. All `atum:*` armor pieces and configured whitelisted magic armors are immune to abrasive sandblasting.
- **Mob Physical Wind Push & Storm Scour** (`handleServerMobEffects`):
  - Unsheltered mobs in Atum sandstorms receive physical wind push, scaled by knockback resistance.
  - *Atum Undead, Constructs, and Fauna*: Mummies, Wraiths, Forsaken, Bonestorms, Pharaohs, Stoneguards, Stonewardens, Efreet, and native animals (Desert Wolves, Camels, Scarabs, Tarantulas, Desert Rabbits) have full immunity to storm scour damage.
  - *Atum Bandits & Brigands* (`EntityBanditBase`): Mortal brigands are treated as wearing full Atum gear, taking **2.0 damage per 4 seconds** when unsheltered.
  - *Non-Native Invading Mobs*: Evaluated under standard 6 / 4 / 2 tier damage rules based on equipped armor, plus slowness.
  - Staggered shelter checks (every 20 ticks per entity ID) ensure zero TPS impact.

## 2026-10-27 — Populate column relight

- **Populate no longer runs a light walk per placed block** (`world/PopulateRelight`, `world/PopulateRelightQueue`, `mixin/lighting/MixinChunkPopulateRelight`; early json): while a new chunk populates on the server thread, `Chunk.relightBlock` and `World.checkLight` are recorded (highest Y per column, checked positions) and the opening full `Chunk.checkLight` only marks the chunk terrain-populated. The records replay later, one `relightBlock` per dirty column, then the checks, then the real `checkLight`, in the Chunk Steward slice, nearest chunk first. The chunk is held unsent until its replay finishes; a chunk near a player (`Populate Relight Immediate Chunks`, default 1) replays at populate return. Reading a height from a pending column replays that column first. Pending work runs on unload, world unload, server stop and after `Populate Relight Max Age Ticks`. Switch `Populate Relight` (`depthsupdate` config). New chunks only. A template paste keeps its own recorder.
- **RTG overhang repair waits for the replay** (`RtgLightFixQueue`): a chunk is not repaired while it or a neighbour has a pending populate record, so the replay cannot undo the repair.
- Begins at the opening `checkLight()` call inside populate, not at the head, so Chunk Steward cancelling populate at the head cannot leave a scope open.

## 2026-10-27 — Atum ruins no longer depend on chunk visit order

- **Ruin check fills the pyramid map first** (`MixinMapGenRuin`, `mixins.aqtweaks.atum.json`): Atum rejects a ruin when a pyramid start is near it, but its check reads the pyramid map up to 4 chunks from the candidate while the pyramid generator only fills 8 chunks around the chunk being generated. A candidate at the edge of that window was judged on whichever pyramids had been stored so far, so two generation orders kept different ruins. On the first `isPyramidInChunk` read of each check the mixin now calls the pyramid generator's own `generate` centred on the check (gated by `AtumConfig.PYRAMID_ENABLED`), so the answer is the same in any order. Saved structure data is unchanged; only new chunks get consistent ruins.

## 2026-10-27 — Atum plate footprint alignment

- **One plate per pyramid** (`AtumPyramidPlate.adopt`): `adopt` used to register the template box (33 by 33, vanilla sizes a template box `0..size`) while `retarget` registers a 32 by 32 footprint, so a rotated pyramid got two plates with different keys and bounds, and which one smoothing used depended on map order. `adopt` now recovers the template origin from the box and rotation and registers the same `footprint(...)` as `retarget`, so it finds the existing plate. A new `[AQ-VERIFY] atum duplicate plate` warning (once) fires if a plate is about to be added that overlaps an existing one. Per-thread plate maps are a separate, later pass.

## 2026-10-27 — Atum plate reach and off-thread well memo

- **Atum plate order** (`AtumPyramidSmoothing.adoptNearby`, `AtumPyramidPlate.reachChunks`): the adopt scan now covers the plate's full reach, `(32 + apron + falloff + corridor + 15) >> 4` chunks (5 with the defaults), instead of a fixed 4. A saved start 5 chunks away is registered before the chunk is smoothed, whatever order chunks are visited in. A late adopt (more than 4 chunks) logs once at debug level as `[AQ-VERIFY] atum late plate adopt`.
- **Village well worker memo** (`VillageWellCache`): `startRejectReason` and `findDryWell` called off the server thread (Chunk Steward pipeline legs) used to skip the memo and rescan up to 4,224 columns each time. Workers now use their own memo maps (`workerRejectMemo`, `workerDryWellMemo`, same TTL, cap and config reset); the server-thread memo is unchanged and never shared with workers, so verdicts do not leak between threads and the pipeline verify stays an independent comparison. `[AQ-PERF]` detail strings carry ` worker` on those calls. `chunkExists` is unchanged.

## 2026-11-01 — Heal focus no longer fights Thaumic Tweaker

- **Heal focus** (`MixinFocusEffectHeal`, `ThaumcraftFocusHooks`): the `heal` redirect is gone, so Thaumic Tweaker can replace that call without a launch failure. During the heal-focus cast, the living target's heal amount is scaled on `LivingHealEvent`. Undead damage is unchanged. `Nerfed Heal Focus Effect` on the DEVBOX is `false`, so the focus still heals instantly.

## 2026-10-27 — Village start loads again

- **Launch crash** (`MixinMapGenVillageWell`): the `@Shadow` on `hasMoreThanTwoComponents` missed the runtime field and `MapGenVillage.Start` failed to load. The sizeable flag is now the start class's own boolean. After expansion it is set when non-road pieces, including the well, are more than two.

## 2026-10-26 — Village paths stay on their line

- **Path continuation** (`VillageLandHelper.pathContinuationCandidates`): a wet path is rebuilt on its own line, back along the road and forward, in the original facing. It is kept only when the new box meets the old one and it is not 1 to 4 blocks beside a road already placed. Pieces that path had added are removed with it. Houses, waystones, and RC village pieces still use the inland search.
- **Cap** (`Village Path Retry Cap`, default 3): after three successful path slides, further wet paths in that village are omitted. `0` omits every wet path.

## 2026-10-27 — Village bridges: dead ends dropped, approach at deck height

- **Dead-end bridges** (`VillageBridgePrune`, called from `MixinMapGenVillageStart` before `VillagePlate.remember`): a bridge road (a road crossing water) whose either end touches no other kept piece (within 2 blocks of the end slice) is dropped, together with the pieces that were reachable from the well only through it. A bridge that runs out into open water fails the far-end test. The village is never vetoed. New `Village Bridge Require Both Ends` (default true). Log: `bridge drop dead-end reason=no-near-piece|no-far-piece`.
- **Approach at deck height** (`VillageBridges.approach`): the road's dry columns within 8 slices of water are brought to the deck Y (`max(round(plate), sea level + 1)`): ground below is filled with dirt and topped with grass path (gravel on sand and the gravel list), ground above (seal wall, cliff lip, up to 6 blocks) is cut through the road width only. Deck and approach share one Y. Wet slices come from landscape and are cached, so every clip agrees.

## 2026-10-26 — RC relocation and village path link

- **RC relocation** (`RcStructureVillageOverlapHandler`): a cancelled overlapping natural structure is held (id and box). On the next server tick one walk of `Village RC Relocate Walk` blocks (default 64, steps of 8, nearest first) looks for a shifted box outside the village whose chunks are loaded; the structure is generated there once (seed from world seed, id and original box; RC's placer picks Y). No spot, or RC refuses it: dropped, no retry. The relocated suggest never queues another walk. `[AQ-RC]` lines in `latest.log`.
- **Path link** (`VillagePathLinker`, after `VillageRoadStamper` in the paste hook): a road end in the open is joined to the nearest other road or the well within `Village Path Link Gap` (default 16) by a one-wide grass path (gravel on sand and the gravel block list). Never crosses a village piece or water; clip-bounded and a pure function of the kept pieces. 0 turns it off.

## 2026-10-26 — Village plate snapshot (T1, behaviour-preserving)

- `VillagePlateSnapshot`: the flatten (`aqtweaks$flattenNoise`) and seal (`aqtweaks$sealVillagePlate`) passes now read an immutable value copy of the village records near the chunk (xz, well, start chunk, land, building and shrine boxes) plus each plate height, built once per pass by `aqtweaks$snapshot`. Same records, same order, same plate-height resolve, so the output does not change. Groundwork for running these passes in a Chunk Steward worker (plan `20261026-0008-village-plate-snapshot-for-chunksteward-pipeline.md`; T2 needs the Chunk Steward 1.1 jar).
- `PipelineContext.active()` (constant `false` until T2) guards the generator stash and `registerVillages`, so a pipeline worker generator can never become the stashed one. The layout scratch `ChunkPrimer` is now one per thread.
- New `Verify Village Snapshot` (`aqtweaks_rtg.cfg`, Surface, dev-only, default off): compares each snapshot with the live records and a fresh plate-height resolve and logs the first `[AQ-VERIFY] village snapshot mismatch`.

## 2026-10-26 — Village well before layout, BOP gravel roads, swamp paths, RC overlap

- **Well before layout** (`MixinMapGenVillageWell`): a never-raise well column is resolved before the town grows, so roads and houses are laid out on the dry spot. The structure-map key stays the original chunk. The old post-layout shift is gone (`well-before` in `villagepatch.log`).
- **BOP roads** (`VillageRoadStamper`): after paste, a road column whose top is `biomesoplenty:grass` or `biomesoplenty:dirt` with air above becomes gravel. Vanilla grass path, sand-to-gravel, and river bridges are unchanged. List: `Village Road Gravel Blocks`.
- **Swamp and lake paths** (`pathWetFraction`): flooded columns that are not ocean or river, and that sit inside another kept piece's pad, count as dry. Ocean, river, and the bridge rule are unchanged.
- **RC natural structures** (`RcStructureVillageOverlapHandler`): a first-time suggest whose box overlaps a village is cancelled before paste. RC village pieces are not this event. `latest.log`: `[AQ-RC]`.

## 2026-10-26 — Atum sandstorm upwind reach and exposure

- **Upwind band** (`AtumOutdoorDustEmitter`): in a storm, outdoor dust spawns from 50 blocks upwind to 16 blocks downwind, and lives 38 ticks, so looking into the wind is a wall of sand. Clear weather stays ±25 blocks along the wind.
- **Inflow** (`ParticleAtumDustSheet`): sand still spawns only under open sky. Once moving, it can blow through a doorway or window and dies when it hits a solid block.
- **Exposure** (`AtumStormGameplayHandler`): without protective headgear, an unsheltered player loses breath and chokes when the bar is empty. Missing chest, legs, or boots (or armor under 10) takes abrasion every 4 seconds; Atum wanderer or mummy cloth on those slots stops it. A strong storm plays an occasional low howl. Keys: `Enable Sand Inhalation Choking`, `Enable Abrasive Sand Scour`, `Enable Desert Storm Ambience`.

## 2026-10-26 — Light chunk resend, pyramid door lift, denser sandstorm

- **Direct light repair resends the chunk** (`RtgLightFixQueue.finishColumnRepair`): when `RTG Light Hack Fix` is on and a fix entry repaired at least one column, that chunk is sent once to players who already have it. The direct `checkLightFor` writes sky light on the server only; the resend is the same chunk packet a relog sends. Wool mode does not send it. RTG and Atum share the repair, so one send covers both. `/aqlight fix direct` still does not.
- **Atum pyramid door lift** (`AtumPyramidPlate`, `Pyramid Door Lift`, default 3): a new pyramid's apron stays on the dune average, and the template is raised so the door is 3 blocks above that apron. The entrance walk slopes from the door down to the sand. Pyramids already pasted keep their door.
- **Atum sandstorm** (`AtumOutdoorDustEmitter`, `AtumFogHandler`): clear weather and storms both spawn 80% small flecks and 20% moderate sheets, at the same sizes and speeds. Storm attempts are `700 + stormStrength * 1100`. `Max Sandstorm Dust Particles` defaults to 12000. Outdoors at full storm the terrain fog runs from the camera to about `0.12 * far`; under a roof the fog stays at the clear range.

## 2026-10-26 — Village layout counts, Atum pyramid plateau

- **Village layout audit** (`rtg/VillageLayoutAudit`): placement is unchanged. When village debug or the perf timers are on, each new village logs roads and houses placed, how many were retried off water, which slot won (back, lateral, toward the well, spiral), and a 5×5 dry/height sample around the well. Lateral is still the first dry street slot, which is the parallel-road case the counts are there to measure.
- **Atum pyramid plateau** (`atum/AtumPyramidPlate`, `atum/AtumPyramidSmoothing`): on new Atum chunks the pyramid's exterior base (template Y=8) sits on the average height of its rotated 32×32 footprint, clamped to at least Y 55. A 4-block apron and a 16-block Hermite slope blend the dunes, and a 6-wide entrance walk slopes from that plateau out to the sand. The two blocks under the sand cap are limestone (Atum has no sandstone block). Keys in `aqtweaks_atum.cfg`.

## 2026-10-26 — Atum gets RTG's light fix, and `/aqlight`

- **Non-RTG light fix** (`world/NonRtgLightFixTrigger`, `mixin/lighting/MixinChunkPopulateLight`, `RtgLightFixQueue.request`): chunks in `Non-RTG Light Fix Dimensions` (default `17`, Atum) now get RTG's own column light fix, with the same direct repair (`RTG Light Hack Fix`) and the same Chunk Steward paced, tiered, deduped queue. RTG's `fixLightingAround` and `fixLightingOnLoad` only run for `BiomeProviderRTG`, so Atum never got the fix and showed black cells under overhangs. A chunk is requested on load and, when a chunk finishes populating (a hook at the end of `Chunk.populate`), the chunk and its four diagonal neighbours. Keys: `Non-RTG Light Fix Enable`, `Non-RTG Light Fix Dimensions`. Same limits as RTG: Y 255 to 49 only, in-chunk neighbours only, and the direct repair sends no block-change packet.
- **`/aqlight`** (OP): server-side light at the block you look at (sky, block, neighbours, height map, `canSeeSky`, queued, whether RTG's rule would fire); `/aqlight scan [radius]` counts cells RTG's test would repair, plus the cells it misses (below Y 49, only brighter across the chunk edge); `/aqlight fix [direct|wool] [radius] [all]` repairs on the spot. Use it to tell missing light from client-stale light.

## 2026-10-26 — `RTG Light Hack Fix` replaces the wool switch

- The repair switch is now `RTG Light Hack Fix` in `aqtweaks_rtg.cfg`. **True (default) uses the direct `checkLightFor(SKY)` repair.** False restores RTG's wool place-and-remove. The old key `RTG Light Fix Wool` is unread; true on that key meant wool. See [rtg.md](rtg.md).

## 2026-10-26 — RTG light-fix drain follows the player

- `RtgLightFixQueue` drains in three tiers when `RTG Light Fix Adaptive Slice` is on (default). Chunks in the travel or look cone, and chunks inside the immediate radius, run first and may use up to the near slice, capped by Chunk Steward headroom minus `RTG Light Fix Reserve Ms` (4). Near chunks outside the cone use the normal slice. Unseen chunks wait for `RTG Light Fix Unseen Min Headroom Ms` (15) and a longer quiet period (`RTG Light Fix Unseen Settle Ticks`, 80). One visible chunk still runs each tick; below 20 TPS that is the only start. `RTG Light Fix Neighbourhood Trigger` skips the quiet wait once the four diagonal neighbours are terrain-populated. `[AQ-PERF]` adds `rtg.lightFix.latency.t0` / `.t1` / `.t2`, `pending.t0` / `.t1` / `.t2` (the printed milliseconds are tick counts and queue sizes), and `rtg.lightFix.budget`. See [rtg.md](rtg.md).

## 2026-10-25 — RTG `fixLightingOnLoad` deferral and direct `checkLightFor` repair (Steps C & D)

- **Queue `fixLightingOnLoad` (`mixin/rtg/MixinRtgFixLightingDefer`)**: `EventHandlerCommon.fixLightingOnLoad(ChunkEvent.Load)` is now intercepted at `HEAD` and enqueued into `RtgLightFixQueue` when `Defer RTG Light Fix` is enabled. Loaded chunks deduplicate with populate-time requests and drain under the shared tick slice budget, eliminating synchronous 256-column walks on `ChunkEvent.Load`.
- **Direct `checkLightFor` instead of wool place-and-remove (`mixin/rtg/MixinRtgFixLightingRepair`)**: In `EventHandlerCommon.fixLighting(Chunk, int, int)`, redirects `World.setBlockState(wool, 3)` and `World.setBlockToAir` to a single direct `World.checkLightFor(EnumSkyBlock.SKY, pos)` call when `RTG Light Fix Wool` is `false`. **The default is `true` (RTG's original wool behaviour)** since 2026-10-26: the direct call sends no block-change packet, so clients that already hold the chunk may keep stale light; a client update (`markBlockForUpdate`) is planned before the direct relight becomes the default. The direct relight skips the second light flood, block state changes, and neighbor block notifications (`markAndNotifyBlock`). `[AQ-PERF]` `rtg.woolRelight` continues to count column repairs.

## 2026-10-25 — Remove Astral Sorcery small shrine from village generation

- Disabled Astral Sorcery small shrine generation in villages by default (`enableAstralSmallShrineVillagePiece = false` in `ArcanaQuestTweaksConfig` and `aqtweaks_rtg.cfg`).
- `VillageAstralSmallShrineHandler.register()` now conditionally registers `IVillageCreationHandler` only when the config knob is enabled, and `getVillagePieceWeight()` returns `null` when disabled.
- Deserialization registration (`MapGenStructureIO.registerStructureComponent(VillagePieceAstralSmallShrine.class, "AQTSmallShrine")`) remains active to ensure clean backward compatibility for existing saved worlds.
- Wild Astral Sorcery shrines in the natural world remain completely untouched.

## 2026-10-25 — Atum visual overhaul: Linear fog, clear skies & 3D storm particles

- Atum 2 fog and sandstorm overhaul (`client/atum/` & `mixin/atum/`):
  - Injects late mixin `MixinAtumClientEvents` at `HEAD` of `ClientEvents.renderFog` and `ClientEvents.renderSand(float, int...)` to completely disable Atum's yellow EXP screen fog and 2D HUD sandstorm overlay (`sandstormoverlay.png`).
  - `AtumFogHandler` enforces clean `FogMode.LINEAR` across sky (`0.0F` to `farPlane`) and terrain (`0.75 * farPlane` to `farPlane`, or `0.40 * farPlane` during sandstorms), removing the blinding yellow EXP fog. Horizon colors remain untouched, keeping views through doorways and windows bright and clear without indoor darkening.
  - Replaces the 2D HUD storm with true volumetric 3D billboard dust particles (`AtumOutdoorDustEmitter`, `ParticleAtumDustSheet`). Particles render with soft depth blending and warm sandy tint `(0.92, 0.78, 0.44)`. Clear weather maintains a steady desert breeze (32 attempts/tick, 0.22 blocks/tick, up to 300 particles).
  - Wind vector is strictly locked to a single uniform heading across the entire dimension ($\theta = 0.45$ rad). All particles stream in unison in the exact same direction with zero angle jitter or conflicting directions.
  - Suppresses Dynamic Surroundings dust motes in Atum via late mixin `MixinDustJetEffect`, and installs an `IRenderHandler` weather suppressor so DSurround's diagonal rain/dust precipitation is completely disabled in Atum.
  - Spawns ~75% large billowing clouds (3.6–5.2 blocks clear, 5.0–7.5 blocks storm) and ~25% small sand grains/flecks (0.6–1.1 blocks clear, 0.8–1.5 blocks storm), all moving along the unified wind heading.
  - When a sandstorm is active (`/atumweather sandstorm`), wind speed (0.90–1.30 blocks/tick), particle density (160 attempts/tick, >2x increase), and cap (up to 1400 particles) accelerate for a thick, roaring desert gale.
  - Strict `world.canSeeSky(pos)` enforcement on spawn and in-flight guarantees that particles never spawn or blow inside covered rooms, pyramids, or underground caves.
  - Configurable via `aqtweaks_client.cfg` under category `"Atum Fog"`. See [client.md](client.md).

## 2026-10-25 — Deep vis crystals below Y 0

- `DeepCrystalGenerator` seeds Thaumcraft vis crystal clusters on cave surfaces in new Overworld chunks, Y −60 through −1 by default. Thaumcraft's own pass samples `nextInt(max(5, surfaceY - 5))`, so it never tries below Y 0. The deep pass copies that cluster shape, shard roll, attempt count and size budget (`Deep Crystal Density Scale`, default 1.0). `Enable Deep Vis Crystals` off, or Thaumcraft `generateCrystals` off, places nothing. Growth still uses the chunk aura. See [thaumcraft.md](thaumcraft.md).

## 2026-10-25 — 1.10: Deep vis crystals (Thaumcraft)

- `thaumcraft/DeepCrystalGenerator` (an `IWorldGenerator`, registered from `CommonProxy.init` when Thaumcraft is loaded) seeds Thaumcraft vis-crystal clusters on cave surfaces below Y 0. Thaumcraft's own pass picks the crystal Y with `nextInt(max(5, surface - 5))` and so never samples below 0; this mirrors its attempt count, size budget, aspect roll and 3x3x3 cluster rule (air or replaceable, touching rock) with Y in a configurable band. Config in `aqtweaks_thaumcraft.cfg`: `Enable Deep Vis Crystals` (true), `Deep Crystal Min Y` (-60), `Deep Crystal Max Y` (-1), `Deep Crystal Density Scale` (1.0). Spread, vis drain and flux pollution are Thaumcraft's own and have no Y limit. Confirmed in game. `[AQ-PERF]` counter `thaum.deepCrystals`. See [thaumcraft.md](thaumcraft.md).

## 2026-10-25 — 1.10: chunk gap recheck pacing and RTG light fix deferral

- **Chunk gap recheck pacing** (`world/GapRecheckPacer`, `mixin/lighting/MixinChunkGapRecheck`, early json). After the High City paste was fixed (3.4 s, profile `3heUXWJhoX`), the remaining stall was `Chunk.onTick` -> `Chunk.recheckGaps` (18.3 s): vanilla rechecks every flagged skylight column of a chunk in one server call (the client passes `onlyOne = true`). The server now rechecks one column per call and skips the call when the shared tick slice is spent (flags stay set, resume next tick), floor 2 columns per tick, near-player chunks use a larger slice. Config `Pace Chunk Gap Recheck` (true), `Gap Recheck Max Slice Ms` (10), `Gap Recheck Near Slice Ms` (30), `Gap Recheck Near Chunks` (3) in `aqtweaks_depths.cfg`. `[AQ-PERF]`: `light.gap.cols`, `light.gap.skipped`.
- **RTG light fix deferral and dedupe** (`world/RtgLightFixQueue`, `mixin/rtg/MixinRtgFixLightingDefer`). RTG's `fixLighting(World, ChunkPos)` for the four diagonal neighbours of each populated chunk (wool place and remove per column, 91% of its 28.2 s) is queued per chunk with repeat requests merged, and run once after the neighbourhood is quiet (`RTG Light Fix Settle Ticks`, 40), paced by the Chunk Steward slice (`RTG Light Fix Max Slice Ms` 15, near `RTG Light Fix Near Slice Ms` 30, `RTG Light Fix Near Chunks` 6, `RTG Light Fix Immediate Chunks` 2, `RTG Light Fix Max Age Ticks` 6000; switch `Defer RTG Light Fix` in `aqtweaks_rtg.cfg`). Flushed on chunk unload, world unload and server stop. `[AQ-PERF]`: `rtg.lightFix.calls`, `.repeat`, `.immediate`, `.run`, `.drain`, `.unloadFlush`.
- **Shared tick slice** (`pacing/TickSlice`): `DeferredRelight`, `PasteRelightQueue`, `RtgLightFixQueue` and the gap pacing start one clock per server tick and measure against it, so their combined work stays near the largest single limit.

## 2026-10-25 — 1.10: Dynamic Stealth server cost

- New `dynamicstealth/StealthPerf` with three trims, all configurable in the new `aqtweaks_dynamicstealth.cfg` (see [dynamicstealth.md](dynamicstealth.md)): a per-trace chunk cache for `ImprovedRayTracing.rayTraceBlocks` (`MixinImprovedRayTracing`, exact), a 3-tick memo of the entity tracker's `Sight.canSee` per (player, entity) (`MixinDSEntityTrackerEntry`, `@Redirect`; client entity visibility can lag up to 3 ticks; AI sight and the gauge untouched) and a 3-tick memo of `Sight.seenEntities` for the client visibility packet (`MixinDSVisibilityPacket`). No range, threshold or formula is changed. `FantasticLib-1.12.2.070.jar` added to `libs/` and the build script for `ImprovedRayTracing`. `[AQ-PERF]` adds `ds.trackerCanSee`, `ds.trackerMemoHit`, `ds.seenEntities`, `ds.seenMemoHit`.

## 2026-10-24 — 1.10: chunkgen Phase 0 and 3.1: biome cache, tunnel noise skip, relight probe

- `VillageLandHelper`: `isOceanBiome`, `isOceanOrRiverBiome`, `isBeachBiome` and `swampTagged` read a per-`Biome` bitmask cache (`ConcurrentHashMap`) instead of repeating `BiomeDictionary` lookups and `String.toLowerCase` per column. Same answers as before (the uncached bodies are kept as `*Uncached`). New `Verify Biome Class Cache` (`aqtweaks_rtg.cfg`, dev-only, default off) compares every cached answer to the uncached one and logs the first `[AQ-VERIFY]` mismatch. Target: `VillageCharmPaste.shouldSkip` (3.6 s / 9.7 s in the two Oct 23 profiles).
- `UpperTunnelNetwork.columnData`: B-noise (`type1B` / `type2B`) is sampled only from the first Y where the A noise reaches its threshold (no cell below it can dig, so the result is identical). Tunnel dig flags are `int` bitmasks instead of `boolean[27]`, and the four sample arrays are per-thread scratch. New `Verify Upper Tunnel Noise` (`depthsupdate` config, dev-only, default off) recomputes each column the old way and logs the first `[AQ-VERIFY]` mismatch.
- Deferred paste relight, near-player change. The first deferred test generated the High City inside the 4-chunk radius and the "replay at once near a player" rule ran the whole batch in the paste tick (profile: `PasteRelightQueue.enqueue` to `flushBatch` 10.0 s of the 12.65 s paste; the drain for other batches was 3.2 s over about 25 s). Now only columns within `Defer Template Relight Immediate Chunks` (default 1) of a player are relit in the paste tick. A paste touching a chunk within `Defer Template Relight Near Chunks` (4) is queued as a priority batch: it goes to the front and drains with `Defer Template Relight Near Slice Ms` (default 40, capped by `StewardPacing.sliceBudgetMs` headroom; 96 units per tick when Chunk Steward is absent). `paste.queued` detail now shows `immediate=N` and `,priority`.
- Deferred paste relight (`world/PasteRelightQueue`): the Bulk Template Relight replay no longer runs inside the paste tick. The first High City test with bulk mode alone cut `da.paste` from 26.0 s to 16.0 s, of which 13.2 s was the replay. Each paste is now queued as a batch and drained on world-tick END under the `StewardPacing` slice (`sliceBudgetMs`, `remainingMs(40)`, `getLoadFactor`, same pattern as `DeferredRelight`), nearest chunks to a player first, with a floor of 2 units per tick. Phase 1 relights every column, phase 2 runs the recorded checks. Pending work is flushed at once when a player is within `Defer Template Relight Near Chunks` (default 4) of any touched chunk, per chunk on `ChunkEvent.Unload` (fires before `saveChunkData`), per world on world unload, for everything on `FMLServerStoppingEvent`, and for batches older than `Defer Template Relight Max Age Ticks` (6000). New config in `aqtweaks_depths.cfg`: `Defer Template Relight` (true), `Defer Template Relight Max Slice Ms` (15), `Defer Template Relight Near Chunks` (4), `Defer Template Relight Max Age Ticks` (6000). `[AQ-PERF]` adds `paste.queued`, `paste.drain` (ms, units, pending), `paste.latency`, `paste.unloadFlush`, `paste.expired`. Block placement (about 3 s for the High City) still runs in the paste tick.
- Bulk Template Relight (`world/BulkTemplateRelight`, `mixin/lighting/MixinChunkBulkRelight` + `MixinTemplateBulkRelight`, plus a HEAD hook in `MixinWorldCheckLight`; early json). While a `Template.addBlocksToWorld` paste with a bounding volume of at least `Bulk Template Relight Min Volume` (default 4096) runs on the server, `Chunk.relightBlock` (highest Y per column) and `World.checkLight` (each position) are recorded instead of run, then replayed once when the paste returns: relight first, then the checks. Targets the Dungeon Additions High City paste, one 26.0 s call of which 22.6 s was per-block relight. Config `Bulk Template Relight` (default on) in `aqtweaks_depths.cfg`. `[AQ-PERF]` gains `paste.template` (time and size, bulk or plain), `paste.replay` (columns, checks, recorded relights) and `relight.calls` (count of relightBlock calls outside a bulk paste). No verify flag: confirm by visiting a pasted structure.
- New diagnostic `MixinRtgFixLightingProbe` (main json, no behaviour change): with `Performance Stage Timers` on, `[AQ-PERF]` gains `rtg.fixLightingAround` (calls and time) and `rtg.woolRelight` (count of RTG wool-trick relights; its time is 0 by design). Used to size the lighting work before changing RTG's relight path.

## 2026-10-19 — RTG 9.0.3 compatibility: `LatePopulatingChunk` seal target and mineshaft paste hook

- Updated RTG dependency from `RTG-1.12.2-7.3.3.6.jar` to `RTG-1.12.2-9.0.3.jar` in `build_gradle.ps1` (pruning 7.3.3.6 from `libs/`).
- Retargeted `MixinChunkGeneratorRTGVillage.aqtweaks$sealVillagePlateBeforeChunk` from `Chunk.<init>` to `LatePopulatingChunk.<init>` (which `ChunkGeneratorRTG.func_185932_a` now instantiates).
- Added `MixinChunkGeneratorRTGMineshaftPaste` (`mixins.aqtweaks.bettermineshafts.json`, `required: false`) at RETURN of `ChunkGeneratorRTG.generateStructure(World, Random, StructureBoundingBox, StructureStart)`. RTG 9.0.3 pastes mineshafts directly via `StructureComponent.func_74875_a` instead of calling `StructureStart.func_75068_a`, which bypassed `MixinStructureStartMineshaftBox`. The new hook restores Start AABB refreshing and `MineshaftSurfaceShaft.carveForStart` on RTG 9 chunks while keeping `MixinStructureStartMineshaftBox` for non-RTG dimensions. See [rtg.md](rtg.md) and [bettermineshafts.md](bettermineshafts.md).

## 2026-10-19 — Toolchain doc: MixinBooter / Fugue / Cleanroom runtime

- [compatibility-matrix.md](compatibility-matrix.md) Toolchain now lists the shipped `!mixinbooter-11.18.jar` and `+Fugue-0.24.4.jar` (was 11.13 / 0.23.7), the pack runtime Cleanroom 0.6.13-alpha, and that Fugue 0.25+ needs Cleanroom 0.7.3-alpha+ (on 0.6.x it surfaces as a misleading Music Triggers / Impossible Library `NoClassDefFoundError`).

## 2026-10-19 — Server module: GeographiCraft dedicated-server spawn crash

- New Server module ([server.md](server.md)). `MixinDimensionManager` (`mixins.aqtweaks.geographicraft.json`, `required: false`) redirects the client-only `Biome.func_185359_l` (`getBiomeName`) call in GeographiCraft's `DimensionManager.salvageSpawn` to the biome registry name (`"unknown"` if null). Dedicated servers no longer throw `NoSuchMethodError` while placing the first spawn. The name only fed a log line.
- `AQTweaksLateMixinLoader` overrides `shouldMixinConfigQueue(String)` and queues the GeographiCraft json only when `geographicraft` is loaded. The compile stub `zone/rong/mixinbooter/ILateMixinLoader` gains the matching default method.
- `villagegen_info.md` now names the shipped GeographiCraft **1.0** jar (was 0.9.8.5); the biome-list behavior it describes is unchanged in 1.0.

## 2026-10-19 — Default World Type to RTG in World Creation GUI

- In `GuiCreateWorld` (`MixinGuiCreateWorld`), the Create New World screen initializes with `selectedIndex` pointing to RTG (`WorldType.getName() == "RTG"`) instead of vanilla Default (0). Backed by client config key `Default World Type` under category `world creation` in `aqtweaks_client.cfg` (default `"RTG"`). Empty or unregistered names leave vanilla Default. Recreate World continues to copy the source world's type because `recreateFromExistingWorld` executes after constructor return. See [client.md](client.md).

## 2026-10-18 — Infernal Furnace smelts gated metals

- The Infernal Furnace cooks the ores and dusts whose vanilla furnace recipes `furnace_gated_metals.zs` removed: one ingot per cook, with a nugget and Rare Earth roll only when that ore does not already have that bonus. Gold and silver keep Thaumcraft's nuggets. Flag off does not restore the furnace recipes. See [thaumcraft.md](thaumcraft.md).

## 2026-10-18 — Village walls build down to sea level; DA StructureStart shadow fix & LibraryEx CME fix

- In `MixinChunkGeneratorRTGVillage`, village ocean/cliff walls are allowed to generate down to sea level (`y62`, `colFloor = min(sealFloor, 62)`) instead of stopping at `plateY - villagePlateDepth` (which was leaving open gaps and floating unsupported walls above the beach or water on high-elevation cliffs). Sealing of interior village columns is unchanged and still respects `plateY - villagePlateDepth` to preserve underground caves. See [rtg.md](rtg.md).
- `MixinDaStructureStart`: removed `@Shadow(remap = false)` on `StructureStart.field_75075_a` and `field_75074_b` (which missed MCP-named superclass fields under Cleanroom and disabled the mixin on all 14 targets) in favor of casting `(StructureStart) (Object) this` and calling `getComponents()` / `getBoundingBox()`. DA village-overlap skip and `da.init` / `da.paste` stage timers apply again. See [rtg.md](rtg.md).
- New `MixinEntityHelper` (`mixins.aqtweaks.libraryex.json`, `required: false`) redirects `Map.computeIfAbsent` in LibraryEx's `EntityHelper.getEntityLocation` and synchronizes on the cache map so concurrent client/server living-entity ticks (e.g. NetherEx freeze checks) do not throw `ConcurrentModificationException` under Java 25. See [README.md](README.md) and [compatibility-matrix.md](compatibility-matrix.md).

## 2026-10-17 — Deferred relight uses average headroom

- `DeferredRelight` no longer stops at `remainingMs(40)` on world-tick end, which was clamping near torches to 4 checks whenever that tick had already used ~40 ms even at a solid 20 TPS. One slice per server tick is `min(Deferred Relight Max Slice Ms, headroom)` at full TPS (default 8 ms, not raised when headroom is smaller). Below 20 TPS it uses remaining time against 40 ms. Under half load factor the slice is 0. The 4-check near floor still runs. Distant emitters keep a 1-check/20-tick floor when the slice is already used up, and that floor is all they get while optional work is paused.
- `shouldPauseOptionalWork()` is true when the server is behind, or when chunks are generating and average headroom is under 4 ms. At 20 TPS with at least 4 ms free, Somnia's already-lit 20-tick re-check runs while chunks trickle in. `!isLightPopulated()` is still every tick. A missing or stale Chunk Steward jar skips the slice clock. See [rtg.md](rtg.md) and [somnia.md](somnia.md).

## 2026-10-17 — First chunk light pass is not paced

- `SomniaOptimizationHandler` runs `chunk.checkLight()` every tick while `!chunk.isLightPopulated()`, including when Chunk Steward reports busy. `PlayerChunkMap` only sends a chunk after that first pass, so the 20-tick throttle and `shouldPauseOptionalWork()` were holding new chunks invisible until generation stopped. Those two gates stay on already light-populated re-checks. The Somnia sleep branch is unchanged. `DeferredRelight` only retries emitter flood-fills after a chunk can already be sent, so its busy skip stays. See [somnia.md](somnia.md).

## 2026-10-16 — Depths HeightManager cache and village well cache/memo

- **Part A (`HeightManager` cache):** New `MixinHeightManager` (`mixins.aqtweaks.depthsupdate.json`, `required: false`, `defaultRequire: 1`) and `depths/HeightCache` replace `HeightManager.get(int)` and `isExtended(int)` with a dimension-indexed array (`-64..191`, size 256) and hash-map fallback outside that range, invalidated on `HeightManager.initialize` and Depths config reload. Eliminates the 14.37% Server-thread `ConcurrentHashMap.computeIfAbsent` hotspot in Spark. Configurable via `Enable HeightManager Cache` and `Verify HeightManager Cache` in `aqtweaks_depths.cfg`. See [depths.md](depths.md).
- **Part B (`VillageWellCache`):** New `rtg/VillageWellCache` caches `VillageLandHelper.chunkExists` per dimension (peeking `ChunkProviderServer.loadedChunks` via `AccessorChunkProviderServer` without touching `unloadQueued`, checking a permanent positive `LongOpenHashSet` and a short-TTL negative `Long2LongOpenHashMap` default 200 ticks before calling `World.isChunkGeneratedAt`) and memoizes `startRejectReason` / `findDryWell` with a short TTL (default 600 ticks) keyed by `(dimension, seed, configFingerprint)`. A `null` (allow) verdict is only cached/used while no `StructureStart` exists at that well chunk so `forgetRejectedStarts` still vets newly created starts into `VETTED_STARTS`. `StageTimer` records `village.wellReject` and `village.findDryWell` with distinct/repeat/memoHit counters. Configurable via 5 new keys in `aqtweaks_rtg.cfg`. See [rtg.md](rtg.md).

## 2026-10-16 — Chunk Steward headroom pacing & worldgen stage timers

- New `pacing/StewardPacing` runtime bridge to `ChunkStewardAPI` (compile-hard against `ChunkSteward-0.3.jar`, `after:chunksteward` on `@Mod`, inner static `Api` class loaded only when `chunksteward` is present with `LinkageError` fallback).
- `DeferredRelight` paces near-player emitter checks against `StewardPacing.remainingMs(40.0)` with a 4-check/tick floor, and limits distant (>96 blocks) emitters to a 1-check/20-tick floor when `StewardPacing.shouldPauseOptionalWork()` is true so they cannot starve indefinitely.
- `SomniaOptimizationHandler` skips non-priority 20-tick ambient light checks when `StewardPacing.shouldPauseOptionalWork()` is true; priority chunks within 2 chunks of an awake player still drain every tick.
- Step 0 worldgen stage timer (`perf/StageTimer`, gated behind `Performance Stage Timers` in `aqtweaks_rtg.cfg`, default off) logs 5-second summaries (`[AQ-PERF]`) for village layout, flatten, seal, paste sub-stages, and Dungeon Additions `Start` `create` vs `func_75068_a`.
- `/aqvillage` (`CommandAqVillage.nearestUnexplored`) logs a stopwatch breakdown of total ms, cells scanned, `isChunkGeneratedAt`, `canSpawnVillage`, and `resolvedWellForChunk`.

## 2026-10-16 — Dungeon Additions village overlap skip

- When `Skip Structures On Village` is on, Overworld Dungeon Additions structure starts (`WorldGenFrozenCastle$Start`, `WorldGenLichTower$Start`, `WorldGenOutposts$Start`, and the other 11 DA worldgen starts) cancel `generateStructure` if their bounding box overlaps a village pad + Y volume. No retry. Nether, End, and Aether DA structures are unaffected. Optional `mixins.aqtweaks.da.json`. New chunks only. See [rtg.md](rtg.md).

## 2026-10-16 — Mineshaft mouth stone-brick collar

- The fallback surface shaft's one-block oak ring is now a 3×3 stone-brick collar five blocks deep (shorter shafts stop at the tunnel). The center stays the hole and ladder, so a hillside no longer slices the mouth into a plank lip. New chunks only. See [bettermineshafts.md](bettermineshafts.md).

## 2026-10-06 — Version 1.9

- `build.gradle` `version`, `ArcanaQuestTweaks.VERSION` and `mcmod.info` (via `${version}`) are now **1.9**. Curse already holds 1.8, so the file for the upload must be a new version; everything dated 2026-10-04 to 2026-10-06 below ships in 1.9. No rebuild was done for the bump: the next `.\build_gradle.ps1` produces `ArcanaQuestTweaks-1.9.jar`.

## 2026-10-04 — Surface indoor insulation and boat wetness

- `DynamicModifierInsulation` pulls biome, time, altitude, snow, and Serene Seasons temperature toward neutral when the sampled position is sheltered, covering the surface houses Simple Difficulty's `Y >= 64` underground cutoff leaves exposed. Heaters, chillers, armor, baubles, held items, and wetness keep full effect indoors.
- Shelter is an opaque **or glass** roof plus, by default, at least 3 of 4 cardinal collision walls. Glass ceilings count because glass does not raise the light heightmap. Serene Seasons greenhouse glass shifts the target from 12 to 14 — more heating in cold, less cooling in hot — matching Serene Seasons' own 7-block, ignore-what's-between scan so warmth and crop fertility agree. Greenhouse glass is read by registry name, so no Serene Seasons jar is added.
- `MixinWorldUtil` lifts the temperature sample one block when a player rides a boat over water, so an open boat in clear weather no longer reads as wet `-6`. Rain, submerged boats, swimming, and other mounts are unchanged. Seven new `aqtweaks_stamina.cfg` keys. New module doc: [temperature.md](temperature.md).

## 2026-10-04 — Somnia wake no longer desyncs the mouse

- Somnia's wake packet closes the sleep screen on the network thread, so the cursor grab did not take effect and the camera stopped following the mouse until a click or Escape. `MixinMinecraftMouseGrab` now re-queues any off-thread `displayGuiScreen` onto the client thread.

## 2026-10-06 — Server relight queue skips liquids, nearest first

- `DeferredRelight` ignores liquid emitters (underground lava made up most of the 10,000+ queued per minute) and retries nearby emitters (within 96 blocks of a player) first; distant ones use leftover budget. Same rule as the client sweep.

## 2026-10-06 — Client relight sweep skips lava, prioritizes nearby

- First version's queue sat at its 8192 cap with only 0 to 52 checks run per minute: underground lava was flagged stale by the thousand and blocked real torches. The sweep now skips liquid emitters, caps 64 stale cells per section, remembers positions it already checked, drops the oldest when full, and runs checks nearest-first (within 128 blocks).

## 2026-10-06 — Client relight sweep

- New client-only `ClientRelightSweep`: when a chunk arrives, emitters (torches, lanterns, lava) whose stored light, or an open neighbor's, is lower than they should give get a `World.checkLight` on the client thread. Fixes village torches and lanterns (Charm perimeter lights) that stayed dark on the client until a torch was placed nearby. Config `Enable Client Relight Sweep` default on, `Client Relight Debug` default off.

## 2026-10-06 — Deferred relight diagnostics

- New `Deferred Relight Debug` (rtg cfg, default off): `[AQ-RELIGHT]` log line per minute per world with lit-at-placement, queued, retried, expired and overflow counts and sample coordinates. For diagnosing village torches that stay dark in new chunks.

## 2026-10-06 — Deferred relight queues only light emitters

- `DeferredRelight.onCheckFailed` ignores positions whose block does not emit light. The lighting check runs for every opaque block placed during generation, so shrines and villages queued thousands of stone blocks; the 8192 cap dropped the torches and lanterns first, and each retry did a full flood-fill. Never reads an unloaded chunk.

## 2026-10-06 — Deferred relight for chunk-generation lighting

- Torches and lanterns placed during chunk generation (villages, ocean monuments, shrines) no longer stay dark. `World.checkLight` silently did nothing when the surrounding chunks were not loaded and nothing retried. New `MixinWorldCheckLight` (early json) queues each failed server-side check in `DeferredRelight`, which retries it once the area is loaded (bounded: 8192 entries, 6000 ticks, 64 per tick per world). Config `Enable Deferred Relight`, default on. See `rtg.md` item 28.

## 2026-10-06 — Tab-complete crash traced to Serene Seasons

- The `"list1" is null` soft crash in `MinecraftServer.getTabCompletions` (AQ-005) comes from `/ss` (`/sereneseasons`), e.g. `/ss setseason <TAB>`: `SSCommand.getTabCompletions` returns `null` for any argument position but the first. New optional `mixins.aqtweaks.sereneseasons.json` / `MixinSSCommand` returns the sub-season names (`early_spring` ... `late_winter`, filtered by what you typed) for `/ss setseason <TAB>` and an empty list elsewhere. Compile-hard against the Serene Seasons jar (`libs/`, `$deps`).

## 2026-10-04 — Remote rift lighting retries until its chunks load

- `RiftLighting` kept a `checkLight` job for one attempt only. `World.checkLight` silently returns false while the surrounding area is not loaded, so the far end of a pair (and a rift seen right after a dimension change) stayed unlit until another light update. Jobs now stay queued until `checkLight` returns true, up to 30 s.

## 2026-10-04 — Portal: fading particles, rift always tracked under Dynamic Stealth

- Rift particles thin out as the rift's time runs down (100% to 15% of the 2 per tick). New synced `LIFESPAN` parameter on `EntityArcaneRift`.
- `MixinDSEntityTrackerEntry` keeps the rift tracked whenever it is in range and the player's chunk is loaded, so DS no longer removes it when you look away, and without the full-bypass render failure on return. The rift is not in the DS Full Bypass list.

## 2026-10-04 — Homestead I Learning is 4 minutes

- Extra Alchemy Learning from Homestead / Comfort tier I lasts 4:00 (4800 ticks), down from 8:00. Tier II and III effects are unchanged.

## 2026-10-04 — Sleep minimum 5 hours

- `Sleep Minimum Hours` default is now 5 (was 6). A test sleep that began at about 00:20 and ended at dawn lasted 5.7 hours and was correctly refused at 6. Confirmed working in play; the temporary `[AQ-SLEEP]` diagnostics were removed. An existing `aqtweaks_thaumcraft.cfg` keeps its saved value until edited.

## 2026-10-04 — Cleanup Batch 4 (performance, robustness, hygiene)

- Performance: dimension-warp 2s delay is a server-tick queue (no `new Thread` per dimension); Thaumcraft exposure grants parsed once; `SpawnLayerFilter` caches entity id strings; Comfort cozy scan caches `Block -> CozyConfig` (cleared on comfort config reload); Stamina caches weapon/throwing classification per item (cleared on config change, `Locale.ROOT`); `PerkWishlist` fortify/unyielding/slow fall test the cheap condition before the perk lookup; `QualityDurability.trace` is gated by `Trace Wear` (default off, no NBT reads or formatting when off).
- Leaks: `PerkDurability` marks expire after one tick (a cancelled break no longer makes an unrelated `damageItem` free) and clear on logout; `LAST_NOTIFY_TICK` and `MISMATCH_AT` prune expired entries; `VETTED_STARTS` is bounded.
- Simple Tomb: slot map handed to the tomb through `TombSlotMapAccess` instead of an NBT write/read round trip; grave-slot tags and a stale death context are cleared on respawn and after drops.
- `MixinEntityAITasks` logs the first NPE and any reset failure per task class (`[AQ-AI]`); `MixinASMHooksVillagePaste` logs once if its bridge fails (`[AQ-CHARM]`).
- Reskillable: perk requirement rows that name another trait (`trait|`) are applied after every Tweaks trait is registered. The boot "Unlockable not found" errors remain: they are Reskillable re-reading the saved requirement strings in `config/reskillable.cfg` while it constructs each trait (7 forward references plus `elenaidodge2:dodge`), which Tweaks cannot intercept. They are cosmetic; the restamp sets the correct requirements. Building perk icons: Drafter, Sculptor and Transpose now use the Effortless Building radial-menu icons (wall, cube, replace).
- Build: the four required mixin jsons now set `injectors.defaultRequire = 1`. First boot crashed on `MixinStructureStartVillagePaste.aqtweaks$skipWetVillagePaste` (0 targets: Charm's ASM rewrites that invoke into `ASMHooks.addComponentParts`, which `MixinASMHooksVillagePaste` handles), so that one redirector is now `require = 0`; `mcmod.info` takes its version from `build.gradle` (`verifyReleaseJar` fails if `ArcanaQuestTweaks.VERSION` differs) and lists `charm`; `depthsupdate` is in the `$deps` copy list; build artifacts (`.gradle/`, `build/`, `remap_out*.txt`, `_spark_scan.py`) removed from the Git index (files kept, now ignored).

## 2026-10-04 — Building Reach I / II / III perks

- The EB reach upgrade items are also hidden (creative tab removed in `postInit`, so JEI drops them). Perk descriptions state the measured ranges, about 7 / 15 / 30 blocks.

- New Building perks Reach I, II, III (chained, Building 10 / 20 / 28) set Effortless Building's reach tier to 20 / 50 / 100. The EB reach upgrade items are disabled (no recipes, right-click cancelled, stored upgrade level ignored); `Disable EB Reach Upgrade Items` = false restores them. Icons reuse the EB item art.

## 2026-10-04 — Building skill raises vanilla block reach

- The Building skill now adds to the Forge `generic.reachDistance` attribute (`Vanilla Reach Per Level` 0.0625, `Vanilla Reach Max` 3.0), so block placing, breaking and using reach further in Normal mode. Before, the bonus only touched Effortless Building's non-Normal modes, so it was never visible in plain play. Attack and mob reach are unchanged.

## 2026-10-04 — Tree Chopper harvest perks, mineshaft shaft aimed at the tunnels

- Lumberjack, Reforester and every other harvest perk now work on trees felled by Tree Chopper. Its `DestroyTree` breaks blocks with `World.destroyBlock(pos, true)`, which raised `HarvestDropsEvent` with no harvester, so all perks were skipped. `MixinTreeHandler` (new optional `mixins.aqtweaks.treechopper.json`, Tree Chopper jar added to `libs/`) records the felling player in `HarvestActor`.
- Reforester is now a gathering-scaled chance per leaf (`Reforester Chance Per Level` 0.03) instead of one per leaf, with `Reforester Max Per Tree` = 64 for a felled tree and `Reforester Guaranteed` to restore the old rule. Skill descriptions updated.
- The Better Mineshafts surface shaft is built by a Start-level hook that can see the sibling tunnel pieces: straight down the hub when it lies inside one, otherwise at the nearest piece box. Fixes shafts that ended in solid stone. One `[AQ-MINESHAFT]` log line per Start.

## 2026-10-04 — Sleep warp cleanse by hours slept

- The sleep cleanse no longer requires morning or `wakeImmediately == false`. It requires `Sleep Minimum Hours` (default 6, in-game, counted on the world clock so Somnia fast-forward counts) between going to bed and waking. Previously waking before morning, or leaving the bed after a long sleep, cleared nothing.
- Temporary warp: sleep time that player ticks did not cover (Somnia Case B) is credited at the player's Homestead band rate (2 / 3 / 6 progress per 30s, 12 per temp warp) through the existing Comfort counter. New config `Sleep Minimum Hours`, `Sleep Comfort Temporary Warp Clear`.

## 2026-10-04 — Mineshaft surface shaft, recipe skip, build script

- Better Mineshafts entrances that BM refuses to build (common on RTG terrain) now get a 1x1 ladder shaft with a plank collar from tunnel level to the ground (`MineshaftSurfaceShaft`; config `Surface Shaft Fallback`, default on). Previously only a hidden stub was kept and the mineshaft had no way in.
- `RecipeJsonSkip` also matches unqualified ids (`"tape"`) inside the owning mod's own recipes, which silences the `bibliocraft:tapemeasure` parse error.
- `build_gradle.ps1`: no more false failure when stderr is merged (`GRADLE_OPTS` native-access flag, exit-code check), env-var overrides for the DEVBOX and JDK paths, exact-version jar copy.

## 2026-10-04 — Glider cancel at 0 feathers, plate depth rule

- `OpenGliderHelper.undeploy` also sends Open Glider's `PacketClientGliding(false)`. Before, the server flag cleared but the client kept gliding, so billing stopped and feathers regenerated mid-glide.
- Village plate seal: new `Village Plate Depth` (default 15). The seal runs from `max(Village Plate Min Y, plate Y - Depth)` to the plate Y (Min Y stays 50).

## 2026-10-04 — `Reflect` removed (cleanup batch 3)

- `util/Reflect.java` (3,900 lines, ~190 reflective handles) is deleted. `remap = false` only affects mixin annotation strings; method bodies are remapped by `remapJar`, so vanilla is called directly everywhere. Verified in the built jar: a `remap = false` mixin body references `field_151587_i` / `func_185904_a`.
- Protected/private vanilla members go through new accessors in `mixin.vanilla` (`AccessorMapGenBase`, `AccessorMapGenStructure`, `AccessorMapGenVillage`, `AccessorStructureComponent`, `AccessorVillageStart`, `AccessorChunkProviderServer`) wrapped by `rtg/StructureAccess`. This also turns the village save (`saveMapGenStructureStart`) and box refresh into working code instead of silent no-ops.
- Elenai feathers/weight moved to `stamina/StaminaFeathers`; `PerkAccess.has` is the guarded Reskillable entry; `SimpleDifficultyHelper` gained guarded thirst/temperature getters; Depths block constants are `DepthsBlocks`.
- The village seal loops in `MixinChunkGeneratorRTGVillage` use `PrimerAccess` instead of per-block reflective invokes. `ThaumcraftModule` per-tick checks are plain field reads.
- The startup `[AQ-REFLECT]` audit and its 12 WARN handles are gone with the class.

## 2026-10-04 — Compile-hard conversions (cleanup batch 2)

- Grappling Hook, Open Glider and Waystones jars added to `libs/` and `build_gradle.ps1`. `stamina/GrappleHelper`, `OpenGliderHelper`, `rtg/WaystoneBridge` replace the reflection in `Reflect`, `GrappleClientInput` and `MixinStructureVillagePieces`. `MixinGrappleController` is a class-target mixin with a direct `motor` redirect (no per-tick field lookup).
- `ThaumcraftHelper` calls the Thaumcraft warp capability directly; one failure logs once instead of disabling warp for the session.
- Own-class `Class.forName` bridges (`ReskillableBonuses`, Animania Farm products/clocks) are direct calls behind cached `isModLoaded` flags (`AnimaniaAddons.FARM` for the Farm addon, which has no mod id).
- `MixinTileCrucible` asks `SimpleDifficultyHelper.isBurningCampfire` (typed `BlockCampfire.BURNING` check) instead of matching a registry string.
- `MixinGeneratorLakes`, `MixinWorldGenLakes` use `@Shadow`; `MixinSkillActive` targets `SkillActive.class` and reads `getUnlocalizedName` directly.

## 2026-10-04 — Mineshaft Start mixin, locate ranking, village plate depth

- `MixinMapGenBetterMineshaftStart` was skipped entirely at load (its `func_75068_a` hook targeted a method BM's `Start` does not declare). The box refresh moved to `MixinStructureStartMineshaftBox` on `StructureStart`, BM-guarded; the ctor Y override uses local index 5.
- `/locate Mineshaft` ranks the nearest registered Start against the nearest predicted chunk by distance, ignores registered Starts for `findUnexplored`, and skips candidates within 32 blocks of the player. Previously the first generated Start won every locate.
- The village plate seal now fills from `Village Plate Min Y` (default 50) to the plate height instead of from Y1, so caves exist under villages again. New chunks only.
- `check_mixin_targets.py` resolves nested target classes (`Outer.Inner`).

## 2026-10-04 — Dead mixin targets fixed (cleanup batch 1)

- `MixinMapGenVillageWorld` targeted `MapGenBase.func_151539_a`, a 1.7/1.8 name. It now targets `func_186125_a`, and a per-thread stack replaces the per-instance flag so nested layout `generate` calls pop their own generator.
- The Depths Y 0-4 seam pass (`SeamReinforcer`) now runs from RETURN of `ChunkGeneratorRTG.generateChunk`, once per new chunk. It was on `ChunkProviderServer`, which does not declare `func_185932_a`. `MixinChunkProviderServer` is removed.
- Better Mineshafts locate pin moved to `MixinMapGenMineshaftLocate` on `MapGenMineshaft` (BM guarded), because BM does not declare `func_180706_b`.
- `Reflect`: fixed `isSizeableStructure` (`func_75069_d`), `writeStructureComponentsToNBT` (`func_143021_a`), `ChunkProviderServer.chunkGenerator` (`field_186029_c`), `BlockPos.down`, `DamageSource.isDamageAbsolute`, `EntityArrow.shootingEntity`, `markBlockRangeForRenderUpdate`. Removed the nonexistent `setBoundingBox` handle. `updateBoundingBox` now goes through `InvokerStructureStart` (it is protected).
- `Reflect.auditUnresolved` separates parent-mod (INFO) from vanilla (WARN) handles.
- Optional mixin jsons set `injectors.defaultRequire = 1`. Required jsons (`aqtweaks`, `early`, `vanilla`, `charm`) follow after a clean boot.
- New `scripts/check_mixin_targets.py`. See [build-and-release.md](build-and-release.md).

## 2026-10-02 — Campfire neighbor notification and Thaumcraft crucible heat

- Simple Difficulty campfires notify neighbors on light, age, and extinguish so an Inspirations cauldron boils from the configured odd metas, and a fluid-filled Thaumcraft crucible heats from a burning campfire. Spec: [thaumcraft.md](thaumcraft.md).

## 2026-09-30 — Simple Tomb slot restoration, displacement, Baubles, and admin backups

- Death captures player inventory and BaublesEX slot indices in NBT. On grave retrieval, items return to exact original slots. If an occupied slot cannot accept the grave item, existing items are non-destructively displaced into empty main inventory slots, or dropped safely at the player's feet if inventory is full.
- Death events also record up to 3 rolling death backups per player in Overworld WorldSavedData (`aqtweaks_death_backups.dat`).
- Added `/aqtomb <list|recover>` OP level 2 admin command for recovering inventory if graves are lost or fail placement.
- Cosmetic Armor is preserved on death by Corpse Complex and explicitly excluded from tomb handling. Spec: [simpletomb.md](simpletomb.md).

## 2026-09-29 — Broken one use, then destroy; repair bands

- The first break leaves Broken with one use (`max - 1`). Spending that use destroys the stack. Repair from 26% up to 51% turns Broken into wear `gray`. Past 51%, wear or Broken restores the saved quality. Spec: [qualitytools.md](qualitytools.md).

## 2026-09-28 — Thaumcraft ring null texture bind

- A null bauble texture is skipped before `bindTexture`, so a worn Thaumcraft ring no longer aborts the rest of the bauble pass. Spec: [thaumcraft.md](thaumcraft.md).

## 2026-09-28 — Elenai armor tooltip weight list

- Armor tooltips read the weight string locally, so hovering armor in the Baubles screen no longer throws `ConcurrentModificationException` from Elenai's static list. Spec: [stamina.md](stamina.md).

## 2026-09-28 — Dawnstone rune reroll skips current Name

- Same-tier rune hammers pick equally among other qualities of that color (loot weight unused). Step-up still uses the full next-color pool. Type match prefers the file that lists the live quality Name.

## 2026-09-28 — Thaumcraft rings skip amulet model

- `thaumcraft:baubles` meta 1, 3, and 5 no longer get `ModelAmulet`, so rings do not draw a second player skin. A null amulet texture also skips that body draw. Spec: [thaumcraft.md](thaumcraft.md).

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
