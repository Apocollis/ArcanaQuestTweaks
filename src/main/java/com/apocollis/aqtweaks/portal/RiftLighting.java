package com.apocollis.aqtweaks.portal;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.concurrent.CopyOnWriteArrayList;

public final class RiftLighting {

    private static final int LEVEL = 15;
    private static final CopyOnWriteArrayList<Source> SOURCES = new CopyOnWriteArrayList<>();
    private static volatile boolean active;

    private RiftLighting() {}

    public static boolean isActive() {
        return active;
    }

    public static int lightAt(World world, BlockPos pos) {
        if (!active) {
            return 0;
        }
        for (Source source : SOURCES) {
            if (source.world == world && source.pos.getX() == pos.getX()
                    && source.pos.getY() == pos.getY() && source.pos.getZ() == pos.getZ()) {
                return LEVEL;
            }
        }
        return 0;
    }

    public static void tick(EntityArcaneRift rift) {
        BlockPos cell = new BlockPos(
                MathHelper.floor(rift.posX),
                MathHelper.floor(rift.posY + 1.2),
                MathHelper.floor(rift.posZ));
        Source existing = find(rift);
        if (existing == null) {
            SOURCES.add(new Source(rift, cell));
            refreshActive();
            rift.world.checkLight(cell);
            return;
        }
        if (existing.world != rift.world || !existing.pos.equals(cell)) {
            World oldWorld = existing.world;
            BlockPos oldPos = existing.pos;
            existing.world = rift.world;
            existing.pos = cell;
            oldWorld.checkLight(oldPos);
            rift.world.checkLight(cell);
        }
    }

    public static void remove(EntityArcaneRift rift) {
        Source existing = find(rift);
        if (existing == null) {
            return;
        }
        SOURCES.remove(existing);
        refreshActive();
        existing.world.checkLight(existing.pos);
    }

    private static Source find(EntityArcaneRift rift) {
        int id = rift.getEntityId();
        World world = rift.world;
        for (Source source : SOURCES) {
            if (source.entityId == id && source.world == world) {
                return source;
            }
        }
        return null;
    }

    private static void refreshActive() {
        active = !SOURCES.isEmpty();
    }

    private static final class Source {
        private final int entityId;
        private World world;
        private BlockPos pos;

        private Source(EntityArcaneRift rift, BlockPos pos) {
            this.entityId = rift.getEntityId();
            this.world = rift.world;
            this.pos = pos;
        }
    }
}
