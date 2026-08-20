# Stamina module (1.6)

Last updated: 2026-08-20.

Config: `config/arcanaquesttweaks/aqtweaks_stamina.cfg`. Compile against **Elenai Dodge 2 Extended** (`ElenaiDodge2Extended-1.12.2-1.1.3.jar`). Forge modid is still `elenaidodge2`.

This is the tweak layer over Elenai’s feather pool. Elenai owns regen, dodge, weight lists, absorption, and HUD. Tweaks **spends and gates** feathers for mobility, combat, and tools. Costs in cfg are **half-feathers** (Elenai’s unit). `20` ticks = 1 second.

Creative and spectator players are skipped everywhere. Spectator is not billed; creative `hasEnoughStamina` is always true.

## Locked intent

Spend Elenai feathers for jump, melee, bow, throwing, climb, ledge mantle, shield, mine, glider, grapple, and DSS skills. Gate those actions when the pool cannot pay. Do **not** replace Elenai regen, dodge, or HUD icons. Do **not** post `SpendFeatherEvent`.

Optional parents: Grapple motor Ember, Open Glider undeploy, Reskillable Armor Mastery / Mining Efficiency (perk **ids only** — this jar does not register those unlockables), Simple Difficulty thirst on feather regen.

## Hard constraints

- Stay version **1.6**.
- Hard `@Mod` dependency: `required-after:elenaidodge2`. Soft: `after:grapplemod;after:embers`.
- Grapple mixin `mixins.aqtweaks.grapple.json` and DSS mixin `mixins.aqtweaks.dss.json` are late, **`required: false`**.
- Packets 0–2 register on **SERVER** in `CommonProxy.preInit`. Client handlers live on `StaminaModuleClient`.
- `FeathersHelper.decreaseFeathers(EntityPlayerMP, int)` is the **only** spend path.
- Compile jar is Extended **1.1.3**, not 1.1.0 (HUD internals differ).

## How the parent mods work

### Elenai Dodge 2 Extended

Feather pool (`getFeatherLevel`), absorption capability, regen, dodge trait, `DodgeGui`, armor weight list `ModConfig.common.weights.weights` (`item=value` per slot), Lightweight enchantment, `half` feather rounding. Join handlers apply weight from that list — Tweaks briefly empties the array so those handlers do not stamp weight before Tweaks’ Armor Mastery path.

`SpendFeatherEvent` exists in Extended. Tweaks must **not** post it; spend only through `decreaseFeathers`.

### Grappling Hook Mod v13 (`grapplemod`)

Physics are **client** (`grappleController.updatePlayerPos`). `onInputUpdate` copies `moveForward` to `playerforward` then **zeros** vanilla forward — never classify climb from vanilla input. `PlayerMovementMessage` syncs pos/motion, not `onGround`. Server hooked set is `grapplemod.attached`.

Default binds: Shift+W climb, Shift+S descend, unbound dedicated climbup/down, motor on until Shift for Motor Hook. Unbound keys (keycode **0**) are ignored.

### Embers

Portable Ember via `EmberInventoryUtil.getEmberTotal` / `removeEmber` (jar in hand → bulb bauble → cartridge / `IInventoryEmberCell`). Tweaks binds those methods by reflection (`EmberMotorHelper`). If Embers is loaded but the util class is missing, motor stays disabled while `motorRequiresEmber` is true.

### Dynamic Sword Skills

`SkillActive.trigger(World, EntityPlayer, boolean)` returns whether the skill started. Stock skills also call `addExhaustion`. Tweaks gates HEAD, spends RETURN if true, optionally no-ops exhaustion.

### Open Glider

Deployed / gliding flags. Tweaks `Reflect.isGliding` = deployed and not ground/water/lava. Empty feathers → `undeployGlider`.

### Reskillable / Simple Difficulty

Perk lookup is `Reflect.hasUnlockable`. Thirst is `addThirstExhaustion` on SD’s thirst cap. Both no-op if the mod is absent.

