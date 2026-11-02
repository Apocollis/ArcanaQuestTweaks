package com.apocollis.aqtweaks.comfort;

import com.apocollis.aqtweaks.simpledifficulty.SimpleDifficultyHelper;
import com.apocollis.aqtweaks.thaumcraft.ThaumcraftHelper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Comfort rest loop. Furnishings are scanned every 30s. Bonuses and penalties refresh every 2s
 * while the player is in a home context. Rested lingers after the player leaves; Clarity does not.
 *
 * <p>Server only. Hot springs stay on their own tick.
 */
public class ComfortSystemHandler {

    private static final int CHECK_INTERVAL_TICKS = 600;
    private static final int STATE_INTERVAL_TICKS = 40;
    private static final int SCAN_STALE_TICKS = 1200;
    private static final double PET_RADIUS = 16.0;
    private static final String RESTING_TAG = "AQTComfortResting";
    private static final String GRANTED_BAND_TAG = "AQTComfortGrantedBand";
    private static final String BAND_SINCE_TAG = "AQTComfortBandSince";
    private static final String HURT_AT_TAG = "AQTComfortHurtAt";
    private static final String ATTACK_AT_TAG = "AQTComfortAttackAt";
    private static final String REST_POSE_AT = "AQTComfortRestPoseAt";
    private static final String REST_POSE_BONUS = "AQTComfortRestPoseBonus";
    private static final String WARP_CLEANSE_TAG = "WarpCleansingProgress";
    private static final int WARP_CLEANSE_THRESHOLD = 12;

    static final Map<String, CozyConfig> COZY_BLOCKS = new HashMap<>();
    private static final Map<Block, CozyConfig> COZY_BY_BLOCK = new java.util.concurrent.ConcurrentHashMap<>();
    private static final CozyConfig NO_COZY = new CozyConfig(0.0f, "");
    private static final Map<UUID, CachedScan> SCANS = new HashMap<>();

    static void clearCozyCache() {
        COZY_BY_BLOCK.clear();
    }

    static void clearScanCache() {
        SCANS.clear();
    }

    private static CozyConfig cozyFor(Block block) {
        CozyConfig cached = COZY_BY_BLOCK.get(block);
        if (cached != null) return cached == NO_COZY ? null : cached;
        ResourceLocation name = block.getRegistryName();
        CozyConfig found = name == null ? null : COZY_BLOCKS.get(name.toString());
        COZY_BY_BLOCK.put(block, found == null ? NO_COZY : found);
        return found;
    }

    static final Map<String, Integer> CATEGORY_LIMITS = new HashMap<>();
    static final Set<String> ENTRY_REQUIRE_CATEGORIES = new HashSet<>();
    static final Map<String, String> FAMILIES = new HashMap<>();
    static final Set<String> NATURAL_BLOCKS = new HashSet<>();
    static long DAMAGE_COOLDOWN_TICKS = 600L;
    static long ATTACK_COOLDOWN_TICKS = 300L;
    static float PET_COMFORT_VALUE = 3.0f;
    public static float THRESHOLD_HOMESTEAD_1 = 24.0f;
    public static float THRESHOLD_HOMESTEAD_2 = 48.0f;
    public static float THRESHOLD_HOMESTEAD_3 = 66.0f;
    static long PROMOTE_TICKS = 1200L;
    static long PROMOTE_TICKS_2 = 1200L;
    static long PROMOTE_TICKS_3 = 1200L;
    static int SCAN_RADIUS = 12;
    static int SCAN_DY = 2;
    static float REPEAT_DECAY = 0.6f;
    static final Map<String, Float> DECAY_OVERRIDES = new HashMap<>();
    static int BAND_MIN_CATEGORIES = 0;
    static final List<String> BAND_REQUIRE_2 = new ArrayList<>();
    static final List<String> BAND_REQUIRE_3 = new ArrayList<>();
    static String STRUCTURE_DECAY_KEY = "role_family";
    static float NON_SHELL_WEIGHT = 1.0f;
    static float UNLISTED_STRUCTURE_WEIGHT = 0.25f;
    static int VIGOR_BASE_INTERVAL = 30;
    static ComfortSettings.RestBonus REST_BONUS = new ComfortSettings.RestBonus();
    static ComfortSettings.ClarityConfig CLARITY = new ComfortSettings.ClarityConfig();
    static ComfortSettings.RestedConfig RESTED = new ComfortSettings.RestedConfig();
    static boolean PENALTIES_ENABLED = true;
    static boolean BONUSES_ENABLED = true;
    static ComfortSettings.TemperaturePenalty TEMP_PENALTY = ComfortSettings.TemperaturePenalty.defaults();
    static ComfortSettings.RatePenalty THIRST_PENALTY = ComfortSettings.RatePenalty.thirstDefaults();
    static ComfortSettings.RatePenalty HUNGER_PENALTY = ComfortSettings.RatePenalty.hungerDefaults();
    static ComfortSettings.HealthPenalty HEALTH_PENALTY = ComfortSettings.HealthPenalty.defaults();
    static List<ComfortSettings.PotionModifier> POTION_PENALTIES = ComfortSettings.defaultPenaltyEffects();
    static List<ComfortSettings.PotionModifier> POTION_BONUSES = ComfortSettings.defaultBonusEffects();

