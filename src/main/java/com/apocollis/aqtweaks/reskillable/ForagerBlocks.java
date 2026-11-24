package com.apocollis.aqtweaks.reskillable;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.IPlantable;

/**
 * What the Forager perk treats as a plant (plan 20261120-1700, section 17): anything that is a bush or plantable
 * (tall grass, ferns, flowers, saplings, mushrooms, crops, lily pads, stems, cactus), plant or vine material, a fully
 * grown pumpkin or melon, or a registry name on {@code Forager Free Blocks}. Logs, leaves and gravel are not plants.
 */
public final class ForagerBlocks {

    private ForagerBlocks() {}

    public static boolean isPlant(IBlockState state) {
        if (state == null) {
            return false;
        }
        Block block = state.getBlock();
        if (block instanceof BlockBush || block instanceof IPlantable) {
            return true;
        }
        Material material = state.getMaterial();
        if (material == Material.PLANTS || material == Material.VINE) {
            return true;
        }
        try {
            if (ReskillableBonuses.isMatureCrop(state)) {
                return true;
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Reskillable classes unavailable: the plain tests above stand.
        }
        ResourceLocation key = Block.REGISTRY.getNameForObject(block);
        return key != null && matchesAny(key.toString(), ArcanaQuestTweaksConfig.StaminaModuleConfig.mining.foragerFreeBlocks);
    }

    /** Registry-name patterns with {@code *} wildcards, case-insensitive. */
    public static boolean matchesAny(String name, String[] patterns) {
        if (name == null || patterns == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (String pattern : patterns) {
            if (pattern == null || pattern.trim().isEmpty()) {
                continue;
            }
            StringBuilder regex = new StringBuilder();
            String[] parts = pattern.trim().toLowerCase(Locale.ROOT).split("[*]", -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    regex.append(".*");
                }
                regex.append(Pattern.quote(parts[i]));
            }
            if (lower.matches(regex.toString())) {
                return true;
            }
        }
        return false;
    }
}
