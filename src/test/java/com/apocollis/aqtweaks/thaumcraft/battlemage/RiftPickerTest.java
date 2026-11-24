package com.apocollis.aqtweaks.thaumcraft.battlemage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** RiftPicker on a small grid world: '#' stone, 'B' bedrock, '.' air; one level plus floor at y=-1. */
class RiftPickerTest {

    /** A world of one row layout repeated at feet and head levels; the floor (y-1) is solid everywhere. */
    private static class Grid implements RiftPicker.Probe {
        final Set<Long> stone = new HashSet<>();
        final Set<Long> bedrock = new HashSet<>();
        final Set<Long> seen = new HashSet<>();
        final int tx;
        final int tz;

        Grid(int tx, int tz) {
            this.tx = tx;
            this.tz = tz;
        }

        static long key(int x, int z) {
            return ((long) x << 32) ^ (z & 0xffffffffL);
        }

        void wall(int x0, int x1, int z0, int z1) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    stone.add(key(x, z));
                }
            }
        }

        @Override
        public boolean open(int x, int y, int z) {
            return !stone.contains(key(x, z)) && !bedrock.contains(key(x, z));
        }

        @Override
        public boolean riftable(int x, int y, int z) {
            return stone.contains(key(x, z)) && !bedrock.contains(key(x, z));
        }

        @Override
        public boolean standable(int x, int y, int z) {
            return y == 0 && open(x, y, z);
        }

        @Override
        public boolean seenByTarget(int x, int y, int z) {
            return seen.contains(key(x, z));
        }
    }

    private static RiftPicker.Request request(RiftPicker.Mode mode, double tx, double tz) {
        RiftPicker.Request r = new RiftPicker.Request();
        r.mode = mode;
        r.fromX = 0;
        r.fromY = 0;
        r.fromZ = 0;
        r.targetX = tx;
        r.targetZ = tz;
        return r;
    }

    @Test
    void offensiveRiftThroughAThinWallLandsInTheBandOnTheTargetsSide() {
        Grid g = new Grid(20, 0);
        g.wall(2, 4, -6, 6); // a 3 thick wall between the mage (x=0) and the target (x=20)
        RiftPicker.Request r = request(RiftPicker.Mode.OFFENSIVE, 20.0D, 0.0D);
        r.bandMin = 3.0D;
        r.bandMax = 14.0D;
        RiftPicker.Plan p = RiftPicker.pick(r, g, new Random(1L));
        assertNotNull(p);
        assertTrue(p.exitX >= 5, "past the wall");
        double d = Math.hypot(p.exitX + 0.5D - 20.0D, p.exitZ + 0.5D);
        assertTrue(d >= 3.0D && d <= 20.0D - RiftPicker.PROGRESS_BLOCKS, "closer to the target than the start: " + d);
        assertEquals(p.thickness * 2, p.holes.size(), "feet and head columns for each wall block");
        assertTrue(p.thickness >= 3);
    }

    @Test
    void aWallThatIsTooThickIsRefused() {
        Grid g = new Grid(30, 0);
        g.wall(2, 20, -30, 30);
        RiftPicker.Request r = request(RiftPicker.Mode.OFFENSIVE, 30.0D, 0.0D);
        r.maxThickness = 12;
        assertNull(RiftPicker.pick(r, g, new Random(2L)));
    }

    @Test
    void bedrockInTheWayMakesTheLineInvalid() {
        Grid g = new Grid(20, 0);
        g.wall(2, 4, -30, 30);
        for (int z = -30; z <= 30; z++) {
            g.bedrock.add(Grid.key(3, z));
        }
        RiftPicker.Request r = request(RiftPicker.Mode.OFFENSIVE, 20.0D, 0.0D);
        assertNull(RiftPicker.pick(r, g, new Random(3L)));
    }

    @Test
    void theHoleCapIsHonoured() {
        Grid g = new Grid(20, 0);
        g.wall(2, 12, -30, 30);
        RiftPicker.Request r = request(RiftPicker.Mode.OFFENSIVE, 22.0D, 0.0D);
        r.maxHoles = 6; // 11 thick needs 22 holes
        assertNull(RiftPicker.pick(r, g, new Random(4L)));
    }

    @Test
    void defensiveRiftLandsFartherAndOutOfSight() {
        Grid g = new Grid(-6, 0);
        g.wall(3, 5, -8, 8); // wall east of the mage; the player is to the west
        for (int x = -10; x <= 2; x++) {
            for (int z = -10; z <= 10; z++) {
                g.seen.add(Grid.key(x, z)); // everything on the player's side is in view
            }
        }
        RiftPicker.Request r = request(RiftPicker.Mode.DEFENSIVE, -6.0D, 0.0D);
        RiftPicker.Plan p = RiftPicker.pick(r, g, new Random(5L));
        assertNotNull(p);
        assertTrue(p.exitX >= 6, "behind the wall, away from the player");
        assertTrue(Math.hypot(p.exitX + 0.5D + 6.0D, p.exitZ) > Math.hypot(6.0D, 0.0D) + 2.0D);
    }

    @Test
    void noWallNearbyMeansNoRift() {
        Grid g = new Grid(20, 0);
        assertNull(RiftPicker.pick(request(RiftPicker.Mode.DEFENSIVE, 5.0D, 0.0D), g, new Random(6L)));
    }

    @Test
    void anExitInTheOpenWithNoFloorIsNotUsed() {
        Grid g = new Grid(20, 0) {
            @Override
            public boolean standable(int x, int y, int z) {
                return x < 5 && open(x, y, z);
            }
        };
        g.wall(2, 4, -30, 30);
        RiftPicker.Request r = request(RiftPicker.Mode.OFFENSIVE, 20.0D, 0.0D);
        assertNull(RiftPicker.pick(r, g, new Random(7L)));
    }
}
