package com.apocollis.aqtweaks.nether;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.hazard.HazardEvent;
import com.apocollis.aqtweaks.hazard.HazardState;
import com.apocollis.aqtweaks.hazard.Hazards;
import com.apocollis.aqtweaks.network.PacketSplashTelegraph;
import com.apocollis.aqtweaks.network.PacketSpoutTelegraph;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.play.server.SPacketSoundEffect;
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
 * each player gets a run of staggered eruptions; each picks a lava source near the player (preferring shore),
 * telegraphs for 3 s, bursts and burns the column, then warns where its thrown lava will land (the landing spots
 * were chosen when the spout started) and, {@code Splash Warning Ticks} later, places small patches of temporary
 * flowing lava there, tracked in {@link SplashQueue} and cleared after its lifetime.
 */
public class LavaSpoutHandler {

    public static final DamageSource DAMAGE_SPOUT = new DamageSource("aqtweaks_spout").setDamageBypassesArmor()
            .setFireDamage();

    private static final int TELEGRAPH_TICKS = 60;
    private static final int COLUMN_HEIGHT = 12;

    private static final class Pending {
        final BlockPos site;
        final long eruptAt;
        final List<BlockPos> centres;

        Pending(BlockPos site, long eruptAt, List<BlockPos> centres) {
            this.site = site;
            this.eruptAt = eruptAt;
            this.centres = centres;
        }
    }

    /** A burst spout's landing spots, warned about now and filled with lava at {@code placeAt}. */
    private static final class Landing {
        final List<BlockPos> centres;
        final long placeAt;

        Landing(List<BlockPos> centres, long placeAt) {
            this.centres = centres;
            this.placeAt = placeAt;
        }
    }

