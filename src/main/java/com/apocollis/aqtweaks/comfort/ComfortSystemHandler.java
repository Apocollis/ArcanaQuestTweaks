package com.apocollis.aqtweaks.comfort;

import com.apocollis.aqtweaks.thaumcraft.ThaumcraftHelper;
import com.apocollis.aqtweaks.util.Reflect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Core comfort system event handler.
 *
 * Activation Flow:
 * 1. Every 30 seconds, check if the player is resting (sleeping, sitting, sneaking, or stationary).
 * 2. If resting, scan a 25x5x25 area for cozy blocks and nearby pets.
 * 3. Calculate a category-limited comfort score using the top-X highest values per category,
 *    then add bonuses and subtract player-state penalties.
 * 4. Entry also needs a hearth, bedding, or seating block in that scan, and must be outside
 *    the hurt (30s) and attack (15s) cooldowns.
 * 5. If effective score >= Homestead I, set the "Resting" tag and apply silent benefits.
 * 6. Granted band starts at I and promotes after promote_ticks while score still supports the next band.
 * 7. While the tag is active, continue scanning even if the player moves; furniture must stay in range.
 * 8. Cancel the tag immediately on taking damage, attacking, dropping below Homestead I, or losing furniture.
 *
 * <p>Everything here is server-side: every entry point returns early on {@code world.isRemote}.
 * The caches below rely on that for thread confinement.
 */
public class ComfortSystemHandler {

    private static final int CHECK_INTERVAL_TICKS = 600; // 30 seconds
    private static final int HOMESTEAD_DURATION_TICKS = 900; // 45 seconds; overlaps scans by 15s
    private static final int EIGHT_MINUTES_TICKS = 9600;
    private static final int FOUR_MINUTES_TICKS = 4800;
    private static final double DETECT_RADIUS = 12.0;
    private static final double PET_RADIUS = 16.0;
    private static final String RESTING_TAG = "AQTComfortResting";
    private static final String GRANTED_BAND_TAG = "AQTComfortGrantedBand";
    private static final String BAND_SINCE_TAG = "AQTComfortBandSince";
    private static final String HURT_AT_TAG = "AQTComfortHurtAt";
    private static final String ATTACK_AT_TAG = "AQTComfortAttackAt";
    private static final String LEARNING_POTION = "extraalchemy:effect.learning";
    private static final String XP_BOOST_POTION = "soot:experience_boost";
    private static final String WARP_CLEANSE_TAG = "WarpCleansingProgress";
    private static final int WARP_CLEANSE_THRESHOLD = 12;

    static final Map<String, CozyConfig> COZY_BLOCKS = new HashMap<>();
    static final Map<String, Integer> CATEGORY_LIMITS = new HashMap<>();
    static final Set<String> ENTRY_REQUIRE_CATEGORIES = new HashSet<>();
    static long DAMAGE_COOLDOWN_TICKS = 600L;
    static long ATTACK_COOLDOWN_TICKS = 300L;
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

    /**
     * Resolved potion ids, including negative results. The registry is fixed after load, and the
     * penalty and bonus lists are walked on every comfort check, so an unregistered id would
     * otherwise cost a registry miss every time.
     */
    private static final Map<String, Potion> POTION_CACHE = new HashMap<>();

    private static Boolean thaumcraftLoaded;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        if (player == null || player instanceof FakePlayer) return;
        World world = player.world;
        if (world == null || world.isRemote) return;

        // Offset by entity id so a server full of players that logged in together does not run
        // every scan on the same tick. Homestead lasts 45s against a 30s interval, so shifting
        // the phase cannot open a gap.
        if ((player.ticksExisted + player.getEntityId()) % CHECK_INTERVAL_TICKS != 0) return;

        boolean currentlyResting = isComfortResting(player);
        if (!currentlyResting) {
            if (!isPlayerResting(player) || isEntryOnCooldown(player, world)) return;
        }

