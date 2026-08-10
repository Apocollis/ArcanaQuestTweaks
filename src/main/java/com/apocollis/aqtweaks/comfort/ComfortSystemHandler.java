package com.apocollis.aqtweaks.comfort;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

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
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
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
 * 3. Calculate a category-limited comfort score using the top-X highest values per category.
 * 4. If score >= 5.0, set the "Resting" tag and apply silent benefits (warp drain, potions).
 * 5. While the tag is active, continue scanning even if the player moves.
 * 6. Cancel the tag immediately on taking damage, attacking, or leaving the cozy area.
 */
public class ComfortSystemHandler {

    private static final int CHECK_INTERVAL_TICKS = 300; // 15 seconds
    private static final double DETECT_RADIUS = 12.0;    // 24x5x24 scan area (12 block horizontal radius)
    private static final double PET_RADIUS = 16.0;       // Pet detection radius
    private static final String RESTING_TAG = "AQTComfortResting";

    // Populated by ComfortConfigLoader during preInit
    static final Map<String, CozyConfig> COZY_BLOCKS = new HashMap<>();
    static final Map<String, Integer> CATEGORY_LIMITS = new HashMap<>();
    static float PET_COMFORT_VALUE = 3.0f;
    public static float THRESHOLD_HOMESTEAD_1 = 5.0f;
    public static float THRESHOLD_HOMESTEAD_2 = 15.0f;
    public static float THRESHOLD_HOMESTEAD_3 = 30.0f;

    // ==================== Event Handlers ====================

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        if (player == null) return;
        World world = Reflect.getWorld(player);
        if (Reflect.isRemote(player)) return;

        // Only evaluate at the configured interval
        if (Reflect.getTicksExisted(player) % CHECK_INTERVAL_TICKS != 0) return;

        boolean currentlyResting = isComfortResting(player);