## Design plan (spend and gate)

1. Client may predict motion (ledge, climb slide, grapple motor GET).
2. Server is authority for feathers. Packets carry climb jump, ledge request, grapple mode/motor/grounded.
3. `hasEnoughStamina` → `decreaseFeathers` or cancel/slide/detach/undeploy/reset hand.
4. Failures that still allow the vanilla action (mining break, melee hit) drain remaining usable feathers and/or apply a one-shot damage penalty.

## Feather accounting

`Reflect.hasEnoughStamina(player, cost)` (same model as Elenai spend):

1. Creative always passes.
2. Absorption feathers cover as much of `cost` as they can.
3. Remainder must fit in **usable regular feathers** = `getFeatherLevel − weight` (client uses `ClientStorage.dodges`).
4. Resulting regular pool must stay ≥ 0 **and** ≥ weight.

`Reflect.getWeight`:

1. Sum Elenai weight list for equipped armor (one match per slot).
2. Subtract Lightweight enchantment levels.
3. If Elenai `half` is false, round down to even (`floor(w/2)*2`).
4. Armor Mastery: if Reskillable flag on and perk unlocked, subtract `round(armorPieces × armorMasteryReductionPerPiece)` (default 1 per piece), floor 0.

This jar does **not** register `aqtweaks:armor_mastery` or `aqtweaks:mining_efficiency`. The pack’s Reskillable content must provide those ids or the perks never apply.

On join (`EntityJoinWorldEvent`): **HIGHEST** backup + clear Elenai’s weight array for `EntityPlayerMP`; **LOWEST** restore. That window is so Elenai’s join handler does not apply weight first. Client tick restores `ClientStorage.weightValues` if emptied and clears `ArmorTickEventListener.previousArmor` so weight re-evaluates. Armor Mastery also sends Elenai `SWeightMessage` when `ClientStorage.weight` disagrees (`ClientTickEvent` END LOWEST).

Respawn: `PlayerRespawnEvent` → `FeathersHelper.fillFeathers`. Do not set the pool by hand.

## Files

| Piece | Role |
| --- | --- |
| `ArcanaQuestTweaksConfig.StaminaModuleConfig` | All knobs → `aqtweaks_stamina.cfg`. `DssSkillCosts.invalidate()` on any `aqtweaks` cfg change |
| `stamina/StaminaModule.java` | Server spend/gate |
| `stamina/StaminaModuleClient.java` | HUD if dodge locked; climb fall + jump packet; ledge FSM; grapple `InputUpdateEvent` LOWEST |
| `stamina/GrappleClientInput.java` | Climb / descend / swing / motor / grounded from Grapple keys + controller |
| `stamina/PacketSyncClimbingInput` | Channel **0** — climb jump held |
| `stamina/PacketLedgeClimb` | Channel **1** — mantle request (empty payload) |
| `stamina/PacketSyncGrappleInput` | Channel **2** — mode, motor, grounded |
| `stamina/EmberMotorHelper.java` | Reflect Ember total/remove; `hasEmber` true if Ember not required or Embers absent |
| `stamina/DssSkillCosts.java` | `registry_name=N` map |
| `util/Reflect.java` | Feathers, weight, Grapple attach/detach, glider, ropes, thirst, `hasEnoughStamina` |
| `mixin/grapple/MixinGrappleController.java` | Redirect `GrappleCustomization.motor` GET in `updatePlayerPos` |
| `mixin/dss/MixinSkillActive.java` | Gate/spend/exhaustion on `trigger` |

## Default costs (half-feathers)

Tune in cfg unless noted. Interval `20` = once per second.

