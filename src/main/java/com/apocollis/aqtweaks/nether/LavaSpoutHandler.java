package com.apocollis.aqtweaks.nether;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.hazard.HazardEvent;
import com.apocollis.aqtweaks.hazard.HazardState;
import com.apocollis.aqtweaks.hazard.Hazards;
import com.apocollis.aqtweaks.network.PacketSpoutTelegraph;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderHell;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Lava spouts in the Nether: clustered eruption events on the {@code spout} hazard channel. During the event
 * each player gets a few staggered eruptions; each picks a lava source near the player, telegraphs for 3 s,
 * then burns the column and throws a small patch of temporary flowing lava onto nearby ground, tracked in
 * {@link SplashQueue} and cleared after its lifetime.
 */
public class LavaSpoutHandler {

    public static final DamageSource DAMAGE_SPOUT = new DamageSource("aqtweaks_spout").setDamageBypassesArmor()
            .setFireDamage();

    private static final int TELEGRAPH_TICKS = 60;
    private static final int COLUMN_HEIGHT = 12;

    private static final class Pending {
        final BlockPos site;
        final long eruptAt;

        Pending(BlockPos site, long eruptAt) {
            this.site = site;
            this.eruptAt = eruptAt;
        }
    }

    private final Random rand = new Random();
    private long eventId = Long.MIN_VALUE;
    private final Set<UUID> scheduled = new HashSet<>();
    private final Map<UUID, List<Long>> due = new HashMap<>();
    private final List<Pending> pending = new ArrayList<>();

    public int activeSpouts() {
        return pending.size();
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient() || !(event.world instanceof WorldServer)) return;
        WorldServer world = (WorldServer) event.world;
        if (!(world.provider instanceof WorldProviderHell)) return;
        long now = world.getTotalWorldTime();

        SplashQueue queue = SplashQueue.get(world);
        if (now % 20L == 0L) {
            queue.sweep(world, now);
        }
        if (!ArcanaQuestTweaksConfig.NetherModuleConfig.enableSpouts) return;

        HazardState state = Hazards.state(world, NetherHazards.SPOUT);
        if (state.event != null && state.event.id != eventId) {
            eventId = state.event.id;
            scheduled.clear();
            due.clear();
        }

        if (state.phase == HazardEvent.Phase.ACTIVE) {
            for (EntityPlayer player : world.playerEntities) {
                if (player.isCreative() || player.isSpectator() || player.isDead) continue;
                if (scheduled.add(player.getUniqueID())) {
                    drawFor(player, now, state.ticksToEnd);
                }
            }
        }

