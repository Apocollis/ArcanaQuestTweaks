package com.apocollis.aqtweaks.aurorian;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.hazard.HazardEvent;
import com.apocollis.aqtweaks.hazard.HazardState;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

/**
 * Snow that piles up while a Moonfrost storm rages, and the burial hazard that goes with it. Server side.
 *
 * <p>Like vanilla snowfall it samples the loaded chunks around players, but faster and up to 16 layers deep, and
 * smoothly: a layer meant for a column goes to a lower neighbour when it would leave the column more than
 * {@code Snow Max Step} layers deeper ({@link SnowLeveling}). It only ever places vanilla snow layers into air (never
 * replacing a block), keeps no state, and leaves melting to vanilla ({@code BlockSnow} melts near bright light).
 */
public class MoonfrostSnowHandler {

    public static final DamageSource DAMAGE_SNOW_BURIAL = new DamageSource("aqtweaks_snow_burial").setDamageBypassesArmor();
    static final String NBT_DEBT = "AQTSnowAirDebt";

    private final Random rand = new Random();
    private final SnowRate.Carry carry = new SnowRate.Carry();
    private long[] chunkKeys = new long[0];
    private long keysBuiltAt = Long.MIN_VALUE;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private final Long2IntOpenHashMap depthCache = new Long2IntOpenHashMap();
    private final Long2IntOpenHashMap topCache = new Long2IntOpenHashMap();
    private double windX;
    private double windZ;

