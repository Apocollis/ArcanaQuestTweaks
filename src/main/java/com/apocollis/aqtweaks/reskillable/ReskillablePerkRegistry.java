package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import codersafterdark.reskillable.api.unlockable.Unlockable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
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
    }
}
