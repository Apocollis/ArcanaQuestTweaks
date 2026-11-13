package com.apocollis.aqtweaks.twilightforest;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.Loader;

/**
 * Charges carried stored-energy items during a surge: Embers items with the ember capability and Botania mana
 * items. Each parent is behind {@code Loader.isModLoaded} in a nested holder, so an absent mod is skipped.
 * (Thaumic Augmentation's caster vis battery is not in the pack's libs and is not handled.)
 */
public final class SurgeCharging {
    private SurgeCharging() {}

    private static final class Embers {
        static boolean charge(ItemStack stack, double percent, double exposure, double intensity) {
            teamroots.embers.api.power.IEmberCapability cap = stack.getCapability(
                    teamroots.embers.api.capabilities.EmbersCapabilities.EMBER_CAPABILITY, null);
            if (cap == null || cap.getEmberCapacity() <= 0.0D || cap.getEmber() >= cap.getEmberCapacity()) return false;
            double amount = SurgeMath.chargeAmount(cap.getEmberCapacity(), percent, exposure, intensity);
            if (amount <= 0.0D) return false;
            return cap.addAmount(amount, true) > 0.0D;
        }
    }

    private static final class Botania {
        static boolean charge(ItemStack stack, double percent, double exposure, double intensity) {
            if (!(stack.getItem() instanceof vazkii.botania.api.mana.IManaItem)) return false;
            vazkii.botania.api.mana.IManaItem item = (vazkii.botania.api.mana.IManaItem) stack.getItem();
            int max = item.getMaxMana(stack);
            if (max <= 0 || item.getMana(stack) >= max) return false;
            int amount = (int) Math.round(SurgeMath.chargeAmount(max, percent, exposure, intensity));
            if (amount <= 0) return false;
            item.addMana(stack, Math.min(amount, max - item.getMana(stack)));
            return true;
        }
    }

    /** One charging pulse over the player's inventory and offhand. True if any item gained charge. */
    public static boolean pulse(EntityPlayer player, double exposure, double intensity) {
        double percent = ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley.chargePercent;
        boolean any = false;
        boolean embers = Loader.isModLoaded("embers");
        boolean botania = Loader.isModLoaded("botania");
        if (!embers && !botania) return false;
        any |= scan(player.inventory.mainInventory, embers, botania, percent, exposure, intensity);
        any |= scan(player.inventory.offHandInventory, embers, botania, percent, exposure, intensity);
        return any;
    }

    private static boolean scan(NonNullList<ItemStack> stacks, boolean embers, boolean botania, double percent,
                                double exposure, double intensity) {
        boolean any = false;
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            if (embers && Embers.charge(stack, percent, exposure, intensity)) any = true;
            if (botania && Botania.charge(stack, percent, exposure, intensity)) any = true;
        }
        return any;
    }
}
