package com.apocollis.aqtweaks.twilightforest;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.Loader;

import java.util.ArrayList;
import java.util.List;

/**
 * Charges carried stored-energy items during a surge: Embers items with the ember capability, Botania mana items,
 * Thaumcraft rechargeable items ({@code IRechargable}) and Thaumic Augmentation caster vis batteries (installed in a
 * caster gauntlet or held as the augment). It scans the main inventory, offhand, worn armor and Baubles slots. Each
 * parent is behind {@code Loader.isModLoaded} in a nested holder, so an absent mod is skipped. Placed Thaumcraft Vis
 * Batteries are handled by {@link VisBatteryTracker}.
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

    /** Thaumcraft rechargeable items (rings, amulets, charms, harness, Runic and Vis gear...), charged without draining aura. */
    private static final class Thaumcraft {
        static boolean charge(ItemStack stack, EntityPlayer player, double percent, double exposure, double intensity) {
            if (!(stack.getItem() instanceof thaumcraft.api.items.IRechargable)) return false;
            thaumcraft.api.items.IRechargable item = (thaumcraft.api.items.IRechargable) stack.getItem();
            int max = item.getMaxCharge(stack, player);
            if (max <= 0) return false;
            int have = thaumcraft.api.items.RechargeHelper.getCharge(stack);
            if (have >= max) return false;
            int amount = (int) Math.round(SurgeMath.chargeAmount(max, percent, exposure, intensity));
            amount = Math.min(amount, max - have);
            if (amount <= 0) return false;
            thaumcraft.api.items.RechargeHelper.rechargeItemBlindly(stack, player, amount);
            return true;
        }
    }

    /** Thaumic Augmentation caster vis batteries, installed in a caster gauntlet or held as the augment item. */
    private static final class Augments {
        static boolean charge(ItemStack stack, double percent, double exposure, double intensity) {
            boolean any = chargeAugment(stack, percent, exposure, intensity);
            thecodex6824.thaumicaugmentation.api.augment.IAugmentableItem gauntlet = stack.getCapability(
                    thecodex6824.thaumicaugmentation.api.augment.CapabilityAugmentableItem.AUGMENTABLE_ITEM, null);
            if (gauntlet != null) {
                for (ItemStack augment : gauntlet.getAllAugments()) {
                    if (augment != null && !augment.isEmpty()) {
                        any |= chargeAugment(augment, percent, exposure, intensity);
                    }
                }
            }
            return any;
        }

        private static boolean chargeAugment(ItemStack stack, double percent, double exposure, double intensity) {
            thecodex6824.thaumicaugmentation.api.augment.IAugment augment = stack.getCapability(
                    thecodex6824.thaumicaugmentation.api.augment.CapabilityAugment.AUGMENT, null);
            if (!(augment instanceof thecodex6824.thaumicaugmentation.common.capability.AugmentCasterVisBattery)) {
                return false;
            }
            thecodex6824.thaumicaugmentation.common.capability.AugmentCasterVisBattery battery =
                    (thecodex6824.thaumicaugmentation.common.capability.AugmentCasterVisBattery) augment;
            float max = battery.getMaxVis();
            float vis = battery.getVis();
            if (max <= 0.0F || vis >= max) return false;
            double amount = SurgeMath.chargeAmount(max, percent, exposure, intensity);
            if (amount <= 0.0D) return false;
            battery.setVis((float) Math.min((double) max, vis + amount));
            return true;
        }
    }

    private static final class Baubles {
        static List<ItemStack> stacks(EntityPlayer player) {
            List<ItemStack> out = new ArrayList<>();
            baubles.api.cap.IBaublesItemHandler handler = baubles.api.BaublesApi.getBaublesHandler(player);
            if (handler == null) return out;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) out.add(stack);
            }
            return out;
        }
    }

    /** One charging pulse over the player's inventory, offhand, worn armor and Baubles. True if any item gained charge. */
    public static boolean pulse(EntityPlayer player, double exposure, double intensity) {
        double percent = ArcanaQuestTweaksConfig.TwilightForestModuleConfig.ley.chargePercent;
        boolean embers = Loader.isModLoaded("embers");
        boolean botania = Loader.isModLoaded("botania");
        boolean thaumcraft = Loader.isModLoaded("thaumcraft");
        boolean augments = Loader.isModLoaded("thaumicaugmentation");
        if (!embers && !botania && !thaumcraft && !augments) return false;
        boolean any = false;
        any |= scan(player.inventory.mainInventory, player, embers, botania, thaumcraft, augments, percent, exposure,
                intensity);
        any |= scan(player.inventory.offHandInventory, player, embers, botania, thaumcraft, augments, percent, exposure,
                intensity);
        any |= scan(player.inventory.armorInventory, player, embers, botania, thaumcraft, augments, percent, exposure,
                intensity);
        if (Loader.isModLoaded("baubles")) {
            for (ItemStack stack : Baubles.stacks(player)) {
                any |= chargeOne(stack, player, embers, botania, thaumcraft, augments, percent, exposure, intensity);
            }
        }
        return any;
    }

    private static boolean scan(NonNullList<ItemStack> stacks, EntityPlayer player, boolean embers, boolean botania,
                                boolean thaumcraft, boolean augments, double percent, double exposure, double intensity) {
        boolean any = false;
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            any |= chargeOne(stack, player, embers, botania, thaumcraft, augments, percent, exposure, intensity);
        }
        return any;
    }

    private static boolean chargeOne(ItemStack stack, EntityPlayer player, boolean embers, boolean botania,
                                     boolean thaumcraft, boolean augments, double percent, double exposure,
                                     double intensity) {
        boolean any = false;
        if (embers && Embers.charge(stack, percent, exposure, intensity)) any = true;
        if (botania && Botania.charge(stack, percent, exposure, intensity)) any = true;
        if (thaumcraft && Thaumcraft.charge(stack, player, percent, exposure, intensity)) any = true;
        if (augments && Augments.charge(stack, percent, exposure, intensity)) any = true;
        return any;
    }
}
