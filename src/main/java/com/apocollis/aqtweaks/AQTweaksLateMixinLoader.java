package com.apocollis.aqtweaks;

import net.minecraftforge.fml.common.Loader;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.util.Arrays;
import java.util.List;

public class AQTweaksLateMixinLoader implements ILateMixinLoader {
    @Override
    public List<String> getMixinConfigs() {
        return Arrays.asList(
                "mixins.aqtweaks.json",
                "mixins.aqtweaks.vanilla.json",
                "mixins.aqtweaks.grapple.json",
                "mixins.aqtweaks.dss.json",
                "mixins.aqtweaks.elenaidodge.json",
                "mixins.aqtweaks.baubles.json",
                "mixins.aqtweaks.toughnessbar.json",
                "mixins.aqtweaks.astral.json",
                "mixins.aqtweaks.bewitchment.json",
                "mixins.aqtweaks.mysticalworld.json",
                "mixins.aqtweaks.biomesoplenty.json",
                "mixins.aqtweaks.da.json",
                "mixins.aqtweaks.depthsupdate.json",
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
                "mixins.aqtweaks.embers.json",
                "mixins.aqtweaks.playerrevive.json",
                "mixins.aqtweaks.prospectus.json",
                "mixins.aqtweaks.simpletomb.json",
                "mixins.aqtweaks.treechopper.json",
                "mixins.aqtweaks.sereneseasons.json",
                "mixins.aqtweaks.libraryex.json",
                "mixins.aqtweaks.geographicraft.json",
                "mixins.aqtweaks.atum.json",
                "mixins.aqtweaks.dsurround.json");
    }

    @Override
    public boolean shouldMixinConfigQueue(String mixinConfig) {
        if ("mixins.aqtweaks.geographicraft.json".equals(mixinConfig)) {
            return Loader.isModLoaded("geographicraft");
        }
        if ("mixins.aqtweaks.atum.json".equals(mixinConfig)) {
            return Loader.isModLoaded("atum");
        }
        if ("mixins.aqtweaks.dsurround.json".equals(mixinConfig)) {
            return Loader.isModLoaded("dsurround");
        }
        return true;
    }
}