        applyEvaluation(player, evaluate(player));
    }

    /**
     * Apply Cold Resistance potion effect while standing or submerged in Biomes O' Plenty Hot Springs Water.
     */
    @SubscribeEvent
    public void onHotSpringsWaterTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        EntityPlayer player = event.player;
        if (player == null || player instanceof FakePlayer) return;
        World world = player.world;
        if (world == null || world.isRemote || player.ticksExisted % 20 != 0) return;

        BlockPos pos = new BlockPos(player.posX, player.getEntityBoundingBox().minY, player.posZ);
        if (isHotSpringWater(world, pos) || isHotSpringWater(world, pos.up())) {
            applyNamedPotion(player, "simpledifficulty:cold_resist", 200, 0);
        }
    }

    private static boolean isHotSpringWater(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        ResourceLocation name = block.getRegistryName();
        return name != null && "biomesoplenty:hot_spring_water".equals(name.toString());
    }

    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer player)) return;
        if (player.world == null || player.world.isRemote) return;
        setComfortResting(player, false);
        if (event.getAmount() > 0.0f) {
            player.getEntityData().setLong(HURT_AT_TAG, player.world.getTotalWorldTime());
        }
    }

    @SubscribeEvent
    public void onPlayerAttack(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world == null || player.world.isRemote) return;
        setComfortResting(player, false);
        player.getEntityData().setLong(ATTACK_AT_TAG, player.world.getTotalWorldTime());
    }

    private static boolean isEntryOnCooldown(EntityPlayer player, World world) {
        return remainingHurtTicks(player, world) > 0L || remainingAttackTicks(player, world) > 0L;
    }

    static long remainingHurtTicks(EntityPlayer player, World world) {
        return remainingCooldown(player, world, HURT_AT_TAG, DAMAGE_COOLDOWN_TICKS);
    }

    static long remainingAttackTicks(EntityPlayer player, World world) {
        return remainingCooldown(player, world, ATTACK_AT_TAG, ATTACK_COOLDOWN_TICKS);
    }

    private static long remainingCooldown(EntityPlayer player, World world, String tag, long cooldownTicks) {
        if (cooldownTicks <= 0L) return 0L;
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(tag)) return 0L;
        long left = cooldownTicks - (world.getTotalWorldTime() - data.getLong(tag));
        return left > 0L ? left : 0L;
    }

    /**
     * Full 25×5×25 scan plus bonuses/penalties. Used by the 30s tick and {@code /aqcomfort}.
     */
    static ComfortEval evaluate(EntityPlayer player) {
        World world = player.world;
        CozyScan cozy = calculateCozyScan(player);
        if (world == null) {
            return new ComfortEval(
                    false, false, cozy.meetsEntryFurniture(), 0L, 0L,
                    cozy.score, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                    Math.max(0.0f, cozy.score), scoreBand(Math.max(0.0f, cozy.score)), 0, -1L,
                    cozy.cappedByCategory, Collections.emptyList(), Collections.emptyList());
        }
        List<ComfortSettings.PotionModifier> activeBonuses = activeModifiers(player, POTION_BONUSES, BONUSES_ENABLED);
        List<ComfortSettings.PotionModifier> activePenaltyEffects = activeModifiers(player, POTION_PENALTIES, PENALTIES_ENABLED);
        float bonusTotal = 0.0f;
        for (ComfortSettings.PotionModifier m : activeBonuses) {
            bonusTotal += m.amount;
        }
        float temp = PENALTIES_ENABLED ? temperaturePenalty(player) : 0.0f;
        float thirst = PENALTIES_ENABLED ? thirstPenalty(player) : 0.0f;
        float hunger = PENALTIES_ENABLED ? hungerPenalty(player) : 0.0f;
        float health = PENALTIES_ENABLED ? healthPenalty(player) : 0.0f;
        float potionPenalties = 0.0f;
        for (ComfortSettings.PotionModifier m : activePenaltyEffects) {
            potionPenalties += m.amount;
        }
        float penaltyTotal = temp + thirst + hunger + health + potionPenalties;
        float effective = Math.max(0.0f, cozy.score + bonusTotal - penaltyTotal);
        boolean resting = isComfortResting(player);
        int granted = resting ? getGrantedBand(player) : 0;
        long promoteIn = -1L;
        int scoreBand = scoreBand(effective);
        if (resting && world != null && scoreBand > granted && granted >= 1 && granted < 3) {
            promoteIn = Math.max(0L, PROMOTE_TICKS - (world.getTotalWorldTime() - getBandSince(player)));
        }
        return new ComfortEval(
                resting,
                isPlayerResting(player),
                cozy.meetsEntryFurniture(),
                remainingHurtTicks(player, world),
                remainingAttackTicks(player, world),
                cozy.score,
                bonusTotal,
                penaltyTotal,
                temp,
                thirst,
                hunger,
                health,
                potionPenalties,
                effective,
                scoreBand,
                granted,
                promoteIn,
                cozy.cappedByCategory,
                activeBonuses,
                activePenaltyEffects);
    }

    /**
     * Same ladder rules as the interval tick. Does nothing if the player cannot enter and is not already resting.
     */
    static void applyEvaluation(EntityPlayer player, ComfortEval eval) {
        World world = player.world;
        if (world == null) return;

        if (!eval.currentlyResting) {
            if (!eval.restPose || eval.hurtRemain > 0L || eval.attackRemain > 0L) return;
            if (!eval.furniture || eval.scoreBand <= 0) return;
            startLadder(player, world, 1);
            setComfortResting(player, true);
            applyComfortBenefits(player, 1);
            return;
        }

        if (!eval.furniture || eval.scoreBand <= 0) {
            setComfortResting(player, false);
            return;
        }

        int granted = eval.grantedBand < 1 ? 1 : eval.grantedBand;
        if (eval.scoreBand < granted) {
            granted = eval.scoreBand;
            stampLadder(player, world, granted);
        } else if (eval.scoreBand > granted && world.getTotalWorldTime() - getBandSince(player) >= PROMOTE_TICKS) {
            granted = Math.min(granted + 1, eval.scoreBand);
            stampLadder(player, world, granted);
        }
        applyComfortBenefits(player, granted);
    }

    private static List<ComfortSettings.PotionModifier> activeModifiers(
            EntityPlayer player, List<ComfortSettings.PotionModifier> modifiers, boolean masterEnabled) {
        if (!masterEnabled || modifiers == null) return Collections.emptyList();
        List<ComfortSettings.PotionModifier> active = new ArrayList<>();
        for (ComfortSettings.PotionModifier modifier : modifiers) {
            if (modifier == null || !modifier.enabled) continue;
            if (hasPotion(player, modifier.potion)) {
                active.add(modifier);
            }
        }
        return active;
    }

    private static boolean isPlayerResting(EntityPlayer player) {
        if (player.isPlayerSleeping()) return true;
        if (player.isRiding()) return true;
        if (player.isSneaking()) return true;

        double hSpeedSq = player.motionX * player.motionX + player.motionZ * player.motionZ;
        return hSpeedSq < 0.001D;
    }

    private static boolean isComfortResting(EntityPlayer player) {
        return player.getEntityData().getBoolean(RESTING_TAG);
    }

    private static void setComfortResting(EntityPlayer player, boolean resting) {
        NBTTagCompound data = player.getEntityData();
        data.setBoolean(RESTING_TAG, resting);
        if (!resting) {
            player.removePotionEffect(PotionHomestead.INSTANCE);
            data.removeTag(GRANTED_BAND_TAG);
            data.removeTag(BAND_SINCE_TAG);
        }
    }

    private static void startLadder(EntityPlayer player, World world, int band) {
        stampLadder(player, world, band);
    }

    private static void stampLadder(EntityPlayer player, World world, int band) {
        NBTTagCompound data = player.getEntityData();
        data.setInteger(GRANTED_BAND_TAG, band);
        data.setLong(BAND_SINCE_TAG, world.getTotalWorldTime());
    }

    private static int getGrantedBand(EntityPlayer player) {
        int band = player.getEntityData().getInteger(GRANTED_BAND_TAG);
        if (band < 1) return 1;
        if (band > 3) return 3;
        return band;
    }

    private static long getBandSince(EntityPlayer player) {
        return player.getEntityData().getLong(BAND_SINCE_TAG);
    }

    private static int scoreBand(float score) {
        if (score < THRESHOLD_HOMESTEAD_1) return 0;
        if (score < THRESHOLD_HOMESTEAD_2) return 1;
        if (score < THRESHOLD_HOMESTEAD_3) return 2;
        return 3;
    }

    /**
     * Cozy blocks + pets. Furniture gate is whether any required category appeared in the scan.
     */
    private static CozyScan calculateCozyScan(EntityPlayer player) {
        World world = player.world;
        if (world == null) return CozyScan.EMPTY;

        BlockPos center = player.getPosition();
        Map<String, List<Float>> categoryScores = new HashMap<>();
        boolean hasEntryFurniture = ENTRY_REQUIRE_CATEGORIES.isEmpty();

        int rX = (int) DETECT_RADIUS;
        int rY = 2;
        int rZ = (int) DETECT_RADIUS;

        // One cursor for the whole 25x5x25 scan instead of 3,125 throwaway positions.
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -rX; dx <= rX; dx++) {
            for (int dz = -rZ; dz <= rZ; dz++) {
                for (int dy = -rY; dy <= rY; dy++) {
                    cursor.setPos(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!world.isBlockLoaded(cursor)) continue;

                    IBlockState state = world.getBlockState(cursor);
                    ResourceLocation name = state.getBlock().getRegistryName();
                    if (name == null) continue;

                    CozyConfig config = COZY_BLOCKS.get(name.toString());
                    if (config != null) {
                        categoryScores.computeIfAbsent(config.category, k -> new ArrayList<>()).add(config.weight);
                        if (!hasEntryFurniture && ENTRY_REQUIRE_CATEGORIES.contains(config.category)) {
                            hasEntryFurniture = true;
                        }
                    }
                }
            }
        }

        AxisAlignedBB searchBox = petSearchBox(center);
        List<EntityTameable> nearbyPets = world.getEntitiesWithinAABB(EntityTameable.class, searchBox);
        for (EntityTameable pet : nearbyPets) {
            if (pet.isTamed() && pet.getOwnerId() != null && pet.getOwnerId().equals(player.getUniqueID())) {
                categoryScores.computeIfAbsent("pets", k -> new ArrayList<>()).add(PET_COMFORT_VALUE);
            }
        }

        float totalScore = 0.0f;
        Map<String, Float> capped = new LinkedHashMap<>();
        for (Map.Entry<String, List<Float>> entry : categoryScores.entrySet()) {
            String category = entry.getKey();
            List<Float> weights = entry.getValue();
            weights.sort(Collections.reverseOrder());
            int limit = CATEGORY_LIMITS.getOrDefault(category, 1);
            int toTake = Math.min(weights.size(), limit);
            float categoryTotal = 0.0f;
            for (int i = 0; i < toTake; i++) {
                categoryTotal += weights.get(i);
            }
            capped.put(category, categoryTotal);
            totalScore += categoryTotal;
        }
        return new CozyScan(totalScore, hasEntryFurniture, capped);
    }

    /** The block-centred box {@code Reflect.grow(BlockPos, double)} built: one block plus radius. */
    private static AxisAlignedBB petSearchBox(BlockPos center) {
        int x = center.getX();
        int y = center.getY();
        int z = center.getZ();
        return new AxisAlignedBB(
                x - PET_RADIUS, y - PET_RADIUS, z - PET_RADIUS,
                x + 1 + PET_RADIUS, y + 1 + PET_RADIUS, z + 1 + PET_RADIUS);
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
        Potion potion = potionById(id);
        return potion != null && player.isPotionActive(potion);
    }

    /** Server-thread only; see the class javadoc. */
    private static Potion potionById(String id) {
        if (id == null || id.isEmpty()) return null;
        Potion cached = POTION_CACHE.get(id);
        if (cached != null || POTION_CACHE.containsKey(id)) return cached;
        Potion resolved = Potion.getPotionFromResourceLocation(id);
        POTION_CACHE.put(id, resolved);
        return resolved;
    }

    private static boolean thaumcraftLoaded() {
        Boolean cached = thaumcraftLoaded;
        if (cached == null) {
            cached = Loader.isModLoaded("thaumcraft");
            thaumcraftLoaded = cached;
        }
        return cached;
    }

    private static void applyNamedPotion(EntityPlayer player, String id, int duration, int amplifier) {
        Potion potion = potionById(id);
        if (potion == null) return;
        player.addPotionEffect(new PotionEffect(potion, duration, amplifier, true, false));
    }

    private static void removeNamedPotion(EntityPlayer player, String id) {
        Potion potion = potionById(id);
        if (potion == null) return;
        player.removePotionEffect(potion);
    }

    /**
     * Applies benefits for the granted Homestead band (1–3), not the raw score band.
     */
    private static void applyComfortBenefits(EntityPlayer player, int grantedBand) {
        int progressToAdd;
        int homesteadAmplifier;

        if (grantedBand == 1) {
            progressToAdd = 2;
            homesteadAmplifier = 0;
        } else if (grantedBand == 2) {
            progressToAdd = 3;
            homesteadAmplifier = 1;
        } else if (grantedBand >= 3) {
            progressToAdd = 6;
            homesteadAmplifier = 2;
        } else {
            return;
        }

        player.addPotionEffect(new PotionEffect(PotionHomestead.INSTANCE, HOMESTEAD_DURATION_TICKS, homesteadAmplifier, true, false));

        if (grantedBand == 1) {
            applyNamedPotion(player, LEARNING_POTION, EIGHT_MINUTES_TICKS, 0);
        } else {
            removeNamedPotion(player, LEARNING_POTION);
            int xpAmp = grantedBand >= 3 ? 1 : 0;
            applyNamedPotion(player, XP_BOOST_POTION, EIGHT_MINUTES_TICKS, xpAmp);
        }

        if (grantedBand == 2) {
            applyNamedPotion(player, "elenaidodge2:endurance", EIGHT_MINUTES_TICKS, 0);
            applyNamedPotion(player, "elenaidodge2:replenishment", FOUR_MINUTES_TICKS, 0);
        } else if (grantedBand >= 3) {
            applyNamedPotion(player, "elenaidodge2:endurance", EIGHT_MINUTES_TICKS, 1);
            applyNamedPotion(player, "elenaidodge2:replenishment", EIGHT_MINUTES_TICKS, 0);
        }

        if (progressToAdd > 0 && thaumcraftLoaded()) {
            NBTTagCompound data = player.getEntityData();
            NBTTagCompound persisted;
            if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
                persisted = new NBTTagCompound();
                data.setTag(EntityPlayer.PERSISTED_NBT_TAG, persisted);
            } else {
                persisted = data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
            }
            int stored = persisted.getInteger(WARP_CLEANSE_TAG);
            // Pre-1.8 0–100 bars would fire every scan at threshold 12.
            if (stored > WARP_CLEANSE_THRESHOLD) {
                stored = 0;
            }
            int currentProgress = stored + progressToAdd;
            if (currentProgress >= WARP_CLEANSE_THRESHOLD) {
                int currentWarp = ThaumcraftHelper.getWarp(player, 1);
                if (currentWarp > 0) {
                    ThaumcraftHelper.reduceWarp(player, 1, 1);
                    ThaumcraftHelper.syncWarp(player);
                }
                currentProgress = 0;
            }
            persisted.setInteger(WARP_CLEANSE_TAG, currentProgress);
        }
    }

    static final class ComfortEval {
        final boolean currentlyResting;
        final boolean restPose;
        final boolean furniture;
        final long hurtRemain;
        final long attackRemain;
        final float cozy;
        final float bonuses;
        final float penalties;
        final float tempPenalty;
        final float thirstPenalty;
        final float hungerPenalty;
        final float healthPenalty;
        final float potionPenalties;
        final float effective;
        final int scoreBand;
        final int grantedBand;
        final long promoteInTicks;
        final Map<String, Float> cappedByCategory;
        final List<ComfortSettings.PotionModifier> activeBonuses;
        final List<ComfortSettings.PotionModifier> activePenaltyEffects;

        ComfortEval(
                boolean currentlyResting,
                boolean restPose,
                boolean furniture,
                long hurtRemain,
                long attackRemain,
                float cozy,
                float bonuses,
                float penalties,
                float tempPenalty,
                float thirstPenalty,
                float hungerPenalty,
                float healthPenalty,
                float potionPenalties,
                float effective,
                int scoreBand,
                int grantedBand,
                long promoteInTicks,
                Map<String, Float> cappedByCategory,
                List<ComfortSettings.PotionModifier> activeBonuses,
                List<ComfortSettings.PotionModifier> activePenaltyEffects) {
            this.currentlyResting = currentlyResting;
            this.restPose = restPose;
            this.furniture = furniture;
            this.hurtRemain = hurtRemain;
            this.attackRemain = attackRemain;
            this.cozy = cozy;
            this.bonuses = bonuses;
            this.penalties = penalties;
            this.tempPenalty = tempPenalty;
            this.thirstPenalty = thirstPenalty;
            this.hungerPenalty = hungerPenalty;
            this.healthPenalty = healthPenalty;
            this.potionPenalties = potionPenalties;
            this.effective = effective;
            this.scoreBand = scoreBand;
            this.grantedBand = grantedBand;
            this.promoteInTicks = promoteInTicks;
            this.cappedByCategory = cappedByCategory;
            this.activeBonuses = activeBonuses;
            this.activePenaltyEffects = activePenaltyEffects;
        }
    }

    static final class CozyScan {
        static final CozyScan EMPTY = new CozyScan(0.0f, false, Map.of());

        final float score;
        final boolean hasEntryFurniture;
        final Map<String, Float> cappedByCategory;

        CozyScan(float score, boolean hasEntryFurniture, Map<String, Float> cappedByCategory) {
            this.score = score;
            this.hasEntryFurniture = hasEntryFurniture;
            this.cappedByCategory = cappedByCategory;
        }

        boolean meetsEntryFurniture() {
            return hasEntryFurniture;
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
