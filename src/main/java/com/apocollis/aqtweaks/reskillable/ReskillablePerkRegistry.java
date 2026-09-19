package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import codersafterdark.reskillable.api.unlockable.Unlockable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ReskillablePerkRegistry {

    public static void preInit() {
        MinecraftForge.EVENT_BUS.register(new ReskillablePerkRegistry());
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
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void stampHillwalkerCost(RegistryEvent.Register<Unlockable> event) {
        Unlockable hill = event.getRegistry().getValue(new ResourceLocation("reskillable", "hillwalker"));
        if (hill != null) {
            hill.getUnlockableConfig().setCost(6);
        }
    }
}