| Action | Enable | Cost | Interval | Empty / fail |
| --- | --- | --- | --- | --- |
| Jump | `enableJumpCost` | 1 | per jump | If below `jumpThreshold` (1): `motionY = 0` |
| Melee light | `enableAttackCost` | 1 | connecting hit (and block-punch swing) | Drain remaining usable; next hurt × `0.8` |
| Melee medium | | 2 | | remaining; × `0.5` |
| Melee heavy | | 4 | | remaining; × `0.3` |
| Bow draw | `enableBowCost` | 2 | on `UseItem.Start` | Cancel draw |
| Bow hold | | 1 | 20, **two timers** | Cancel / reset active hand |
| Throw hold | `enableThrowingCost` | bow hold cost | bow interval × **2** | Reset active hand |
| Throw release | | 1 | `UseItem.Stop` | Cancel throw |
| Ladder climb | `enableClimbCost` | 2 | 20 | `fallOnDepleted`: slide `motionY = -0.15` |
| Vine climb | | 3 | 20 | same |
| Rope climb | `enableRopeCost` | 3 | 20 | same; option off = free on ropes |
| Climb cling | | same cost | interval × **2** | same |
| Climb slide down | | 0 | — | attached |
| Ledge mantle | `enableLedgeClimb` | 2 | per mantle | Server: no spend, no grace. Client may still animate |
| Shield hold | `enableShieldCost` | 1 | 20 after first tick | Lower shield |
| Mine default | `enableMiningCost` | 1 | per break | Drain remaining usable; **block still breaks** |
| Mine ore / obsidian | | 2 | | same; Mining Efficiency perk −1 |
| Mining Fatigue III | | — | while **regular** feathers ≤ **4** | 40-tick refresh, amp 2, ambient |
| Glider | `enableGliderCost` | 1 | 20 | Undeploy |
| Grapple climb | `enableGrappleCost` | 3 | 20 | Detach |
| Grapple swing | | 2 | 20 | Detach |
| Grapple hang | | 1 | 20 | Detach |
| Grapple descend | | 0 | — | Stay attached |
| Grapple grounded, no motor packet | | 0 | — | Stay attached |
| Motor pull | `motorRequiresEmber` | hang stamina **+ Ember 40 / s** | 20 | Empty Ember: motor off, stay hooked. Empty feathers: detach |
| DSS skill | `enableSkillCost` | 0 unless listed | successful `trigger` | Skill does not start |

DSS default list is every stock skill `=0`. Change `Skill Costs` in cfg; no rebuild. Missing ids use `defaultSkillCost` (0).

## Event map (server unless noted)

| Event | Handler | What happens |
| --- | --- | --- |
| `EntityJoinWorldEvent` HIGHEST/LOWEST | `StaminaModule` | Clear/restore Elenai weight array. Thrown-entity intercept is a **no-op** (release is billed on `ItemUseStop`) |
| `TickEvent.PlayerTickEvent` START | `StaminaModule` | Bow hold, throw hold, climb, grapple, glider, shield, mining fatigue, thirst-on-regen |
| `LivingJumpEvent` | | Spend jump or zero `motionY` |
| `AttackEntityEvent` | | Melee spend or drain + `StaminaTweaksAttackPenalty` |
| `PlayerInteractEvent.LeftClickBlock` | | Melee spend if enough (block punch). **No** remaining-drain / penalty |
| `PlayerInteractEvent.LeftClickEmpty` | | Intended air-swing spend; Forge fires this **client-side**, and the handler returns on `isRemote` → **air swings do not spend** |
| `LivingHurtEvent` | | If **attacker** has penalty NBT, multiply damage then clear tag |
| `LivingEntityUseItemEvent.Start` | | Bow (`ItemBow`) draw cost or cancel. Throwing weapons are not billed here unless they are `ItemBow` |
| `LivingEntityUseItemEvent.Tick` | | Extra bow-hold spend on use-duration cadence (independent of player-tick timer) |
| `LivingEntityUseItemEvent.Stop` | | Throw release or cancel |
| `BlockEvent.BreakEvent` | | Mining spend (break is not cancelled) |
| `PlayerRespawnEvent` | | `fillFeathers` |
| `TickEvent.PlayerTickEvent` START (client, local) | `StaminaModuleClient` | Weight restore; climb jump packet + client slide; ledge state machine |
| `InputUpdateEvent` LOWEST (client) | | Grapple packet |
| `InputUpdateEvent` NORMAL (client) | | Climb: clear jump/sneak when empty so vanilla doesn’t keep climbing |
| `TickEvent.ClientTickEvent` END LOWEST | | Armor Mastery: sync reduced weight via Elenai `SWeightMessage` |
| `RenderGameOverlayEvent.Post` | | Feather HUD if dodge trait locked |