    private static final Map<String, Potion> POTION_CACHE = new HashMap<>();
    private static Boolean thaumcraftLoaded;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        EntityPlayer player = event.player;
        if (player == null || player instanceof FakePlayer) return;
        World world = player.world;
        if (world == null || world.isRemote) return;
        RestedBonuses.flush(player);

        int phase = player.ticksExisted + player.getEntityId();
        boolean scanTick = phase % CHECK_INTERVAL_TICKS == 0;
        boolean stateTick = phase % STATE_INTERVAL_TICKS == 0;
        if (!scanTick && !stateTick) return;

        CachedScan cache = SCANS.get(player.getUniqueID());
        boolean stale = isStale(player, cache);
        boolean home = isHome(player, cache);
        if (scanTick || (stateTick && home && stale)) {
            cache = scanFurnishings(player);
            home = isHome(player, cache);
        }
        if (!scanTick && !(stateTick && home)) return;

        ComfortEval eval = finishEval(player, cache.scan);
        int comfortEvent = ComfortFeedback.EVENT_NONE;
        if (scanTick || isComfortResting(player)) {
            comfortEvent = applyEvaluation(player, eval, scanTick);
            eval = finishEval(player, cache.scan);
        }
        ComfortFeedback.send(player, eval, comfortEvent, false);
    }

    @SubscribeEvent
    public void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntityLiving() instanceof EntityPlayer player)) return;
        if (player.world == null || player.world.isRemote || player instanceof FakePlayer) return;
        forceState(player, true);
    }

    @SubscribeEvent
    public void onWake(PlayerWakeUpEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world == null || player.world.isRemote || player instanceof FakePlayer) return;
        forceState(player, true);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player == null) return;
        UUID id = event.player.getUniqueID();
        SCANS.remove(id);
        ComfortFeedback.clearPlayer(id);
    }

    @SubscribeEvent
    public void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player == null) return;
        SCANS.remove(event.player.getUniqueID());
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.player instanceof EntityPlayerMP player)) return;
        if (player.world == null || player.world.isRemote) return;
        ComfortFeedback.sendLogin(player);
    }

    @SubscribeEvent
    public void onPlace(BlockEvent.PlaceEvent event) {
        if (event.getWorld() == null || event.getWorld().isRemote) return;
        EntityPlayer player = event.getPlayer();
        if (!(player instanceof EntityPlayerMP mp) || player instanceof FakePlayer) return;
        CachedScan cache = SCANS.get(player.getUniqueID());
        if (!isHome(player, cache)) return;
        IBlockState state = event.getPlacedBlock();
        ResourceLocation name = state.getBlock().getRegistryName();
        if (name == null) return;
        boolean adds = cache != null && wouldAddScore(cache.scan, name.toString());
        ComfortFeedback.sendPlaced(mp, event.getPos(), adds);
    }

    /**
     * Apply Cold Resistance while standing or submerged in Biomes O' Plenty hot spring water.
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
        if (event.getAmount() <= 0.0f) return;
        boolean wasResting = isComfortResting(player);
        if (wasResting) {
            endResting(player);
        }
        player.getEntityData().setLong(HURT_AT_TAG, player.world.getTotalWorldTime());
        if (wasResting) {
            ComfortFeedback.send(player, evalNow(player), ComfortFeedback.EVENT_HURT, true);
        }
    }

    @SubscribeEvent
    public void onPlayerAttack(AttackEntityEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player == null || player.world == null || player.world.isRemote) return;
        boolean wasResting = isComfortResting(player);
        if (wasResting) {
            endResting(player);
        }
        player.getEntityData().setLong(ATTACK_AT_TAG, player.world.getTotalWorldTime());
        if (wasResting) {
            ComfortFeedback.send(player, evalNow(player), ComfortFeedback.EVENT_ATTACK, true);
        }
    }

    private void forceState(EntityPlayer player, boolean allowEntry) {
        CachedScan cache = SCANS.get(player.getUniqueID());
        if (isStale(player, cache)) {
            cache = scanFurnishings(player);
        }
        ComfortEval eval = finishEval(player, cache.scan);
        int comfortEvent = ComfortFeedback.EVENT_NONE;
        if (allowEntry || isComfortResting(player)) {
            comfortEvent = applyEvaluation(player, eval, false);
            eval = finishEval(player, cache.scan);
        }
        ComfortFeedback.send(player, eval, comfortEvent, true);
    }

    private static boolean isStale(EntityPlayer player, CachedScan cache) {
        if (cache == null || player.world == null) return true;
        if (cache.dimension != player.world.provider.getDimension()) return true;
        return player.world.getTotalWorldTime() - cache.time > SCAN_STALE_TICKS;
    }

    private static boolean isHome(EntityPlayer player, CachedScan cache) {
        if (isComfortResting(player)) return true;
        if (player.isPotionActive(PotionHomestead.INSTANCE)) return true;
        return cache != null && cache.scan.hasEntryFurniture;
    }

    private static ComfortEval evalNow(EntityPlayer player) {
        CachedScan cache = SCANS.get(player.getUniqueID());
        if (cache != null && !isStale(player, cache)) {
            return finishEval(player, cache.scan);
        }
        return evaluate(player);
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

    /** Fresh scan, then bonuses and penalties. Used by {@code /aqcomfort} and the sleep credit. */
    static ComfortEval evaluate(EntityPlayer player) {
        return finishEval(player, scanFurnishings(player).scan);
    }

    /**
     * Same ladder as the tick. {@code advanceWarp} is true only on the 30s furnishing scan.
     * @return a {@link ComfortFeedback} event code
     */
    static int applyEvaluation(EntityPlayer player, ComfortEval eval, boolean advanceWarp) {
        World world = player.world;
        if (world == null) return ComfortFeedback.EVENT_NONE;

        if (!eval.currentlyResting) {
            if (!eval.restPose || eval.hurtRemain > 0L || eval.attackRemain > 0L) return ComfortFeedback.EVENT_NONE;
            if (!eval.furniture || eval.scoreBand <= 0) return ComfortFeedback.EVENT_NONE;
            startLadder(player, world, 1);
            setComfortResting(player, true);
            applyComfortBenefits(player, 1, advanceWarp);
            return ComfortFeedback.EVENT_ENTERED;
        }

        if (!eval.furniture || eval.scoreBand <= 0) {
            endResting(player);
            return eval.furniture ? ComfortFeedback.EVENT_DEMOTED : ComfortFeedback.EVENT_LOST_FURNITURE;
        }

        int granted = eval.grantedBand < 1 ? 1 : eval.grantedBand;
        int comfortEvent = ComfortFeedback.EVENT_NONE;
        if (eval.scoreBand < granted) {
            granted = eval.scoreBand;
            stampLadder(player, world, granted);
            comfortEvent = ComfortFeedback.EVENT_DEMOTED;
        } else if (eval.scoreBand > granted && world.getTotalWorldTime() - getBandSince(player) >= promoteTicksFor(granted)) {
            granted = Math.min(granted + 1, eval.scoreBand);
            stampLadder(player, world, granted);
            comfortEvent = ComfortFeedback.EVENT_PROMOTED;
        }
        applyComfortBenefits(player, granted, advanceWarp);
        return comfortEvent;
    }

    private static long promoteTicksFor(int grantedBand) {
        if (grantedBand >= 2) return PROMOTE_TICKS_3 > 0L ? PROMOTE_TICKS_3 : PROMOTE_TICKS;
        return PROMOTE_TICKS_2 > 0L ? PROMOTE_TICKS_2 : PROMOTE_TICKS;
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
        player.getEntityData().setBoolean(RESTING_TAG, resting);
    }

    /** Ends the at-home state. Rested keeps counting down. Clarity is removed. */
    private static void endResting(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        data.setBoolean(RESTING_TAG, false);
        data.removeTag(GRANTED_BAND_TAG);
        data.removeTag(BAND_SINCE_TAG);
        player.removePotionEffect(PotionClarity.INSTANCE);
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

    private static int capBand(int band, CozyScan cozy) {
        if (BAND_MIN_CATEGORIES > 0 && cozy.positiveCategories < BAND_MIN_CATEGORIES) {
            band = 0;
        }
        if (band >= 3 && !hasCategories(cozy, BAND_REQUIRE_3)) band = 2;
        if (band >= 2 && !hasCategories(cozy, BAND_REQUIRE_2)) band = 1;
        return band;
    }

    private static boolean hasCategories(CozyScan cozy, List<String> required) {
        if (required == null || required.isEmpty()) return true;
        for (String category : required) {
            if (category == null || category.isEmpty()) continue;
            if (cozy.seen.getOrDefault(category, 0) <= 0) return false;
        }
        return true;
    }

    private static CachedScan scanFurnishings(EntityPlayer player) {
        CozyScan scan = calculateCozyScan(player);
        World world = player.world;
        CachedScan cached = new CachedScan(
                scan,
                world == null ? 0L : world.getTotalWorldTime(),
                world == null ? 0 : world.provider.getDimension());
        SCANS.put(player.getUniqueID(), cached);
        return cached;
    }

    private static CozyScan calculateCozyScan(EntityPlayer player) {
        World world = player.world;
        if (world == null) return CozyScan.EMPTY;

        BlockPos center = player.getPosition();
        Map<String, List<Piece>> pieces = new HashMap<>();
        boolean hasEntryFurniture = ENTRY_REQUIRE_CATEGORIES.isEmpty();
        Map<Long, Byte> shell = ShellSampler.sample(player);
        int radius = Math.max(1, Math.min(16, SCAN_RADIUS));
        int dy = Math.max(0, Math.min(4, SCAN_DY));
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int y = -dy; y <= dy; y++) {
                    cursor.setPos(center.getX() + dx, center.getY() + y, center.getZ() + dz);
                    if (!world.isBlockLoaded(cursor)) continue;
                    IBlockState state = world.getBlockState(cursor);
                    Material material = state.getMaterial();
                    if (material == Material.AIR || material.isLiquid()) continue;
                    Block block = state.getBlock();
                    ResourceLocation registryName = block.getRegistryName();
                    if (registryName == null) continue;
                    String id = registryName.toString();
                    CozyConfig config = cozyFor(block);
                    float weight;
                    String category;
                    if (config != null) {
                        weight = config.weight;
                        category = config.category;
                    } else if (isNatural(id) || UNLISTED_STRUCTURE_WEIGHT <= 0.0f) {
                        continue;
                    } else {
                        weight = UNLISTED_STRUCTURE_WEIGHT;
                        category = "structure";
                    }
                    byte role = 0;
                    if ("structure".equals(category)) {
                        Byte tagged = shell.get(cursor.toLong());
                        role = tagged == null ? 0 : tagged.byteValue();
                        if (role == 0) {
                            weight *= NON_SHELL_WEIGHT;
                        }
                    }
                    if (weight == 0.0f) continue;
                    pieces.computeIfAbsent(category, k -> new ArrayList<>())
                            .add(new Piece(id, ComfortFamilies.family(id), weight, role));
                    if (!hasEntryFurniture && ENTRY_REQUIRE_CATEGORIES.contains(category)) {
                        hasEntryFurniture = true;
                    }
                }
            }
        }

        AxisAlignedBB searchBox = petSearchBox(center);
        for (EntityTameable pet : world.getEntitiesWithinAABB(EntityTameable.class, searchBox)) {
            if (pet.isTamed() && pet.getOwnerId() != null && pet.getOwnerId().equals(player.getUniqueID())) {
                pieces.computeIfAbsent("pets", k -> new ArrayList<>())
                        .add(new Piece("pet", "pet", PET_COMFORT_VALUE, (byte) 0));
            }
        }

        float total = 0.0f;
        Map<String, Float> capped = new LinkedHashMap<>();
        Map<String, Integer> seen = new LinkedHashMap<>();
        Map<String, Integer> counted = new LinkedHashMap<>();
        Map<String, Integer> familyCounts = new HashMap<>();
        Map<String, Integer> roles = new LinkedHashMap<>();
        List<String> samples = new ArrayList<>();
        int positive = 0;
        boolean roleFamily = "role_family".equalsIgnoreCase(STRUCTURE_DECAY_KEY);

        for (Map.Entry<String, List<Piece>> entry : pieces.entrySet()) {
            String category = entry.getKey();
            List<Piece> list = entry.getValue();
            seen.put(category, list.size());
            float decay = DECAY_OVERRIDES.getOrDefault(category, REPEAT_DECAY);
            if (decay < 0.0f) decay = 0.0f;
            if (decay > 1.0f) decay = 1.0f;
            Map<String, List<Piece>> groups = new HashMap<>();
            for (Piece piece : list) {
                String key = ("structure".equals(category) && roleFamily)
                        ? ShellSampler.roleName(piece.role) + "|" + piece.family
                        : piece.family;
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(piece);
                familyCounts.merge(category + "|" + piece.family, 1, Integer::sum);
            }
            List<Scored> scored = new ArrayList<>();
            for (List<Piece> group : groups.values()) {
                group.sort(Comparator.comparingDouble((Piece piece) -> piece.weight).reversed());
                for (int i = 0; i < group.size(); i++) {
                    Piece piece = group.get(i);
                    float value = piece.weight * (float) Math.pow(decay, i);
                    scored.add(new Scored(piece, value));
                }
            }
            scored.sort(Comparator.comparingDouble((Scored item) -> item.value).reversed());
            int limit = CATEGORY_LIMITS.getOrDefault(category, 1);
            int take = Math.min(scored.size(), Math.max(0, limit));
            float categoryTotal = 0.0f;
            for (int i = 0; i < take; i++) {
                Scored item = scored.get(i);
                categoryTotal += item.value;
                if ("structure".equals(category)) {
                    String roleName = ShellSampler.roleName(item.piece.role);
                    roles.merge(roleName, 1, Integer::sum);
                    if (samples.size() < 6) {
                        samples.add(item.piece.id + " " + roleName);
                    }
                }
            }
            counted.put(category, take);
            capped.put(category, categoryTotal);
            total += categoryTotal;
            if (categoryTotal > 0.0f) positive++;
        }
        return new CozyScan(total, hasEntryFurniture, capped, seen, counted, familyCounts, roles, samples, positive);
    }

    private static boolean isNatural(String id) {
        if (NATURAL_BLOCKS.contains(id)) return true;
        String path = id;
        int colon = id.indexOf(':');
        if (colon >= 0) path = id.substring(colon + 1);
        return path.contains("_ore") || path.startsWith("ore_");
    }

    private static boolean wouldAddScore(CozyScan scan, String id) {
        CozyConfig config = COZY_BLOCKS.get(id);
        String category;
        if (config != null) {
            category = config.category;
        } else if (isNatural(id) || UNLISTED_STRUCTURE_WEIGHT <= 0.0f) {
            return false;
        } else {
            category = "structure";
        }
        int limit = CATEGORY_LIMITS.getOrDefault(category, 1);
        int have = scan.counted.getOrDefault(category, 0);
        if (have >= limit) return false;
        float decay = DECAY_OVERRIDES.getOrDefault(category, REPEAT_DECAY);
        int seenFamily = scan.familyCounts.getOrDefault(category + "|" + ComfortFamilies.family(id), 0);
        return Math.pow(Math.max(0.0f, decay), seenFamily) >= 0.2;
    }

    private static AxisAlignedBB petSearchBox(BlockPos center) {
        int x = center.getX();
        int y = center.getY();
        int z = center.getZ();
        return new AxisAlignedBB(
                x - PET_RADIUS, y - PET_RADIUS, z - PET_RADIUS,
                x + 1 + PET_RADIUS, y + 1 + PET_RADIUS, z + 1 + PET_RADIUS);
    }

    private static ComfortEval finishEval(EntityPlayer player, CozyScan cozy) {
        World world = player.world;
        float restBonus = world == null ? 0.0f : restBonus(player, world);
        if (world == null) {
            return new ComfortEval(
                    false, false, cozy.hasEntryFurniture, 0L, 0L,
                    cozy.score, restBonus, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                    Math.max(0.0f, cozy.score + restBonus), scoreBand(Math.max(0.0f, cozy.score + restBonus)),
                    0, -1L, false, cozy, Collections.emptyList(), Collections.emptyList(), 0, 0.0f, 0, 0, 0);
        }
        List<ComfortSettings.PotionModifier> activeBonuses = activeModifiers(player, POTION_BONUSES, BONUSES_ENABLED);
        List<ComfortSettings.PotionModifier> activePenaltyEffects = activeModifiers(player, POTION_PENALTIES, PENALTIES_ENABLED);
        float bonusTotal = restBonus;
        for (ComfortSettings.PotionModifier modifier : activeBonuses) {
            bonusTotal += modifier.amount;
        }
        boolean heat = false;
        float temp = 0.0f;
        if (PENALTIES_ENABLED) {
            float[] temperature = temperaturePenalty(player);
            temp = temperature[0];
            heat = temperature[1] > 0.0f;
        }
        float thirst = PENALTIES_ENABLED ? thirstPenalty(player) : 0.0f;
        float hunger = PENALTIES_ENABLED ? hungerPenalty(player) : 0.0f;
        float health = PENALTIES_ENABLED ? healthPenalty(player) : 0.0f;
        float potionPenalties = 0.0f;
        for (ComfortSettings.PotionModifier modifier : activePenaltyEffects) {
            potionPenalties += modifier.amount;
        }
        float penaltyTotal = temp + thirst + hunger + health + potionPenalties;
        float effective = Math.max(0.0f, cozy.score + bonusTotal - penaltyTotal);
        boolean resting = isComfortResting(player);
        int granted = resting ? getGrantedBand(player) : 0;
        int band = capBand(scoreBand(effective), cozy);
        long promoteIn = -1L;
        if (resting && band > granted && granted >= 1 && granted < 3) {
            promoteIn = Math.max(0L, promoteTicksFor(granted) - (world.getTotalWorldTime() - getBandSince(player)));
        }
        int tier = RestedBonuses.restedTier(player);
        float insight = tier == 1 ? RESTED.insight_1 : tier == 2 ? RESTED.insight_2 : tier >= 3 ? RESTED.insight_3 : 0.0f;
        int vigor = tier == 1 ? RESTED.vigor_1 : tier == 2 ? RESTED.vigor_2 : tier >= 3 ? RESTED.vigor_3 : VIGOR_BASE_INTERVAL;
        int hearts = tier <= 0 ? 0 : 2 * tier;
        int feathers = tier == 1 ? RESTED.stamina_1 : tier == 2 ? RESTED.stamina_2 : tier >= 3 ? RESTED.stamina_3 : 0;
        return new ComfortEval(
                resting,
                isPlayerResting(player),
                cozy.hasEntryFurniture,
                remainingHurtTicks(player, world),
                remainingAttackTicks(player, world),
                cozy.score,
                restBonus,
                bonusTotal,
                penaltyTotal,
                temp,
                thirst,
                hunger,
                health,
                potionPenalties,
                effective,
                band,
                granted,
                promoteIn,
                heat,
                cozy,
                activeBonuses,
                activePenaltyEffects,
                ComfortFeedback.restedTicks(player),
                insight,
                vigor,
                hearts,
                feathers);
    }

    private static float restBonus(EntityPlayer player, World world) {
        if (REST_BONUS == null || !REST_BONUS.enabled) return 0.0f;
        boolean lying = player.isPlayerSleeping();
        boolean sitting = isSeat(player);
        NBTTagCompound data = player.getEntityData();
        if (lying || sitting) {
            data.setLong(REST_POSE_AT, world.getTotalWorldTime());
            data.setFloat(REST_POSE_BONUS, lying ? REST_BONUS.lying : REST_BONUS.sitting);
        }
        long at = data.getLong(REST_POSE_AT);
        if (at == 0L) return 0.0f;
        int grace = Math.max(0, REST_BONUS.grace_ticks);
        if (world.getTotalWorldTime() - at > grace) return 0.0f;
        return data.getFloat(REST_POSE_BONUS);
    }

    private static boolean isSeat(EntityPlayer player) {
        Entity ridden = player.getRidingEntity();
        if (ridden == null) return false;
        if (ridden instanceof EntityLivingBase || ridden instanceof EntityBoat || ridden instanceof EntityMinecart) {
            return false;
        }
        ResourceLocation key = EntityList.getKey(ridden);
        if (key != null && REST_BONUS.ignore_vehicles != null) {
            String id = key.toString();
            for (String ignored : REST_BONUS.ignore_vehicles) {
                if (id.equals(ignored)) return false;
            }
        }
        return true;
    }

    private static float[] temperaturePenalty(EntityPlayer player) {
        if (TEMP_PENALTY == null || !TEMP_PENALTY.enabled) return new float[] {0.0f, 0.0f};
        Integer temp = SimpleDifficultyHelper.temperatureOrNull(player);
        if (temp == null) return new float[] {0.0f, 0.0f};
        int t = temp;
        if (t >= TEMP_PENALTY.comfort_min && t <= TEMP_PENALTY.comfort_max) return new float[] {0.0f, 0.0f};
        boolean heat = t > TEMP_PENALTY.comfort_max;
        if (heat && hasAnyPotion(player, TEMP_PENALTY.heat_ignore_potions)) return new float[] {0.0f, 0.0f};
        if (!heat && hasAnyPotion(player, TEMP_PENALTY.cold_ignore_potions)) return new float[] {0.0f, 0.0f};
        int outside = heat ? t - TEMP_PENALTY.comfort_max : TEMP_PENALTY.comfort_min - t;
        return new float[] {outside * TEMP_PENALTY.per_point_outside, heat ? 1.0f : 0.0f};
    }

    private static float thirstPenalty(EntityPlayer player) {
        if (THIRST_PENALTY == null || !THIRST_PENALTY.enabled) return 0.0f;
        Integer thirst = SimpleDifficultyHelper.thirstOrNull(player);
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
        float base = player.getMaxHealth();
        PotionEffect rested = player.getActivePotionEffect(PotionHomestead.INSTANCE);
        if (rested != null) {
            base -= 2.0f * (rested.getAmplifier() + 1);
        }
        if (base < 1.0f) base = 1.0f;
        float ratio = player.getHealth() / base;
        if (ratio > 1.0f) ratio = 1.0f;
        float missingFraction = 1.0f - ratio;
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

    private static void applyComfortBenefits(EntityPlayer player, int grantedBand, boolean advanceWarp) {
        if (grantedBand < 1) return;
        int amplifier = Math.min(2, grantedBand - 1);
        int duration = grantedBand >= 3 ? RESTED.duration_3 : grantedBand == 2 ? RESTED.duration_2 : RESTED.duration_1;
        if (duration < 1) duration = 1;
        PotionEffect current = player.getActivePotionEffect(PotionHomestead.INSTANCE);
        boolean replace = current != null && current.getAmplifier() != amplifier;
        RestedBonuses.beginReplace();
        try {
            if (replace) {
                player.removePotionEffect(PotionHomestead.INSTANCE);
            }
            player.addPotionEffect(new PotionEffect(PotionHomestead.INSTANCE, duration, amplifier, true, false));
        } finally {
            RestedBonuses.endReplace();
        }
        RestedBonuses.reconcile(player);
        applyTierPotions(player, grantedBand);
        boolean clarity = applyClarity(player, amplifier);
        if (advanceWarp && clarity) {
            advanceWarp(player, grantedBand);
        }
    }

    private static void applyTierPotions(EntityPlayer player, int grantedBand) {
        List<ComfortSettings.TierPotion> active = potionsFor(grantedBand);
        for (ComfortSettings.TierPotion potion : allTierPotions()) {
            if (potion != null && potion.learning && grantedBand >= 2) {
                removeNamedPotion(player, potion.potion);
            }
        }
        for (ComfortSettings.TierPotion potion : active) {
            if (potion == null || potion.potion == null || potion.potion.isEmpty()) continue;
            if (potion.learning && grantedBand >= 2) continue;
            applyNamedPotion(player, potion.potion, Math.max(1, potion.duration), Math.max(0, potion.amplifier));
        }
    }

    private static List<ComfortSettings.TierPotion> potionsFor(int band) {
        if (band >= 3) return RESTED.potions_3 == null ? Collections.emptyList() : RESTED.potions_3;
        if (band == 2) return RESTED.potions_2 == null ? Collections.emptyList() : RESTED.potions_2;
        return RESTED.potions_1 == null ? Collections.emptyList() : RESTED.potions_1;
    }

    private static List<ComfortSettings.TierPotion> allTierPotions() {
        List<ComfortSettings.TierPotion> all = new ArrayList<>();
        if (RESTED.potions_1 != null) all.addAll(RESTED.potions_1);
        if (RESTED.potions_2 != null) all.addAll(RESTED.potions_2);
        if (RESTED.potions_3 != null) all.addAll(RESTED.potions_3);
        return all;
    }

    private static boolean applyClarity(EntityPlayer player, int amplifier) {
        if (!isComfortResting(player) || !thaumcraftLoaded()) {
            player.removePotionEffect(PotionClarity.INSTANCE);
            return false;
        }
        if (CLARITY.require_temp_warp && ThaumcraftHelper.getWarp(player, 1) <= 0) {
            player.removePotionEffect(PotionClarity.INSTANCE);
            return false;
        }
        int duration = CLARITY.duration_ticks > 0 ? CLARITY.duration_ticks : 900;
        player.addPotionEffect(new PotionEffect(PotionClarity.INSTANCE, duration, amplifier, true, false));
        return true;
    }

    private static void advanceWarp(EntityPlayer player, int grantedBand) {
        int progressToAdd = grantedBand >= 3 ? 6 : grantedBand == 2 ? 3 : 2;
        NBTTagCompound persisted = persisted(player);
        int stored = persisted.getInteger(WARP_CLEANSE_TAG);
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

    /**
     * Credit temporary-warp cleansing for game time the player ticks did not cover.
     * One 30s scan is worth 2 / 3 / 6 progress at granted band I / II / III.
     *
     * @return temporary warp points cleared
     */
    public static int applyAcceleratedWarpCleanse(EntityPlayer player, long uncoveredTicks) {
        if (player == null || player.world == null || player.world.isRemote || player instanceof FakePlayer) return 0;
        if (uncoveredTicks < CHECK_INTERVAL_TICKS || !thaumcraftLoaded()) return 0;

        int band;
        if (isComfortResting(player)) {
            band = getGrantedBand(player);
        } else {
            ComfortEval eval = evaluate(player);
            band = eval.furniture ? eval.scoreBand : 0;
        }
        int perScan = band == 1 ? 2 : (band == 2 ? 3 : (band >= 3 ? 6 : 0));
        if (perScan == 0) return 0;

        NBTTagCompound persisted = persisted(player);
        int stored = persisted.getInteger(WARP_CLEANSE_TAG);
        if (stored > WARP_CLEANSE_THRESHOLD) {
            stored = 0;
        }
        long total = stored + uncoveredTicks * perScan / CHECK_INTERVAL_TICKS;
        int due = (int) (total / WARP_CLEANSE_THRESHOLD);
        int remainder = (int) (total % WARP_CLEANSE_THRESHOLD);
        int cleared = 0;
        if (due > 0) {
            int warp = ThaumcraftHelper.getWarp(player, 1);
            cleared = Math.min(due, warp);
            if (cleared > 0) {
                ThaumcraftHelper.reduceWarp(player, 1, cleared);
                ThaumcraftHelper.syncWarp(player);
            }
            if (cleared < due) {
                remainder = 0;
            }
        }
        persisted.setInteger(WARP_CLEANSE_TAG, remainder);
        return cleared;
    }

    private static NBTTagCompound persisted(EntityPlayer player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) {
            data.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        return data.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    static final class ComfortEval {
        final boolean currentlyResting;
        final boolean restPose;
        final boolean furniture;
        final long hurtRemain;
        final long attackRemain;
        final float cozy;
        final float restBonus;
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
        final boolean heatPenalty;
        final Map<String, Float> cappedByCategory;
        final Map<String, Integer> categorySeen;
        final Map<String, Integer> categoryCounted;
        final Map<String, Integer> roleCounts;
        final List<String> structureSamples;
        final List<ComfortSettings.PotionModifier> activeBonuses;
        final List<ComfortSettings.PotionModifier> activePenaltyEffects;
        final int restedTicks;
        final float insight;
        final int vigorInterval;
        final int halfHearts;
        final int halfFeathers;

        ComfortEval(
                boolean currentlyResting,
                boolean restPose,
                boolean furniture,
                long hurtRemain,
                long attackRemain,
                float cozy,
                float restBonus,
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
                boolean heatPenalty,
                CozyScan cozyScan,
                List<ComfortSettings.PotionModifier> activeBonuses,
                List<ComfortSettings.PotionModifier> activePenaltyEffects,
                int restedTicks,
                float insight,
                int vigorInterval,
                int halfHearts,
                int halfFeathers) {
            this.currentlyResting = currentlyResting;
            this.restPose = restPose;
            this.furniture = furniture;
            this.hurtRemain = hurtRemain;
            this.attackRemain = attackRemain;
            this.cozy = cozy;
            this.restBonus = restBonus;
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
            this.heatPenalty = heatPenalty;
            this.cappedByCategory = cozyScan.cappedByCategory;
            this.categorySeen = cozyScan.seen;
            this.categoryCounted = cozyScan.counted;
            this.roleCounts = cozyScan.roles;
            this.structureSamples = cozyScan.samples;
            this.activeBonuses = activeBonuses;
            this.activePenaltyEffects = activePenaltyEffects;
            this.restedTicks = restedTicks;
            this.insight = insight;
            this.vigorInterval = vigorInterval;
            this.halfHearts = halfHearts;
            this.halfFeathers = halfFeathers;
        }
    }

    static final class CozyScan {
        static final CozyScan EMPTY = new CozyScan(
                0.0f, false, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), List.of(), 0);

        final float score;
        final boolean hasEntryFurniture;
        final Map<String, Float> cappedByCategory;
        final Map<String, Integer> seen;
        final Map<String, Integer> counted;
        final Map<String, Integer> familyCounts;
        final Map<String, Integer> roles;
        final List<String> samples;
        final int positiveCategories;

        CozyScan(float score, boolean hasEntryFurniture, Map<String, Float> cappedByCategory,
                Map<String, Integer> seen, Map<String, Integer> counted, Map<String, Integer> familyCounts,
                Map<String, Integer> roles, List<String> samples, int positiveCategories) {
            this.score = score;
            this.hasEntryFurniture = hasEntryFurniture;
            this.cappedByCategory = cappedByCategory;
            this.seen = seen;
            this.counted = counted;
            this.familyCounts = familyCounts;
            this.roles = roles;
            this.samples = samples;
            this.positiveCategories = positiveCategories;
        }
    }

    private static final class CachedScan {
        final CozyScan scan;
        final long time;
        final int dimension;

        CachedScan(CozyScan scan, long time, int dimension) {
            this.scan = scan;
            this.time = time;
            this.dimension = dimension;
        }
    }

    private static final class Piece {
        final String id;
        final String family;
        final float weight;
        final byte role;

        Piece(String id, String family, float weight, byte role) {
            this.id = id;
            this.family = family;
            this.weight = weight;
            this.role = role;
        }
    }

    private static final class Scored {
        final Piece piece;
        final float value;

        Scored(Piece piece, float value) {
            this.piece = piece;
            this.value = value;
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
