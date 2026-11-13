package com.apocollis.aqtweaks.twilightforest;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.weather.Wildcard;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

/**
 * Decides whether an item is a magic item for the spell backlash: a Thaumcraft {@code ICaster}, a Botania
 * mana-using item, or a registry-name match on {@code Magic Item Patterns}. The parent checks live in nested holder
 * classes so an absent mod is never touched.
 */
public final class ArcaneItemClassifier {
    private ArcaneItemClassifier() {}

    private static final class Thaumcraft {
        static boolean isCaster(ItemStack stack) {
            return stack.getItem() instanceof thaumcraft.api.casters.ICaster;
        }
    }

    private static final class BotaniaCheck {
        static boolean usesMana(ItemStack stack) {
            return stack.getItem() instanceof vazkii.botania.api.mana.IManaUsingItem
                    && ((vazkii.botania.api.mana.IManaUsingItem) stack.getItem()).usesMana(stack);
        }
    }

    /** Pure name match, used by the classifier and its test. */
    public static boolean matchesPatterns(String registryName, String[] patterns) {
        return registryName != null && Wildcard.matchesAny(registryName, patterns);
    }

    /** The reason the stack counts as a magic item ({@code caster}, {@code mana}, {@code pattern}), or null. */
    public static String reason(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (Loader.isModLoaded("thaumcraft") && Thaumcraft.isCaster(stack)) return "caster";
        if (Loader.isModLoaded("botania") && BotaniaCheck.usesMana(stack)) return "mana";
        ResourceLocation reg = stack.getItem().getRegistryName();
        if (reg != null && matchesPatterns(reg.toString(),
                ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley.magicItemPatterns)) {
            return "pattern";
        }
        return null;
    }

    public static boolean isCaster(ItemStack stack) {
        return stack != null && !stack.isEmpty() && Loader.isModLoaded("thaumcraft") && Thaumcraft.isCaster(stack);
    }
}
