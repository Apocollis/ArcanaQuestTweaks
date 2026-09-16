package com.apocollis.aqtweaks.portal;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Glowstone-level light at each live rift. Forge {@code Block.getLightValue} reports 15 at the
 * cell ({@code MixinBlockRiftLight}); {@code MixinWorldRiftLight} covers {@code getRawLight}.
 * Vanilla {@code checkLight} then flood-fills, but only at world-tick {@code END} — never from
 * the rift entity tick (that races {@code RenderGlobal.updateClouds} on integrated SP).
 *
 * <p>{@code getRawLight} is one of the hottest methods in the game, so the lookup has to be O(1)
 * and allocation-free. Cells are held in a map keyed by the {@link BlockPos} vanilla already handed
 * us ({@code Vec3i} equality is by value, so the {@code MutableBlockPos} vanilla often passes
 * matches a stored immutable key), and {@link #isActive()} short-circuits the hook entirely when no
 * rift exists anywhere.
 */
public final class RiftLighting {

    private static final int LEVEL = 15;

    /** Lit cell → the world it belongs to. Keys are immutable. */
    private static final Map<BlockPos, World> CELLS = new ConcurrentHashMap<>();
    /** Entity id → the cell it currently lights, so a moving rift can relight the cell it left. */
    private static final Map<Integer, Source> SOURCES = new ConcurrentHashMap<>();
    private static final Set<LightJob> PENDING = ConcurrentHashMap.newKeySet();
    private static volatile boolean active;

    private RiftLighting() {}

    public static boolean isActive() {
        return active;
    }

    public static int lightAt(World world, BlockPos pos) {
        if (!active) {
            return 0;
        }
        return CELLS.get(pos) == world ? LEVEL : 0;
    }

    public static void tick(EntityArcaneRift rift) {
        int x = MathHelper.floor(rift.posX);
        int y = MathHelper.floor(rift.posY + 1.2);
        int z = MathHelper.floor(rift.posZ);

        Source existing = SOURCES.get(rift.getEntityId());
        if (existing != null && existing.world == rift.world && existing.cell.getX() == x
                && existing.cell.getY() == y && existing.cell.getZ() == z) {
            return;
        }

        BlockPos cell = new BlockPos(x, y, z);
        if (existing == null) {
            SOURCES.put(rift.getEntityId(), new Source(rift.world, cell));
            CELLS.put(cell, rift.world);
            refreshActive();
            requestCheck(rift.world, cell);
            return;
        }

        World oldWorld = existing.world;
        BlockPos oldCell = existing.cell;
        CELLS.remove(oldCell, oldWorld);

        existing.world = rift.world;
        existing.cell = cell;
        CELLS.put(cell, rift.world);
        requestCheck(oldWorld, oldCell);
        requestCheck(rift.world, cell);
    }

    public static void remove(EntityArcaneRift rift) {
        Source existing = SOURCES.remove(rift.getEntityId());
        if (existing == null) {
            return;
        }
        CELLS.remove(existing.cell, existing.world);
        refreshActive();
        requestCheck(existing.world, existing.cell);
    }

    /**
     * A rift that vanishes without {@link #remove} — server shutdown, world unload, an entity
     * dropped without {@code setDead} — would otherwise pin its {@link World} forever and keep
     * {@link #isActive()} true, taxing every light query for the rest of the session.
     */
    static void forgetWorld(World world) {
        for (Iterator<Map.Entry<Integer, Source>> it = SOURCES.entrySet().iterator(); it.hasNext(); ) {
            if (it.next().getValue().world == world) {
                it.remove();
            }
        }
        CELLS.entrySet().removeIf(e -> e.getValue() == world);
        PENDING.removeIf(job -> job.world == world);
        refreshActive();
    }

    private static void requestCheck(World world, BlockPos pos) {
        PENDING.add(new LightJob(world, pos.toImmutable()));
    }

    private static void drain(World world) {
        if (world == null) {
            return;
        }
        PENDING.removeIf(job -> {
            if (job.world != world) {
                return false;
            }
            world.checkLight(job.pos);
            return true;
        });
    }

    private static boolean shouldBake(World world) {
        if (world.isRemote) {
            return true;
        }
        MinecraftServer server = world.getMinecraftServer();
        return server != null && server.isDedicatedServer();
    }

    private static void refreshActive() {
        active = !SOURCES.isEmpty();
    }

    private static final class Source {
        private World world;
        private BlockPos cell;

        private Source(World world, BlockPos cell) {
            this.world = world;
            this.cell = cell;
        }
    }

    private static final class LightJob {
        private final World world;
        private final BlockPos pos;

        private LightJob(World world, BlockPos pos) {
            this.world = world;
            this.pos = pos;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof LightJob job)) {
                return false;
            }
            return world == job.world && pos.equals(job.pos);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(world) * 31 + pos.hashCode();
        }
    }

    @Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
    public static final class Events {
        @SubscribeEvent
        public static void onWorldTick(TickEvent.WorldTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.world == null || !shouldBake(event.world)) {
                return;
            }
            drain(event.world);
        }

        @SubscribeEvent
        public static void onWorldUnload(WorldEvent.Unload event) {
            if (event.getWorld() != null) {
                forgetWorld(event.getWorld());
            }
        }
    }
}
