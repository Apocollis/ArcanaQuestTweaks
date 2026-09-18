package com.apocollis.aqtweaks.bettermineshafts;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.yungnickyoung.minecraft.bettermineshafts.world.generator.MineshaftVariantSettings;

/**
 * Local Y range for a Start without mutating shared biome-variant settings.
 */
public final class BetterMineshaftStartSettings {
    private BetterMineshaftStartSettings() {}

    public static MineshaftVariantSettings withTweaksY(MineshaftVariantSettings shared) {
        if (shared == null || !ArcanaQuestTweaksConfig.BetterMineshaftsModuleConfig.general.enable) {
            return shared;
        }
        int min = Math.max(1, ArcanaQuestTweaksConfig.BetterMineshaftsModuleConfig.general.tunnelMinY);
        int max = Math.min(255, ArcanaQuestTweaksConfig.BetterMineshaftsModuleConfig.general.tunnelMaxY);
        if (min > max) {
            int swap = min;
            min = max;
            max = swap;
        }
        int parentMin = shared.minY == 0 ? 17 : shared.minY;
        int parentMax = shared.maxY == 0 ? 37 : shared.maxY;
        if (parentMin == min && parentMax == max) {
            return shared;
        }
        MineshaftVariantSettings copy = copy(shared);
        copy.minY = min;
        copy.maxY = max;
        return copy;
    }

    private static MineshaftVariantSettings copy(MineshaftVariantSettings src) {
        MineshaftVariantSettings copy = new MineshaftVariantSettings();
        copy.biomeTags = src.biomeTags;
        copy.mainSelector = src.mainSelector;
        copy.floorSelector = src.floorSelector;
        copy.brickSelector = src.brickSelector;
        copy.legSelector = src.legSelector;
        copy.mainBlock = src.mainBlock;
        copy.supportBlock = src.supportBlock;
        copy.slabBlock = src.slabBlock;
        copy.gravelBlock = src.gravelBlock;
        copy.stoneWallBlock = src.stoneWallBlock;
        copy.stoneSlabBlock = src.stoneSlabBlock;
        copy.minY = src.minY;
        copy.maxY = src.maxY;
        copy.vineChance = src.vineChance;
        copy.snowChance = src.snowChance;
        copy.cactusChance = src.cactusChance;
        copy.deadBushChance = src.deadBushChance;
        copy.mushroomChance = src.mushroomChance;
        copy.legVariant = src.legVariant;
        copy.replacementRate = src.replacementRate;
        return copy;
    }
}
