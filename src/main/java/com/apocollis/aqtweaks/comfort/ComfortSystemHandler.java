package com.apocollis.aqtweaks.comfort;

import com.apocollis.aqtweaks.thaumcraft.ThaumcraftHelper;
import com.apocollis.aqtweaks.util.Reflect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Core comfort system event handler.
 *
 * Activation Flow:
 * 1. Every 15 seconds, check if the player is resting (sleeping, sitting, sneaking, or stationary).
 * 2. If resting, scan a 24x5x24 area for cozy blocks and nearby pets.
 * 3. Calculate a category-limited comfort score using the top-X highest values per category,
 *    then add bonuses and subtract player-state penalties.
 * 4. If effective score >= Homestead I, set the "Resting" tag and apply silent benefits.
 * 5. Granted band starts at I and promotes after promote_ticks while score still supports the next band.
 * 6. While the tag is active, continue scanning even if the player moves.
 * 7. Cancel the tag immediately on taking damage, attacking, or dropping below Homestead I.
 */
public class ComfortSystemHandler {

    private static final int CHECK_INTERVAL_TICKS = 300; // 15 seconds
    private static final int HOMESTEAD_DURATION_TICKS = CHECK_INTERVAL_TICKS + 40;
    private static final int EIGHT_MINUTES_TICKS = 9600;
    private static final int FOUR_MINUTES_TICKS = 4800;
    private static final double DETECT_RADIUS = 12.0;
    private static final double PET_RADIUS = 16.0;
    private static final String RESTING_TAG = "AQTComfortResting";
    private static final String GRANTED_BAND_TAG = "AQTComfortGrantedBand";
    private static final String BAND_SINCE_TAG = "AQTComfortBandSince";

    static final Map<String, CozyConfig> COZY_BLOCKS = new HashMap<>();
    static final Map<String, Integer> CATEGORY_LIMITS = new HashMap<>();
    static float PET_COMFORT_VALUE = 3.0f;
    public static float THRESHOLD_HOMESTEAD_1 = 15.0f;
    public static float THRESHOLD_HOMESTEAD_2 = 40.0f;
    public static float THRESHOLD_HOMESTEAD_3 = 60.0f;
    static long PROMOTE_TICKS = 1200L;
    static boolean PENALTIES_ENABLED = true;
    static boolean BONUSES_ENABLED = true;
    static ComfortSettings.TemperaturePenalty TEMP_PENALTY = ComfortSettings.TemperaturePenalty.defaults();
    static ComfortSettings.RatePenalty THIRST_PENALTY = ComfortSettings.RatePenalty.thirstDefaults();
    static ComfortSettings.RatePenalty HUNGER_PENALTY = ComfortSettings.RatePenalty.hungerDefaults();
    static ComfortSettings.HealthPenalty HEALTH_PENALTY = ComfortSettings.HealthPenalty.defaults();
    static List<ComfortSettings.PotionModifier> POTION_PENALTIES = ComfortSettings.defaultPenaltyEffects();
    static List<ComfortSettings.PotionModifier> POTION_BONUSES = ComfortSettings.defaultBonusEffects();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        if (player == null) return;
        World world = Reflect.getWorld(player);
        if (Reflect.isRemote(player) || world == null) return;

        if (Reflect.getTicksExisted(player) % CHECK_INTERVAL_TICKS != 0) return;

        boolean currentlyResting = isComfortResting(player);
        float score = calculateComfortScore(player);
        int scoreBand = scoreBand(score);

        if (scoreBand <= 0) {
            if (currentlyResting) {
                setComfortResting(player, false);
            }
            return;
        }

        if (!currentlyResting) {
            if (!isPlayerResting(player)) return;
            startLadder(player, world, 1);
            setComfortResting(player, true);
            applyComfortBenefits(player, 1);
            return;
        }

        int granted = getGrantedBand(player);
        if (scoreBand < granted) {
            granted = scoreBand;
            stampLadder(player, world, granted);
        } else if (scoreBand > granted && world.getTotalWorldTime() - getBandSince(player) >= PROMOTE_TICKS) {
            granted = Math.min(granted + 1, scoreBand);
            stampLadder(player, world, granted);
        }

