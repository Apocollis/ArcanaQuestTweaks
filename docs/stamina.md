# Stamina module

Config: `config/arcanaquesttweaks/aqtweaks_stamina.cfg`. Compile against **Elenai Dodge 2 Extended** (`ElenaiDodge2Extended-1.12.2-1.1.3.jar`). The Forge modid is still `elenaidodge2`.

## What Tweaks does

Spends and gates **feathers** (Elenai’s stamina) for jump, melee, bow, throwing weapons, climb/cling, shield, mining, glider, grapple, ledge climb, and Dynamic Sword Skills. Shows the feather HUD even when the dodge trait is locked. Fills feathers on respawn. Optionally requires Embers for the grapple motor.

## How the parent mods work

### Elenai Dodge 2 Extended

Elenai owns the feather pool, regen, weight, absorption feathers, dodge trait, and HUD (`DodgeGui`).

- **API:** `com.elenai.elenaidodge2.api.FeathersHelper`
  - `getFeatherLevel(EntityPlayerMP)`
  - `decreaseFeathers(EntityPlayerMP, int)` — spend; Extended handles its own events internally
  - `fillFeathers(EntityPlayerMP)` — refill (used on respawn)
- **Do not** post `SpendFeatherEvent` from Tweaks. That double-spent or fought Extended. Always spend through `decreaseFeathers`.
- **Weight lists** (`ModConfig.common.weights.weights`) are applied when a player joins. Tweaks briefly clears that array at `EntityJoinWorldEvent` HIGHEST and restores at LOWEST so Elenai’s join handler does not apply weight from config during that window (weight is still used later via Reflect helpers).
- **HUD:** If `Utils.dodgeTraitUnlocked(player)`, Elenai’s own `DodgeGui` draws. If not, Tweaks draws the **same** Extended `DodgeGui.renderFeathers` / `renderAbsorptionFeathers` on the same overlay pass (`FOOD` or `ALL` when compat HUD is on).
- Half-feathers: costs in config are in Elenai’s half-feather units. Respect `ModConfig.common.feathers.half` via Reflect when comparing.

### Grappling Hook (`grapplemod`)

`com.yyon.grapplinghook.controllers.grappleController.updatePlayerPos` reads `GrappleCustomization.motor`. If motor is true, the controller pulls the player.

Tweaks **Redirects** that `motor` field get: motor is only true if the player has enough Ember (when `motorRequiresEmber`). Stamina drain for swinging is still event/tick based in `StaminaModule` + client input packets (`PacketSyncGrappleInput`). Client mixin is optional (`mixins.aqtweaks.grapple.json`).

### Embers

`teamroots.embers.util.EmberInventoryUtil.getEmberTotal` / `removeEmber`. Bound by reflection in `EmberMotorHelper`. If Embers is missing, motor is not Ember-gated.

### Dynamic Sword Skills

Skills live on `dynamicswordskills.skills.SkillActive.trigger(World, EntityPlayer, boolean)`. Vanilla DSS spends **hunger exhaustion** (`addExhaustion`) when a skill fires.

Tweaks mixins `trigger` (`mixins.aqtweaks.dss.json`):

- HEAD: if configured cost > 0 and not enough feathers, return `false` (skill does not start).
- RETURN: if trigger succeeded, `decreaseFeathers`.
- Redirect `addExhaustion`: skip when `replaceHungerExhaustion` so feathers replace hunger, not stack with it.

Costs: `dynamicswordskills:skill_id=N` in cfg. Default 0 (free until tuned). `DssSkillCosts.invalidate()` on config change.

### Other parents (events only, no mixin)

| Mod | Parent behavior | Tweaks hook |
| --- | --- | --- |
| Vanilla | Jump, attack, bow use, shield, block break | Forge events; spend or cancel |
| Spartan Weaponry | Thrown entities / use-item stop | Aiming hold uses `throwingHoldIntervalMultiplier` (default 2 = half bow drain rate); release costs `throwingReleaseCost` (1 half-feather) |
| Reskillable | Unlockable perks | Reflect `hasUnlockable` for `aqtweaks:armor_mastery` (reduces stamina weight penalty per armor piece) and `aqtweaks:mining_efficiency` (reduces block breaking cost) |
| Simple Difficulty | (used more by Comfort) | Regenerating feathers can optionally generate thirst exhaustion |
| Open Glider | Glider flight | Periodic feather drain while actively gliding |

## Tweaks implementation

| Piece | Role |
| --- | --- |
| `stamina/StaminaModule.java` | Server events: spend, gate, climb, respawn fill |
| `stamina/StaminaModuleClient.java` | HUD when dodge locked; client climb fall; ledge climb detection |
| `stamina/PacketSyncClimbingInput`, `PacketLedgeClimb`, `PacketSyncGrappleInput` | Client → server (channels 0–2) |
| `stamina/EmberMotorHelper.java` | Embers bind |
| `stamina/DssSkillCosts.java` | DSS cost map |
| `mixin/grapple/MixinGrappleController.java` | Motor field |
| `mixin/dss/MixinSkillActive.java` | Skill gate/spend |

### Climbing & Clinging
- Cost is charged while **ascending** ladders, vines, or ropes.
- Clinging in place uses `clingIntervalMultiplier` (default 2 = half drain rate).
- `fallOnDepleted` drops grip server-side with matching client physics when feathers reach 0.
- Vertical displacement is tracked so stationary clinging is never billed as full ascension.

### Ledge Climbing
- Tweaks-owned mobility mechanic (`stamina/StaminaModuleClient.java` + `PacketLedgeClimb`).
- When a player is airborne/jumping and moving forward against a 1- or 2-block ledge, client-side raytracing detects the top surface.
- If unobstructed, sends `PacketLedgeClimb` to the server, charges `ledgeClimbCost` (default 2 half-feathers), and applies an upward velocity boost to mantle the ledge.

## Do not regress

- Spend only via `FeathersHelper.decreaseFeathers`.
- HUD must use Extended `DodgeGui`, not a fork of old Elenai 1.1.0 icons.
- Respawn uses `fillFeathers`, not a manual set.
- DSS mixins must stay `required: false`.
- Keep compile jar **Extended 1.1.3**; vanilla Elenai 1.1.0 is the wrong API.