    // ---- accumulation ----

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || !(event.world instanceof WorldServer)) return;
        if (!MoonfrostState.isAurorian(event.world)) return;
        WorldServer world = (WorldServer) event.world;
        if (!ArcanaQuestTweaksConfig.AurorianModuleConfig.enableSnowAccumulation || !MoonfrostState.enabled()) {
            carry.reset();
            return;
        }
        HazardState state = MoonfrostState.get(world);
        if (state.phase != HazardEvent.Phase.ACTIVE || state.strength < SnowRate.MIN_STRENGTH) {
            carry.reset();
            return;
        }
        long now = world.getTotalWorldTime();
        if (keysBuiltAt == Long.MIN_VALUE || now - keysBuiltAt >= 20L) {
            rebuildChunks(world);
            keysBuiltAt = now;
        }
        if (chunkKeys.length == 0) return;

        int maxChanges = ArcanaQuestTweaksConfig.AurorianModuleConfig.snowMaxChangesPerTick;
        int attempts = carry.take(chunkKeys.length, ArcanaQuestTweaksConfig.AurorianModuleConfig.snowLayersPerChunkPerSecond,
                state.strength);
        attempts = Math.min(attempts, maxChanges * 3);
        if (attempts <= 0) return;

        Vec3d wind = AurorianWindHelper.getWindVector(world, true);
        windX = wind.x;
        windZ = wind.z;
        int changes = 0;
        for (int i = 0; i < attempts && changes < maxChanges; i++) {
            long key = chunkKeys[rand.nextInt(chunkKeys.length)];
            int cx = (int) key;
            int cz = (int) (key >> 32);
            if (world.getChunkProvider().getLoadedChunk(cx, cz) == null) continue;
            int x = (cx << 4) + rand.nextInt(16);
            int z = (cz << 4) + rand.nextInt(16);
            if (placeOne(world, x, z)) changes++;
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote || !MoonfrostState.isAurorian(event.getWorld())) return;
        chunkKeys = new long[0];
        keysBuiltAt = Long.MIN_VALUE;
        carry.reset();
        depthCache.clear();
        topCache.clear();
    }

    /** The loaded chunks within the radius of any non-spectator player; a chunk near two players is listed once. */
    private void rebuildChunks(WorldServer world) {
        LongOpenHashSet set = new LongOpenHashSet();
        int radius = ArcanaQuestTweaksConfig.AurorianModuleConfig.snowAccumulationRadius;
        int rc = (radius >> 4) + 1;
        double reach = radius + 11.0D;
        for (EntityPlayer player : world.playerEntities) {
            if (player.isSpectator() || player.isDead) continue;
            int pcx = (int) Math.floor(player.posX) >> 4;
            int pcz = (int) Math.floor(player.posZ) >> 4;
            for (int dx = -rc; dx <= rc; dx++) {
                for (int dz = -rc; dz <= rc; dz++) {
                    int cx = pcx + dx;
                    int cz = pcz + dz;
                    double ox = cx * 16.0D + 8.0D - player.posX;
                    double oz = cz * 16.0D + 8.0D - player.posZ;
                    if (ox * ox + oz * oz > reach * reach) continue;
                    if (world.getChunkProvider().getLoadedChunk(cx, cz) == null) continue;
                    set.add(((long) cx & 0xFFFFFFFFL) | ((long) cz << 32));
                }
            }
        }
        chunkKeys = set.toLongArray();
    }

    /**
     * Reads a column: its snow depth in layers (a full 8 layer block continues the stack upward), caching the Y where
     * the next layer would go. -1 when the column cannot hold snow (unloaded, roofed, water, a plant or any
     * non-air block at the surface, or no valid base).
     */
    private int readColumn(WorldServer world, long col) {
        if (depthCache.containsKey(col)) return depthCache.get(col);
        int x = SnowLeveling.unpackX(col);
        int z = SnowLeveling.unpackZ(col);
        int depth = -1;
        int topY = -1;
        Chunk chunk = world.getChunkProvider().getLoadedChunk(x >> 4, z >> 4);
        if (chunk != null) {
            int y = chunk.getPrecipitationHeight(new BlockPos(x, 0, z)).getY();
            int d = 0;
            while (y >= 1 && y < 255) {
                IBlockState st = chunk.getBlockState(x, y, z);
                if (st.getBlock() == Blocks.SNOW_LAYER) {
                    int layers = st.getValue(BlockSnow.LAYERS);
                    d += layers;
                    if (layers >= 8) {
                        y++;
                        continue;
                    }
                    depth = d;
                    topY = y;
                    break;
                }
                // Only air takes snow: tall grass, flowers, crops, water, torches and other blocks are never replaced.
                pos.setPos(x, y, z);
                if (st.getBlock().isAir(st, world, pos) && Blocks.SNOW_LAYER.canPlaceBlockAt(world, pos)) {
                    depth = d;
                    topY = y;
                }
                break;
            }
        }
        depthCache.put(col, depth);
        topCache.put(col, topY);
        return depth;
    }

    private boolean placeOne(WorldServer world, int x, int z) {
        depthCache.clear();
        topCache.clear();
        long start = SnowLeveling.pack(x, z);
        int d0 = readColumn(world, start);
        if (d0 < 0) return false;

        if (ArcanaQuestTweaksConfig.AurorianModuleConfig.snowWindShadow) {
            int surfaceY = topCache.get(start);
            boolean shadow = SnowShadow.inShadow(surfaceY, ArcanaQuestTweaksConfig.AurorianModuleConfig.snowFallAngle, dist -> {
                int ux = x - (int) Math.round(windX * dist);
                int uz = z - (int) Math.round(windZ * dist);
                Chunk c = world.getChunkProvider().getLoadedChunk(ux >> 4, uz >> 4);
                return c == null ? -1 : c.getPrecipitationHeight(new BlockPos(ux, 0, uz)).getY();
            });
            // In the lee a layer only lands about a third of the time: slower, not never.
            if (shadow && rand.nextFloat() > 0.35F) return false;
        }

        long target = SnowLeveling.walk(start, c -> readColumn(world, c),
                ArcanaQuestTweaksConfig.AurorianModuleConfig.snowMaxStep,
                ArcanaQuestTweaksConfig.AurorianModuleConfig.maxSnowLayers, SnowLeveling.MAX_HOPS);
        if (target == SnowLeveling.NONE) return false;

        int tx = SnowLeveling.unpackX(target);
        int tz = SnowLeveling.unpackZ(target);
        int ty = topCache.get(target);
        if (ty < 1) return false;
        pos.setPos(tx, ty, tz);
        IBlockState cur = world.getBlockState(pos);
        IBlockState next;
        if (cur.getBlock() == Blocks.SNOW_LAYER) {
            int layers = cur.getValue(BlockSnow.LAYERS);
            if (layers >= 8) return false;
            next = cur.withProperty(BlockSnow.LAYERS, layers + 1);
        } else if (cur.getBlock().isAir(cur, world, pos)) {
            next = Blocks.SNOW_LAYER.getDefaultState();
        } else {
            return false;
        }
        // Flag 2: tell the clients, no neighbour cascade. Entities are not skipped, so a still player can be buried.
        return world.setBlockState(pos, next, 2);
    }

    // ---- burial ----

    /** Once a second per Aurorian player (from {@link MoonfrostHandler}): slowness at the chest, air loss at the head. */
    public static void burialSecond(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        if (!ArcanaQuestTweaksConfig.AurorianModuleConfig.enableSnowBurial || player.isCreative() || player.isSpectator()
                || player instanceof FakePlayer) {
            if (data.hasKey(NBT_DEBT)) data.removeTag(NBT_DEBT);
            return;
        }
        World world = player.world;
        int x = (int) Math.floor(player.posX);
        int z = (int) Math.floor(player.posZ);
        int y0 = Math.max(1, (int) Math.floor(player.posY) - 1);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        double top = Double.NEGATIVE_INFINITY;
        for (int y = y0; y <= y0 + 4 && y < 255; y++) {
            p.setPos(x, y, z);
            if (!world.isBlockLoaded(p)) break;
            IBlockState st = world.getBlockState(p);
            if (st.getBlock() == Blocks.SNOW_LAYER) {
                int layers = st.getValue(BlockSnow.LAYERS);
                top = y + layers * 0.125D;
                if (layers < 8) break;
            } else if (top > Double.NEGATIVE_INFINITY) {
                break;
            }
        }

        SnowBurial.Level level = top == Double.NEGATIVE_INFINITY ? SnowBurial.Level.NONE
                : SnowBurial.level(player.posY + 0.8D, player.posY + player.getEyeHeight(), top);
        int debt = data.getInteger(NBT_DEBT);
        if (level == SnowBurial.Level.NONE) {
            debt = SnowBurial.debtWithHeadFree(debt);
        } else {
            if (ArcanaQuestTweaksConfig.AurorianModuleConfig.snowBurialSlowness) {
                player.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 1, false, false));
            }
            if (level == SnowBurial.Level.HEAD) {
                int drain = ArcanaQuestTweaksConfig.AurorianModuleConfig.snowBurialAirDrainPerSecond;
                debt = SnowBurial.debtWithHeadUnder(debt, drain);
                if (drain > 0 && SnowBurial.outOfAir(debt)) {
                    float dmg = SnowBurial.damage(player.getHealth(), (float) ArcanaQuestTweaksConfig.AurorianModuleConfig.snowBurialDamage);
                    if (dmg > 0.0F) player.attackEntityFrom(DAMAGE_SNOW_BURIAL, dmg);
                }
            } else {
                debt = SnowBurial.debtWithHeadFree(debt);
            }
        }
        if (debt <= 0) {
            if (data.hasKey(NBT_DEBT)) data.removeTag(NBT_DEBT);
        } else {
            data.setInteger(NBT_DEBT, debt);
        }
    }

    /** Every tick: vanilla refills the air bar each tick, so hold it at the burial level while the debt lasts. */
    public static void holdAir(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        if (!data.hasKey(NBT_DEBT)) return;
        int debt = data.getInteger(NBT_DEBT);
        if (debt > 0) player.setAir(Math.max(0, SnowBurial.AIR_MAX - debt));
    }

    public static void clearBurial(EntityPlayerMP player) {
        NBTTagCompound data = player.getEntityData();
        if (data.hasKey(NBT_DEBT)) data.removeTag(NBT_DEBT);
    }
}
