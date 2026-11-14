package com.apocollis.aqtweaks.weather;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.stamina.StaminaFeathers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared client wind-push resistance across all wind-pushing dimension hazards (Aether squalls, Atum sandstorms,
 * Aurorian Moonfrost). Combines Elenai Dodge 2 iron-feather weight before Armor Mastery (via
 * {@link StaminaFeathers#getWeightBeforeMastery}, keeping Lightweight/Endurance/Weight potion), aerodynamic
 * {@code Wind Resistant} gear, grounded-only {@code Anchoring} boots, and sneaking/crouching additively up to
 * 100% push reduction (push multiplier floor {@code 0.0}).
 */
public final class WindResistanceGear {

    private static final int CACHE_TTL_TICKS = 10;

    private static final class LiveSlots {
        static final EntityEquipmentSlot[] ARMOR = {
                EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST, EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET
        };
    }

    private static final class CachedGear {
        long tick;
        final Item[] items = new Item[4];
        final int[] metas = new int[4];
        int windResistantPieces;
        boolean hasAnchoringBoots;
        double weight;
    }

    private static final Map<UUID, CachedGear> GEAR_CACHE = new ConcurrentHashMap<>();

    private WindResistanceGear() {}

    public static void invalidateCache() {
        GEAR_CACHE.clear();
    }

    // ---- pure core (no Minecraft types, unit tested) ----

    public static double weightReduction(double weightHalfFeathers, double weightThreshold, double maxWeightRef,
                                         double maxWeightReduction) {
        double maxRed = clamp(maxWeightReduction, 0.0D, 1.0D);
        if (weightHalfFeathers <= weightThreshold || maxRed <= 0.0D) {
            return 0.0D;
        }
        if (maxWeightRef <= weightThreshold) {
            return maxRed;
        }
        double t = clamp((weightHalfFeathers - weightThreshold) / (maxWeightRef - weightThreshold), 0.0D, 1.0D);
        return t * maxRed;
    }

    public static double pushFactor(double weightHalfFeathers, int windResistantPieces, boolean hasAnchoringBoots,
                                    boolean onGround, boolean sneaking, double sneakReduction, boolean enableWindGear,
                                    boolean enableWeightResistance, double weightThreshold, double maxWeightRef,
                                    double maxWeightReduction, double fullSetReduction, double bootGroundedReduction) {
        double sneakRed = sneaking ? clamp(sneakReduction, 0.0D, 1.0D) : 0.0D;
        if (!enableWindGear) {
            return clamp(1.0D - sneakRed, 0.0D, 1.0D);
        }
        int pieces = Math.max(0, Math.min(4, windResistantPieces));
        double wRed = enableWeightResistance
                ? weightReduction(weightHalfFeathers, weightThreshold, maxWeightRef, maxWeightReduction)
                : 0.0D;
        double setRed = Math.max(0.0D, fullSetReduction) * (pieces / 4.0D);
        double bootRed = (onGround && hasAnchoringBoots) ? Math.max(0.0D, bootGroundedReduction) : 0.0D;
        double totalReduction = wRed + setRed + bootRed + sneakRed;
        return clamp(1.0D - totalReduction, 0.0D, 1.0D);
    }

    private static double clamp(double value, double min, double max) {
        return value < min ? min : (value > max ? max : value);
    }

    // ---- live player wrapper (client wind push) ----

    public static double factor(EntityPlayer player, double sneakReduction) {
        if (player == null) return 1.0D;
        ArcanaQuestTweaksConfig.WindGear wind = ArcanaQuestTweaksConfig.HazardGearConfig.wind;
        boolean sneaking = player.isSneaking();
        if (!wind.enableWindGear) {
            return clamp(1.0D - (sneaking ? clamp(sneakReduction, 0.0D, 1.0D) : 0.0D), 0.0D, 1.0D);
        }
        CachedGear gear = cachedGearFor(player, wind);
        return pushFactor(
                gear.weight,
                gear.windResistantPieces,
                gear.hasAnchoringBoots,
                player.onGround,
                sneaking,
                sneakReduction,
                true,
                wind.enableWeightPushResistance,
                wind.weightReductionThreshold,
                wind.weightFullReductionAt,
                wind.maxWeightPushReduction,
                wind.windResistantFullSetPushReduction,
                wind.anchoringGroundedPushReduction
        );
    }

    private static CachedGear cachedGearFor(EntityPlayer player, ArcanaQuestTweaksConfig.WindGear wind) {
        long now = player.world != null ? player.world.getTotalWorldTime() : player.ticksExisted;
        UUID id = player.getUniqueID();
        CachedGear cached = GEAR_CACHE.get(id);
        boolean matches = cached != null && now >= cached.tick && now - cached.tick < CACHE_TTL_TICKS;
        if (matches) {
            for (int i = 0; i < 4; i++) {
                ItemStack stack = player.getItemStackFromSlot(LiveSlots.ARMOR[i]);
                Item item = stack.isEmpty() ? null : stack.getItem();
                int meta = stack.isEmpty() ? 0 : stack.getMetadata();
                if (cached.items[i] != item || cached.metas[i] != meta) {
                    matches = false;
                    break;
                }
            }
        }
        if (matches) {
            return cached;
        }
        CachedGear fresh = cached != null ? cached : new CachedGear();
        fresh.tick = now;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = player.getItemStackFromSlot(LiveSlots.ARMOR[i]);
            fresh.items[i] = stack.isEmpty() ? null : stack.getItem();
            fresh.metas[i] = stack.isEmpty() ? 0 : stack.getMetadata();
        }
        fresh.windResistantPieces = GearTierRule.countOnList(player, wind.windResistantGear);
        fresh.hasAnchoringBoots = GearTierRule.slotMatches(player, EntityEquipmentSlot.FEET, wind.anchoringBoots);
        fresh.weight = (wind.enableWeightPushResistance && Loader.isModLoaded("elenaidodge2"))
                ? ElenaiWeightReader.read(player)
                : 0.0D;
        GEAR_CACHE.put(id, fresh);
        return fresh;
    }

    private static final class ElenaiWeightReader {
        static double read(EntityPlayer player) {
            try {
                return StaminaFeathers.getWeightBeforeMastery(player);
            } catch (Throwable ignored) {
                return 0.0D;
            }
        }
    }
}