## Subsystems

### HUD

If `Utils.dodgeTraitUnlocked`, Elenai’s `DodgeGui` draws. If not, Tweaks calls the **same** Extended `DodgeGui.renderFeathers` / `renderAbsorptionFeathers` on `FOOD` (or `ALL` when `compatHud`). Do not fork old 1.1.0 icons. Respect `ModConfig.client.hud.hud` and creative/spectator.

### Jump

`LivingJumpEvent`, server. Enough for `jumpThreshold` → spend `jumpCost`. Else `motionY = 0`. Threshold and cost can differ (defaults both 1).

### Melee

`getWeaponType` order: empty stack → **LIGHT**; custom registry lists (exact `modid:item_id`); path keywords (dagger/parrying_dagger/rapier/knife light; saber/katana/longsword/mace/spear medium; greatsword/battleaxe/hammer/warhammer/halberd/pike/glaive/lance/scythe/staff heavy); `ItemSword` → medium; `ItemAxe` → heavy; else **NONE** (no cost). Unarmed punches are LIGHT.

**Connecting hit** (`AttackEntityEvent`): enough → spend; else drain remaining usable (regular−weight + absorption) and stash multiplier on attacker NBT. `LivingHurtEvent` applies it once then clears.

**Block punch** (`LeftClickBlock` on server): spend only if enough. No penalty tag.

**Air swing:** not billed with the current Forge event sides.

A player who punches a block and also hits a mob in the same sequence can pay twice (block click + attack). Do not “fix” that unless asked — document it.

### Bow and throwing

Bow: draw on `Start` if `ItemBow`. Hold on **both** `PlayerTick` `StaminaTweaksBowTicks` and `UseItemEvent.Tick` (`ticksUsed % interval == 0`). Those counters are independent, so hold can bill **twice per interval**. Empty hold cancels the use / resets the hand.

Throwing weapons: class/registry keywords (`throwingweapon`, javelin, throwing_knife/axe, dagger). Aiming hold uses **bow hold cost** at `bowHoldInterval × throwingHoldIntervalMultiplier` (default half rate), **player tick only**. Release spends `throwingReleaseCost` or cancels. Entity-join intercept for thrown projectiles is unused.

### Climbing (ladders / vines / ropes)

Server: `isOnLadder`. Block at feet: registry contains `rope` → rope; else `BlockVine` or simple class name contains `vine` → vine; else ladder. `dy > 0.02` ascend; sneak or `|dy| ≤ 0.02` cling; sliding down without sneak is free.

On interval: if enough, spend; **always reset the timer** even if spend fails (avoids latching). Then `fallOnDepleted` may set `motionY = -0.15` unless ledge state is 1, `ticksExisted` ≤ `StaminaTweaksLedgeClimbGrace`, or climb jump packet is true.

Client: while on a climbable, send `PacketSyncClimbingInput` (jump held). Slide locally if empty. `InputUpdateEvent` clears jump/sneak when empty so vanilla climb input dies. Mantle intent (jump+forward) skips the slide/clear.

### Ledge climb (Tweaks-owned)

**Client FSM:** airborne, not water/lava/riding, hold jump + forward ≥ 5 ticks, `motionY ≤ 0`, solid wall 0.7 along look at heights 0.4/0.7/1.0/1.3/1.6, two air collision boxes above. Sends `PacketLedgeClimb`, sets state 1, motion Y `0.090625` and look × `0.005` until `posY ≥ targetY + 0.2`. Water/lava/ride or releasing jump/forward aborts.

