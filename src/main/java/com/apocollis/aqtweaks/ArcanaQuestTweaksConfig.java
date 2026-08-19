package com.apocollis.aqtweaks;

import com.apocollis.aqtweaks.stamina.DssSkillCosts;
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
        public int ladderCost = 2;

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
        @Config.Comment("Stamina cost (in half-feathers) per ledge climb")
        @Config.RangeInt(min = 0)
        public int ledgeClimbCost = 2;
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_grimoireofgaia")
    public static class GrimoireOfGaiaConfig {
        @Config.Name("Disable Piercing Damage")
        @Config.Comment("Should piercing/penetrating damage from Grimoire of Gaia mobs be converted to normal damage that is reducible by physical armor?")
        public static boolean disablePiercingDamage = true;
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
        @Config.Comment("Should temporary warp be reduced on successful sleep?")
        public static boolean clearTempWarp = true;

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
        public static int dimensionNormalWarp = 2;

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

        @Config.Name("Exposure Dimensions Config")
        @Config.Comment({
            "List of dimensions where the player slowly accumulates temporary warp, with their exposure intervals.",
            "Format: dimension_id=interval_seconds",
            "Example: -1=300 (Nether accumulates 1 warp every 5 minutes)",
            "Example: 1=180 (The End accumulates 1 warp every 3 minutes)"
        })
        public static String[] exposureDimensionsConfig = new String[] {
            "-1=300",
            "1=180"
        };

        @Config.Name("Enable Deep Underground Exposure")
        @Config.Comment("Should players slowly accumulate temporary warp while deep underground?")
        public static boolean enableUndergroundExposure = true;

        @Config.Name("Underground Y Threshold")
        @Config.Comment("Y level at or below which the player accumulates temporary warp.")
        @Config.RangeInt(min = -1, max = 256)
        public static int exposureUndergroundY = 30;

        @Config.Name("Underground Exposure Interval")
        @Config.Comment("Seconds of underground exposure required to gain 1 point of temporary warp.")
        @Config.RangeInt(min = 1)
        public static int exposureUndergroundInterval = 300;

        @Config.Name("Enable Dungeon Exposure")
        @Config.Comment("Should players slowly accumulate temporary warp while inside a Roguelike Dungeon?")
        public static boolean enableDungeonExposure = true;

        @Config.Name("Dungeon Exposure Interval")
        @Config.Comment("Seconds of dungeon exposure required to gain 1 point of temporary warp.")
        @Config.RangeInt(min = 1)
        public static int exposureDungeonInterval = 180;

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
            @Config.Comment("The minimum Y coordinate boundary of the world (defaults to -64 for Depths Update).")
            @Config.RangeInt(min = -256, max = 0)
            public int minWorldY = -64;

            @Config.Name("Better Depths Caves")
            @Config.Comment("Enable AQTweaks Depths cave generation (BC-style upper tunnels, chambers, lower deep, sparse shafts, Y0 mouths into +Y Better Caves). Affects new chunks only. When false, AQTweaks skips that carve path.")
            public boolean enableBetterDepthsCaves = true;
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
            @Config.Comment("Should YUNG's Better Caves bedrock generation layer be shifted down to Minimum World Y?")
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
        @Config.Comment("Should RTG terrain around villages be flattened to the village bounding box, then blended into surrounding hills?")
        public boolean enableVillageSmoothing = true;

        @Config.Name("Village Edge Falloff")
        @Config.Comment("Blocks outside the village bounding box to blend from flattened height back to raw RTG terrain. 0 = hard village box only.")
        @Config.RangeInt(min = 0, max = 256)
        public int villageEdgeFalloff = 48;

        @Config.Name("Village Plate Slope")
        @Config.Comment("Inside the village box: 0 = fully flat plate at the box-average height. 30 = allow at most 1 block of height change per 30 blocks from the plate center.")
        @Config.RangeInt(min = 0, max = 256)
        public int villagePlateSlopeBlocks = 0;

        @Config.Name("Skip Water Village Pieces")
        @Config.Comment("If a village house or road would spawn on a wet column (water biome, RTG river, or below min well height), skip that slot and retry nearby land along the same street.")
        public boolean skipWaterVillagePieces = true;

        @Config.Name("Village Water Retry Distance")
        @Config.Comment("How far (blocks) to step back or sideways along the street when retrying a water village piece. 0 = skip only, no retry.")
        @Config.RangeInt(min = 0, max = 48)
        public int villageWaterRetryDistance = 20;

        @Config.Name("Reject Coastal Village Starts")
        @Config.Comment("Veto a vanilla village candidate if the well is in a water/beach biome, below min well height, or within the coast buffer of deep ocean. Does not move the village; that grid cell is empty.")
        public boolean rejectCoastalVillageStarts = true;

        @Config.Name("Village Min Well Height")
        @Config.Comment("Village wells and wet-column checks treat RTG terrain below this Y as water. Vanilla sea level is 63.")
        @Config.RangeInt(min = 1, max = 255)
        public int villageMinWellHeight = 65;

        @Config.Name("Village Coast Buffer")
        @Config.Comment("Blocks around the well to scan for deep ocean. 0 = well column only.")
        @Config.RangeInt(min = 0, max = 128)
        public int villageCoastBuffer = 32;

        @Config.Name("Enable Village Bounding Box Detection")
        @Config.Comment("Treat the village start bounding box (yards, roads, gaps) as Village for isInsideStructure / InControl, not only child pieces.")
        public boolean enableVillageBoxDetection = true;

        @Config.Name("Village Box XZ Pad")
        @Config.Comment("Extra blocks outside the village hull that still count as Village and are fully flattened. 0 = exact hull.")
        @Config.RangeInt(min = 0, max = 64)
        public int villageBoxXZPad = 8;

        @Config.Name("Village Box Height")
        @Config.Comment("Blocks above the pad surface that still count as Village. Floor is the pad; below the pad is not Village.")
        @Config.RangeInt(min = 0, max = 256)
        public int villageBoxHeight = 32;

        @Config.Name("Village Flatten Debug")
        @Config.Comment("Log once per village when RTG flatten applies (box size and plate Y). Off by default.")
        public boolean villageFlattenDebug = false;
    }

    @Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
    public static class ConfigEventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(ArcanaQuestTweaks.MODID)) {
                ConfigManager.sync(ArcanaQuestTweaks.MODID, Config.Type.INSTANCE);
                DssSkillCosts.invalidate();
            }
        }
    }
}
