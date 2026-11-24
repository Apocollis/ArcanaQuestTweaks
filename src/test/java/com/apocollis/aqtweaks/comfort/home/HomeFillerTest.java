package com.apocollis.aqtweaks.comfort.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HomeFillerTest {

    /** A box of air with solid blocks placed by the test; everything outside the loaded bounds is unloaded. */
    static final class Grid implements HomeFiller.Probe {
        final Set<Long> solid = new HashSet<>();
        /** Doors, gates and trapdoors: neither solid nor passable, judged by the door test. */
        final Set<Long> portals = new HashSet<>();
        boolean skylight;
        int lx0 = -40;
        int ly0 = 0;
        int lz0 = -40;
        int lx1 = 40;
        int ly1 = 60;
        int lz1 = 40;

        void put(int x, int y, int z) {
            solid.add(HomeRegion.pack(x, y, z));
        }

        void remove(int x, int y, int z) {
            solid.remove(HomeRegion.pack(x, y, z));
        }

        /** Replaces the block with a portal (a door half, gate or trapdoor). */
        void door(int x, int y, int z) {
            solid.remove(HomeRegion.pack(x, y, z));
            portals.add(HomeRegion.pack(x, y, z));
        }

        void shell(int x0, int y0, int z0, int x1, int y1, int z1, boolean roof) {
            for (int x = x0; x <= x1; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = z0; z <= z1; z++) {
                        boolean edge = x == x0 || x == x1 || z == z0 || z == z1 || y == y0 || (roof && y == y1);
                        if (edge) {
                            put(x, y, z);
                        }
                    }
                }
            }
        }

        @Override
        public boolean loaded(int x, int y, int z) {
            return x >= lx0 && x <= lx1 && y >= ly0 && y <= ly1 && z >= lz0 && z <= lz1;
        }

        @Override
        public boolean passable(int x, int y, int z) {
            long key = HomeRegion.pack(x, y, z);
            return !solid.contains(key) && !portals.contains(key);
        }

        @Override
        public boolean portal(int x, int y, int z) {
            return portals.contains(HomeRegion.pack(x, y, z));
        }

        @Override
        public boolean skyExposed(int x, int y, int z) {
            if (!skylight) {
                return false;
            }
            for (int yy = y + 1; yy <= ly1; yy++) {
                long key = HomeRegion.pack(x, yy, z);
                if (solid.contains(key) || portals.contains(key)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static HomeRegion fill(Grid grid, int bx, int by, int bz, int maxCells) {
        HomeFiller filler = new HomeFiller(grid, bx, by, bz, maxCells, 12, 6, 8);
        for (int i = 0; i < 1000 && !filler.step(400); i++) {
            // keep stepping
        }
        assertTrue(filler.isDone());
        return filler.result();
    }

    /** Room interior x1..5, y1..3, z1..5 inside walls at 0 and 6, floor y0, roof y4; bed block at (3,1,3). */
    private static Grid room() {
        Grid grid = new Grid();
        grid.shell(0, 0, 0, 6, 4, 6, true);
        grid.put(3, 1, 3);
        return grid;
    }

    @Test
    void oneRoomIsItsInterior() {
        HomeRegion region = fill(room(), 3, 1, 3, 4000);
        assertFalse(region.isBox());
        assertEquals(5 * 3 * 5 - 1, region.cellCount());
        assertTrue(region.contains(2, 1, 2));
        assertTrue(region.contains(3, 3, 3));
        assertFalse(region.contains(3, 1, 3), "the bed block itself is not a passable cell");
        assertFalse(region.contains(0, 1, 0), "walls are boundary, not interior");
        assertFalse(region.contains(10, 1, 10));
    }

    @Test
    void boundaryHoldsTheWallsAndTheBed() {
        HomeRegion region = fill(room(), 3, 1, 3, 4000);
        assertTrue(region.boundaryCount() > 0);
        boolean sawBed = false;
        boolean roof = false;
        boolean floor = false;
        boolean wall = false;
        for (int i = 0; i < region.boundary().length; i++) {
            long p = region.boundary()[i];
            if (HomeRegion.unpackX(p) == 3 && HomeRegion.unpackY(p) == 1 && HomeRegion.unpackZ(p) == 3) {
                sawBed = true;
            }
            byte role = region.boundaryRoles()[i];
            roof |= role == HomeRegion.ROLE_ROOF;
            floor |= role == HomeRegion.ROLE_FLOOR;
            wall |= role == HomeRegion.ROLE_WALL;
        }
        assertTrue(sawBed, "furniture touching the interior is scored");
        assertTrue(roof && floor && wall);
    }

    @Test
    void twoFloorsJoinedByAShaftAreOneHome() {
        Grid grid = new Grid();
        grid.shell(0, 0, 0, 6, 4, 6, true); // lower room, slab at y 4
        grid.shell(0, 4, 0, 6, 8, 6, true); // upper room over it, roof y 8
        grid.remove(1, 4, 1); // the stair shaft through the slab
        grid.put(3, 1, 3);
        HomeRegion region = fill(grid, 3, 1, 3, 4000);
        assertFalse(region.isBox());
        assertTrue(region.contains(3, 6, 3), "the upper floor counts");
        assertTrue(region.contains(1, 4, 1));
        assertEquals(74 + 1 + 75, region.cellCount());
    }

    @Test
    void slabWithoutAShaftSplitsTheFloors() {
        Grid grid = new Grid();
        grid.shell(0, 0, 0, 6, 4, 6, true);
        grid.shell(0, 4, 0, 6, 8, 6, true);
        grid.put(3, 1, 3);
        HomeRegion region = fill(grid, 3, 1, 3, 4000);
        assertFalse(region.contains(3, 6, 3));
        assertEquals(74, region.cellCount());
    }

    @Test
    void aClosedDoorIsAWallAnOpenDoorLeaksOutside() {
        Grid closed = room();
        closed.remove(6, 1, 3);
        closed.remove(6, 2, 3);
        closed.put(6, 1, 3); // closed door: solid
        closed.put(6, 2, 3);
        HomeRegion region = fill(closed, 3, 1, 3, 4000);
        assertFalse(region.isBox());

        Grid open = room();
        open.remove(6, 1, 3); // open door: passable, and outside is open air bigger than the cap
        open.remove(6, 2, 3);
        HomeRegion leak = fill(open, 3, 1, 3, 400);
        assertTrue(leak.isBox());
        assertTrue(leak.reason().contains("cell cap"), leak.reason());
    }

    @Test
    void capHitFallsBackToTheBoxAroundTheBed() {
        HomeRegion region = fill(room(), 3, 1, 3, 50);
        assertTrue(region.isBox());
        assertEquals(3 - 12, region.minX());
        assertEquals(3 + 12, region.maxX());
        assertEquals(3 - 12, region.minZ());
        assertEquals(3 + 12, region.maxZ());
        assertEquals(0, region.minY(), "bed y 1 minus 6 is clamped at the world floor");
        assertEquals(1 + 8, region.maxY());
        assertTrue(region.contains(3, 5, 3));
        assertFalse(region.contains(3, 20, 3));
    }

    @Test
    void openRoofIntoUnloadedChunksFallsBack() {
        Grid grid = new Grid();
        grid.shell(0, 0, 0, 6, 4, 6, false); // no roof
        grid.put(3, 1, 3);
        grid.lx0 = -1;
        grid.lx1 = 7;
        grid.lz0 = -1;
        grid.lz1 = 7;
        grid.ly1 = 4; // loaded stops at the roof line
        HomeRegion region = fill(grid, 3, 1, 3, 4000);
        assertTrue(region.isBox());
        assertTrue(region.reason().contains("unloaded"), region.reason());
    }

    @Test
    void openRoofInLoadedAirHitsTheCap() {
        Grid grid = new Grid();
        grid.shell(0, 0, 0, 6, 4, 6, false);
        grid.put(3, 1, 3);
        HomeRegion region = fill(grid, 3, 1, 3, 600);
        assertTrue(region.isBox());
        assertTrue(region.reason().contains("cell cap"), region.reason());
    }

    @Test
    void aBedWithNoOpenCellFallsBack() {
        Grid grid = new Grid();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    grid.put(3 + dx, 5 + dy, 3 + dz);
                }
            }
        }
        HomeRegion region = fill(grid, 3, 5, 3, 4000);
        assertTrue(region.isBox());
        assertTrue(region.reason().contains("no start"), region.reason());
    }

    @Test
    void slicedFillGivesTheSameRegionAsOneShot() {
        Grid grid = room();
        HomeFiller sliced = new HomeFiller(grid, 3, 1, 3, 4000, 12, 6, 8);
        int steps = 0;
        while (!sliced.step(5)) {
            steps++;
            assertTrue(steps < 1000);
        }
        assertTrue(steps > 3, "a 74-cell fill at 5 cells per step takes several steps");
        HomeRegion one = fill(room(), 3, 1, 3, 4000);
        assertEquals(one.cellCount(), sliced.result().cellCount());
        assertEquals(one.boundaryCount(), sliced.result().boundaryCount());
    }

    @Test
    void occupancyTestsFeetAndHead() {
        HomeRegion region = fill(room(), 3, 1, 3, 4000);
        assertTrue(region.occupies(2, 1, 2));
        // standing on a half block: feet cell is solid (the bed), the head cell is inside the room
        assertTrue(region.occupies(3, 1, 3));
        assertFalse(region.occupies(9, 1, 9));
        // feet below the floor but head inside is still inside
        assertFalse(region.occupies(2, -1, 2));
    }

    @Test
    void dirtyTestUsesBoundsWidenedByOneForFilledAndExactForBox() {
        HomeRegion filled = fill(room(), 3, 1, 3, 4000);
        assertTrue(filled.affectedBy(0, 1, 3), "a wall block touching the interior");
        assertTrue(filled.affectedBy(3, 4, 3), "the roof block");
        assertFalse(filled.affectedBy(8, 1, 3));
        assertFalse(filled.affectedBy(3, 1, 20));

        HomeRegion box = HomeRegion.box(0, 0, 0, 10, 10, 10, "box");
        assertTrue(box.affectedBy(10, 10, 10));
        assertFalse(box.affectedBy(11, 5, 5));
    }

    @Test
    void packRoundTripsNegativeCoordinates() {
        long p = HomeRegion.pack(-1234567, 200, -7654321);
        assertEquals(-1234567, HomeRegion.unpackX(p));
        assertEquals(200, HomeRegion.unpackY(p));
        assertEquals(-7654321, HomeRegion.unpackZ(p));
        long q = HomeRegion.pack(5, 0, 5);
        assertEquals(5, HomeRegion.unpackX(q));
        assertEquals(0, HomeRegion.unpackY(q));
        assertEquals(5, HomeRegion.unpackZ(q));
    }

    @Test
    void homeNamesFormatAndFallBack() {
        assertEquals("Hugh's Home", HomeNames.format("%s's Home", "Hugh"));
        assertEquals("Someone's Home", HomeNames.format("%s's Home", ""));
        assertEquals("Home of Hugh", HomeNames.format("Home of %s", "Hugh"));
        assertEquals("Hugh's Home", HomeNames.format("%d bad %q", "Hugh"));
        assertEquals("Hugh's Home", HomeNames.format(null, "Hugh"));
        assertTrue(HomeNames.isHomeName("Home"));
        assertTrue(HomeNames.isHomeName("home"));
        assertFalse(HomeNames.isHomeName("Village"));
        assertFalse(HomeNames.isHomeName(null));
    }
}