        if (!currentlyResting) {
            // Player is NOT in resting state; check if they should enter it
            if (!isPlayerResting(player)) return;

            // Player is resting; run the comfort scan
            float score = calculateComfortScore(player);
            if (score >= THRESHOLD_HOMESTEAD_1) {
                setComfortResting(player, true);
                applyComfortBenefits(player, score);
            }
        } else {
            // Player IS in resting state; re-evaluate comfort while allowing movement
            float score = calculateComfortScore(player);
            if (score >= THRESHOLD_HOMESTEAD_1) {
                applyComfortBenefits(player, score);
            } else {
                // No longer in a cozy area; cancel resting state
                setComfortResting(player, false);
            }
        }
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
            Potion coldResist = Potion.getPotionFromResourceLocation("simpledifficulty:cold_resist");
            if (coldResist != null) {
                Reflect.addPotionEffect(player, new PotionEffect(coldResist, 200, 0, true, false));
            }
        }
    }

    /**
     * Cancel comfort resting when the player takes damage from any source.
     */
    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if (!Reflect.isRemote(player)) {
                setComfortResting(player, false);
            }
        }
    }

    /**
     * Cancel comfort resting when the player attacks any entity.
     */
    @SubscribeEvent
    public void onPlayerAttack(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (!Reflect.isRemote(player)) {
            setComfortResting(player, false);
        }
    }

    // ==================== Resting State Checks ====================

    /**
     * Checks if the player is in a resting state:
     * sleeping in a bed, sitting (riding a mount/chair entity), sneaking, or standing still.
     */
    private static boolean isPlayerResting(EntityPlayer player) {
        if (Reflect.isPlayerSleeping(player)) return true;
        if (Reflect.isRiding(player)) return true;
        if (Reflect.isSneaking(player)) return true;

        // Check if nearly stationary (horizontal velocity near zero)
        double hSpeedSq = Reflect.getMotionX(player) * Reflect.getMotionX(player) + Reflect.getMotionZ(player) * Reflect.getMotionZ(player);
        return hSpeedSq < 0.001D;
    }

    private static boolean isComfortResting(EntityPlayer player) {
        return Reflect.getBoolean(Reflect.getEntityData(player), RESTING_TAG);
    }

    private static void setComfortResting(EntityPlayer player, boolean resting) {
        Reflect.setBoolean(Reflect.getEntityData(player), RESTING_TAG, resting);
        if (!resting) {
            Reflect.removePotionEffect(player, PotionHomestead.INSTANCE);
        }
    }

    // ==================== Comfort Scoring ====================

    /**
     * Scans the 24x5x24 area around the player for registered cozy blocks and nearby pets.
     * Groups all found comfort values by category, sorts each category descending,
     * and sums only the highest values up to each category's configured limit.
     */
    private static float calculateComfortScore(EntityPlayer player) {
        World world = Reflect.getWorld(player);
        if (world == null) return 0.0f;

        BlockPos center = Reflect.getPosition(player);
        Map<String, List<Float>> categoryScores = new HashMap<>();

        int rX = (int) DETECT_RADIUS;
        int rY = 2;
        int rZ = (int) DETECT_RADIUS;

        // Scan 24x5x24 area
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

        // Scan for tamed pets owned by the player
        AxisAlignedBB searchBox = Reflect.grow(center, PET_RADIUS);
        List<EntityTameable> nearbyPets = Reflect.getEntitiesWithinAABB(world, EntityTameable.class, searchBox);
        for (EntityTameable pet : nearbyPets) {
            if (pet.isTamed() && pet.getOwnerId() != null && pet.getOwnerId().equals(Reflect.getUniqueID(player))) {
                categoryScores.computeIfAbsent("pets", k -> new ArrayList<>()).add(PET_COMFORT_VALUE);
            }
        }

        // Process each category: sort descending, sum only the top X (limit) items
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

    // ==================== Benefit Application ====================

    /**
     * Applies scaling benefits silently based on comfort score thresholds.
     * Applies the visible Homestead status effect icon on the HUD without particle swirls.
     */
    private static void applyComfortBenefits(EntityPlayer player, float score) {
        int progressToAdd = 0;
        int potionDurationTicks = CHECK_INTERVAL_TICKS + 40; // 17 seconds to ensure no-gap coverage
        int homesteadAmplifier = 0;

        // --- Threshold I: Score 5-14 ---
        if (score >= THRESHOLD_HOMESTEAD_1 && score < THRESHOLD_HOMESTEAD_2) {
            progressToAdd = 9;
            homesteadAmplifier = 0; // Homestead I
        }
        // --- Threshold II: Score 15-29 ---
        else if (score >= THRESHOLD_HOMESTEAD_2 && score < THRESHOLD_HOMESTEAD_3) {
            progressToAdd = 13;
            homesteadAmplifier = 1; // Homestead II
            Reflect.addPotionEffect(player, new PotionEffect(MobEffects.REGENERATION, potionDurationTicks, 0, true, false));
            applySimpleDifficultyThermals(player, potionDurationTicks);
        }
        // --- Threshold III: Score 30+ ---
        else if (score >= THRESHOLD_HOMESTEAD_3) {
            progressToAdd = 25;
            homesteadAmplifier = 2; // Homestead III
            Reflect.addPotionEffect(player, new PotionEffect(MobEffects.REGENERATION, potionDurationTicks, 1, true, false));
            Reflect.addPotionEffect(player, new PotionEffect(MobEffects.SATURATION, potionDurationTicks, 0, true, false));
            applySimpleDifficultyThermals(player, potionDurationTicks);
        }

        // Apply Homestead Status Buff on HUD (ambient = true, showParticles = false)
        Reflect.addPotionEffect(player, new PotionEffect(PotionHomestead.INSTANCE, potionDurationTicks, homesteadAmplifier, true, false));

        // Drain temporary warp via reflection-safe ThaumcraftHelper
        if (progressToAdd > 0 && Loader.isModLoaded("thaumcraft")) {
            net.minecraft.nbt.NBTTagCompound persisted = Reflect.getPersistedTag(player);
            int currentProgress = Reflect.getInteger(persisted, "WarpCleansingProgress") + progressToAdd;
            if (currentProgress >= 100) {
                int currentWarp = ThaumcraftHelper.getWarp(player, 1); // 1 = TEMPORARY
                if (currentWarp > 0) {
                    ThaumcraftHelper.reduceWarp(player, 1, 1);
                    ThaumcraftHelper.syncWarp(player);
                }
                currentProgress = 0;
            }
            Reflect.setInteger(persisted, "WarpCleansingProgress", currentProgress);
        }
    }

    /**
     * Safely applies SimpleDifficulty heat and cold protection potions.
     * Uses dynamic resource location lookups to avoid compile-time dependencies.
     */
    private static void applySimpleDifficultyThermals(EntityPlayer player, int duration) {
        try {
            Potion heatProtection = Potion.getPotionFromResourceLocation("simpledifficulty:heat_protection");
            if (heatProtection != null) {
                Reflect.addPotionEffect(player, new PotionEffect(heatProtection, duration, 0, true, false));
            }

            Potion coldProtection = Potion.getPotionFromResourceLocation("simpledifficulty:cold_protection");
            if (coldProtection != null) {
                Reflect.addPotionEffect(player, new PotionEffect(coldProtection, duration, 0, true, false));
            }
        } catch (Exception e) {
            // Failsafe: Prevent crashes if SimpleDifficulty is not loaded
        }
    }



    // ==================== Inner Classes ====================

    /**
     * Configuration data class for each cozy block type.
     */
    static class CozyConfig {
        public final float weight;
        public final String category;

        public CozyConfig(float weight, String category) {
            this.weight = weight;
            this.category = category;
        }
    }
}
