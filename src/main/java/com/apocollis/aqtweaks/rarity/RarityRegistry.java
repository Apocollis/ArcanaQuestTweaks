package com.apocollis.aqtweaks.rarity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import net.minecraft.item.EnumRarity;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.EnumHelper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Adds the Legendary and Mythic {@link EnumRarity} constants and recolors the existing ones. */
public final class RarityRegistry {
    private static final Logger LOG = LogManager.getLogger("AQTweaks-Rarity");

    private RarityRegistry() {}

    private static EnumRarity legendary;
    private static EnumRarity mythic;
    private static boolean added;
    private static Field colorField;
    private static boolean colorFieldSearched;

    /** Adds the two constants once. If Forge's EnumHelper fails they stay null and callers fall back to Epic. */
    public static synchronized void addTiers(TextFormatting legendaryColor) {
        if (added) return;
        added = true;
        try {
            legendary = EnumHelper.addRarity("LEGENDARY", legendaryColor, "Legendary");
            mythic = EnumHelper.addRarity("MYTHIC", legendaryColor, "Mythic");
        } catch (Throwable t) {
            LOG.error("Could not add the Legendary and Mythic rarities; those tiers will show as Epic.", t);
        }
    }

    /** The vanilla or added constant for a tier. Legendary and Mythic fall back to Epic if they could not be added. */
    public static EnumRarity forTier(RarityTier tier) {
        switch (tier) {
            case COMMON:
                return EnumRarity.COMMON;
            case UNCOMMON:
                return EnumRarity.UNCOMMON;
            case RARE:
                return EnumRarity.RARE;
            case EPIC:
                return EnumRarity.EPIC;
            case LEGENDARY:
                return legendary != null ? legendary : EnumRarity.EPIC;
            default:
                return mythic != null ? mythic : EnumRarity.EPIC;
        }
    }

    /** The tier for a rarity constant, or null for an unknown constant a mod added. */
    public static RarityTier tierOf(EnumRarity rarity) {
        if (rarity == null) return null;
        try {
            return RarityTier.valueOf(rarity.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Sets the display color of a tier on its constant (the TextFormatting field of EnumRarity). */
    public static void recolor(RarityTier tier, TextFormatting color) {
        EnumRarity target = forTier(tier);
        if (tier == RarityTier.LEGENDARY && legendary == null) return;
        if (tier == RarityTier.MYTHIC && mythic == null) return;
        Field f = colorField();
        if (f == null) return;
        try {
            f.set(target, color);
        } catch (Throwable t) {
            LOG.warn("Could not recolor {}: {}", tier, t.toString());
        }
    }

    private static synchronized Field colorField() {
        if (colorFieldSearched) return colorField;
        colorFieldSearched = true;
        // The field name differs between mapped and runtime builds, so find it by type.
        for (Field f : EnumRarity.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && f.getType() == TextFormatting.class) {
                try {
                    f.setAccessible(true);
                    colorField = f;
                    break;
                } catch (Throwable t) {
                    LOG.warn("Could not open the EnumRarity color field; vanilla rarity colors stay: {}", t.toString());
                }
            }
        }
        if (colorField == null) LOG.warn("EnumRarity has no color field to recolor; vanilla rarity colors stay.");
        return colorField;
    }
}