        applyComfortBenefits(player, granted);
    }

    /**
     * Apply Cold Resistance potion effect while standing or submerged in Biomes O' Plenty Hot Springs Water.
     */
    @SubscribeEvent
    public void onHotSpringsWaterTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        EntityPlayer player = event.player;
        if (player == null || Reflect.isRemote(player) || Reflect.getTicksExisted(player) % 20 != 0) return;

        World world = Reflect.getWorld(player);
        if (world == null) return;

        BlockPos pos = new BlockPos(Reflect.getPosX(player), Reflect.getBoundingBoxMinY(player), Reflect.getPosZ(player));
        IBlockState state = Reflect.getBlockState(world, pos);
        Block block = Reflect.getBlock(state);
        String registryName = block.getRegistryName() != null ? block.getRegistryName().toString() : "";

        BlockPos headPos = Reflect.up(pos);
        IBlockState headState = Reflect.getBlockState(world, headPos);
        Block headBlock = Reflect.getBlock(headState);
        String headName = headBlock.getRegistryName() != null ? headBlock.getRegistryName().toString() : "";

        if (registryName.equals("biomesoplenty:hot_spring_water") || headName.equals("biomesoplenty:hot_spring_water")) {
            applyNamedPotion(player, "simpledifficulty:cold_resist", 200, 0);
        }
    }

    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if (!Reflect.isRemote(player)) {
                setComfortResting(player, false);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerAttack(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!Reflect.isRemote(player)) {
            setComfortResting(player, false);
        }
    }

    private static boolean isPlayerResting(EntityPlayer player) {
        if (Reflect.isPlayerSleeping(player)) return true;
        if (Reflect.isRiding(player)) return true;
        if (Reflect.isSneaking(player)) return true;

        double hSpeedSq = Reflect.getMotionX(player) * Reflect.getMotionX(player)
            + Reflect.getMotionZ(player) * Reflect.getMotionZ(player);
        return hSpeedSq < 0.001D;
    }

    private static boolean isComfortResting(EntityPlayer player) {
        return Reflect.getBoolean(Reflect.getEntityData(player), RESTING_TAG);
    }

    private static void setComfortResting(EntityPlayer player, boolean resting) {
        NBTTagCompound data = Reflect.getEntityData(player);
        Reflect.setBoolean(data, RESTING_TAG, resting);
        if (!resting) {
            Reflect.removePotionEffect(player, PotionHomestead.INSTANCE);
            data.removeTag(GRANTED_BAND_TAG);
            data.removeTag(BAND_SINCE_TAG);
        }
    }

    private static void startLadder(EntityPlayer player, World world, int band) {
        stampLadder(player, world, band);
    }

    private static void stampLadder(EntityPlayer player, World world, int band) {
        NBTTagCompound data = Reflect.getEntityData(player);
        data.setInteger(GRANTED_BAND_TAG, band);
        data.setLong(BAND_SINCE_TAG, world.getTotalWorldTime());
    }

    private static int getGrantedBand(EntityPlayer player) {
        int band = Reflect.getEntityData(player).getInteger(GRANTED_BAND_TAG);
        if (band < 1) return 1;
        if (band > 3) return 3;
        return band;
    }

    private static long getBandSince(EntityPlayer player) {
        return Reflect.getEntityData(player).getLong(BAND_SINCE_TAG);
    }

    private static int scoreBand(float score) {
        if (score < THRESHOLD_HOMESTEAD_1) return 0;
        if (score < THRESHOLD_HOMESTEAD_2) return 1;
        if (score < THRESHOLD_HOMESTEAD_3) return 2;
        return 3;
    }

    /**
     * Cozy blocks + pets, then bonuses, then penalties. Result is never negative.
     */
    private static float calculateComfortScore(EntityPlayer player) {
        float cozy = calculateCozyScore(player);
        float bonuses = calculateBonuses(player);
        float penalties = calculatePenalties(player);
        return Math.max(0.0f, cozy + bonuses - penalties);
    }

    private static float calculateCozyScore(EntityPlayer player) {
        World world = Reflect.getWorld(player);
        if (world == null) return 0.0f;

        BlockPos center = Reflect.getPosition(player);
        Map<String, List<Float>> categoryScores = new HashMap<>();

        int rX = (int) DETECT_RADIUS;
        int rY = 2;
        int rZ = (int) DETECT_RADIUS;

        for (int dx = -rX; dx <= rX; dx++) {
            for (int dz = -rZ; dz <= rZ; dz++) {
                for (int dy = -rY; dy <= rY; dy++) {
                    BlockPos pos = Reflect.add(center, dx, dy, dz);
                    if (Reflect.isBlockLoaded(world, pos)) {
                        IBlockState state = Reflect.getBlockState(world, pos);
                        Block block = Reflect.getBlock(state);
                        String nameStr = block.getRegistryName() != null ? block.getRegistryName().toString() : "";
                        if (COZY_BLOCKS.containsKey(nameStr)) {
                            CozyConfig config = COZY_BLOCKS.get(nameStr);
                            categoryScores.computeIfAbsent(config.category, k -> new ArrayList<>()).add(config.weight);
                        }
                    }
                }
            }
        }

        AxisAlignedBB searchBox = Reflect.grow(center, PET_RADIUS);
        List<EntityTameable> nearbyPets = Reflect.getEntitiesWithinAABB(world, EntityTameable.class, searchBox);
        for (EntityTameable pet : nearbyPets) {
            if (pet.isTamed() && pet.getOwnerId() != null && pet.getOwnerId().equals(Reflect.getUniqueID(player))) {
                categoryScores.computeIfAbsent("pets", k -> new ArrayList<>()).add(PET_COMFORT_VALUE);
            }
        }

        float totalScore = 0.0f;
        for (Map.Entry<String, List<Float>> entry : categoryScores.entrySet()) {
            String category = entry.getKey();
            List<Float> weights = entry.getValue();
            weights.sort(Collections.reverseOrder());
            int limit = CATEGORY_LIMITS.getOrDefault(category, 1);
            int toTake = Math.min(weights.size(), limit);
            for (int i = 0; i < toTake; i++) {
                totalScore += weights.get(i);
            }
        }
        return totalScore;
    }

    private static float calculateBonuses(EntityPlayer player) {
        if (!BONUSES_ENABLED) return 0.0f;
        return sumPotionModifiers(player, POTION_BONUSES);
    }

    private static float calculatePenalties(EntityPlayer player) {
        if (!PENALTIES_ENABLED) return 0.0f;
        return temperaturePenalty(player)
            + thirstPenalty(player)
            + hungerPenalty(player)
            + healthPenalty(player)
            + sumPotionModifiers(player, POTION_PENALTIES);
    }

    private static float sumPotionModifiers(EntityPlayer player, List<ComfortSettings.PotionModifier> modifiers) {
        if (modifiers == null) return 0.0f;
        float total = 0.0f;
        for (ComfortSettings.PotionModifier modifier : modifiers) {
            if (modifier == null || !modifier.enabled) continue;
            if (hasPotion(player, modifier.potion)) {
                total += modifier.amount;
            }
        }
        return total;
    }

    private static float temperaturePenalty(EntityPlayer player) {
        if (TEMP_PENALTY == null || !TEMP_PENALTY.enabled) return 0.0f;
        Integer temp = Reflect.getSdTemperatureLevel(player);
        if (temp == null) return 0.0f;
        int t = temp;
        if (t >= TEMP_PENALTY.comfort_min && t <= TEMP_PENALTY.comfort_max) return 0.0f;
        if (t > TEMP_PENALTY.comfort_max && hasAnyPotion(player, TEMP_PENALTY.heat_ignore_potions)) return 0.0f;
        if (t < TEMP_PENALTY.comfort_min && hasAnyPotion(player, TEMP_PENALTY.cold_ignore_potions)) return 0.0f;
        int outside = t < TEMP_PENALTY.comfort_min
            ? TEMP_PENALTY.comfort_min - t
            : t - TEMP_PENALTY.comfort_max;
        return outside * TEMP_PENALTY.per_point_outside;
    }

    private static float thirstPenalty(EntityPlayer player) {
        if (THIRST_PENALTY == null || !THIRST_PENALTY.enabled) return 0.0f;
        Integer thirst = Reflect.getSdThirstLevel(player);
        if (thirst == null) return 0.0f;
        int missing = Math.max(0, 20 - thirst);
        return missing * THIRST_PENALTY.per_missing_point;
    }

    private static float hungerPenalty(EntityPlayer player) {
        if (HUNGER_PENALTY == null || !HUNGER_PENALTY.enabled) return 0.0f;
        int missing = Math.max(0, 20 - player.getFoodStats().getFoodLevel());
        return missing * HUNGER_PENALTY.per_missing_point;
    }

    private static float healthPenalty(EntityPlayer player) {
        if (HEALTH_PENALTY == null || !HEALTH_PENALTY.enabled) return 0.0f;
        float max = player.getMaxHealth();
        if (max <= 0.0f) return 0.0f;
        float missingFraction = 1.0f - (player.getHealth() / max);
        if (missingFraction <= 0.0f) return 0.0f;
        return missingFraction * HEALTH_PENALTY.per_missing_fraction;
    }

    private static boolean hasAnyPotion(EntityPlayer player, String[] ids) {
        if (ids == null) return false;
        for (String id : ids) {
            if (hasPotion(player, id)) return true;
        }
        return false;
    }

    private static boolean hasPotion(EntityPlayer player, String id) {
        if (id == null || id.isEmpty()) return false;
        Potion potion = Potion.getPotionFromResourceLocation(id);
        return potion != null && player.isPotionActive(potion);
    }

    private static void applyNamedPotion(EntityPlayer player, String id, int duration, int amplifier) {
        Potion potion = Potion.getPotionFromResourceLocation(id);
        if (potion == null) return;
        Reflect.addPotionEffect(player, new PotionEffect(potion, duration, amplifier, true, false));
    }

    /**
     * Applies benefits for the granted Homestead band (1–3), not the raw score band.
     */
    private static void applyComfortBenefits(EntityPlayer player, int grantedBand) {
        int progressToAdd = 0;
        int homesteadAmplifier = 0;
        int xpAmp = 0;

        if (grantedBand == 1) {
            progressToAdd = 9;
            homesteadAmplifier = 0;
            xpAmp = 0;
        } else if (grantedBand == 2) {
            progressToAdd = 13;
            homesteadAmplifier = 1;
            xpAmp = 1;
        } else if (grantedBand >= 3) {
            progressToAdd = 25;
            homesteadAmplifier = 2;
            xpAmp = 2;
        } else {
            return;
        }

        Reflect.addPotionEffect(player, new PotionEffect(PotionHomestead.INSTANCE, HOMESTEAD_DURATION_TICKS, homesteadAmplifier, true, false));
        applyNamedPotion(player, "soot:experience_boost", EIGHT_MINUTES_TICKS, xpAmp);

        if (grantedBand == 2) {
            applyNamedPotion(player, "elenaidodge2:endurance", EIGHT_MINUTES_TICKS, 0);
            applyNamedPotion(player, "elenaidodge2:replenishment", FOUR_MINUTES_TICKS, 0);
        } else if (grantedBand >= 3) {
            applyNamedPotion(player, "elenaidodge2:endurance", EIGHT_MINUTES_TICKS, 1);
            applyNamedPotion(player, "elenaidodge2:replenishment", EIGHT_MINUTES_TICKS, 0);
        }

        if (progressToAdd > 0 && Loader.isModLoaded("thaumcraft")) {
            net.minecraft.nbt.NBTTagCompound persisted = Reflect.getPersistedTag(player);
            int currentProgress = Reflect.getInteger(persisted, "WarpCleansingProgress") + progressToAdd;
            if (currentProgress >= 100) {
                int currentWarp = ThaumcraftHelper.getWarp(player, 1);
                if (currentWarp > 0) {
                    ThaumcraftHelper.reduceWarp(player, 1, 1);
                    ThaumcraftHelper.syncWarp(player);
                }
                currentProgress = 0;
            }
            Reflect.setInteger(persisted, "WarpCleansingProgress", currentProgress);
        }
    }

    static class CozyConfig {
        public final float weight;
        public final String category;

        public CozyConfig(float weight, String category) {
            this.weight = weight;
            this.category = category;
        }
    }
}
