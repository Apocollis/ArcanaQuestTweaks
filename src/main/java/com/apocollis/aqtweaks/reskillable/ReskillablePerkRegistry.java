package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import codersafterdark.reskillable.api.data.RequirementHolder;
import codersafterdark.reskillable.api.unlockable.Unlockable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ReskillablePerkRegistry {

    public static void preInit() {
        MinecraftForge.EVENT_BUS.register(new ReskillablePerkRegistry());
        if (net.minecraftforge.fml.common.Loader.isModLoaded("bewitchment")) {
            MinecraftForge.EVENT_BUS.register(new StitchRecipeEvents());
        }
    }

    @SubscribeEvent
    public void registerUnlockables(RegistryEvent.Register<Unlockable> event) {
        var perks = ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks;
        event.getRegistry().register(new AqtweaksTrait("melee_efficiency", perks.meleeEfficiency));
        event.getRegistry().register(new AqtweaksTrait("ranged_efficiency", perks.rangedEfficiency));
        event.getRegistry().register(new AqtweaksTrait("shield_efficiency", perks.shieldEfficiency));
        event.getRegistry().register(new AqtweaksTrait("adrenaline", perks.adrenaline));
        event.getRegistry().register(new AqtweaksTrait("expert_climber", perks.expertClimber));
        event.getRegistry().register(new AqtweaksTrait("cardio_master", perks.cardioMaster));
        event.getRegistry().register(new AqtweaksTrait("mining_expert", perks.miningExpert));
        event.getRegistry().register(new AqtweaksTrait("evasion", perks.evasion));
        event.getRegistry().register(new AqtweaksTrait("respite", perks.respite));
        event.getRegistry().register(new AqtweaksTrait("power_attack", perks.powerAttack));
        event.getRegistry().register(new AqtweaksTrait("armor_mastery", perks.armorMastery));
        event.getRegistry().register(new AqtweaksTrait("mining_efficiency", perks.miningEfficiency));
        event.getRegistry().register(new AqtweaksTrait("precision_shot", perks.precisionShot));
        event.getRegistry().register(new AqtweaksTrait("herbalist", perks.herbalist));
        event.getRegistry().register(new AqtweaksTrait("gathering_efficiency", perks.gatheringEfficiency));
        event.getRegistry().register(new AqtweaksTrait("bountiful_harvest", perks.bountifulHarvest));
        event.getRegistry().register(new AqtweaksTrait("rancher", perks.rancher));
        event.getRegistry().register(new AqtweaksTrait("drafter", perks.drafter));
        event.getRegistry().register(new AqtweaksTrait("sculptor", perks.sculptor));
        event.getRegistry().register(new AqtweaksTrait("transpose", perks.transpose));
        event.getRegistry().register(new AqtweaksTrait("vis_thrift", perks.visThrift));
        event.getRegistry().register(new AqtweaksTrait("quiet_mind", perks.quietMind));
        event.getRegistry().register(new AqtweaksTrait("glass_cutter", perks.glassCutter));
        event.getRegistry().register(new AqtweaksTrait("herd_abundance", perks.herdAbundance));
        event.getRegistry().register(new AqtweaksTrait("iron_gut", perks.ironGut));
        event.getRegistry().register(new AqtweaksTrait("water_collector", perks.waterCollector));
        event.getRegistry().register(new AqtweaksTrait("full_font", perks.fullFont));
        event.getRegistry().register(new AqtweaksTrait("blood_pact", perks.bloodPact));
        event.getRegistry().register(new AqtweaksTrait("druid", perks.druid));
        event.getRegistry().register(new AqtweaksTrait("mana_veil", perks.manaVeil));
        event.getRegistry().register(new AqtweaksTrait("living_edge", perks.livingEdge));
        event.getRegistry().register(new AqtweaksTrait("witch", perks.witch));
        event.getRegistry().register(new AqtweaksTrait("cold_iron_mind", perks.coldIronMind));
        event.getRegistry().register(new AqtweaksTrait("stitch", perks.stitch));
        event.getRegistry().register(new AqtweaksTrait("astromancer", perks.astromancer));
        event.getRegistry().register(new AqtweaksTrait("astral_warmth", perks.astralWarmth));
        event.getRegistry().register(new AqtweaksTrait("star_powered", perks.starPowered));
        event.getRegistry().register(new AqtweaksTrait("artificer", perks.artificer));
        event.getRegistry().register(new AqtweaksTrait("live_spark", perks.liveSpark));
        event.getRegistry().register(new AqtweaksTrait("cinder_ward", perks.cinderWard));
        event.getRegistry().register(new AqtweaksTrait("benevolent", perks.benevolent));
        event.getRegistry().register(new AqtweaksTrait("dark_vision", perks.darkVision));
        event.getRegistry().register(new AqtweaksTrait("spelunker", perks.spelunker));
        event.getRegistry().register(new AqtweaksTrait("tunnel_sense", perks.tunnelSense));
        event.getRegistry().register(new AqtweaksTrait("prospector", perks.prospector));
        event.getRegistry().register(new AqtweaksTrait("motherlode", perks.motherlode));
        event.getRegistry().register(new AqtweaksTrait("lithomancy", perks.lithomancy));
        event.getRegistry().register(new AqtweaksTrait("stone_cleaver", perks.stoneCleaver));
        event.getRegistry().register(new AqtweaksTrait("lumberjack", perks.lumberjack));
        event.getRegistry().register(new AqtweaksTrait("reforester", perks.reforester));
        event.getRegistry().register(new AqtweaksTrait("sifter", perks.sifter));
        event.getRegistry().register(new AqtweaksTrait("wood_splitter", perks.woodSplitter));
        event.getRegistry().register(new AqtweaksTrait("orchard", perks.orchard));
        event.getRegistry().register(new AqtweaksTrait("sower", perks.sower));
        event.getRegistry().register(new AqtweaksTrait("husbandry", perks.husbandry));
        event.getRegistry().register(new AqtweaksTrait("hearty_meal", perks.heartyMeal));
        event.getRegistry().register(new AqtweaksTrait("seed_harvester", perks.seedHarvester));
        event.getRegistry().register(new AqtweaksTrait("finisher", perks.finisher));
        event.getRegistry().register(new AqtweaksTrait("aura_breaker", perks.auraBreaker));
        event.getRegistry().register(new AqtweaksTrait("bleeding_edge", perks.bleedingEdge));
        event.getRegistry().register(new AqtweaksTrait("pinning_shot", perks.pinningShot));
        event.getRegistry().register(new AqtweaksTrait("opportunistic", perks.opportunistic));
        event.getRegistry().register(new AqtweaksTrait("fortify", perks.fortify));
        event.getRegistry().register(new AqtweaksTrait("unyielding", perks.unyielding));
        event.getRegistry().register(new AqtweaksTrait("awareness", perks.awareness));
        event.getRegistry().register(new AqtweaksTrait("fast_revive", perks.fastRevive));
        event.getRegistry().register(new AqtweaksTrait("taunt", perks.taunt));
        event.getRegistry().register(new AqtweaksTrait("low_profile", perks.lowProfile));
        event.getRegistry().register(new AqtweaksTrait("soft_step", perks.softStep));
        event.getRegistry().register(new AqtweaksTrait("tumble", perks.tumble));
        event.getRegistry().register(new AqtweaksTrait("slow_fall", perks.slowFall));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void stampParentPerkCosts(RegistryEvent.Register<Unlockable> event) {
        IForgeRegistry<Unlockable> registry = event.getRegistry();
        stamp(registry, "reskillable", "hillwalker", 4, "reskillable:agility|32");
        stamp(registry, "reskillable", "drop_guarantee", 4, "reskillable:gathering|20", "reskillable:attack|8");
        stamp(registry, "reskillable", "battle_spirit", 3, "reskillable:attack|16", "reskillable:defense|16", "reskillable:agility|12");
        stamp(registry, "reskillable", "neutralissse", 2, "reskillable:attack|24", "reskillable:agility|8");
        stamp(registry, "reskillable", "undershirt", 2, "reskillable:defense|12", "reskillable:agility|4");
        stamp(registry, "reskillable", "effect_twist", 3, "reskillable:defense|20", "reskillable:attack|16", "reskillable:magic|16");
        stamp(registry, "elenaidodge2", "dodge", 2, "reskillable:agility|8");
        stamp(registry, "reskillable", "roadwalk", 2, "reskillable:agility|12", "reskillable:building|8");
        stamp(registry, "reskillable", "fossil_digger", 2, "reskillable:mining|6");
        stamp(registry, "reskillable", "obsidian_smasher", 2, "reskillable:mining|16");
        stamp(registry, "reskillable", "lucky_fisherman", 2, "reskillable:gathering|12", "reskillable:magic|4");
        stamp(registry, "reskillable", "more_wheat", 2, "reskillable:farming|8");
        stamp(registry, "reskillable", "green_thumb", 4, "reskillable:farming|20", "reskillable:magic|16");
        stamp(registry, "reskillable", "perfect_recover", 2, "reskillable:building|12", "reskillable:gathering|4", "reskillable:mining|6");
        stamp(registry, "reskillable", "chorus_transmute", 4, "reskillable:building|16", "reskillable:magic|16");
        stamp(registry, "reskillable", "transmutation", 4, "reskillable:building|16", "reskillable:magic|16");
        stamp(registry, "reskillable", "golden_osmosis", 2,
                "reskillable:magic|8", "reskillable:mining|6", "reskillable:gathering|6", "reskillable:attack|6");
        stamp(registry, "reskillable", "safe_port", 2,
                "reskillable:magic|12", "reskillable:agility|12", "reskillable:defense|12");

        var perks = ArcanaQuestTweaksConfig.ReskillableModuleConfig.perks;
        restampTraits(registry, "druid", perks.druid);
        restampTraits(registry, "witch", perks.witch);
        restampTraits(registry, "astromancer", perks.astromancer);
        restampTraits(registry, "artificer", perks.artificer);
        restampTraits(registry, "mana_veil", perks.manaVeil);
        restampTraits(registry, "living_edge", perks.livingEdge);
        restampTraits(registry, "cold_iron_mind", perks.coldIronMind);
        restampTraits(registry, "stitch", perks.stitch);
        restampTraits(registry, "astral_warmth", perks.astralWarmth);
        restampTraits(registry, "star_powered", perks.starPowered);
        restampTraits(registry, "live_spark", perks.liveSpark);
        restampTraits(registry, "cinder_ward", perks.cinderWard);
        restampTraits(registry, "fortify", perks.fortify);
        restampTraits(registry, "taunt", perks.taunt);
        restampTraits(registry, "low_profile", perks.lowProfile);
    }

    private static void stamp(IForgeRegistry<Unlockable> registry, String namespace, String path, int cost, String... reqs) {
        Unlockable trait = registry.getValue(new ResourceLocation(namespace, path));
        if (trait == null) return;
        trait.getUnlockableConfig().setCost(cost);
        trait.getUnlockableConfig().setRequirementHolder(RequirementHolder.fromStringList(reqs));
    }

    /** CAD resolves {@code trait|} during construction, before later traits exist. */
    private static void restampTraits(IForgeRegistry<Unlockable> registry, String path, ReskillablePerkLayout layout) {
        if (layout == null || layout.requirements == null) return;
        Unlockable trait = registry.getValue(new ResourceLocation("aqtweaks", path));
        if (trait == null) return;
        trait.getUnlockableConfig().setCost(layout.cost);
        trait.getUnlockableConfig().setRequirementHolder(RequirementHolder.fromStringList(layout.requirements));
    }
}
