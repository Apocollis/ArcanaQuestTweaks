package com.apocollis.aqtweaks;

import com.apocollis.aqtweaks.stamina.DssSkillCosts;
import com.apocollis.aqtweaks.reskillable.ReskillablePerkLayout;
import com.apocollis.aqtweaks.thaumcraft.ThaumcraftModule;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ArcanaQuestTweaksConfig {

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_stamina", category = "")
    public static class StaminaModuleConfig {
        @Config.Name("Jumping")
        @Config.Comment("Configure jumping stamina consumption")
        public static final Jumping jumping = new Jumping();

        @Config.Name("Sprinting")
        @Config.Comment("Configure sprinting stamina consumption")
        public static final Sprinting sprinting = new Sprinting();

        @Config.Name("Bow Drawing")
        @Config.Comment("Configure bow-drawing stamina consumption")
        public static final BowDrawing bowDrawing = new BowDrawing();

        @Config.Name("Climbing")
        @Config.Comment("Configure ladder, vine, and rope climbing stamina consumption")
        public static final Climbing climbing = new Climbing();

        @Config.Name("Weapons (Melee)")
        @Config.Comment("Configure melee weapon stamina consumption")
        public static final Weapons weapons = new Weapons();

        @Config.Name("Grapple Mod")
        @Config.Comment("Configure grappling hook stamina consumption")
        public static final Grapple grapple = new Grapple();

        @Config.Name("Open Glider")
        @Config.Comment("Configure hang glider stamina consumption")
        public static final Glider glider = new Glider();

        @Config.Name("Shield Blocking")
        @Config.Comment("Configure shield stamina consumption")
        public static final Shield shield = new Shield();

        @Config.Name("Mining")
        @Config.Comment("Configure mining/block breaking stamina consumption")
        public static final Mining mining = new Mining();

        @Config.Name("Reskillable Integration")
        @Config.Comment("Configure Reskillable integration settings")
        public static final Reskillable reskillable = new Reskillable();

        @Config.Name("Simple Difficulty Integration")
        @Config.Comment("Configure Simple Difficulty integration settings")
        public static final SimpleDifficulty simpleDifficulty = new SimpleDifficulty();

        @Config.Name("Ledge Climbing")
        @Config.Comment("Configure ledge climbing stamina consumption")
        public static final LedgeClimb ledgeClimb = new LedgeClimb();

        @Config.Name("Throwing Weapons")
        @Config.Comment("Configure throwing weapon (javelins, throwing knives, etc.) stamina consumption")
        public static final ThrowingWeapons throwingWeapons = new ThrowingWeapons();

        @Config.Name("Dynamic Sword Skills")
        @Config.Comment("Stamina cost when Dynamic Sword Skills activate. All stock skills default to 0 for in-game tuning.")
        public static final DynamicSwordSkills dynamicSwordSkills = new DynamicSwordSkills();
    }

    public static class Jumping {
        @Config.Name("Enable Jump Stamina Cost")
        @Config.Comment("Does jumping consume stamina?")
        public boolean enableJumpCost = true;

        @Config.Name("Jump Feather Cost")
        @Config.Comment("Feather cost (in half-feathers) per jump")
        @Config.RangeInt(min = 1)
        public int jumpCost = 1;

        @Config.Name("Jump Threshold")
        @Config.Comment("Minimum feathers required to jump. If below this, jump will be blocked/reduced.")
        @Config.RangeInt(min = 0)
        public int jumpThreshold = 1;
    }

    public static class Sprinting {
        @Config.Name("Enable Sprint Stamina Cost")
        @Config.Comment("Does sprinting consume stamina?")
        public boolean enableSprintCost = true;

        @Config.Name("Sprint Feather Cost")
        @Config.Comment("Feather cost (in half-feathers) per sprint interval")
        @Config.RangeInt(min = 0)
        public int sprintCost = 1;

        @Config.Name("Sprint Tick Interval")
        @Config.Comment("Ticks between feather consumption while sprinting (20 ticks = 1 second)")
        @Config.RangeInt(min = 1)
        public int sprintInterval = 20;

        @Config.Name("Sprint Threshold")
        @Config.Comment("Minimum usable half-feathers (after weight, via hasEnoughStamina) to start or keep sprinting")
        @Config.RangeInt(min = 0)
        public int sprintThreshold = 2;
    }

    public static class BowDrawing {
        @Config.Name("Enable Bow Stamina Cost")
        @Config.Comment("Does drawing and holding a bow consume stamina?")
        public boolean enableBowCost = true;

        @Config.Name("Bow Draw Cost")
        @Config.Comment("Stamina cost (in half-feathers) charged instantly upon drawing a bow")
        @Config.RangeInt(min = 0)
        public int bowDrawCost = 2;

        @Config.Name("Bow Hold Tick Interval")
        @Config.Comment("Ticks between feather consumption while holding a bow fully drawn (20 ticks = 1 second)")
        @Config.RangeInt(min = 1)
        public int bowHoldInterval = 20;

        @Config.Name("Bow Hold Cost")
        @Config.Comment("Stamina cost (in half-feathers) consumed per hold interval")
        @Config.RangeInt(min = 0)
        public int bowHoldCost = 1;
    }

    public static class ThrowingWeapons {
        @Config.Name("Enable Throwing Weapon Stamina Cost")
        @Config.Comment("Does holding and throwing throwing weapons consume stamina?")
        public boolean enableThrowingCost = true;

        @Config.Name("Throwing Hold Interval Multiplier")
        @Config.Comment("Multiplier on Bow Hold Interval for throwing weapons (2 = half the bow drain rate)")
        @Config.RangeInt(min = 1)
        public int throwingHoldIntervalMultiplier = 2;

        @Config.Name("Throwing Release Cost")
        @Config.Comment("Stamina cost (in half-feathers) charged when releasing a throw (same as light weapon attack)")
        @Config.RangeInt(min = 0)
        public int throwingReleaseCost = 1;
    }

    public static class DynamicSwordSkills {
        @Config.Name("Enable DSS Skill Stamina Cost")
        @Config.Comment("Should Dynamic Sword Skills spend Elenai feathers when a skill triggers?")
        public boolean enableSkillCost = true;

        @Config.Name("Replace Hunger Exhaustion")
        @Config.Comment("If true, DSS hunger exhaustion is skipped and feathers are the cost instead.")
        public boolean replaceHungerExhaustion = true;

        @Config.Name("Default Skill Cost")
        @Config.Comment("Half-feathers spent if a skill is missing from Skill Costs. 0 = free.")
        @Config.RangeInt(min = 0)
        public int defaultSkillCost = 0;

        @Config.Name("Skill Costs")
        @Config.Comment("Per-skill costs as registry_name=cost. 0 = free. Tune in this cfg without rebuilding.")
        public String[] skillCosts = new String[] {
                "dynamicswordskills:basic_technique=0",
                "dynamicswordskills:armor_break=0",
                "dynamicswordskills:dodge=0",
                "dynamicswordskills:leaping_blow=0",
                "dynamicswordskills:parry=0",
                "dynamicswordskills:dash=0",
                "dynamicswordskills:spin_attack=0",
                "dynamicswordskills:super_spin_attack=0",
                "dynamicswordskills:mortal_draw=0",
                "dynamicswordskills:sword_break=0",
                "dynamicswordskills:rising_cut=0",
                "dynamicswordskills:ending_blow=0",
                "dynamicswordskills:back_slice=0",
                "dynamicswordskills:sword_beam=0"
        };
    }

    public static class Climbing {
        @Config.Name("Enable Climbing Stamina Cost")
        @Config.Comment("Does ascending or clinging to ladders/vines consume stamina?")
        public boolean enableClimbCost = true;

        @Config.Name("Ladder Climb Tick Interval")
        @Config.Comment("Ticks between feather consumption while ascending ladders (20 ticks = 1 second)")
        @Config.RangeInt(min = 1)
        public int ladderInterval = 20;

        @Config.Name("Ladder Climb Cost")
        @Config.Comment("Stamina cost (in half-feathers) per ascend interval on ladders")
        @Config.RangeInt(min = 0)
        public int ladderCost = 1;

        @Config.Name("Vine Climb Tick Interval")
        @Config.Comment("Ticks between feather consumption while ascending vines (20 ticks = 1 second)")
        @Config.RangeInt(min = 1)
        public int vineInterval = 20;

        @Config.Name("Vine Climb Cost")
        @Config.Comment("Stamina cost (in half-feathers) per ascend interval on vines")
        @Config.RangeInt(min = 0)
        public int vineCost = 3;

        @Config.Name("Enable Rope Climb Cost")
        @Config.Comment("Does ascending or clinging to ropes consume stamina?")
        public boolean enableRopeCost = true;

        @Config.Name("Rope Climb Tick Interval")
        @Config.Comment("Ticks between feather consumption while ascending ropes (20 ticks = 1 second)")
        @Config.RangeInt(min = 1)
        public int ropeInterval = 20;

        @Config.Name("Rope Climb Cost")
        @Config.Comment("Stamina cost (in half-feathers) per ascend interval on ropes")
        @Config.RangeInt(min = 0)
        public int ropeCost = 3;

        @Config.Name("Cling Interval Multiplier")
        @Config.Comment("Multiplier applied to the climb interval while holding/clinging without ascending (2 = half the ascend drain rate)")
        @Config.RangeInt(min = 1)
        public int clingIntervalMultiplier = 2;

        @Config.Name("Fall on Stamina Depleted")
        @Config.Comment("If true, the player will lose their grip and fall/slide when out of stamina.")
        public boolean fallOnDepleted = true;
    }

    public static class Weapons {
        @Config.Name("Enable Melee Attack Cost")
        @Config.Comment("Does attacking with melee weapons consume stamina?")
        public boolean enableAttackCost = true;

        @Config.Name("Light Weapons Cost")
        @Config.Comment("Feather cost (in half-feathers) for light weapons")
        @Config.RangeInt(min = 0)
        public int lightCost = 1;

        @Config.Name("Light Weapons Damage Multiplier")
        @Config.Comment("Damage multiplier applied if attacking without enough feathers")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double lightDamageMultiplier = 0.8;

        @Config.Name("Light Weapons Custom List")
        @Config.Comment("List of registry names of custom items to treat as light weapons (e.g. modid:item_id)")
        public String[] lightWeaponsCustom = new String[] {};

        @Config.Name("Medium Weapons Cost")
        @Config.Comment("Feather cost (in half-feathers) for medium weapons")
        @Config.RangeInt(min = 0)
        public int mediumCost = 2;

        @Config.Name("Medium Weapons Damage Multiplier")
        @Config.Comment("Damage multiplier applied if attacking without enough feathers")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double mediumDamageMultiplier = 0.5;

        @Config.Name("Medium Weapons Custom List")
        @Config.Comment("List of registry names of custom items to treat as medium weapons")
        public String[] mediumWeaponsCustom = new String[] {};

        @Config.Name("Heavy Weapons Cost")
        @Config.Comment("Feather cost (in half-feathers) for heavy weapons")
        @Config.RangeInt(min = 0)
        public int heavyCost = 4;

        @Config.Name("Heavy Weapons Damage Multiplier")
        @Config.Comment("Damage multiplier applied if attacking without enough feathers")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double heavyDamageMultiplier = 0.3;

        @Config.Name("Heavy Weapons Custom List")
        @Config.Comment("List of registry names of custom items to treat as heavy weapons")
        public String[] heavyWeaponsCustom = new String[] {};
    }

    public static class Grapple {
        @Config.Name("Enable Grapple Stamina Cost")
        @Config.Comment("Does active grappling consume stamina?")
        public boolean enableGrappleCost = true;

        @Config.Name("Grapple Climb Tick Interval")
        @Config.Comment("Ticks between feather consumption while reeling in / climbing the rope (20 ticks = 1 second)")
        @Config.RangeInt(min = 1)
        public int grappleClimbInterval = 20;

        @Config.Name("Grapple Climb Cost")
        @Config.Comment("Stamina cost (in half-feathers) per climb interval")
        @Config.RangeInt(min = 0)
        public int grappleClimbCost = 3;

        @Config.Name("Grapple Swing Tick Interval")
        @Config.Comment("Ticks between feather consumption while swinging (20 ticks = 1 second)")
        @Config.RangeInt(min = 1)
        public int grappleSwingInterval = 20;

        @Config.Name("Grapple Swing Cost")
        @Config.Comment("Stamina cost (in half-feathers) per swing interval")
        @Config.RangeInt(min = 0)
        public int grappleSwingCost = 2;

        @Config.Name("Grapple Hold Tick Interval")
        @Config.Comment("Ticks between feather consumption while hanging still (20 ticks = 1 second)")
        @Config.RangeInt(min = 1)
        public int grappleHoldInterval = 20;

        @Config.Name("Grapple Hold Cost")
        @Config.Comment("Stamina cost (in half-feathers) per hold interval. Lower than climb/swing; 1 is the minimum non-zero cost.")
        @Config.RangeInt(min = 0)
        public int grappleHoldCost = 1;

        @Config.Name("Grapple Swing Speed Threshold")
        @Config.Comment("3D speed at or above which hanging becomes swinging. High enough to ignore small hook bob; pendulum arcs still keep swing cost through the apex.")
        @Config.RangeDouble(min = 0.0)
        public double grappleSwingSpeedThreshold = 0.35;

        @Config.Name("Motor Uses Hang Cost")
        @Config.Comment("If true, an active motor pull drains hang stamina instead of climb/swing")
        public boolean motorUsesHangCost = true;

        @Config.Name("Motor Requires Ember")
        @Config.Comment("If true, motor pull also consumes portable Ember (jar / cartridge / bulb). Ignored if Embers is not loaded.")
        public boolean motorRequiresEmber = true;

        @Config.Name("Motor Ember Tick Interval")
        @Config.Comment("Ticks between Ember consumption while the motor is pulling (20 ticks = 1 second)")
        @Config.RangeInt(min = 1)
        public int motorEmberInterval = 20;

        @Config.Name("Motor Ember Cost")
        @Config.Comment("Ember consumed per motor interval (40 = one standard Ember pulse)")
        @Config.RangeDouble(min = 0.0)
        public double motorEmberCost = 40.0;
    }

    public static class Glider {
        @Config.Name("Enable Glider Stamina Cost")
        @Config.Comment("Does gliding with a hang glider consume stamina?")
        public boolean enableGliderCost = true;

        @Config.Name("Glider Glide Tick Interval")
        @Config.Comment("Ticks between feather consumption while gliding (20 ticks = 1 second)")
        @Config.RangeInt(min = 1)
        public int gliderInterval = 20;

        @Config.Name("Glider Glide Cost")
        @Config.Comment("Stamina cost (in half-feathers) consumed per glide interval")
        @Config.RangeInt(min = 0)
        public int gliderCost = 1;
    }

    public static class Shield {
        @Config.Name("Enable Shield Stamina Cost")
        @Config.Comment("Does raising and holding a shield consume stamina?")
        public boolean enableShieldCost = true;

        @Config.Name("Shield Hold Tick Interval")
        @Config.Comment("Ticks between feather consumption while holding a shield raised (20 ticks = 1 second)")
        @Config.RangeInt(min = 1)
        public int shieldHoldInterval = 20;

        @Config.Name("Shield Hold Cost")
        @Config.Comment("Stamina cost (in half-feathers) consumed per hold interval")
        @Config.RangeInt(min = 0)
        public int shieldHoldCost = 1;
    }

    public static class Mining {
        @Config.Name("Enable Mining Stamina Cost")
        @Config.Comment("Does breaking blocks consume stamina?")
        public boolean enableMiningCost = true;

        @Config.Name("Ore/Obsidian Break Cost")
        @Config.Comment("Stamina cost (in half-feathers) for breaking ores and obsidian")
        @Config.RangeInt(min = 0)
        public int oreCost = 2;

        @Config.Name("Default Block Break Cost")
        @Config.Comment("Stamina cost (in half-feathers) for breaking any other block")
        @Config.RangeInt(min = 0)
        public int defaultCost = 1;

        @Config.Name("Mining Fatigue Feather Threshold")
        @Config.Comment("Number of regular feathers (in half-feathers, e.g. 4 = 2 full feathers) at or below which Mining Fatigue III is applied")
        @Config.RangeInt(min = 0)
        public int miningFatigueThreshold = 4;
    }

    public static class Reskillable {
        @Config.Name("Enable Reskillable Perks")
        @Config.Comment("Should we enable integration with custom Reskillable perks?")
        public boolean enableReskillable = true;

        @Config.Name("Armor Mastery Perk ID")
        @Config.Comment("Registry ID of the Armor Mastery perk (defaults to aqtweaks:armor_mastery)")
        public String armorMasteryPerkId = "aqtweaks:armor_mastery";

        @Config.Name("Armor Mastery Reduction")
        @Config.Comment("Stamina weight cost reduction (in half-feathers) per piece of armor worn")
        @Config.RangeDouble(min = 0.0, max = 10.0)
        public double armorMasteryReductionPerPiece = 1.0;

        @Config.Name("Mining Efficiency Perk ID")
        @Config.Comment("Registry ID of the Mining Efficiency perk (defaults to aqtweaks:mining_efficiency)")
        public String miningEfficiencyPerkId = "aqtweaks:mining_efficiency";

        @Config.Name("Mining Efficiency Reduction")
        @Config.Comment("Stamina cost reduction (in half-feathers) applied to mining actions")
        @Config.RangeInt(min = 0, max = 20)
        public int miningEfficiencyReduction = 1;

        @Config.Name("Gathering Efficiency Perk ID")
        public String gatheringEfficiencyPerkId = "aqtweaks:gathering_efficiency";

        @Config.Name("Gathering Efficiency Reduction")
        @Config.Comment("Subtracted from forage BreakEvent stamina only (logs, leaves, gravel, flowers, mushrooms, tall grass). Not ore, not shear, not fish.")
        @Config.RangeInt(min = 0, max = 20)
        public int gatheringEfficiencyReduction = 1;

        @Config.Name("Melee Efficiency Perk ID")
        public String meleeEfficiencyPerkId = "aqtweaks:melee_efficiency";

        @Config.Name("Melee Efficiency Reduction")
        @Config.Comment("Subtracted from light/medium/heavy melee costs (half-feathers). Floor 0.")
        @Config.RangeInt(min = 0, max = 20)
        public int meleeEfficiencyReduction = 1;

        @Config.Name("Ranged Efficiency Perk ID")
        public String rangedEfficiencyPerkId = "aqtweaks:ranged_efficiency";

        @Config.Name("Ranged Draw Reduction")
        @Config.Comment("Subtracted from bow draw cost (half-feathers). Floor 0.")
        @Config.RangeInt(min = 0, max = 20)
        public int rangedDrawReduction = 1;

        @Config.Name("Ranged Hold Interval Multiplier")
        @Config.Comment("Multiplies bow hold interval (and throw hold, which uses bow interval). 1.5 → 20 becomes 30.")
        @Config.RangeDouble(min = 1.0, max = 8.0)
        public double rangedHoldIntervalMultiplier = 1.5;

        @Config.Name("Shield Efficiency Perk ID")
        public String shieldEfficiencyPerkId = "aqtweaks:shield_efficiency";

        @Config.Name("Shield Hold Interval Multiplier")
        @Config.Comment("Multiplies shield hold interval. 2.0 → 20 becomes 40.")
        @Config.RangeDouble(min = 1.0, max = 8.0)
        public double shieldHoldIntervalMultiplier = 2.0;

        @Config.Name("Adrenaline Perk ID")
        public String adrenalinePerkId = "aqtweaks:adrenaline";

        @Config.Name("Adrenaline Threshold")
        @Config.Comment("Proc when regular feathers (half-feathers) are below this.")
        @Config.RangeInt(min = 0, max = 40)
        public int adrenalineThreshold = 3;

        @Config.Name("Adrenaline Restore")
        @Config.Comment("Set regular feathers to this (half-feathers), capped at max.")
        @Config.RangeInt(min = 0, max = 40)
        public int adrenalineRestore = 20;

        @Config.Name("Adrenaline Cooldown Ticks")
        @Config.Comment("Ticks after a proc before it can fire again. 400 = 20 seconds.")
        @Config.RangeInt(min = 0, max = 12000)
        public int adrenalineCooldownTicks = 400;

        @Config.Name("Expert Climber Perk ID")
        public String expertClimberPerkId = "aqtweaks:expert_climber";

        @Config.Name("Expert Climber Reduction")
        @Config.Comment("Subtracted from ladder/vine/rope/cling, ledge, and grapple climb/swing/hang (half-feathers).")
        @Config.RangeInt(min = 0, max = 20)
        public int expertClimberReduction = 1;

        @Config.Name("Cardio Master Perk ID")
        public String cardioMasterPerkId = "aqtweaks:cardio_master";

        @Config.Name("Cardio Master Reduction")
        @Config.Comment("Subtracted from jump and sprint costs (half-feathers). Floor 0.")
        @Config.RangeInt(min = 0, max = 20)
        public int cardioMasterReduction = 1;

        @Config.Name("Evasion Perk ID")
        public String evasionPerkId = "aqtweaks:evasion";

        @Config.Name("Evasion Cooldown Ticks")
        @Config.Comment("Ticks after a successful evade. 600 = 30 seconds. Feather cost is Elenai dodge cost.")
        @Config.RangeInt(min = 0, max = 12000)
        public int evasionCooldownTicks = 600;

        @Config.Name("Power Attack Perk ID")
        public String powerAttackPerkId = "aqtweaks:power_attack";

        @Config.Name("Power Attack Extra Spend")
        @Config.Comment("Extra half-feathers on a full-bar medium/heavy hit. Not reduced by Melee Efficiency.")
        @Config.RangeInt(min = 0, max = 40)
        public int powerAttackExtraSpend = 2;

        @Config.Name("Power Attack Medium Multiplier")
        @Config.RangeDouble(min = 1.0, max = 8.0)
        public double powerAttackMediumMultiplier = 1.5;

        @Config.Name("Power Attack Heavy Multiplier")
        @Config.RangeDouble(min = 1.0, max = 8.0)
        public double powerAttackHeavyMultiplier = 2.0;
    }

    public static class SimpleDifficulty {
        @Config.Name("Enable Thirst Cost")
        @Config.Comment("Should restoring feathers consume Simple Difficulty hydration?")
        public boolean enableThirstCost = true;

        @Config.Name("Thirst Exhaustion Per Feather")
        @Config.Comment("Thirst exhaustion added per half-feather regenerated (4.0 exhaustion consumes 1 point of thirst saturation/level)")
        @Config.RangeDouble(min = 0.0, max = 4.0)
        public double thirstExhaustionPerFeather = 0.25;
    }

    public static class LedgeClimb {
        @Config.Name("Enable Ledge Climbing")
        @Config.Comment("Should players be able to grab and climb up ledges?")
        public boolean enableLedgeClimb = true;

        @Config.Name("Ledge Climb Cost")
        @Config.Comment("Half-feathers spent when the grab starts (short mantle). Expert Climber applies.")
        @Config.RangeInt(min = 0)
        public int ledgeClimbCost = 2;

        @Config.Name("Ledge Climb Extra Cost")
        @Config.Comment("Half-feathers added each extra interval after the short-mantle window")
        @Config.RangeInt(min = 0)
        public int ledgeClimbExtraCost = 1;

        @Config.Name("Ledge Climb Extra After Ticks")
        @Config.Comment("Mantle ticks before the first extra spend. Short catch is about 7 ticks at current speed.")
        @Config.RangeInt(min = 1)
        public int ledgeClimbExtraAfterTicks = 7;

        @Config.Name("Ledge Climb Extra Interval")
        @Config.Comment("Ticks between extra spends after the first extra window. 8 × 3 extras → max climb ≈ 5 with grab 2.")
        @Config.RangeInt(min = 1)
        public int ledgeClimbExtraInterval = 8;

        @Config.Name("Ledge Climb Max Extra Spends")
        @Config.Comment("Cap on extra spends after the grab (3 → total 5 when grab is 2 and extra is 1).")
        @Config.RangeInt(min = 0, max = 20)
        public int ledgeClimbMaxExtraSpends = 3;

        @Config.Name("Ledge Climb Land Pause Ticks")
        @Config.Comment("After a successful mantle, suppress jump and WASD so held W+space does not hop or run off. 0 disables.")
        @Config.RangeInt(min = 0, max = 40)
        public int ledgeClimbLandPauseTicks = 8;
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_client", category = "")
    public static class ClientModuleConfig {
        @Config.Name("HUD")
        @Config.Comment("Client overlay placement. Does not change Elenai feather or thirst icons.")
        public static final Hud hud = new Hud();

        @Config.Name("Tooltips")
        @Config.Comment("Client item tooltip lines.")
        public static final Tooltips tooltips = new Tooltips();
    }

    public static class Hud {
        @Config.Name("Move Toughness Bar To Armor Side")
        @Config.Comment("If Toughness Bar is loaded, draw it above armor (left), left to right. Feathers and thirst stay above hunger (right).")
        public boolean moveToughnessBarToArmorSide = true;
    }

    public static class Tooltips {
        @Config.Name("Metallurgy Tooltip Compat")
        @Config.Comment("Add Metallurgy-style harvest / durability / efficiency lines to non-Metallurgy pickaxes, axes, shovels, and tools.")
        public boolean metallurgyTooltipCompat = true;
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_somnia")
    public static class SomniaModuleConfig {
        @Config.Name("Enable Somnia Module")
        @Config.Comment("Master toggle for AQTweaks Somnia enhancements. When enabled, AQTweaks controls sleep threshold, time multipliers, and fatigue rates, overriding Somnia's corresponding config keys.")
        public static boolean enableSomniaModule = true;

        @Config.Name("Sleep Percentage Threshold")
        @Config.Comment("Percentage of non-spectator players required to activate time fast-forward (0.50 = 50%). Below this is Case C.")
        @Config.RangeDouble(min = 0.01, max = 1.0)
        public static double sleepPercentage = 0.50;

        @Config.Name("Case B Partial Sleep Time Multiplier")
        @Config.Comment("Total world-time and sleeper-fatigue rate vs one vanilla world tick during Case B (>=50% and <100%). Default 2.0 = double (vanilla +1 already ran; Tweaks adds multiplier-1). Mobs and awake players remain at 1x tick rate.")
        @Config.RangeDouble(min = 1.0, max = 100.0)
        public static double caseBTimeMultiplier = 2.0;

        @Config.Name("Fatigue Recovered Per In-Game Hour")
        @Config.Comment("Points of Somnia fatigue recovered per in-game hour spent sleeping in bed (1 in-game hour = 1000 ticks). Default: 10.0.")
        @Config.RangeDouble(min = 0.0, max = 100.0)
        public static double fatigueRecoveredPerHour = 10.0;

        @Config.Name("Enable Sleep Notifications")
        @Config.Comment("Broadcast Morpheus-style chat notifications when players enter/leave bed and when fast-forward sleep begins.")
        public static boolean enableSleepNotifications = true;
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_grimoireofgaia")
    public static class GrimoireOfGaiaConfig {
        @Config.Name("Disable Piercing Damage")
        @Config.Comment("When true, drop Gaia melee/archer extra MAGIC pierce, retype bolts/bombs, and apply per-mob attack JSON. When false, Gaia vanilla.")
        public static boolean disablePiercingDamage = true;

        @Config.Name("Enable Deep Dwarf")
        @Config.Comment("Register aqtweaks:deep_dwarf, a hostile Gaia dwarf clone. Requires Grimoire of Gaia.")
        public static boolean enableDeepDwarf = true;

        @Config.Name("Deep Dwarf Attack Damage")
        @Config.Comment("ATTACK_DAMAGE base for aqtweaks:deep_dwarf. Weapons and Strength still stack. Wins over gaia_mob_damage.json for this id.")
        @Config.RangeDouble(min = 0.0, max = 40.0)
        public static double deepDwarfAttackDamage = 10.0;
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_thaumcraft")
    public static class ThaumcraftConfig {
        @Config.Name("Enable Sleep Warp Cleansing")
        @Config.Comment("Should sleeping in a bed clear a small amount of Thaumcraft warp?")
        public static boolean enableWarpCleansing = true;

        @Config.Name("Clear Normal Warp")
        @Config.Comment("Should normal (sticky) warp be reduced on successful sleep?")
        public static boolean clearNormalWarp = true;

        @Config.Name("Normal Warp Reduction")
        @Config.Comment("Amount of normal (sticky) warp to clear per successful sleep")
        @Config.RangeInt(min = 0)
        public static int normalWarpReduction = 1;

        @Config.Name("Clear Temporary Warp")
        @Config.Comment("Should temporary warp be reduced on successful sleep? Default false: Somnia already ticks TC decay and Comfort Homestead.")
        public static boolean clearTempWarp = false;

        @Config.Name("Temporary Warp Reduction")
        @Config.Comment("Amount of temporary warp to clear per successful sleep")
        @Config.RangeInt(min = 0)
        public static int tempWarpReduction = 2;

        @Config.Name("Enable Sleep Chat Message")
        @Config.Comment("Should players receive a chat message informing them that their mind feels clearer upon waking up?")
        public static boolean enableChatMessage = true;

        @Config.Name("Sleep Chat Message Text")
        @Config.Comment("The text of the message sent to players when their warp is reduced by sleeping")
        public static String chatMessageText = "§5You wake up feeling refreshed, and the whispers in your mind grow quieter...§r";

        @Config.Name("Enable Dimension Entry Warp")
        @Config.Comment("Should players gain warp when entering a new dimension for the first time?")
        public static boolean enableDimensionWarp = true;

        @Config.Name("Dimension Entry Normal Warp")
        @Config.Comment("Amount of normal (sticky) warp gained when entering a new dimension for the first time")
        @Config.RangeInt(min = 0)
        public static int dimensionNormalWarp = 5;

        @Config.Name("Dimension Entry Temporary Warp")
        @Config.Comment("Amount of temporary warp gained when entering a new dimension for the first time")
        @Config.RangeInt(min = 0)
        public static int dimensionTempWarp = 5;

        @Config.Name("Dimension Chat Message Text")
        @Config.Comment("The text of the message sent to players when they gain warp from entering a new dimension")
        public static String dimensionChatMessageText = "§5Entering this strange dimension fills your mind with ancient whispers...§r";

        @Config.Name("Dimension Entry Sound")
        @Config.Comment("The sound registry name to play when the player gains warp from entering a new dimension. Leave empty to play no sound.")
        public static String dimensionEntrySound = "thaumcraft:whispers";

        @Config.Name("Dimension Entry Sound Volume")
        @Config.Comment("The volume of the sound played when entering a new dimension. Higher values increase sound reach and loudness.")
        @Config.RangeDouble(min = 0.0, max = 10.0)
        public static float dimensionEntrySoundVolume = 2.0F;

        @Config.Name("Enable Warp Exposure")
        @Config.Comment("Should players slowly accumulate temporary warp over time when exposed to certain environments?")
        public static boolean enableExposureWarp = true;

        @Config.Name("Exposure Tick Seconds")
        @Config.Comment("How often (real seconds) the exposure pass runs. Progress and grants only happen on this interval.")
        @Config.RangeInt(min = 1, max = 3600)
        public static int exposureTickSeconds = 30;

        @Config.Name("Exposure Grant Seconds")
        @Config.Comment("Seconds banked on the winning source before granting that source's temporary warp.")
        @Config.RangeInt(min = 1, max = 36000)
        public static int exposureGrantSeconds = 300;

        @Config.Name("Exposure Dimension Grants")
        @Config.Comment({
            "Dimensions that grant temporary warp from lingering, with grant amount G (not seconds).",
            "Format: dimension_id=G",
            "Every Exposure Grant Seconds, the matching source with the highest G is banked."
        })
        public static String[] exposureDimensionGrants = new String[] {
            "-1=7",
            "1=9",
            "4=6",
            "7=5",
            "20=8",
            "17=6",
            "10=7",
            "14676=10",
            "424=6"
        };

        @Config.Name("Enable Deep Underground Exposure")
        @Config.Comment("Should players slowly accumulate temporary warp while underground (upper and deep Y bands)?")
        public static boolean enableUndergroundExposure = true;

        @Config.Name("Underground Y Max")
        @Config.Comment("Inclusive max Y for the upper underground band.")
        @Config.RangeInt(min = -256, max = 256)
        public static int exposureUndergroundYMax = 30;

        @Config.Name("Underground Y Min")
        @Config.Comment("Inclusive min Y for the upper underground band. Y below this is the deep band.")
        @Config.RangeInt(min = -256, max = 256)
        public static int exposureUndergroundYMin = -20;

        @Config.Name("Underground Exposure Warp")
        @Config.Comment("Temporary warp G for the upper underground band (Y Min through Y Max).")
        @Config.RangeInt(min = 0)
        public static int exposureUndergroundWarp = 5;

        @Config.Name("Deep Underground Exposure Warp")
        @Config.Comment("Temporary warp G for Y strictly below Underground Y Min.")
        @Config.RangeInt(min = 0)
        public static int exposureDeepUndergroundWarp = 6;

        @Config.Name("Enable Dungeon Exposure")
        @Config.Comment("Should players slowly accumulate temporary warp while inside a Roguelike Dungeon?")
        public static boolean enableDungeonExposure = true;

        @Config.Name("Dungeon Exposure Warp")
        @Config.Comment("Temporary warp G while inside a Roguelike Dungeon.")
        @Config.RangeInt(min = 0)
        public static int exposureDungeonWarp = 4;

        @Config.Name("Enable Exposure Sound")
        @Config.Comment("Should a sound effect play when temporary warp is gained from environmental exposure?")
        public static boolean enableExposureSound = true;

        @Config.Name("Exposure Sound Effect")
        @Config.Comment("The sound registry name to play when temporary warp is gained from environmental exposure. Default: thaumcraft:whispers")
        public static String exposureSoundEffect = "thaumcraft:whispers";

        @Config.Name("Exposure Sound Volume")
        @Config.Comment("The volume of the sound played when temporary warp is gained from environmental exposure. Higher values increase sound reach and loudness.")
        @Config.RangeDouble(min = 0.0, max = 10.0)
        public static float exposureSoundVolume = 2.0F;
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_bewitchment")
    public static class BewitchmentConfig {
        @Config.Name("Ritual Warp List")
        @Config.Comment({
            "List of Bewitchment ritual registry names that should grant Thaumcraft warp upon completion.",
            "Format: registry_name=normal,temporary,permanent (permanent is optional and defaults to 0)"
        })
        public static String[] ritualWarpList = new String[] {
            "bewitchment:conjure_imp=1,3",
            "bewitchment:conjure_demon=2,5",
            "bewitchment:conjure_baphomet=5,15,2",
            "bewitchment:conjure_leonard=5,15,2",
            "bewitchment:lesser_hellmouth=2,5",
            "bewitchment:hellmouth=3,8",
            "bewitchment:greater_hellmouth=4,10",
            "bewitchment:sowing_salt=2,4",
            "bewitchment:drought=2,4",
            "bewitchment:hungry_flames=2,4",
            "bewitchment:conjure_wither=3,6"
        };
    }

    /**
     * The one supported Depths floor. {@code MixinCaveNoiseGenerator}'s vertical bands are hand
     * calibrated to it, so this is a constant rather than a knob.
     */
    public static final int DEPTHS_FLOOR_Y = -64;

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_depths", category = "")
    public static class DepthsModuleConfig {

        @Config.Name("General")
        @Config.Comment("Core Depths Update compatibility")
        public static final General general = new General();

        @Config.Name("Client")
        @Config.Comment("Client-only visuals for negative Y")
        public static final Client client = new Client();

        @Config.Name("Compatibility")
        @Config.Comment("Per-mod negative-Y hooks")
        public static final Compatibility compatibility = new Compatibility();

        public static class General {
            @Config.Name("Enable Depths Module")
            @Config.Comment("Should negative Y-level compatibility enhancements be enabled for Depths Update?")
            public boolean enableDepthsModule = true;

            @Config.Name("Minimum World Y Elevation")
            @Config.Comment({
                "The minimum Y coordinate boundary of the world. PINNED to -64 and not tunable.",
                "The Depths cave bands (lower cavern, lava level, decor) are calibrated to a -64",
                "floor, so any other value produces a mismatched or out-of-range carve. Edits to",
                "this key are clamped back to -64 on load."
            })
            @Config.RangeInt(min = DEPTHS_FLOOR_Y, max = DEPTHS_FLOOR_Y)
            public int minWorldY = DEPTHS_FLOOR_Y;

            @Config.Name("Better Depths Caves")
            @Config.Comment("Enable AQTweaks Depths cave generation (BC-style upper tunnels, chambers, lower deep, sparse shafts, Y0 mouths into +Y Better Caves). Affects new chunks only. When false, AQTweaks skips that carve path.")
            public boolean enableBetterDepthsCaves = true;

            @Config.Name("Enable Quark Speleothems")
            @Config.Comment("Place Quark speleothem clusters in the lower cavern primer (new chunks only). Skipped if Quark is not loaded. Does not replace Deepslate columns, spikes, or bridges.")
            public boolean enableQuarkSpeleothems = true;
        }

        public static class Client {
            @Config.Name("Deep Cave Fog")
            @Config.Comment("When below Y = 0 in the Overworld, apply dark gray fog starting about 32 blocks from the camera. Client-only. Skipped in water/lava.")
            public boolean deepCaveFog = true;

            @Config.Name("Hide Skybox Below Y 0")
            @Config.Comment("When below Y = 0 in the Overworld, skip rendering the skybox (sun, moon, stars, sky dome). Client-only.")
            public boolean hideSkyBelowZero = true;
        }

        public static class Compatibility {
            @Config.Name("Enable CoFH World Negative Y")
            @Config.Comment("Should CoFH World features evaluate and place blocks below Y = 0 down to Minimum World Y?")
            public boolean enableCoFHNegativeY = true;

            @Config.Name("Enable Better Caves Negative Y")
            @Config.Comment("Should YUNG's Better Caves -Y compatibility hooks run (e.g. surface altitude utils)? Separate from Better Depths Caves generation.")
            public boolean enableBetterCavesNegativeY = true;

            @Config.Name("Adjust Better Caves Bedrock Height")
            @Config.Comment({
                "Should YUNG's Better Caves bedrock flattening pass be cancelled? Better Caves",
                "flattens bedrock at Y 0-4, which would seal the Y0 breach mouths into the Depths.",
                "Nothing is shifted down — the Depths bedrock floor is laid at Minimum World Y by",
                "the Depths carve instead."
            })
            public boolean adjustBetterCavesBedrock = true;

            @Config.Name("Enable Recurrent Complex Negative Y")
            @Config.Comment("Should Recurrent Complex Volts placement rays scan below Y = 0 down to Minimum World Y?")
            public boolean enableRecurrentComplexNegativeY = true;
        }
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_rtg", category = "")
    public static class RtgModuleConfig {
        @Config.Name("Surface")
        @Config.Comment("RTG surface height and structure placement tweaks")
        public static final Surface surface = new Surface();
    }

    public static class Surface {
        @Config.Name("Enable RTG Village Terrain Smoothing")
        @Config.Comment("Flatten RTG land under village components (well, roads, houses) with a 12-block hard pad around each. Overlapping pads are the village plate. Never fill ocean or river biomes.")
        public boolean enableVillageSmoothing = true;

        @Config.Name("Village Component Pad")
        @Config.Comment("Full-plate radius around each land component (well, roads, houses, RC). Overlapping pads form the village footprint. Default 12.")
        @Config.RangeInt(min = 0, max = 64)
        public int villageComponentPad = 12;

        @Config.Name("Village Edge Falloff")
        @Config.Comment("Blocks of Hermite slope beyond the component pad, back to raw RTG. 0 = hard pad edge. Existing cfg with 48 stays 48 until changed.")
        @Config.RangeInt(min = 0, max = 256)
        public int villageEdgeFalloff = 12;

        @Config.Name("Village Water Bank")
        @Config.Comment("Blocks of slope from the plate down to water or shore. 0 = old vertical cutoff at the waterline.")
        @Config.RangeInt(min = 0, max = 64)
        public int villageWaterBank = 16;

        @Config.Name("Village Ocean Wall")
        @Config.Comment("Replace the outside-facing plate rim (8-connected to non-plate / ocean) with stone brick below the plate top. Top stays sand/grass. Inland cliffs are unchanged.")
        public boolean villageOceanWall = true;

        @Config.Name("Village Shore Smooth")
        @Config.Comment("Open 1-block sand jetties off the coastal plate and close 1-block notches. Houses and roads stay plated.")
        public boolean villageShoreSmooth = true;

        @Config.Name("Village Shore Smooth Radius")
        @Config.Comment("Chebyshev kernel for shore opening. 1 = default. 0 = no opening (hole fill still uses Village Shore Close Ocean).")
        @Config.RangeInt(min = 0, max = 2)
        public int villageShoreSmoothRadius = 1;

        @Config.Name("Village Shore Close Ocean")
        @Config.Comment("Fill 1-block ocean/river notches inside the hard pad (cardinal-enclosed or 7 of 8 land neighbors). Not a pier into open water.")
        public boolean villageShoreCloseOcean = true;

        @Config.Name("Village Plate Slope")
        @Config.Comment("Inside the village box: 0 = fully flat plate at the box-average height. 30 = allow at most 1 block of height change per 30 blocks from the plate center.")
        @Config.RangeInt(min = 0, max = 256)
        public int villagePlateSlopeBlocks = 0;

        @Config.Name("Skip Water Village Pieces")
        @Config.Comment("If a village house, Recurrent Complex building, waystone, shrine, or path would touch river, ocean, or flooded water, skip it and retry inland. Paths that still touch ocean/river or are mostly lake after retry are omitted. Swamp and other non-ocean water under the pad is filled up to plate Y.")
        public boolean skipWaterVillagePieces = true;

        @Config.Name("Village Water Retry Distance")
        @Config.Comment("How far (blocks) to walk inland when retrying a water village piece (street, then toward the well). 0 = skip only, no retry.")
        @Config.RangeInt(min = 0, max = 48)
        public int villageWaterRetryDistance = 20;

        @Config.Name("Reject Coastal Village Starts")
        @Config.Comment("Veto a village only if the well is ocean/river (or RTG river) and there is no dry land within Village Water Retry Distance. Land wells too close to ocean still fail Village Coast Buffer. Dry land below Village Min Well Height is kept and raised. Nearby river does not cancel a dry well.")
        public boolean rejectCoastalVillageStarts = true;

        @Config.Name("Village Min Well Height")
        @Config.Comment("Dry village wells and lake/flood plate floors use at least this Y. River/ocean columns are never filled. Vanilla sea level is 63.")
        @Config.RangeInt(min = 1, max = 255)
        public int villageMinWellHeight = 64;

        @Config.Name("Village Coast Buffer")
        @Config.Comment("Chebyshev blocks around a dry land well. Veto if ocean-like is closer than this. Nearby river does not cancel. Distance at or beyond this is allowed. 0 = well column only.")
        @Config.RangeInt(min = 0, max = 128)
        public int villageCoastBuffer = 16;

        @Config.Name("Enable Village Bounding Box Detection")
        @Config.Comment("Treat the flatten plate (Village Component Pad plus Village Edge Falloff around land boxes, including kept roads and yards) as Village for isInsideStructure / InControl. Y is well-shaft floor through plate plus Village Box Height. New villages also save that volume as extra Village.dat pieces.")
        public boolean enableVillageBoxDetection = true;

        @Config.Name("Village Box XZ Pad")
        @Config.Comment("Swamp dock-approach slope radius for flatten only. Detection uses Village Component Pad, not this value.")
        @Config.RangeInt(min = 0, max = 64)
        public int villageBoxXZPad = 8;

        @Config.Name("Village Box Height")
        @Config.Comment("Blocks above the pad surface that still count as Village. Floor is the well shaft (about 11-14 below the plate). Existing cfg with 32 stays 32 until changed.")
        @Config.RangeInt(min = 0, max = 256)
        public int villageBoxHeight = 30;

        @Config.Name("Enable Village Relight")
        @Config.Comment("After village pieces paste in a chunk, re-check block light at torches and other sources so lamps actually light the plate. Does not change blocks.")
        public boolean enableVillageRelight = true;

        @Config.Name("Village Flatten Debug")
        @Config.Comment("Write village terrain traces to logs/villagepatch.log (not latest.log). Off by default: appends stall chunk gen. Turn on only while diagnosing villages.")
        public boolean villageFlattenDebug = false;

        @Config.Name("Skip Structures On Village")
        @Config.Comment("Cancel Astral shrines and Bewitchment Cambion houses on village overlap. Mystical World huts/barrows and Bewitchment stone circles/menhir/wickerman skip that spot and retry nearby. Vanilla water lakes and BOP water/quicksand lakes that overlap a village pad are skipped. Lava lakes are not skipped.")
        public boolean skipStructuresOnVillage = true;

        @Config.Name("Enable Structure Land Settle")
        @Config.Comment("Fill under and ramp around those structures after they place. Does not rewrite the structure blocks.")
        public boolean enableStructureLandSettle = true;

        @Config.Name("Enable Astral Shrine Settle")
        @Config.Comment("Apply village-skip and land settle to Astral Sorcery surface shrines.")
        public boolean enableAstralShrineSettle = true;

        @Config.Name("Enable Cambion House Settle")
        @Config.Comment("Skip Bewitchment Cambion houses that overlap a village. House pastes one above ground (cobble on the grass). The 6-pad stays at plains Y and only fills air holes.")
        public boolean enableCambionHouseSettle = true;

        @Config.Name("Enable Astral Small Shrine Village Piece")
        @Config.Comment("Allow Astral small shrines as a village building, at most once per village.")
        public boolean enableAstralSmallShrineVillagePiece = true;

        @Config.Name("Enable Mystical Hut Settle")
        @Config.Comment("Apply village-skip, nearby retry, and land settle to Mystical World thatch huts. Barrows skip/retry on village overlap but are not plated.")
        public boolean enableMysticalHutSettle = true;

        @Config.Name("Structure Fill Depth")
        @Config.Comment("Max blocks to fill downward under a structure pad. 0 = no fill.")
        @Config.RangeInt(min = 0, max = 64)
        public int structureFillDepth = 16;

        @Config.Name("Structure Rim Bank")
        @Config.Comment("Blocks of slope from large shrine and hut pads down to surrounding terrain. Small Astral shrines/ruins use Small Shrine Pad instead. 0 = no rim.")
        @Config.RangeInt(min = 0, max = 64)
        public int structureRimBank = 16;

        @Config.Name("Small Shrine Pad")
        @Config.Comment("Max blocks of land buffer around small Astral shrines/ruins (wild settle rim and village shrine flatten). 0 = footprint only.")
        @Config.RangeInt(min = 0, max = 16)
        public int smallShrinePad = 3;

        @Config.Name("Cambion House Pad")
        @Config.Comment("Radius around a Cambion house AABB used to fill air holes up to ground Y. Default 6. Does not raise the pad above plains.")
        @Config.RangeInt(min = 0, max = 32)
        public int cambionHousePad = 6;

        @Config.Name("Cambion House Falloff")
        @Config.Comment("Hermite slope beyond Cambion House Pad, raise-only, back to RTG. Default 12.")
        @Config.RangeInt(min = 0, max = 64)
        public int cambionHouseFalloff = 12;
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_portal", category = "")
    public static class PortalModuleConfig {
        @Config.Name("General")
        @Config.Comment("Spatial rift items and entity.")
        public static final PortalGeneral general = new PortalGeneral();
    }

    public static class PortalGeneral {
        @Config.Name("Enable Portal Module")
        @Config.Comment("If false, rift items do not attune or open. Registry still loads.")
        public boolean enable = true;

        @Config.Name("Rift Lifespan Ticks")
        @Config.Comment("How long both rifts stay open. 1200 = 60 seconds.")
        @Config.RangeInt(min = 20, max = 24000)
        public int lifespanTicks = 1200;

        @Config.Name("Teleport Cooldown Ticks")
        @Config.Comment("Vanilla-style timeUntilPortal after open and after a trip so entities do not bounce. 60 = 3 seconds.")
        @Config.RangeInt(min = 1, max = 400)
        public int cooldownTicks = 60;

        @Config.Name("Spawn Offset")
        @Config.Comment("Blocks along look direction to place the source rift in front of the player.")
        @Config.RangeDouble(min = 0.5, max = 4.0)
        public double spawnOffset = 1.5;

        @Config.Name("Exit Offset")
        @Config.Comment("Unused for teleport (arrival is dest rift XYZ). Kept so existing cfg files still load.")
        @Config.RangeDouble(min = 0.5, max = 4.0)
        public double exitOffset = 1.5;

        @Config.Name("Companion Radius")
        @Config.Comment("Blocks around the player to pull standing owned pets and leashed mobs.")
        @Config.RangeDouble(min = 4.0, max = 32.0)
        public double companionRadius = 16.0;

        @Config.Name("Wild Min Distance")
        @Config.Comment("Minimum blocks from the player for an unstable tear landing.")
        @Config.RangeInt(min = 16, max = 8000)
        public int wildMinDistance = 4000;

        @Config.Name("Wild Max Distance")
        @Config.Comment("Maximum blocks from the player for an unstable tear landing.")
        @Config.RangeInt(min = 32, max = 16000)
        public int wildMaxDistance = 6000;

        @Config.Name("Wild Search Attempts")
        @Config.Comment("Random land columns to try before giving up.")
        @Config.RangeInt(min = 8, max = 256)
        public int wildSearchAttempts = 48;
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_twilightforest", category = "")
    public static class TwilightForestModuleConfig {
        @Config.Name("General")
        @Config.Comment("RandomPortals landings into Twilight Forest.")
        public static final TwilightForestGeneral general = new TwilightForestGeneral();
    }

    public static class TwilightForestGeneral {
        @Config.Name("Enable Twilight Forest Portal Safety")
        @Config.Comment("When true, RandomPortals destinations in Destination Dimension ID reroute out of locked biomes and landmarks, and new pads require grass.")
        public boolean enable = true;

        @Config.Name("Destination Dimension ID")
        @Config.Comment("Twilight Forest dimension id. Pack default is 7.")
        public int destinationDimensionId = 7;

        @Config.Name("Always Unsafe Biomes")
        @Config.Comment("Biome registry names that are never valid RandomPortals landings in the destination dimension, including for non-players. Empty entries ignored.")
        public String[] alwaysUnsafeBiomes = {
                "twilightforest:dark_forest_center",
                "twilightforest:fire_swamp",
                "twilightforest:twilight_glacier",
                "twilightforest:thornlands",
                "twilightforest:highlands_center"
        };

        @Config.Name("Safe Biome Allowlist")
        @Config.Comment("If any non-empty names are set, the landing biome must also be one of these registry names. Empty = unused.")
        public String[] safeBiomeAllowlist = {};
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_aether", category = "")
    public static class AetherModuleConfig {
        @Config.Name("General")
        @Config.Comment("RandomPortals landings into The Aether.")
        public static final AetherGeneral general = new AetherGeneral();
    }

    public static class AetherGeneral {
        @Config.Name("Enable Aether Portal Safety")
        @Config.Comment("When true, RandomPortals destinations in Destination Dimension ID snap onto island ground instead of generating in the void.")
        public boolean enable = true;

        @Config.Name("Destination Dimension ID")
        @Config.Comment("Aether dimension id. Pack default is 4.")
        public int destinationDimensionId = 4;
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_qualitytools", category = "")
    public static class QualityToolsModuleConfig {
        @Config.Name("General")
        @Config.Comment("Quality Tools Module: loot stamp, wear/break overlays, Dawnstone rune upgrades.")
        public static final QualityToolsGeneral general = new QualityToolsGeneral();
    }

    public static class QualityToolsGeneral {
        @Config.Name("Enable Quality Tools Module")
        @Config.Comment("Master switch. When false, QT living-update stamp is not skipped and Tweaks wear/break/runes/loot stamp are off.")
        public boolean enable = true;

        @Config.Name("Low Durability")
        @Config.Comment("Wear overlay may apply at or below this remaining/max ratio. Broken items never take wear.")
        @Config.RangeDouble(min = 0.01, max = 1.0)
        public double lowDurability = 0.20;

        @Config.Name("High Durability")
        @Config.Comment("Repair to this remaining/max ratio clears gray/dark_gray overlays and restores QualityBase.")
        @Config.RangeDouble(min = 0.01, max = 1.0)
        public double highDurability = 0.75;

        @Config.Name("Wear Chance")
        @Config.Comment("Multiplier on used × (ref / (max/2)). 0 disables wear. 1 is the default curve.")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double wearChance = 1.0;

        @Config.Name("Wear Chance Floor")
        @Config.Comment("Minimum p after the curve when Wear Chance is above 0.")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double wearChanceFloor = 0.05;

        @Config.Name("Wear Chance Ceiling")
        @Config.Comment("Maximum p after the curve when Wear Chance is above 0. Stops low-max tools from a near-certain first roll.")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double wearChanceCeiling = 0.50;

        @Config.Name("Wear Durability Ref")
        @Config.Comment("Reference max durability (vanilla iron tool = 250). Hardness term is ref / (max/2).")
        @Config.RangeInt(min = 1, max = 100000)
        public int wearDurabilityRef = 250;

        @Config.Name("Wear Check Interval Ticks")
        @Config.Comment("Minimum ticks between wear roll attempts on one stack.")
        @Config.RangeInt(min = 1, max = 1200)
        public int wearCheckIntervalTicks = 40;

        @Config.Name("Common Rune")
        @Config.Comment("sccraftingrunes Common Rune (itemcommonmat). Place the tool first, then this rune. red→white, white→yellow, yellow reroll. common_mat is accepted as an alias.")
        public String commonRune = "sccraftingrunes:itemcommonmat";

        @Config.Name("Uncommon Rune")
        @Config.Comment("yellow→green, then green reroll. uncommon_mat is accepted as an alias.")
        public String uncommonRune = "sccraftingrunes:itemuncommonmat";

        @Config.Name("Rare Rune")
        @Config.Comment("green→blue, then blue reroll. rare_mat is accepted as an alias.")
        public String rareRune = "sccraftingrunes:itemraremat";

        @Config.Name("Legendary Rune")
        @Config.Comment("blue→gold, then gold reroll. legendary_mat is accepted as an alias.")
        public String legendaryRune = "sccraftingrunes:itemlegendarymat";
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_statskeeper", category = "")
    public static class StatsKeeperModuleConfig {
        @Config.Name("General")
        @Config.Comment("Elixir of Vitality drink sound when Stats Keeper accepts the heart.")
        public static final StatsKeeperGeneral general = new StatsKeeperGeneral();
    }

    public static class StatsKeeperGeneral {
        @Config.Name("Elixir Drink Sound")
        @Config.Comment("Sound event id played when Elixir of Vitality is successfully drunk. Empty disables the sound. Unknown or invalid ids fall back to minecraft:entity.player.levelup.")
        public String elixirDrinkSound = "minecraft:entity.player.levelup";
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_gamestages", category = "")
    public static class GameStagesModuleConfig {
        @Config.Name("General")
        @Config.Comment("Gate Chisel crafts behind Recipe Stages / Game Stages.")
        public static final GameStagesGeneral general = new GameStagesGeneral();
    }

    public static class GameStagesGeneral {
        @Config.Name("Enable Game Stages Module")
        @Config.Comment("When false, Chisel GUI and in-world carving are not gated by Recipe Stages.")
        public boolean enable = true;

        @Config.Name("Chisel Stage Allowlist")
        @Config.Comment("Recipe Stages that can lock a Chisel output. Empty = every captured stage. Pack defaults are the Building skill stages.")
        public String[] chiselStageAllowlist = {
                "apprentice_builder",
                "experienced_builder",
                "master_builder"
        };
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_spawning", category = "")
    public static class SpawningModuleConfig {
        @Config.Name("General")
        @Config.Comment("Filter InControl/vanilla potential spawn lists by pack surface vs underground ids.")
        public static final SpawningGeneral general = new SpawningGeneral();
    }

    public static class SpawningGeneral {
        @Config.Name("Enable Spawning Module")
        @Config.Comment("Master switch. When false, PotentialSpawns is not filtered, pack fill and mixed groups are off, the hostile cap is vanilla 70, and cage fail delay is not applied.")
        public boolean enable = true;

        @Config.Name("Filter Potential Spawns")
        @Config.Comment("At the spawn pick coordinate, drop surface-only mobs in caves and underground-only mobs on the surface.")
        public boolean filterPotentialSpawns = true;

        @Config.Name("Spawn Type File")
        @Config.Comment("Pack JSON under the Forge config directory. Default is DEVBOX config/arcanaquest/mob_overworldspawntype.json. Tweaks does not ship or write this file.")
        public String spawnTypeFile = "arcanaquest/mob_overworldspawntype.json";

        @Config.Name("Spawn Structure File")
        @Config.Comment("Pack JSON under the Forge config directory. Structure name to entity ids for cave layer exemption. Tweaks does not ship or write this file.")
        public String spawnStructureFile = "arcanaquest/mob_structurespawns.json";

        @Config.Name("Cave Max Y")
        @Config.Comment("Cave pick when Y is strictly below this and sky light is at or below Max Cave Sky Light.")
        @Config.RangeInt(min = -64, max = 256)
        public int caveMaxY = 60;

        @Config.Name("Max Cave Sky Light")
        @Config.Comment("Closed cave if sky light (0-15) is <= this. 0 avoids treating forest canopy as cave.")
        @Config.RangeInt(min = 0, max = 15)
        public int maxCaveSkyLight = 0;

        @Config.Name("Overworld Only")
        @Config.Comment("Only dimension 0. Nether/End lists are unchanged.")
        public boolean overworldOnly = true;

        @Config.Name("Monster Only")
        @Config.Comment("Only EnumCreatureType.MONSTER. Animals, water, and ambient are unchanged.")
        public boolean monsterOnly = true;

        @Config.Name("Fill Pack Size")
        @Config.Comment("After the first ticking spawn, roll a pack size in [min, max] from pack mob_tier + mob_spawnrules unless overridden.")
        public boolean fillPackSize = true;

        @Config.Name("Max Extra Attempts")
        @Config.Comment("Placement tries (each with a Y scan) to finish a pack after the first mob.")
        @Config.RangeInt(min = 1, max = 64)
        public int maxExtraAttempts = 24;

        @Config.Name("Pack Radius")
        @Config.Comment("XZ radius around the first mob for extra members.")
        @Config.RangeInt(min = 1, max = 16)
        public int packRadius = 8;

        @Config.Name("Y Range")
        @Config.Comment("Vertical scan around the first mob so cave floors still fill when vanilla ΔY is 0.")
        @Config.RangeInt(min = 0, max = 16)
        public int yRange = 8;

        @Config.Name("Group Size Cap")
        @Config.Comment("Hard ceiling for rolled pack size and overrides.")
        @Config.RangeInt(min = 1, max = 16)
        public int groupSizeCap = 8;

        @Config.Name("Group Size Overrides")
        @Config.Comment("modid:path=min-max (or min,max). Overrides pack tier group counts for that id. Delete a line to use the tier table.")
        public String[] groupSizeOverrides = new String[] {
            "grimoireofgaia:goblin_feral=3-5"
        };

        @Config.Name("Spawn Tier File")
        @Config.Comment("Pack JSON under the Forge config directory. Maps entity ids to common/uncommon/rare/…. Tweaks does not ship or write this file.")
        public String spawnTierFile = "arcanaquest/mob_tier.json";

        @Config.Name("Spawn Rules File")
        @Config.Comment("Pack Forge cfg under the Forge config directory. *_group_min / *_group_max per tier. Tweaks does not ship or write this file.")
        public String spawnRulesFile = "arcanaquest/mob_spawnrules.cfg";

        @Config.Name("Enable Mixed Groups")
        @Config.Comment("After a natural ticking spawn, place companions from pack JSON. Cage spawners and TC portals never trigger this.")
        public boolean enableMixedGroups = true;

        @Config.Name("Spawn Parties File")
        @Config.Comment("Pack JSON under the Forge config directory. Tweaks does not ship or write this file. Missing file turns mixed groups off.")
        public String spawnPartiesFile = "arcanaquest/mob_spawnparties.json";

        @Config.Name("Hostile Mob Cap")
        @Config.Comment("EnumCreatureType.MONSTER max used by findChunksForSpawning. Vanilla is 70. Does not affect animals, water, ambient, cage spawners, or TC portals.")
        @Config.RangeInt(min = 1, max = 1000)
        public int hostileMobCap = 200;

        @Config.Name("Cage Fail Recheck Delay")
        @Config.Comment("Ticks to wait after a failed cage spawn attempt. Does not change successful spawn delay (vanilla 200-800). 1 is the minimum to avoid every-tick retry.")
        @Config.RangeInt(min = 1, max = 200)
        public int cageFailRecheckDelay = 20;
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_reskillable", category = "")
    public static class ReskillableModuleConfig {
        @Config.Name("General")
        @Config.Comment("Master switch for per-level Reskillable drip bonuses. Stamina perk ids stay in aqtweaks_stamina.cfg.")
        public static final ReskillableGeneral general = new ReskillableGeneral();

        @Config.Name("Attack")
        public static final ReskillableAttack attack = new ReskillableAttack();

        @Config.Name("Defense")
        public static final ReskillableDefense defense = new ReskillableDefense();

        @Config.Name("Agility")
        public static final ReskillableAgility agility = new ReskillableAgility();

        @Config.Name("Building")
        public static final ReskillableBuilding building = new ReskillableBuilding();

        @Config.Name("Mining")
        public static final ReskillableMining mining = new ReskillableMining();

        @Config.Name("Gathering")
        public static final ReskillableGathering gathering = new ReskillableGathering();

        @Config.Name("Farming")
        public static final ReskillableFarming farming = new ReskillableFarming();

        @Config.Name("Magic")
        public static final ReskillableMagic magic = new ReskillableMagic();

        @Config.Name("Perks")
        @Config.Comment("Tree layout for Tweaks-registered traits. Restart after edit. Does not hot-reload.")
        public static final ReskillablePerks perks = new ReskillablePerks();

        @Config.Name("Respite")
        @Config.Comment("Lethal-save effect knobs. Layout/cost stay under Perks.")
        public static final ReskillableRespite respite = new ReskillableRespite();
    }

    public static class ReskillablePerks {
        @Config.Name("Melee Efficiency")
        public ReskillablePerkLayout meleeEfficiency = new ReskillablePerkLayout(
                2, 2, 4, "reskillable:attack", "reskillable:attack|16", "reskillable:agility|12");

        @Config.Name("Ranged Efficiency")
        public ReskillablePerkLayout rangedEfficiency = new ReskillablePerkLayout(
                2, 3, 4, "reskillable:attack", "reskillable:attack|16", "reskillable:agility|12");

        @Config.Name("Shield Efficiency")
        public ReskillablePerkLayout shieldEfficiency = new ReskillablePerkLayout(
                2, 2, 4, "reskillable:defense", "reskillable:defense|16");

        @Config.Name("Adrenaline")
        public ReskillablePerkLayout adrenaline = new ReskillablePerkLayout(
                2, 1, 4, "reskillable:agility", "reskillable:agility|16", "reskillable:defense|12");

        @Config.Name("Expert Climber")
        public ReskillablePerkLayout expertClimber = new ReskillablePerkLayout(
                1, 2, 4, "reskillable:agility", "reskillable:agility|20");

        @Config.Name("Cardio Master")
        public ReskillablePerkLayout cardioMaster = new ReskillablePerkLayout(
                3, 3, 4, "reskillable:agility", "reskillable:agility|20");

        @Config.Name("Mining Expert")
        public ReskillablePerkLayout miningExpert = new ReskillablePerkLayout(
                3, 3, 4, "reskillable:mining", "reskillable:mining|24");

        @Config.Name("Evasion")
        public ReskillablePerkLayout evasion = new ReskillablePerkLayout(
                3, 2, 3, "reskillable:agility", "reskillable:agility|16");

        @Config.Name("Respite")
        public ReskillablePerkLayout respite = new ReskillablePerkLayout(
                2, 1, 4, "reskillable:defense", "reskillable:defense|16", "reskillable:magic|16");

        @Config.Name("Power Attack")
        public ReskillablePerkLayout powerAttack = new ReskillablePerkLayout(
                2, 1, 3, "reskillable:attack", "reskillable:attack|12");

        @Config.Name("Armor Mastery")
        public ReskillablePerkLayout armorMastery = new ReskillablePerkLayout(
                2, 3, 4, "reskillable:defense", "reskillable:defense|8", "reskillable:agility|16");

        @Config.Name("Mining Efficiency")
        public ReskillablePerkLayout miningEfficiency = new ReskillablePerkLayout(
                2, 3, 6, "reskillable:mining", "reskillable:mining|20");

        @Config.Name("Precision Shot")
        public ReskillablePerkLayout precisionShot = new ReskillablePerkLayout(
                3, 3, 3, "reskillable:attack", "reskillable:attack|12", "reskillable:agility|16");

        @Config.Name("Herbalist")
        public ReskillablePerkLayout herbalist = new ReskillablePerkLayout(
                2, 1, 4, "reskillable:gathering", "reskillable:gathering|16", "reskillable:magic|12");

        @Config.Name("Gathering Efficiency")
        public ReskillablePerkLayout gatheringEfficiency = new ReskillablePerkLayout(
                2, 3, 4, "reskillable:gathering", "reskillable:gathering|12");

        @Config.Name("Bountiful Harvest")
        public ReskillablePerkLayout bountifulHarvest = new ReskillablePerkLayout(
                2, 2, 4, "reskillable:farming", "reskillable:farming|16");

        @Config.Name("Rancher")
        public ReskillablePerkLayout rancher = new ReskillablePerkLayout(
                2, 1, 4, "reskillable:farming", "reskillable:farming|16", "reskillable:gathering|12");

        @Config.Name("Drafter")
        public ReskillablePerkLayout drafter = new ReskillablePerkLayout(
                2, 1, 3, "reskillable:building", "reskillable:building|12");

        @Config.Name("Sculptor")
        public ReskillablePerkLayout sculptor = new ReskillablePerkLayout(
                2, 3, 3, "reskillable:building", "reskillable:building|20");

        @Config.Name("Transpose")
        public ReskillablePerkLayout transpose = new ReskillablePerkLayout(
                3, 1, 3, "reskillable:building", "reskillable:building|24", "reskillable:magic|30");

        @Config.Name("Vis Thrift")
        public ReskillablePerkLayout visThrift = new ReskillablePerkLayout(
                2, 1, 4, "reskillable:magic", "reskillable:magic|20");

        @Config.Name("Quiet Mind")
        public ReskillablePerkLayout quietMind = new ReskillablePerkLayout(
                2, 2, 4, "reskillable:magic", "reskillable:magic|16", "reskillable:defense|12");

        @Config.Name("Glass Cutter")
        public ReskillablePerkLayout glassCutter = new ReskillablePerkLayout(
                2, 2, 2, "reskillable:building", "reskillable:building|12");

        @Config.Name("Herd Abundance")
        public ReskillablePerkLayout herdAbundance = new ReskillablePerkLayout(
                2, 2, 4, "reskillable:gathering", "reskillable:gathering|16", "reskillable:farming|12");

        @Config.Name("Iron Gut")
        public ReskillablePerkLayout ironGut = new ReskillablePerkLayout(
                2, 3, 3, "reskillable:farming", "reskillable:farming|16");

        @Config.Name("Water Collector")
        public ReskillablePerkLayout waterCollector = new ReskillablePerkLayout(
                3, 1, 4, "reskillable:gathering", "reskillable:gathering|16");

        @Config.Name("Full Font")
        public ReskillablePerkLayout fullFont = new ReskillablePerkLayout(
                2, 3, 4, "reskillable:magic", "reskillable:magic|16");

        @Config.Name("Blood Pact")
        public ReskillablePerkLayout bloodPact = new ReskillablePerkLayout(
                3, 3, 4, "reskillable:magic", "reskillable:magic|20");

        @Config.Name("Druid")
        public ReskillablePerkLayout druid = new ReskillablePerkLayout(
                1, 2, 3, "reskillable:magic",
                "reskillable:magic|12",
                "not|trait|aqtweaks:witch",
                "not|trait|aqtweaks:astromancer",
                "not|trait|aqtweaks:artificer");

        @Config.Name("Mana Veil")
        public ReskillablePerkLayout manaVeil = new ReskillablePerkLayout(
                0, 2, 1, "reskillable:magic", "trait|aqtweaks:druid");

        @Config.Name("Living Edge")
        public ReskillablePerkLayout livingEdge = new ReskillablePerkLayout(
                0, 3, 1, "reskillable:magic", "trait|aqtweaks:druid");

        @Config.Name("Witch")
        public ReskillablePerkLayout witch = new ReskillablePerkLayout(
                3, 1, 3, "reskillable:magic",
                "reskillable:magic|12",
                "not|trait|aqtweaks:druid",
                "not|trait|aqtweaks:astromancer",
                "not|trait|aqtweaks:artificer");

        @Config.Name("Cold Iron Mind")
        public ReskillablePerkLayout coldIronMind = new ReskillablePerkLayout(
                3, 0, 1, "reskillable:magic", "trait|aqtweaks:witch");

        @Config.Name("Stitch")
        public ReskillablePerkLayout stitch = new ReskillablePerkLayout(
                4, 0, 1, "reskillable:magic", "trait|aqtweaks:witch");

        @Config.Name("Astromancer")
        public ReskillablePerkLayout astromancer = new ReskillablePerkLayout(
                4, 1, 3, "reskillable:magic",
                "reskillable:magic|12",
                "not|trait|aqtweaks:druid",
                "not|trait|aqtweaks:witch",
                "not|trait|aqtweaks:artificer");

        @Config.Name("Astral Warmth")
        public ReskillablePerkLayout astralWarmth = new ReskillablePerkLayout(
                4, 2, 1, "reskillable:magic", "trait|aqtweaks:astromancer");

        @Config.Name("Star Powered")
        public ReskillablePerkLayout starPowered = new ReskillablePerkLayout(
                4, 3, 1, "reskillable:magic", "trait|aqtweaks:astromancer");

        @Config.Name("Artificer")
        public ReskillablePerkLayout artificer = new ReskillablePerkLayout(
                0, 0, 3, "reskillable:magic",
                "reskillable:magic|12",
                "not|trait|aqtweaks:druid",
                "not|trait|aqtweaks:witch",
                "not|trait|aqtweaks:astromancer");

        @Config.Name("Live Spark")
        public ReskillablePerkLayout liveSpark = new ReskillablePerkLayout(
                1, 0, 1, "reskillable:magic", "trait|aqtweaks:artificer");

        @Config.Name("Cinder Ward")
        public ReskillablePerkLayout cinderWard = new ReskillablePerkLayout(
                2, 0, 1, "reskillable:magic", "trait|aqtweaks:artificer");
    }

    public static class ReskillableRespite {
        @Config.Name("Perk ID")
        public String perkId = "aqtweaks:respite";

        @Config.Name("Cooldown Ticks")
        @Config.Comment("Ticks after a save before it can fire again. 1200 = 60 seconds.")
        @Config.RangeInt(min = 0, max = 12000)
        public int cooldownTicks = 1200;

        @Config.Name("Effect Duration Ticks")
        @Config.Comment("Regen II and Tea Story defence duration. 100 = 5 seconds.")
        @Config.RangeInt(min = 1, max = 12000)
        public int effectDurationTicks = 100;

        @Config.Name("Regen Amplifier")
        @Config.Comment("0 = Regeneration I, 1 = Regeneration II.")
        @Config.RangeInt(min = 0, max = 10)
        public int regenAmplifier = 1;

        @Config.Name("Defence Potion ID")
        public String defencePotionId = "teastory:defence";

        @Config.Name("Defence Amplifier")
        @Config.RangeInt(min = 0, max = 10)
        public int defenceAmplifier = 0;
    }

    public static class ReskillableGeneral {
        @Config.Name("Enable Reskillable Bonuses")
        @Config.Comment("If false, no per-level attributes, harvest extras, magic multiply, or EB building bonuses.")
        public boolean enable = true;
    }

    public static class ReskillableAttack {
        @Config.Name("Damage Per Level")
        @Config.Comment("Added to generic.attackDamage (operation 0). 0.125 → +2 at 16, +4 at 32.")
        @Config.RangeDouble(min = 0.0, max = 4.0)
        public double damagePerLevel = 0.125;
    }

    public static class ReskillableDefense {
        @Config.Name("Armor Per Level")
        @Config.Comment("Added to generic.armor (operation 0). 0.25 → +4 at 16, +8 at 32. Not max health.")
        @Config.RangeDouble(min = 0.0, max = 4.0)
        public double armorPerLevel = 0.25;
    }

    public static class ReskillableAgility {
        @Config.Name("Speed Per Level")
        @Config.Comment("Added to generic.movementSpeed. 0.0003125 → +5% of 0.1 at 16, +10% at 32.")
        @Config.RangeDouble(min = 0.0, max = 0.01)
        public double speedPerLevel = 0.0003125;
    }

    public static class ReskillableBuilding {
        @Config.Name("EB Place Reach Per Level")
        @Config.Comment("Added to Effortless Building getPlacementReach only (not vanilla REACH_DISTANCE, not attack reach). 0.125 → +2 at 16, +4 at 32.")
        @Config.RangeDouble(min = 0.0, max = 4.0)
        public double placeReachPerLevel = 0.125;

        @Config.Name("EB Max Blocks Per Level")
        @Config.Comment("Added to Effortless Building getMaxBlocksPlacedAtOnce in survival. 1 → +16 at 16, +32 at 32.")
        @Config.RangeDouble(min = 0.0, max = 16.0)
        public double maxBlocksPerLevel = 1.0;
    }

    public static class ReskillableMining {
        @Config.Name("Break Speed Per Level")
        @Config.Comment("BreakSpeed multiply: speed × (1 + level × k). 0.01 → +16% at 16, +32% at 32.")
        @Config.RangeDouble(min = 0.0, max = 0.25)
        public double breakSpeedPerLevel = 0.01;

        @Config.Name("Mining Expert Harvest Floor")
        @Config.Comment("Pickaxes with Mining Expert treat harvest as at least this level. Vanilla diamond = 3 (4 tooltip stars).")
        @Config.RangeInt(min = 0, max = 16)
        public int expertHarvestFloor = 3;
    }

    public static class ReskillableGathering {
        @Config.Name("Extra Drop Chance Per Level")
        @Config.Comment("Chance of +1 forage / extra wool / extra fish. 0.00625 → 10% at 16, 20% at 32. Not ores, not crops.")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double extraDropChancePerLevel = 0.00625;

        @Config.Name("Herbalist Namespaces")
        @Config.Comment("Block registry namespaces that always +1 with Herbalist. Restart after edit.")
        public String[] herbalistNamespaces = new String[] {
                "bewitchment", "thaumcraft", "botania", "rustic", "teastory", "astralsorcery", "mysticalworld"
        };
    }

    public static class ReskillableFarming {
        @Config.Name("Extra Drop Chance Per Level")
        @Config.Comment("Chance of +1 on a mature crop harvest. 0.00625 → 10% at 16, 20% at 32.")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double extraDropChancePerLevel = 0.00625;

        @Config.Name("Rancher Range")
        @Config.Comment("Blocks from a Rancher player to extra-tick tended Animania animals.")
        @Config.RangeDouble(min = 1.0, max = 64.0)
        public double rancherRange = 16.0;
    }

    public static class ReskillableMagic {
        @Config.Name("Hurt Multiply Per Level")
        @Config.Comment("Spell-like LivingHurtEvent multiply, capped at 40%. 0.0125 → ±20% at 16, ±40% at 32.")
        @Config.RangeDouble(min = 0.0, max = 0.1)
        public double perLevel = 0.0125;

        @Config.Name("Log Damage Classify")
        @Config.Comment("INFO-log unique damageType / isMagicDamage / source classes for player-involved hits (cap 48). Use to lock allow prefixes after a gauntlet shot.")
        public boolean logClassify = true;

        @Config.Name("Allow Type Prefixes")
        @Config.Comment("If isMagicDamage is false, still treat damageType (lowercase prefix) as spell-like. Default fireball = ghast/blaze/Lich. TC foci use the Thaumcraft mixin, not this list. Do not add thrown (snowballs). Instance files keep an empty list until edited.")
        public String[] allowTypePrefixes = new String[] {"fireball"};

        @Config.Name("Deny Types")
        @Config.Comment("Exact damageType strings that never count as spell-like.")
        public String[] denyTypes = new String[] {"wither", "onFire", "lava", "hotFloor"};

        @Config.Name("Vis Thrift Add")
        @Config.Comment("Added to CasterManager.getTotalVisDiscount when Vis Thrift is unlocked. 0.30 = 30% off foci and Arcane Workbench.")
        @Config.RangeDouble(min = 0.0, max = 0.9)
        public double visThriftAdd = 0.30;

        @Config.Name("Quiet Mind Bound Fraction")
        @Config.Comment("Subtract round(fraction × incident bound) from warp severity after visor.")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double quietMindBoundFraction = 0.35;

        @Config.Name("Quiet Mind Cancel Chat")
        public String quietMindCancelChat = "Whispers try to cloud your thoughts, but you push them away...";

        @Config.Name("Full Font Aura Ratio")
        @Config.Comment("Chunk vis / aura base at or above this counts as full. Flux is ignored.")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double fullFontAuraRatio = 0.9;

        @Config.Name("Full Font Vis Cost")
        @Config.Comment("Multiply ItemCaster.consumeVis amount before Vis Thrift when the aura is full.")
        @Config.RangeDouble(min = 1.0, max = 4.0)
        public double fullFontVisCost = 1.5;

        @Config.Name("Full Font Output")
        @Config.Comment("Outgoing classified Magic / Heal-focus after drip and Blood Pact, then the stamp clears.")
        @Config.RangeDouble(min = 1.0, max = 4.0)
        public double fullFontOutput = 1.5;

        @Config.Name("Blood Pact Max Health")
        @Config.Comment("Unsaved generic.maxHealth add while Blood Pact is unlocked. −8 = four hearts.")
        @Config.RangeDouble(min = -40.0, max = 0.0)
        public double bloodPactMaxHealth = -8.0;

        @Config.Name("Blood Pact Outgoing")
        @Config.Comment("Outgoing classified Magic multiply after drip, before Full Font.")
        @Config.RangeDouble(min = 1.0, max = 4.0)
        public double bloodPactOutgoing = 2.0;

        @Config.Name("School Thrift")
        @Config.Comment("Parent resource spend multiply on the school perk (mana, ritual drain, altar starlight, Ember).")
        @Config.RangeDouble(min = 0.1, max = 1.0)
        public double schoolThrift = 0.70;

        @Config.Name("Grove Range")
        @Config.RangeDouble(min = 1.0, max = 64.0)
        public double groveRange = 24.0;

        @Config.Name("Grove Mana")
        @Config.Comment("Generating-flower addMana multiply while a Druid is in range.")
        @Config.RangeDouble(min = 1.0, max = 2.0)
        public double groveMana = 1.15;

        @Config.Name("Mana Veil Absorb")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double manaVeilAbsorb = 0.20;

        @Config.Name("Mana Veil Per HP")
        @Config.RangeInt(min = 1, max = 100000)
        public int manaVeilManaPerHp = 1000;

        @Config.Name("Living Edge")
        @Config.RangeDouble(min = 1.0, max = 2.0)
        public double livingEdge = 1.15;

        @Config.Name("Hearth Altar Gain")
        @Config.RangeDouble(min = 1.0, max = 2.0)
        public double hearthGain = 1.15;

        @Config.Name("Altar Craft Time")
        @Config.Comment("Astromancer craftingTickTime multiply. 0.8 = 25% faster.")
        @Config.RangeDouble(min = 0.25, max = 1.0)
        public double altarCraftTime = 0.80;

        @Config.Name("Star Powered Damage")
        @Config.RangeDouble(min = 1.0, max = 2.0)
        public double starPoweredDamage = 1.25;

        @Config.Name("Live Spark")
        @Config.RangeDouble(min = 1.0, max = 2.0)
        public double liveSpark = 1.20;

        @Config.Name("Cinder Ward Fire")
        @Config.RangeDouble(min = 0.1, max = 1.0)
        public double cinderWardFire = 0.80;

        @Config.Name("Cinder Ward Heal")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double cinderWardHeal = 0.20;

        @Config.Name("Cinder Temp Max")
        @Config.RangeInt(min = 0, max = 25)
        public int cinderTempMax = 15;

        @Config.Name("Astral Temp Min")
        @Config.RangeInt(min = 0, max = 25)
        public int astralTempMin = 9;

        @Config.Name("Foundry Pulse Every")
        @Config.Comment("Extra machine tick when world time modulo this is 0.")
        @Config.RangeInt(min = 1, max = 40)
        public int foundryPulseEvery = 4;
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_bettermineshafts", category = "")
    public static class BetterMineshaftsModuleConfig {
        @Config.Name("General")
        @Config.Comment("Tweaks mineshaft rate, spacing, ocean skip, and tunnel Y.")
        public static final BetterMineshaftsGeneral general = new BetterMineshaftsGeneral();
    }

    public static class BetterMineshaftsGeneral {
        @Config.Name("Enable Better Mineshafts Tweaks")
        @Config.Comment("When true, Tweaks owns mineshaft can-spawn (rate, chunk spacing, ocean/beach skip) and tunnel Y. When false, parent Better Mineshafts canSpawn and Y run; locate pin and failed-entrance stub still apply.")
        public boolean enable = true;

        @Config.Name("Mineshaft Spawn Rate")
        @Config.Comment("Chance a spacing-eligible land chunk gets a shaft. Tweaks owns this roll; Better Mineshafts cfg spawn rate is unused while Enable is true. New chunks only.")
        @Config.RangeDouble(min = 0.0, max = 1.0)
        public double mineshaftSpawnRate = 0.001;

        @Config.Name("Chunk Spacing")
        @Config.Comment("Only chunks whose X and Z are multiples of this may roll. 1 = no grid. New chunks only.")
        @Config.RangeInt(min = 1, max = 64)
        public int chunkSpacing = 4;

        @Config.Name("Skip Ocean And Beach")
        @Config.Comment("Skip biome-provider ocean and beach at the chunk center (Y=64). Off lets shafts roll there.")
        public boolean skipOceanAndBeach = true;

        @Config.Name("Tunnel Min Y")
        @Config.Comment("Lowest Y for the start tunnel (local copy; does not rewrite Better Mineshafts variant settings). Clamped to 1. New chunks only.")
        @Config.RangeInt(min = 1, max = 255)
        public int tunnelMinY = 17;

        @Config.Name("Tunnel Max Y")
        @Config.Comment("Highest Y for the start tunnel. Clamped to 255. Swapped with min if inverted. Does not go into Depths −Y.")
        @Config.RangeInt(min = 1, max = 255)
        public int tunnelMaxY = 37;
    }

    /**
     * Forces values that are documented as non-tunable back to their pinned constants. Runs after
     * every sync, so a hand-edited instance file is corrected rather than honoured. Readers can
     * keep reading the fields directly.
     */
    public static void normalizePinned() {
        DepthsModuleConfig.general.minWorldY = DEPTHS_FLOOR_Y;
        sanitizePerkRequirements();
    }

    private static void sanitizePerkRequirements() {
        ReskillablePerks perks = ReskillableModuleConfig.perks;
        if (perks == null) {
            return;
        }
        for (java.lang.reflect.Field field : ReskillablePerks.class.getFields()) {
            if (field.getType() != ReskillablePerkLayout.class) {
                continue;
            }
            try {
                ReskillablePerkLayout layout = (ReskillablePerkLayout) field.get(perks);
                if (layout != null) {
                    layout.sanitizeRequirements();
                }
            } catch (IllegalAccessException ignored) {
            }
        }
    }

    @Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
    public static class ConfigEventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(ArcanaQuestTweaks.MODID)) {
                ConfigManager.sync(ArcanaQuestTweaks.MODID, Config.Type.INSTANCE);
                normalizePinned();
                DssSkillCosts.invalidate();
                com.apocollis.aqtweaks.spawning.SpawnTypeLists.reload();
                com.apocollis.aqtweaks.spawning.SpawnStructureLists.reload();
                com.apocollis.aqtweaks.spawning.SpawnParties.reload();
                com.apocollis.aqtweaks.spawning.SpawnGroupCounts.reload();
                com.apocollis.aqtweaks.spawning.SpawnGroupSizes.invalidate();
                if (net.minecraftforge.fml.common.Loader.isModLoaded("reskillable")) {
                    com.apocollis.aqtweaks.reskillable.ReskillableModule.restampOnlinePlayers();
                }
                if (net.minecraftforge.fml.common.Loader.isModLoaded("somnia")) {
                    com.apocollis.aqtweaks.somnia.SomniaSleepHandler.applyConfigOverrides();
                }
            }
        }
    }
}
