package com.apocollis.aqtweaks.thaumcraft.battlemage;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.BattlemageConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * What a player-killed Battlemage drops (plan 13 to 13f): the tier's loot table, one worn gear piece and one faked
 * bauble (a second of each with Looting), and two distinct weighted foci cloned from its own loadout. Every variant
 * uses its tier's table and its own loadout's foci (plan 18.9). Vanilla equipment drop chances stay 0.
 */
public final class BattlemageDrops {

    private static final Logger LOG = LogManager.getLogger("AQTweaks-Battlemage");
    private static final String[] GEAR_SLOTS = {"head", "chest", "legs", "feet", "mainhand", "offhand"};
    private static final EntityEquipmentSlot[] SLOT_ENUMS = {EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
            EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET, EntityEquipmentSlot.MAINHAND, EntityEquipmentSlot.OFFHAND};

    private BattlemageDrops() {}

    public static void onDrops(EntityCultistBattlemage mage, LivingDropsEvent event) {
        if (mage.world.isRemote || !BattlemageConfig.drops || !event.isRecentlyHit()) {
            return;
        }
        TierDef tier = FocusLibrary.data().tier(mage.getTier());
        if (tier == null) {
            return;
        }
        Random rnd = mage.getRNG();
        int looting = event.getLootingLevel();
        List<ItemStack> stacks = new ArrayList<>();
        tableDrops(tier, looting, rnd, stacks);
        gearDrops(mage, looting, rnd, stacks);
        baubleDrops(tier, looting, rnd, stacks);
        focusDrops(mage, rnd, stacks);
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            EntityItem item = new EntityItem(mage.world, mage.posX, mage.posY + 0.5D, mage.posZ, stack);
            item.setDefaultPickupDelay();
            event.getDrops().add(item);
        }
    }

    private static void tableDrops(TierDef tier, int looting, Random rnd, List<ItemStack> out) {
        for (DropEntry d : tier.drops) {
            double chance = DropRules.lootedChance(d.chance, d.chance >= 1.0D ? 0 : looting, BattlemageConfig.tableLootingChance);
            if (rnd.nextDouble() >= chance) {
                continue;
            }
            int count = DropRules.count(d.min, d.max, looting, rnd);
            ItemStack stack = build(d);
            if (stack.isEmpty() || count <= 0) {
                continue;
            }
            stack.setCount(Math.min(count, stack.getMaxStackSize()));
            out.add(stack);
        }
    }

    private static void gearDrops(EntityCultistBattlemage mage, int looting, Random rnd, List<ItemStack> out) {
        List<ItemStack> worn = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        for (int i = 0; i < GEAR_SLOTS.length; i++) {
            ItemStack stack = mage.getItemStackFromSlot(SLOT_ENUMS[i]);
            if (!stack.isEmpty()) {
                worn.add(stack);
                weights.add(DropRules.defaultGearWeight(GEAR_SLOTS[i]));
            }
        }
        if (worn.isEmpty()) {
            return;
        }
        int n = DropRules.pieces(BattlemageConfig.gearExtraChancePerLooting, looting, 2, rnd);
        double[] w = new double[weights.size()];
        for (int i = 0; i < w.length; i++) {
            w[i] = weights.get(i);
        }
        for (int idx : DropRules.pickDistinct(w, n, rnd)) {
            out.add(worn.get(idx).copy());
        }
    }

    private static void baubleDrops(TierDef tier, int looting, Random rnd, List<ItemStack> out) {
        if (tier.baubleDrops.isEmpty()) {
            return;
        }
        int n = DropRules.pieces(BattlemageConfig.baubleExtraChancePerLooting, looting, 2, rnd);
        double[] w = new double[tier.baubleDrops.size()];
        for (int i = 0; i < w.length; i++) {
            w[i] = tier.baubleDrops.get(i).weight;
        }
        for (int idx : DropRules.pickDistinct(w, n, rnd)) {
            DropEntry d = tier.baubleDrops.get(idx);
            ItemStack stack = build(d);
            if (!stack.isEmpty()) {
                out.add(stack);
            }
        }
    }

    /** Two distinct foci from the mage's own loadout, weighted 1 / 3 / 5 (plan 13f); no Looting. */
    private static void focusDrops(EntityCultistBattlemage mage, Random rnd, List<ItemStack> out) {
        List<BattlemageFocus> foci = mage.getFoci();
        if (foci.isEmpty()) {
            return;
        }
        double[] w = new double[foci.size()];
        for (int i = 0; i < w.length; i++) {
            FocusDef d = foci.get(i).def;
            boolean fixed = "fixed".equals(d.slot) || "none".equals(d.slot);
            w[i] = DropRules.focusWeight(d.dropWeight, fixed, d.minTier, BattlemageConfig.fixedFocusDropWeight,
                    BattlemageConfig.rolledFocusDropWeight, BattlemageConfig.highTierFocusDropWeight);
        }
        for (int idx : DropRules.pickDistinct(w, 2, rnd)) {
            ItemStack stack = focusStack(foci.get(idx).def);
            if (!stack.isEmpty()) {
                out.add(stack);
            }
        }
    }

    /** The authored item and tag of a loadout focus, rebuilt from its CraftTweaker line. */
    static ItemStack focusStack(FocusDef def) {
        CtFocusLine line = CtFocusLine.parse(def.nbt);
        if (line == null) {
            return ItemStack.EMPTY;
        }
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(line.itemId));
        if (item == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        if (line.nbt != null) {
            try {
                stack.setTagCompound(JsonToNBT.getTagFromJson(line.nbt));
            } catch (NBTException e) {
                LOG.warn("Battlemage focus drop {} has a bad tag: {}", def.id, e.getMessage());
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    private static ItemStack build(DropEntry d) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(d.item));
        if (item == null) {
            LOG.warn("Battlemage drop {} is not a registered item", d.item);
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item, 1, d.meta);
        if (d.nbt != null && !d.nbt.isEmpty()) {
            try {
                NBTTagCompound tag = JsonToNBT.getTagFromJson(d.nbt);
                stack.setTagCompound(tag);
            } catch (NBTException e) {
                LOG.warn("Battlemage drop {} has a bad tag: {}", d.item, e.getMessage());
            }
        }
        return stack;
    }
}
