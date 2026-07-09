package com.apocollis.aqtweaks;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = ArcanaQuestTweaks.MODID, name = "arcanaquesttweaks")
public class ArcanaQuestTweaksConfig {

    public static StaminaModuleConfig staminaModule = new StaminaModuleConfig();

    @Config.Name("Grimoire of Gaia Module")
    @Config.Comment("Configure the Grimoire of Gaia Module settings")
    public static GrimoireOfGaiaConfig grimoireOfGaia = new GrimoireOfGaiaConfig();

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

        @Config.Name("Shield Raise Cost")
        @Config.Comment("Stamina cost (in half-feathers) charged instantly upon raising a shield")
        @Config.RangeInt(min = 0)
        public int shieldRaiseCost = 1;

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

    public static class GrimoireOfGaiaConfig {
        @Config.Name("Disable Piercing Damage")
        @Config.Comment("Should piercing/penetrating damage from Grimoire of Gaia mobs be converted to normal damage that is reducible by physical armor?")
        public boolean disablePiercingDamage = true;
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