**Server packet:** if enough stamina, spend, set state 1 and grace `ticksExisted + 60`, play wall step sound. If not enough, **does nothing** (client may still be in state 1). Grace exists only after a paid mantle. Client motion is not fully replicated.

### Shield / glider / mining

Shield: `isActiveItemStackBlocking`; first tick arms timer (no spend); then spend each interval or `resetActiveHand`.

Glider: empty → `undeployGlider`.

Mining: `BreakEvent` — registry string contains `ore` or block is obsidian → ore cost, else default. Mining Efficiency perk subtracts `miningEfficiencyReduction` (floor 0). Break is **not** cancelled. Fatigue III (amp 2, 40 ticks, ambient, no particles) while **regular** `getFeatherLevel` ≤ threshold (default 4 = two full feathers). Not usable-after-weight.

### Simple Difficulty thirst

Each server player tick, if `enableThirstCost` and regular feathers increased vs `StaminaTweaksPrevFeathers`, add thirst exhaustion `diff × thirstExhaustionPerFeather` (default 0.25; 4.0 exhaustion ≈ 1 thirst point). Then store current feathers. First tick only seeds the key.

### Grapple stamina and motor Ember

Client `InputUpdateEvent` **LOWEST** (after Grapple zeros forward) → `GrappleClientInput` → `PacketSyncGrappleInput` on change + **10-tick** heartbeat.

| Packet mode | Meaning |
| --- | --- |
| 0 `NEUTRAL` | Hang (or swing via speed). Motor pulling also reports 0 |
| 1 `CLIMB` | Shift+W / bound climb-up; uses controller `playerforward` |
| 2 `DESCEND` | Shift+S / bound climb-down |
| 3 `SWING` | 3D speed ≥ `grappleSwingSpeedThreshold` (default **0.35**) |

Swing is sticky **3 ticks in / 15 out**. Hang↔swing does **not** reset the 20-tick bill (`hangSwingSwap`). Climb/motor/descend **do** reset the timer when cost mode changes. Climb is never inferred from Y.

Grounded = client `onGround` or controller `ongroundtimer > 0`. **Grounded and motor packet false:** free, timers cleared. **Motor packet true:** hang stamina + Ember even if `onGround` (wall, leaving ground). Client `isMotorPulling` already requires Ember, so empty Ember should send motor false.

Ember: server `removeEmber` every `motorEmberInterval` for `motorEmberCost` (**40**). Client mixin Redirects `motor` GET: false unless `hasEmber` (or flag off). Empty Ember does not unhook. Empty feathers detach on climb/swing/hang/motor; **not** on descend or grounded-without-motor.

`motorUsesHangCost`: motor bills hang rate, not climb/swing.

### Dynamic Sword Skills

Mixin `SkillActive.trigger`: HEAD cancel (return false) if cost > 0 and not enough feathers; RETURN spend if trigger returned true; Redirect `addExhaustion` no-op when `enableSkillCost && replaceHungerExhaustion`. Cost from exact `skillCosts` map or `defaultSkillCost`. Client/creative/spectator skipped. Registry name via `getRegistryName()` on the skill instance.

## Config (`aqtweaks_stamina.cfg`)

All live unless noted. Nested Forge categories.

