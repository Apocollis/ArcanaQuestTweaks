package com.apocollis.aqtweaks.stamina;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.reskillable.PerkAccess;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;

import java.util.Locale;
import java.util.Map;

/**
 * Elenai Dodge 2 Extended feather / weight helpers (formerly in {@code Reflect}). Elenai is
 * {@code required-after}, so these call its API directly.
 */
public final class StaminaFeathers {

    private static Map<Item, Double> weightCache = new java.util.HashMap<>();
    private static String[] lastWeightsArray;

    private StaminaFeathers() {}

    public static boolean isRopeBlock(Block block) {
        if (block == null || block.getRegistryName() == null) return false;
        String name = block.getRegistryName().toString().toLowerCase(Locale.ROOT);
        return name.contains("rope");
    }

    public static int getAbsorptionFeathers(EntityPlayer player) {
        if (player.world.isRemote) {
            return com.elenai.elenaidodge2.util.ClientStorage.absorption;
        }
        com.elenai.elenaidodge2.capability.absorption.IAbsorption cap = player.getCapability(
                com.elenai.elenaidodge2.capability.absorption.AbsorptionProvider.ABSORPTION_CAP, null);
        return cap != null ? cap.getAbsorption() : 0;
    }

    public static void invalidateWeightCache() {
        lastWeightsArray = null;
    }

    public static void decreaseFeathers(EntityPlayerMP player, int amount) {
        com.elenai.elenaidodge2.api.FeathersHelper.decreaseFeathers(player, amount);
        com.elenai.elenaidodge2.network.PacketHandler.instance.sendTo(
            new com.elenai.elenaidodge2.network.message.CUpdateAbsorptionMessage(getAbsorptionFeathers(player)),
            player
        );
    }

    public static int getBaseWeight(EntityPlayer player) {
        String[] weights = com.elenai.elenaidodge2.ModConfig.common.weights.weights;
        if (weights == null || weights.length == 0) return 0;
        if (weights != lastWeightsArray) {
            Map<Item, Double> rebuilt = new java.util.HashMap<>();
            for (String entry : weights) {
                String[] itemAndVal = entry.split("=");
                if (itemAndVal.length < 2) continue;
                Item item = Item.getByNameOrId(itemAndVal[0]);
                if (item == null || rebuilt.containsKey(item)) continue;
                rebuilt.put(item, Double.parseDouble(itemAndVal[1]));
            }
            weightCache = rebuilt;
            lastWeightsArray = weights;
        }

        double totalWeight = 0.0;
        Double head = weightCache.get(player.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.HEAD).getItem());
        Double chest = weightCache.get(player.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.CHEST).getItem());
        Double legs = weightCache.get(player.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.LEGS).getItem());
        Double feet = weightCache.get(player.getItemStackFromSlot(net.minecraft.inventory.EntityEquipmentSlot.FEET).getItem());
        if (head != null) totalWeight += head;
        if (chest != null) totalWeight += chest;
        if (legs != null) totalWeight += legs;
        if (feet != null) totalWeight += feet;

        int intWeight = (int) Math.round(totalWeight);
        int lightweightLevel = com.elenai.elenaidodge2.util.Utils.getTotalEnchantmentLevel(
            com.elenai.elenaidodge2.init.EnchantmentInit.LIGHTWEIGHT, player
        );
        intWeight -= lightweightLevel;
        return intWeight;
    }

    public static int getWeight(EntityPlayer player) {
        if (player.isPotionActive(com.elenai.elenaidodge2.init.PotionInit.WEIGHT_EFFECT)) {
            if (player.world.isRemote) {
                return com.elenai.elenaidodge2.util.ClientStorage.weight;
            }
            if (player instanceof EntityPlayerMP) {
                return com.elenai.elenaidodge2.api.FeathersHelper.getWeight((EntityPlayerMP) player);
            }
            return 200;
        }

        int weight = getBaseWeight(player);
        PotionEffect endurance = player.getActivePotionEffect(com.elenai.elenaidodge2.init.PotionInit.ENDURANCE_EFFECT);
        if (endurance != null) {
            weight -= (endurance.getAmplifier() + 1) * 4;
        }

        boolean halfFeathers = com.elenai.elenaidodge2.ModConfig.common.feathers.half;
        if (!halfFeathers) {
            weight = (int) (Math.floor(weight / 2.0) * 2);
        }

        if (ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable.enableReskillable &&
            PerkAccess.has(player, ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable.armorMasteryPerkId)) {
            int pieces = 0;
            for (ItemStack armor : player.getArmorInventoryList()) {
                if (armor != null && !armor.isEmpty()) {
                    pieces++;
                }
            }
            int reduction = (int) Math.round(pieces * ArcanaQuestTweaksConfig.StaminaModuleConfig.reskillable.armorMasteryReductionPerPiece);
            weight -= reduction;
        }
        return Math.max(0, weight);
    }

    public static boolean hasEnoughStamina(EntityPlayer player, int cost) {
        if (player.capabilities.isCreativeMode) return true;
        int dodges = 0;
        if (player.world.isRemote) {
            dodges = com.elenai.elenaidodge2.util.ClientStorage.dodges;
        } else if (player instanceof net.minecraft.entity.player.EntityPlayerMP) {
            dodges = com.elenai.elenaidodge2.api.FeathersHelper.getFeatherLevel((net.minecraft.entity.player.EntityPlayerMP) player);
        }
        int absorption = getAbsorptionFeathers(player);
        int weight = getWeight(player);

        // Model Elenai's spend order: absorption first, overflow to dodges
        int remainingCost = cost;

        // Absorption absorbs as much as it can
        if (absorption > 0) {
            int absorbedByGold = Math.min(absorption, remainingCost);
            remainingCost -= absorbedByGold;
        }

        // Whatever is left comes out of regular dodges
        int dodgesAfterSpend = dodges - remainingCost;

        // Must not go below 0
        if (dodgesAfterSpend < 0) return false;

        // Must not go below weight threshold (iron feathers)
        if (dodgesAfterSpend < weight) return false;

        return true;
    }
}
