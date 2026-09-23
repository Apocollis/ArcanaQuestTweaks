package com.apocollis.aqtweaks;

import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Arrays;
import java.util.List;

public class AQTweaksLateMixinLoader implements ILateMixinLoader {
    @Override
    public List<String> getMixinConfigs() {
        return Arrays.asList(
                "mixins.aqtweaks.json",
                "mixins.aqtweaks.grapple.json",
                "mixins.aqtweaks.dss.json",
                "mixins.aqtweaks.toughnessbar.json",
                "mixins.aqtweaks.astral.json",
                "mixins.aqtweaks.bewitchment.json",
                "mixins.aqtweaks.mysticalworld.json",
                "mixins.aqtweaks.biomesoplenty.json",
                "mixins.aqtweaks.gaia.json",
                "mixins.aqtweaks.effortlessbuilding.json",
                "mixins.aqtweaks.thaumcraft.json",
                "mixins.aqtweaks.simpledifficulty.json",
                "mixins.aqtweaks.rustic.json",
                "mixins.aqtweaks.animania.json",
                "mixins.aqtweaks.somnia.json",
                "mixins.aqtweaks.incontrol.json",
                "mixins.aqtweaks.bettermineshafts.json",
                "mixins.aqtweaks.randomportals.json",
                "mixins.aqtweaks.chisel.json",
                "mixins.aqtweaks.recipestages.json",
                "mixins.aqtweaks.qualitytools.json",
                "mixins.aqtweaks.botania.json",
                "mixins.aqtweaks.embers.json");
    }
}