        if (!due.isEmpty()) {
            Iterator<Map.Entry<UUID, List<Long>>> it = due.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, List<Long>> entry = it.next();
                EntityPlayer player = world.getPlayerEntityByUUID(entry.getKey());
                List<Long> times = entry.getValue();
                times.removeIf(t -> {
                    if (t > now) return false;
                    if (player != null && !player.isDead) startEruption(world, player, now);
                    return true;
                });
                if (times.isEmpty()) it.remove();
            }
        }

        pending.removeIf(p -> {
            if (now < p.eruptAt) return false;
            erupt(world, p.site, queue, now);
            return true;
        });
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        World world = event.getWorld();
        if (world.isRemote || !(world.provider instanceof WorldProviderHell)) return;
        SplashQueue.get(world).clearChunk(world, event.getChunk().x, event.getChunk().z);
    }

    // ---- scheduling ----

    private void drawFor(EntityPlayer player, long now, long windowRemaining) {
        long[] offsets = SpoutSchedule.draw(rand,
                ArcanaQuestTweaksConfig.NetherModuleConfig.spoutMinPerPlayer,
                ArcanaQuestTweaksConfig.NetherModuleConfig.spoutMaxPerPlayer,
                60,
                (int) (ArcanaQuestTweaksConfig.NetherModuleConfig.spoutStaggerMinSeconds * 20),
                (int) (ArcanaQuestTweaksConfig.NetherModuleConfig.spoutStaggerMaxSeconds * 20),
                windowRemaining);
        List<Long> times = new ArrayList<>(offsets.length);
        for (long o : offsets) times.add(now + o);
        if (!times.isEmpty()) due.put(player.getUniqueID(), times);
    }

    private void startEruption(WorldServer world, EntityPlayer player, long now) {
        if (pending.size() >= ArcanaQuestTweaksConfig.NetherModuleConfig.maxActiveSpouts) return;
        BlockPos site = findSite(world, player);
        if (site == null) {
            debug("no spout site found near " + player.getName());
            return;
        }
        pending.add(new Pending(site, now + TELEGRAPH_TICKS));
        ArcanaQuestTweaks.NETWORK.sendToAllAround(new PacketSpoutTelegraph(site.getX(), site.getY(), site.getZ(),
                TELEGRAPH_TICKS), new NetworkRegistry.TargetPoint(world.provider.getDimension(),
                site.getX() + 0.5D, site.getY(), site.getZ() + 0.5D, 128.0D));
        debug("spout telegraph at " + site);
    }

    // ---- site picking ----

    private static boolean isLavaSource(IBlockState state) {
        return (state.getBlock() == Blocks.LAVA || state.getBlock() == Blocks.FLOWING_LAVA)
                && state.getValue(BlockLiquid.LEVEL) == 0;
    }

    private BlockPos findSite(WorldServer world, EntityPlayer player) {
        int range = ArcanaQuestTweaksConfig.NetherModuleConfig.spoutRange;
        double minDist = ArcanaQuestTweaksConfig.NetherModuleConfig.spoutMinDistance;
        int px = (int) Math.floor(player.posX);
        int py = (int) Math.floor(player.posY);
        int pz = (int) Math.floor(player.posZ);
        int[][] others = new int[pending.size()][];
        for (int i = 0; i < others.length; i++) {
            BlockPos s = pending.get(i).site;
            others[i] = new int[] {s.getX(), s.getY(), s.getZ()};
        }
        for (int probe = 0; probe < ArcanaQuestTweaksConfig.NetherModuleConfig.spoutProbes; probe++) {
            int dx = rand.nextInt(range * 2 + 1) - range;
            int dz = rand.nextInt(range * 2 + 1) - range;
            if (!SpoutSitePicker.distanceOk(dx, dz, minDist, range)) continue;
            int x = px + dx;
            int z = pz + dz;
            for (int y = py + 16; y >= py - 24; y--) {
                IBlockState state = LoadedBlocks.peek(world, x, y, z);
                if (state == null) break;
                if (!isLavaSource(state)) continue;
                IBlockState above = LoadedBlocks.peek(world, x, y + 1, z);
                if (above != null && above.getBlock() == Blocks.AIR
                        && SpoutSitePicker.farFromOthers(x, y, z, others, 10.0D)) {
                    return new BlockPos(x, y, z);
                }
                break;
            }
        }
        return null;
    }

    // ---- the eruption ----

    private void erupt(WorldServer world, BlockPos site, SplashQueue queue, long now) {
        world.playSound(null, site, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 0.9F, 0.55F);
        double r = ArcanaQuestTweaksConfig.NetherModuleConfig.spoutColumnRadius;
        AxisAlignedBB column = new AxisAlignedBB(site.getX() + 0.5D - r, site.getY(), site.getZ() + 0.5D - r,
                site.getX() + 0.5D + r, site.getY() + COLUMN_HEIGHT, site.getZ() + 0.5D + r);
        float damage = (float) ArcanaQuestTweaksConfig.NetherModuleConfig.spoutDamage;
        for (EntityLivingBase entity : world.getEntitiesWithinAABB(EntityLivingBase.class, column)) {
            if (entity instanceof EntityPlayer && (((EntityPlayer) entity).isCreative()
                    || ((EntityPlayer) entity).isSpectator())) {
                continue;
            }
            if (isFireProtected(entity)) continue;
            entity.attackEntityFrom(DAMAGE_SPOUT, damage);
            entity.setFire(4);
        }
        if (ArcanaQuestTweaksConfig.NetherModuleConfig.throwLava) {
            for (int i = 0; i < ArcanaQuestTweaksConfig.NetherModuleConfig.splashesPerEruption; i++) {
                placeSplash(world, site, queue, now);
            }
        }
    }

    public static boolean isFireProtected(EntityLivingBase entity) {
        if (entity.isImmuneToFire()) return true;
        String name = ArcanaQuestTweaksConfig.NetherModuleConfig.fireResistanceEffect;
        if (name == null || name.trim().isEmpty()) return false;
        Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(name.trim()));
        return potion != null && entity.isPotionActive(potion);
    }

    private static boolean solidFloor(IBlockState state) {
        return state != null && state.getMaterial().isSolid() && state.isFullCube()
                && !state.getMaterial().isLiquid();
    }

    private void placeSplash(WorldServer world, BlockPos site, SplashQueue queue, long now) {
        List<SplashPatch.Cell> patch = SplashPatch.cells(ArcanaQuestTweaksConfig.NetherModuleConfig.splashCenterLevel);
        int cap = ArcanaQuestTweaksConfig.NetherModuleConfig.maxThrownLavaCells;
        if (queue.size() + patch.size() > cap) {
            debug("splash skipped: thrown lava cap reached");
            return;
        }
        for (int attempt = 0; attempt < 6; attempt++) {
            double angle = rand.nextDouble() * Math.PI * 2.0D;
            double dist = 5.0D + rand.nextDouble() * 3.0D;
            int cx = site.getX() + (int) Math.round(Math.cos(angle) * dist);
            int cz = site.getZ() + (int) Math.round(Math.sin(angle) * dist);
            int cy = Integer.MIN_VALUE;
            for (int y = site.getY() + 4; y >= site.getY() - 6; y--) {
                IBlockState here = LoadedBlocks.peek(world, cx, y, cz);
                IBlockState below = LoadedBlocks.peek(world, cx, y - 1, cz);
                if (here != null && here.getBlock() == Blocks.AIR && solidFloor(below)) {
                    cy = y;
                    break;
                }
            }
            if (cy == Integer.MIN_VALUE) continue;
            String reject = checkArea(world, cx, cy, cz);
            if (reject != null) {
                debug("splash rejected at " + cx + "," + cy + "," + cz + ": " + reject);
                continue;
            }
            long expire = now + ArcanaQuestTweaksConfig.NetherModuleConfig.splashLifetimeSeconds * 20L;
            for (SplashPatch.Cell cell : patch) {
                BlockPos pos = new BlockPos(cx + cell.dx, cy, cz + cell.dz);
                IBlockState here = LoadedBlocks.peek(world, pos.getX(), pos.getY(), pos.getZ());
                if (here == null || here.getBlock() != Blocks.AIR) continue;
                world.setBlockState(pos, Blocks.FLOWING_LAVA.getDefaultState().withProperty(BlockLiquid.LEVEL, cell.level), 2);
                queue.add(pos, expire);
            }
            return;
        }
    }

    /** Returns a reason to reject the area, or null when it is safe: no liquid, and no air without a floor. */
    private static String checkArea(WorldServer world, int cx, int cy, int cz) {
        for (int[] off : SplashPatch.safetyArea()) {
            int x = cx + off[0];
            int z = cz + off[1];
            IBlockState here = LoadedBlocks.peek(world, x, cy, z);
            if (here == null) return "unloaded";
            if (here.getMaterial().isLiquid()) return "liquid";
            if (here.getBlock() == Blocks.AIR) {
                IBlockState below = LoadedBlocks.peek(world, x, cy - 1, z);
                if (!solidFloor(below)) return "no floor";
            }
        }
        return null;
    }

    private static void debug(String message) {
        if (ArcanaQuestTweaksConfig.NetherModuleConfig.netherHazardDebug) {
            org.apache.logging.log4j.LogManager.getLogger("AQTweaks-Nether").info("[nether-hazards] " + message);
        }
    }

    /** Forgets scheduled and pending eruptions (the {@code clear} command). */
    public void reset() {
        scheduled.clear();
        due.clear();
        pending.clear();
    }
}