| Name | Default | Meaning |
| --- | --- | --- |
| Enable Jump Stamina Cost | true | |
| Jump Feather Cost | 1 | |
| Jump Threshold | 1 | Min to allow jump |
| Enable Bow Stamina Cost | true | Draw + hold |
| Bow Draw Cost | 2 | |
| Bow Hold Tick Interval | 20 | |
| Bow Hold Cost | 1 | Also throw-hold cost |
| Enable Throwing Weapon Stamina Cost | true | |
| Throwing Hold Interval Multiplier | 2 | × bow interval |
| Throwing Release Cost | 1 | |
| Enable DSS Skill Stamina Cost | true | |
| Replace Hunger Exhaustion | true | Skip DSS `addExhaustion` |
| Default Skill Cost | 0 | Unknown skill ids |
| Skill Costs | stock skills `=0` | `id=N` lines |
| Enable Climbing Stamina Cost | true | Ladder/vine; ropes have their own flag |
| Ladder/Vine/Rope interval | 20 | |
| Ladder / Vine / Rope cost | 2 / 3 / 3 | |
| Enable Rope Climb Cost | true | Off = ropes free |
| Cling Interval Multiplier | 2 | |
| Fall on Stamina Depleted | true | Slide + clear climb keys |
| Enable Melee Attack Cost | true | |
| Light/Medium/Heavy cost | 1 / 2 / 4 | |
| Light/Medium/Heavy damage multiplier | 0.8 / 0.5 / 0.3 | Empty-stamina hit |
| Custom weapon lists | empty | Exact registry ids |
| Enable Grapple Stamina Cost | true | |
| Climb/Swing/Hold interval | 20 | |
| Climb/Swing/Hold cost | 3 / 2 / 1 | |
| Grapple Swing Speed Threshold | 0.35 | |
| Motor Uses Hang Cost | true | |
| Motor Requires Ember | true | Ignored if Embers missing (`hasEmber` true) |
| Motor Ember Tick Interval | 20 | |
| Motor Ember Cost | 40 | One standard pulse |
| Enable Glider Stamina Cost | true | |
| Glider interval / cost | 20 / 1 | |
| Enable Shield Stamina Cost | true | |
| Shield interval / cost | 20 / 1 | |
| Enable Mining Stamina Cost | true | Break + fatigue |
| Ore/Obsidian / default break | 2 / 1 | |
| Mining Fatigue Feather Threshold | 4 | Regular feathers, not usable |
| Enable Reskillable Perks | true | Both perk lookups |
| Armor Mastery Perk ID | `aqtweaks:armor_mastery` | Pack must register |
| Armor Mastery Reduction | 1.0 | Per armor piece |
| Mining Efficiency Perk ID | `aqtweaks:mining_efficiency` | Pack must register |
| Mining Efficiency Reduction | 1 | Subtracted from break cost |
| Enable Thirst Cost | true | Regen → SD exhaustion |
| Thirst Exhaustion Per Feather | 0.25 | Per half-feather gained |
| Enable Ledge Climbing | true | Client FSM + packet |
| Ledge Climb Cost | 2 | |

## Entity NBT keys (`StaminaTweaks*`)

| Key | Side | Use |
| --- | --- | --- |
| `PrevFeathers` | server | Thirst-on-regen |
| `BowTicks` / `ThrowTicks` | server | Hold timers |
| `ClimbPrevY` / `LadderTicks` / `ClimbJumpInput` | server | Climb dy + cling timer; jump from packet 0 |
| `GrappleTicks` / `GrappleEmberTicks` / `GrappleMode` / `GrappleMotor` / `GrappleGrounded` / `GrappleLastCostMode` / `GrappleSwingStreak` / `GrappleIsSwing` | server | Grapple bill |
| `GrappleClientSent*` | client | Packet debounce |
| `GliderTicks` | server | Glider bill |
| `ShieldActive` / `ShieldTicks` | server | Shield bill |
| `AttackPenalty` | server | Next hurt × multiplier (attacker) |
| `LedgeClimbState` / `LedgeClimbGrace` / `LedgeClimbHeldTicks` / `LedgeClimbTargetY` / `LedgeClimbDx` / `LedgeClimbDz` | both | Mantle FSM; grace is server `ticksExisted` deadline |
| `LastJumpInput` | client | Climb packet edge when leaving ladder |

## Tuning vs code

| Change | How |
| --- | --- |
| Any numeric cost / interval / enable flag | `aqtweaks_stamina.cfg`, reload or restart |
| DSS per-skill costs | cfg `Skill Costs` lines `dynamicswordskills:id=N` |
| Weapon exceptions | cfg custom registry lists |
| New spend **state** (new mode, new packet field) | Java + packet + this doc |
| New parent mod hook | Reflect or late mixin `required: false` |

