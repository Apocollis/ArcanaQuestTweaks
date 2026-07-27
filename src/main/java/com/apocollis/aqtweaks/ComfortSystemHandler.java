package com.apocollis.aqtweaks;

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
        World world = player.world;
        if (world.isRemote) return;

        // Only evaluate at the configured interval
        if (player.ticksExisted % CHECK_INTERVAL_TICKS != 0) return;

        boolean currentlyResting = isComfortResting(player);

        if (!currentlyResting) {
            // Player is NOT in resting state; check if they should enter it
            if (!isPlayerResting(player)) return;

            // Player is resting; run the comfort scan
            float score = calculateComfortScore(world, player, player.getPosition());
            if (score >= THRESHOLD_HOMESTEAD_1) {
                setComfortResting(player, true);
                applyComfortBenefits(player, score);
            }
        } else {
            // Player IS in resting state; re-evaluate comfort while allowing movement
            float score = calculateComfortScore(world, player, player.getPosition());
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
        if (player == null || player.world.isRemote || player.ticksExisted % 20 != 0) return;

        BlockPos pos = new BlockPos(player.posX, player.getEntityBoundingBox().minY, player.posZ);
        IBlockState state = player.world.getBlockState(pos);
        Block block = state.getBlock();
        String registryName = block.getRegistryName() != null ? block.getRegistryName().toString() : "";

        BlockPos headPos = pos.up();
        IBlockState headState = player.world.getBlockState(headPos);
        String headName = headState.getBlock().getRegistryName() != null ? headState.getBlock().getRegistryName().toString() : "";

        if (registryName.equals("biomesoplenty:hot_spring_water") || headName.equals("biomesoplenty:hot_spring_water")) {
            Potion coldResist = Potion.getPotionFromResourceLocation("simpledifficulty:cold_resist");
            if (coldResist != null) {
                player.addPotionEffect(new PotionEffect(coldResist, 200, 0, true, false));
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
            if (!player.world.isRemote) {
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
        if (!player.world.isRemote) {
            setComfortResting(player, false);
        }
    }

    // ==================== Resting State Checks ====================

    /**
     * Checks if the player is in a resting state:
     * sleeping in a bed, sitting (riding a mount/chair entity), sneaking, or standing still.
     */
    private static boolean isPlayerResting(EntityPlayer player) {
        if (player.isPlayerSleeping()) return true;
        if (player.isRiding()) return true;
        if (player.isSneaking()) return true;

        // Check if nearly stationary (horizontal velocity near zero)
        double hSpeedSq = player.motionX * player.motionX + player.motionZ * player.motionZ;
        return hSpeedSq < 0.001D;
    }

    private static boolean isComfortResting(EntityPlayer player) {
        return player.getEntityData().getBoolean(RESTING_TAG);
    }

    private static void setComfortResting(EntityPlayer player, boolean resting) {
        player.getEntityData().setBoolean(RESTING_TAG, resting);
        if (!resting) {
            player.removePotionEffect(PotionHomestead.INSTANCE);
        }
    }

    // ==================== Comfort Scoring ====================

    /**
     * Scans the 24x5x24 area around the player for registered cozy blocks and nearby pets.
     * Groups all found comfort values by category, sorts each category descending,
     * and sums only the highest values up to each category's configured limit.
     */
    private static float calculateComfortScore(World world, EntityPlayer player, BlockPos center) {
        Map<String, List<Float>> categoryScores = new HashMap<>();

        int rX = (int) DETECT_RADIUS;
        int rY = 2;
        int rZ = (int) DETECT_RADIUS;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int x = -rX; x <= rX; x++) {
            for (int y = -rY; y <= rY; y++) {
                for (int z = -rZ; z <= rZ; z++) {
                    mutablePos.setPos(center.getX() + x, center.getY() + y, center.getZ() + z);

                    // Prevent accidental chunk loading at scan boundaries
                    if (!world.isBlockLoaded(mutablePos)) continue;

                    IBlockState state = world.getBlockState(mutablePos);
                    Block block = state.getBlock();
                    ResourceLocation regName = block.getRegistryName();

                    if (regName != null) {
                        String nameStr = regName.toString();
                        if (COZY_BLOCKS.containsKey(nameStr)) {
                            CozyConfig config = COZY_BLOCKS.get(nameStr);
                            categoryScores.computeIfAbsent(config.category, k -> new ArrayList<>()).add(config.weight);
                        }
                    }
                }
            }
        }

        // Scan for tamed pets owned by the player
        AxisAlignedBB searchBox = new AxisAlignedBB(center).grow(PET_RADIUS);
        List<EntityTameable> nearbyPets = world.getEntitiesWithinAABB(EntityTameable.class, searchBox);
        for (EntityTameable pet : nearbyPets) {
            if (pet.isTamed() && pet.getOwnerId() != null && pet.getOwnerId().equals(player.getUniqueID())) {
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
            player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, potionDurationTicks, 0, true, false));
            applySimpleDifficultyThermals(player, potionDurationTicks);
        }
        // --- Threshold III: Score 30+ ---
        else if (score >= THRESHOLD_HOMESTEAD_3) {
            progressToAdd = 25;
            homesteadAmplifier = 2; // Homestead III
            player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, potionDurationTicks, 1, true, false));
            player.addPotionEffect(new PotionEffect(MobEffects.SATURATION, potionDurationTicks, 0, true, false));
            applySimpleDifficultyThermals(player, potionDurationTicks);
        }

        // Apply Homestead Status Buff on HUD (ambient = true, showParticles = false)
        player.addPotionEffect(new PotionEffect(PotionHomestead.INSTANCE, potionDurationTicks, homesteadAmplifier, true, false));

        // Drain temporary warp via reflection-safe ThaumcraftHelper
        if (progressToAdd > 0 && Loader.isModLoaded("thaumcraft")) {
            net.minecraft.nbt.NBTTagCompound persisted = player.getEntityData().getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
            int currentProgress = persisted.getInteger("WarpCleansingProgress") + progressToAdd;
            if (currentProgress >= 100) {
                int currentWarp = ThaumcraftHelper.getWarp(player, 1); // 1 = TEMPORARY
                if (currentWarp > 0) {
                    ThaumcraftHelper.reduceWarp(player, 1, 1);
                    ThaumcraftHelper.syncWarp(player);
                }
                currentProgress = 0;
            }
            persisted.setInteger("WarpCleansingProgress", currentProgress);
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
                player.addPotionEffect(new PotionEffect(heatProtection, duration, 0, true, false));
            }

            Potion coldProtection = Potion.getPotionFromResourceLocation("simpledifficulty:cold_protection");
            if (coldProtection != null) {
                player.addPotionEffect(new PotionEffect(coldProtection, duration, 0, true, false));
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