    private final Random rand = new Random();
    private long eventId = Long.MIN_VALUE;
    private final Set<UUID> scheduled = new HashSet<>();
    private final Map<UUID, List<Long>> due = new HashMap<>();
    private final List<Pending> pending = new ArrayList<>();
    private final List<Landing> landings = new ArrayList<>();

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
            erupt(world, p, now);
            return true;
        });

        if (!landings.isEmpty()) {
            landings.removeIf(l -> {
                if (now < l.placeAt) return false;
                for (BlockPos centre : l.centres) {
                    placePatch(world, centre, queue, now);
                }
                return true;
            });
        }
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
        double toward = Math.atan2(player.posZ - (site.getZ() + 0.5D), player.posX - (site.getX() + 0.5D));
        List<BlockPos> centres = new ArrayList<>();
        if (ArcanaQuestTweaksConfig.NetherModuleConfig.throwLava) {
            for (int i = 0; i < ArcanaQuestTweaksConfig.NetherModuleConfig.splashesPerEruption; i++) {
                BlockPos centre = findSplashCenter(world, site, toward, centres);
                if (centre != null) centres.add(centre);
            }
        }
        pending.add(new Pending(site, now + TELEGRAPH_TICKS, centres));
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

    /** Number of the 16 sample points (8 directions at 4 and 8 blocks) that have solid land at lava level. */
    private static int landScore(WorldServer world, int x, int y, int z) {
        int score = 0;
        for (int dir = 0; dir < 8; dir++) {
            double a = dir * Math.PI / 4.0D;
            for (int d = 4; d <= 8; d += 4) {
                int sx = x + (int) Math.round(Math.cos(a) * d);
                int sz = z + (int) Math.round(Math.sin(a) * d);
                for (int dy = 0; dy <= 2; dy++) {
                    IBlockState state = LoadedBlocks.peek(world, sx, y + dy, sz);
                    if (state != null && state.getMaterial().isSolid() && !state.getMaterial().isLiquid()) {
                        score++;
                        break;
                    }
                }
            }
        }
        return score;
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
        BlockPos best = null;
        int bestScore = -1;
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
                        && SpoutSitePicker.farFromOthers(x, y, z, others, SpoutSitePicker.SEPARATION)) {
                    // Prefer a lava source with land around it, so the thrown lava reaches the bank.
                    int score = landScore(world, x, y, z);
                    if (score > bestScore) {
                        bestScore = score;
                        best = new BlockPos(x, y, z);
                        if (score >= SpoutSitePicker.GOOD_SCORE) return best;
                    }
                }
                break;
            }
        }
        return best;
    }

    // ---- the eruption ----

    private void erupt(WorldServer world, Pending p, long now) {
        BlockPos site = p.site;
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
        if (!ArcanaQuestTweaksConfig.NetherModuleConfig.throwLava || p.centres.isEmpty()) return;

        // The landing spots were chosen when the spout started; check they still hold, re-pick any that do not.
        List<BlockPos> centres = new ArrayList<>(p.centres.size());
        for (BlockPos centre : p.centres) {
            if (centreOk(world, centre.getX(), centre.getY(), centre.getZ())) {
                centres.add(centre);
            } else {
                BlockPos again = findSplashCenter(world, site, Double.NaN, centres);
                if (again != null) centres.add(again);
            }
        }
        if (centres.isEmpty()) return;
        int warn = ArcanaQuestTweaksConfig.NetherModuleConfig.splashWarningTicks;
        landings.add(new Landing(centres, now + warn));

        int[] coords = new int[centres.size() * 3];
        for (int i = 0; i < centres.size(); i++) {
            coords[i * 3] = centres.get(i).getX();
            coords[i * 3 + 1] = centres.get(i).getY();
            coords[i * 3 + 2] = centres.get(i).getZ();
        }
        ArcanaQuestTweaks.NETWORK.sendToAllAround(new PacketSplashTelegraph(coords, warn),
                new NetworkRegistry.TargetPoint(world.provider.getDimension(), site.getX() + 0.5D, site.getY(),
                        site.getZ() + 0.5D, 128.0D));
        // A player standing on a landing hears it even when looking elsewhere.
        for (EntityPlayer player : world.playerEntities) {
            if (!(player instanceof EntityPlayerMP) || player.isCreative() || player.isSpectator()) continue;
            for (BlockPos centre : centres) {
                double dx = player.posX - (centre.getX() + 0.5D);
                double dz = player.posZ - (centre.getZ() + 0.5D);
                if (dx * dx + dz * dz <= 9.0D && Math.abs(player.posY - centre.getY()) <= 3.0D) {
                    ((EntityPlayerMP) player).connection.sendPacket(new SPacketSoundEffect(SoundEvents.ENTITY_GHAST_WARN,
                            SoundCategory.HOSTILE, player.posX, player.posY, player.posZ, 0.8F, 1.6F));
                    break;
                }
            }
        }
    }

    public static boolean isFireProtected(EntityLivingBase entity) {
        if (entity.isImmuneToFire()) return true;
        String name = ArcanaQuestTweaksConfig.HazardGearConfig.nether.fireResistanceEffect;
        if (name == null || name.trim().isEmpty()) return false;
        Potion potion = Potion.REGISTRY.getObject(new ResourceLocation(name.trim()));
        return potion != null && entity.isPotionActive(potion);
    }

    private static boolean solidFloor(IBlockState state) {
        return state != null && state.getMaterial().isSolid() && state.isFullCube()
                && !state.getMaterial().isLiquid();
    }

    /** Y of the standing surface for a patch cell: air over a solid floor, within one block of {@code cy}. */
    private static int cellY(WorldServer world, int x, int cy, int z) {
        int[] order = {0, -1, 1};
        for (int dy : order) {
            int y = cy + dy;
            IBlockState here = LoadedBlocks.peek(world, x, y, z);
            IBlockState below = LoadedBlocks.peek(world, x, y - 1, z);
            if (here != null && here.getBlock() == Blocks.AIR && solidFloor(below)) return y;
        }
        return Integer.MIN_VALUE;
    }

    /** The centre needs a floor, and at least 3 of its 4 neighbours need one too. */
    private static boolean centreOk(WorldServer world, int cx, int cy, int cz) {
        IBlockState here = LoadedBlocks.peek(world, cx, cy, cz);
        IBlockState below = LoadedBlocks.peek(world, cx, cy - 1, cz);
        if (here == null || here.getBlock() != Blocks.AIR || !solidFloor(below)) return false;
        int[][] around = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int good = 0;
        for (int[] d : around) {
            if (cellY(world, cx + d[0], cy, cz + d[1]) != Integer.MIN_VALUE) good++;
        }
        return good >= 3;
    }

    /** Picks a landing spot for one splash, 3 to 12 blocks from the spout, or null if 12 tries find none. */
    private BlockPos findSplashCenter(WorldServer world, BlockPos site, double toward, List<BlockPos> taken) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = SplashAim.angle(rand, toward);
            double dist = SplashAim.distance(rand);
            int cx = site.getX() + (int) Math.round(Math.cos(angle) * dist);
            int cz = site.getZ() + (int) Math.round(Math.sin(angle) * dist);
            int cy = Integer.MIN_VALUE;
            for (int y = site.getY() + 10; y >= site.getY() - 8; y--) {
                IBlockState here = LoadedBlocks.peek(world, cx, y, cz);
                IBlockState below = LoadedBlocks.peek(world, cx, y - 1, cz);
                if (here != null && here.getBlock() == Blocks.AIR && solidFloor(below)) {
                    cy = y;
                    break;
                }
            }
            if (cy == Integer.MIN_VALUE || !centreOk(world, cx, cy, cz)) continue;
            boolean overlaps = false;
            for (BlockPos other : taken) {
                int ox = other.getX() - cx;
                int oz = other.getZ() - cz;
                if (ox * ox + oz * oz < 9) {
                    overlaps = true;
                    break;
                }
            }
            if (!overlaps) return new BlockPos(cx, cy, cz);
        }
        return null;
    }

    /** Places the thrown lava of one landing, cell by cell on the local surface; cells with no floor are skipped. */
    private void placePatch(WorldServer world, BlockPos centre, SplashQueue queue, long now) {
        List<SplashPatch.Cell> patch = SplashPatch.cells(ArcanaQuestTweaksConfig.NetherModuleConfig.splashCenterLevel);
        int cap = ArcanaQuestTweaksConfig.NetherModuleConfig.maxThrownLavaCells;
        if (queue.size() + patch.size() > cap) {
            debug("splash skipped: thrown lava cap reached");
            return;
        }
        long expire = now + ArcanaQuestTweaksConfig.NetherModuleConfig.splashLifetimeSeconds * 20L;
        for (SplashPatch.Cell cell : patch) {
            int x = centre.getX() + cell.dx;
            int z = centre.getZ() + cell.dz;
            int y = cellY(world, x, centre.getY(), z);
            if (y == Integer.MIN_VALUE) continue;
            BlockPos pos = new BlockPos(x, y, z);
            world.setBlockState(pos, Blocks.FLOWING_LAVA.getDefaultState().withProperty(BlockLiquid.LEVEL, cell.level), 2);
            queue.add(pos, expire);
        }
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
        landings.clear();
    }
}