Rebuild (`.\build_gradle.ps1`) only for code/mixin/packet changes.

## Design history (do not regress)

### 1. `SpendFeatherEvent`

Posting Elenai’s spend event double-spent with Extended. **Fix:** only `decreaseFeathers`.

### 2. HUD fork of 1.1.0 icons

Wrong UV / missing absorption on Extended. **Fix:** call Extended `DodgeGui` when dodge is locked.

### 3. Grapple climb from vanilla `moveForward`

Grapple zeros forward after copying to `playerforward`. Classifying from vanilla input never saw climb. **Fix:** read controller after Grapple’s `InputUpdateEvent` (LOWEST).

### 4. Pendulum apex as hang

Speed near zero at the top of a swing reset hang/swing timers and under-billed. **Fix:** sticky swing 3/15; hang↔swing does not reset the bill.

### 5. Motor Ember client-only or unhook on empty Ember

Motor kept pulling with an empty jar, or unhooked when Ember ran out. **Fix:** mixin gates `motor` GET; server consumes Ember; empty Ember leaves the hook.

### 6. Elenai join weight vs Armor Mastery

Elenai applied full weight on join before Tweaks’ reduction. **Fix:** empty the weight array HIGHEST, restore LOWEST; client `SWeightMessage`.

### 7. Climb slide fighting ledge mantle

Empty-stamina slide cancelled the mantle. **Fix:** grace ticks + jump packet + client mantleIntent skip.

## Do not regress

- Spend only via `FeathersHelper.decreaseFeathers`. Never `SpendFeatherEvent`.
- HUD = Extended `DodgeGui`, not 1.1.0 icons. Respawn = `fillFeathers`.
- Compile jar **Extended 1.1.3**. Grapple and DSS mixins stay `required: false`.
- Grapple climb/descend: controller `playerforward` after Grapple’s `InputUpdateEvent`, not vanilla `moveForward`.
- Do not treat pendulum upswing as climb. Hang↔swing must not reset the stamina timer.
- Motor Ember on **both** sides (server consume + client mixin). Empty Ember must not unhook. Empty stamina must not unhook on descend or grounded-without-motor.
- `hasEnoughStamina` must keep absorption-then-usable-after-weight. Armor Mastery must affect `getWeight` and the client `SWeightMessage` sync.
- Ledge grace / jump packet must keep `fallOnDepleted` from cancelling a mantle.
- Mining break is never cancelled. Fatigue uses **regular** feathers.

## Verify

**Combat / tools:** jump costs 1 and blocks at 0; sword 2, axe 4, dagger 1; empty-hand punch is light **on a hit**; short-stamina **hit** deals reduced damage once; bow 2 on draw + hold (hold may tick twice); throw hold slower than bow, 1 on release; shield 1/s then drops; break stone 1, ore 2; Fatigue III at ≤ 2 full feathers; glider 1/s then folds; DSS with cost > 0 spends and blocks when empty; hunger not also drained if replace exhaustion is on.

**Climb / ledge:** ladder 2/s up, cling half rate, slide free; empty slides; jump+forward mantle 2 and does not fight slide.

**Grapple:** plant on ground = 0; Shift+W = 3/s; hang = 1/s; pendulum = 2/s through the apex; Shift+S = 0; motor = hang + 40 Ember/s from jar/cartridge/bulb; empty Ember = motor off, still hooked; empty feathers = unhook except descend / grounded.

**Integrations:** Armor Mastery lowers displayed weight; mining perk −1 on break; regen feathers add SD thirst; dodge-locked still shows Extended feathers.

## Out of scope unless asked

- Billing air swings (would need a server-bound swing packet)
- Deduplicating bow hold (player tick vs `UseItem.Tick`)
- Registering Reskillable perks inside this jar
- Recarving / worldgen (stamina has none)
