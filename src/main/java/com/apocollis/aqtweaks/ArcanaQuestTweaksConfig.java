package com.apocollis.aqtweaks;

import com.apocollis.aqtweaks.thaumcraft.ThaumcraftModule;

import com.apocollis.aqtweaks.stamina.StaminaModule;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ArcanaQuestTweaksConfig {

    @Config.Name("Stamina Module")
    @Config.Comment("Configure the Stamina Module settings")
    public static StaminaModuleConfig staminaModule = new StaminaModuleConfig();

    @Config.Name("Grimoire of Gaia Module")
    @Config.Comment("Configure the Grimoire of Gaia Module settings")
    public static GrimoireOfGaiaConfig grimoireOfGaia = new GrimoireOfGaiaConfig();

    @Config.Name("Thaumcraft Module")
    @Config.Comment("Configure Thaumcraft integration settings")
    public static ThaumcraftConfig thaumcraftModule = new ThaumcraftConfig();

    @Config.Name("Bewitchment Module")
    @Config.Comment("Configure Bewitchment integration settings")
    public static BewitchmentConfig bewitchmentModule = new BewitchmentConfig();

    @Config.Name("Roguelike Dungeons Module")
    @Config.Comment("Configure Roguelike Dungeons structure integration settings")
    public static RoguelikeDungeonsConfig roguelikeModule = new RoguelikeDungeonsConfig();

    @Config.Name("Depths Module")
    @Config.Comment("Configure Depths Update (-Y levels) compatibility settings")
    public static DepthsModuleConfig depthsModule = new DepthsModuleConfig();

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_stamina")
    public static class StaminaModuleConfig {
        @Config.Name("Jumping")
        @Config.Comment("Configure jumping stamina consumption")
        public Jumping jumping = new Jumping();

        @Config.Name("Bow Drawing")
        @Config.Comment("Configure bow-drawing stamina consumption")
        public BowDrawing bowDrawing = new BowDrawing();

        @Config.Name("Climbing")
        @Config.Comment("Configure ladder, vine, and rope climbing stamina consumption")
        public Climbing climbing = new Climbing();

        @Config.Name("Weapons (Melee)")
        @Config.Comment("Configure melee weapon stamina consumption")
        public Weapons weapons = new Weapons();

        @Config.Name("Grapple Mod")
        @Config.Comment("Configure grappling hook stamina consumption")
        public Grapple grapple = new Grapple();

        @Config.Name("Open Glider")
        @Config.Comment("Configure hang glider stamina consumption")
        public Glider glider = new Glider();

        @Config.Name("Shield Blocking")
        @Config.Comment("Configure shield stamina consumption")
        public Shield shield = new Shield();

        @Config.Name("Mining")
        @Config.Comment("Configure mining/block breaking stamina consumption")
        public Mining mining = new Mining();

        @Config.Name("Reskillable Integration")
        @Config.Comment("Configure Reskillable integration settings")
        public Reskillable reskillable = new Reskillable();

        @Config.Name("Simple Difficulty Integration")
        @Config.Comment("Configure Simple Difficulty integration settings")
        public SimpleDifficulty simpleDifficulty = new SimpleDifficulty();

        @Config.Name("Ledge Climbing")
        @Config.Comment("Configure ledge climbing stamina consumption")
        public LedgeClimb ledgeClimb = new LedgeClimb();

        @Config.Name("Throwing Weapons")
        @Config.Comment("Configure throwing weapon (javelins, throwing knives, etc.) stamina consumption")
        public ThrowingWeapons throwingWeapons = new ThrowingWeapons();
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

    public static class Climbing {
        @Config.Name("Enable Climbing Stamina Cost")
        @Config.Comment("Does climbing/clinging to ladders/vines consume stamina?")
        public boolean enableClimbCost = true;

        @Config.Name("Ladder Climb Tick Interval")
        @Config.Comment("Ticks between feather consumption on ladders (20 ticks = 1 second)")
        @Config.RangeInt(min = 1)
        public int ladderInterval = 20;

        @Config.Name("Ladder Climb Cost")
        @Config.Comment("Stamina cost (in half-feathers) per interval on ladders")
        @Config.RangeInt(min = 0)
        public int ladderCost = 2;

        @Config.Name("Vine Climb Tick Interval")
        @Config.Comment("Ticks between feather consumption on vines (20 ticks = 1 second)")
        @Config.RangeInt(min = 1)
        public int vineInterval = 20;

        @Config.Name("Vine Climb Cost")
        @Config.Comment("Stamina cost (in half-feathers) per interval on vines")
        @Config.RangeInt(min = 0)
        public int vineCost = 3;

        @Config.Name("Enable Rope Climb Cost")
        @Config.Comment("Does climbing/clinging to ropes consume stamina?")
        public boolean enableRopeCost = true;

        @Config.Name("Rope Climb Tick Interval")
        @Config.Comment("Ticks between feather consumption on ropes (20 ticks = 1 second)")
        @Config.RangeInt(min = 1)
        public int ropeInterval = 20;

        @Config.Name("Rope Climb Cost")
        @Config.Comment("Stamina cost (in half-feathers) per interval on ropes")
        @Config.RangeInt(min = 0)
        public int ropeCost = 3;

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

        @Config.Name("Grapple Hold Tick Interval")
        @Config.Comment("Ticks between feather consumption while grappling (20 ticks = 1 second)")
        @Config.RangeInt(min = 1)
        public int grappleHoldInterval = 20;

        @Config.Name("Grapple Hold Cost")
        @Config.Comment("Stamina cost (in half-feathers) consumed per hold interval")
        @Config.RangeInt(min = 0)
        public int grappleHoldCost = 1;
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
        public boolean disablePiercingDamage = true;
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_thaumcraft")
    public static class ThaumcraftConfig {
        @Config.Name("Enable Sleep Warp Cleansing")
        @Config.Comment("Should sleeping in a bed clear a small amount of Thaumcraft warp?")
        public boolean enableWarpCleansing = true;

        @Config.Name("Clear Normal Warp")
        @Config.Comment("Should normal (sticky) warp be reduced on successful sleep?")
        public boolean clearNormalWarp = true;

        @Config.Name("Normal Warp Reduction")
        @Config.Comment("Amount of normal (sticky) warp to clear per successful sleep")
        @Config.RangeInt(min = 0)
        public int normalWarpReduction = 1;

        @Config.Name("Clear Temporary Warp")
        @Config.Comment("Should temporary warp be reduced on successful sleep?")
        public boolean clearTempWarp = true;

        @Config.Name("Temporary Warp Reduction")
        @Config.Comment("Amount of temporary warp to clear per successful sleep")
        @Config.RangeInt(min = 0)
        public int tempWarpReduction = 2;

        @Config.Name("Enable Sleep Chat Message")
        @Config.Comment("Should players receive a chat message informing them that their mind feels clearer upon waking up?")
        public boolean enableChatMessage = true;

        @Config.Name("Sleep Chat Message Text")
        @Config.Comment("The text of the message sent to players when their warp is reduced by sleeping")
        public String chatMessageText = "§5You wake up feeling refreshed, and the whispers in your mind grow quieter...§r";

        @Config.Name("Enable Dimension Entry Warp")
        @Config.Comment("Should players gain warp when entering a new dimension for the first time?")
        public boolean enableDimensionWarp = true;

        @Config.Name("Dimension Entry Normal Warp")
        @Config.Comment("Amount of normal (sticky) warp gained when entering a new dimension for the first time")
        @Config.RangeInt(min = 0)
        public int dimensionNormalWarp = 2;

        @Config.Name("Dimension Entry Temporary Warp")
        @Config.Comment("Amount of temporary warp gained when entering a new dimension for the first time")
        @Config.RangeInt(min = 0)
        public int dimensionTempWarp = 5;

        @Config.Name("Dimension Chat Message Text")
        @Config.Comment("The text of the message sent to players when they gain warp from entering a new dimension")
        public String dimensionChatMessageText = "§5Entering this strange dimension fills your mind with ancient whispers...§r";

        @Config.Name("Dimension Entry Sound")
        @Config.Comment("The sound registry name to play when the player gains warp from entering a new dimension. Leave empty to play no sound.")
        public String dimensionEntrySound = "thaumcraft:whispers";

        @Config.Name("Dimension Entry Sound Volume")
        @Config.Comment("The volume of the sound played when entering a new dimension. Higher values increase sound reach and loudness.")
        @Config.RangeDouble(min = 0.0, max = 10.0)
        public float dimensionEntrySoundVolume = 2.0F;

        @Config.Name("Enable Warp Exposure")
        @Config.Comment("Should players slowly accumulate temporary warp over time when exposed to certain environments?")
        public boolean enableExposureWarp = true;

        @Config.Name("Exposure Dimensions Config")
        @Config.Comment({
            "List of dimensions where the player slowly accumulates temporary warp, with their exposure intervals.",
            "Format: dimension_id=interval_seconds",
            "Example: -1=300 (Nether accumulates 1 warp every 5 minutes)",
            "Example: 1=180 (The End accumulates 1 warp every 3 minutes)"
        })
        public String[] exposureDimensionsConfig = new String[] {
            "-1=300",
            "1=180"
        };

        @Config.Name("Enable Deep Underground Exposure")
        @Config.Comment("Should players slowly accumulate temporary warp while deep underground?")
        public boolean enableUndergroundExposure = true;

        @Config.Name("Underground Y Threshold")
        @Config.Comment("Y level at or below which the player accumulates temporary warp.")
        @Config.RangeInt(min = -1, max = 256)
        public int exposureUndergroundY = 30;

        @Config.Name("Underground Exposure Interval")
        @Config.Comment("Seconds of underground exposure required to gain 1 point of temporary warp.")
        @Config.RangeInt(min = 1)
        public int exposureUndergroundInterval = 300;

        @Config.Name("Enable Dungeon Exposure")
        @Config.Comment("Should players slowly accumulate temporary warp while inside a Roguelike Dungeon?")
        public boolean enableDungeonExposure = true;

        @Config.Name("Dungeon Exposure Interval")
        @Config.Comment("Seconds of dungeon exposure required to gain 1 point of temporary warp.")
        @Config.RangeInt(min = 1)
        public int exposureDungeonInterval = 180;

        @Config.Name("Enable Exposure Sound")
        @Config.Comment("Should a sound effect play when temporary warp is gained from environmental exposure?")
        public boolean enableExposureSound = true;

        @Config.Name("Exposure Sound Effect")
        @Config.Comment("The sound registry name to play when temporary warp is gained from environmental exposure. Default: thaumcraft:whispers")
        public String exposureSoundEffect = "thaumcraft:whispers";

        @Config.Name("Exposure Sound Volume")
        @Config.Comment("The volume of the sound played when temporary warp is gained from environmental exposure. Higher values increase sound reach and loudness.")
        @Config.RangeDouble(min = 0.0, max = 10.0)
        public float exposureSoundVolume = 2.0F;
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_bewitchment")
    public static class BewitchmentConfig {
        @Config.Name("Ritual Warp List")
        @Config.Comment({
            "List of Bewitchment ritual registry names that should grant Thaumcraft warp upon completion.",
            "Format: registry_name=normal,temporary,permanent (permanent is optional and defaults to 0)"
        })
        public String[] ritualWarpList = new String[] {
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

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_roguelike")
    public static class RoguelikeDungeonsConfig {
        @Config.Name("Enable Grid Spawning")
        @Config.Comment("Should Roguelike Dungeons be forced to spawn on a predictable mathematical grid instead of randomly?")
        public boolean enableGridSpawning = true;

        @Config.Name("Minimum Chunk Distance")
        @Config.Comment("The minimum chunk distance between Roguelike Dungeon spawns.")
        @Config.RangeInt(min = 1)
        public int minChunkDistance = 32;

        @Config.Name("Maximum Chunk Distance")
        @Config.Comment("The maximum chunk distance between Roguelike Dungeon spawns. MUST be larger than Minimum Chunk Distance.")
        @Config.RangeInt(min = 2)
        public int maxChunkDistance = 48;

        @Config.Name("Grid Seed Offset")
        @Config.Comment("Seed salt value to ensure different worlds spawn dungeons in different grid coordinates.")
        public int gridSeedOffset = 1432289;
    }

    @Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks/aqtweaks_depths")
    public static class DepthsModuleConfig {
        @Config.Name("Enable Depths Module")
        @Config.Comment("Should negative Y-level compatibility enhancements be enabled for Depths Update?")
        public boolean enableDepthsModule = true;

        @Config.Name("Minimum World Y Elevation")
        @Config.Comment("The minimum Y coordinate boundary of the world (defaults to -64 for Depths Update).")
        @Config.RangeInt(min = -256, max = 0)
        public int minWorldY = -64;

        @Config.Name("Enable CoFH World Negative Y")
        @Config.Comment("Should CoFH World features evaluate and place blocks below Y = 0 down to Minimum World Y?")
        public boolean enableCoFHNegativeY = true;

        @Config.Name("Enable Better Caves Negative Y")
        @Config.Comment("Should YUNG's Better Caves carve caves and caverns down to Minimum World Y?")
        public boolean enableBetterCavesNegativeY = true;

        @Config.Name("Adjust Better Caves Bedrock Height")
        @Config.Comment("Should YUNG's Better Caves bedrock generation layer be shifted down to Minimum World Y?")
        public boolean adjustBetterCavesBedrock = true;

        @Config.Name("Enable Recurrent Complex Negative Y")
        @Config.Comment("Should Recurrent Complex Volts placement rays scan below Y = 0 down to Minimum World Y?")
        public boolean enableRecurrentComplexNegativeY = true;

        @Config.Name("Roguelike Dungeons Maximum Levels")
        @Config.Comment("Maximum number of levels for Roguelike Dungeons to generate (levels 5+ extend below Y = 0 into deepslate).")
        @Config.RangeInt(min = 5, max = 10)
        public int roguelikeMaxLevels = 10;

        @Config.Name("Force 10 Dungeon Levels For Testing")
        @Config.Comment("If true, Roguelike Dungeons will always force 10 floors for testing. If false, dungeons generate strictly the number of levels defined in their JSON theme settings.")
        public boolean force10LevelsForTesting = false;

        @Config.Name("Enable RTG Village Terrain Smoothing")
        @Config.Comment("Should RTG terrain around villages be smoothed and leveled out to prevent steep cliffs and floating/buried buildings?")
        public boolean enableRTGVillageSmoothing = true;
    }

    @Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
    public static class ConfigEventHandler {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (event.getModID().equals(ArcanaQuestTweaks.MODID)) {
                ConfigManager.sync(ArcanaQuestTweaks.MODID, Config.Type.INSTANCE);
            }
        }
    }
}
