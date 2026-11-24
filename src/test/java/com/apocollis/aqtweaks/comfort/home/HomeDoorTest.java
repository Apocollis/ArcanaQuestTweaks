package com.apocollis.aqtweaks.comfort.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The layered door test: open sky, rays, then the speculative sub-fill. */
class HomeDoorTest {

    private static HomeFiller.DoorSettings settings(int probeCells, int maxDoors) {
        HomeFiller.DoorSettings s = HomeFiller.DoorSettings.defaults();
        s.probeCells = probeCells;
        s.maxDoors = maxDoors;
        return s;
    }

    private static HomeFiller run(HomeFillerTest.Grid grid, int maxCells, HomeFiller.DoorSettings s) {
        HomeFiller filler = new HomeFiller(grid, 3, 1, 3, maxCells, 12, 6, 8, s);
        for (int i = 0; i < 4000 && !filler.step(400); i++) {
            // keep stepping
        }
        assertTrue(filler.isDone());
        return filler;
    }

    /** Room 1: interior x1..5, y1..3, z1..5, walls x0 and x6, floor y0, roof y4, bed block at (3,1,3). */
    private static HomeFillerTest.Grid room1() {
        HomeFillerTest.Grid grid = new HomeFillerTest.Grid();
        grid.shell(0, 0, 0, 6, 4, 6, true);
        grid.put(3, 1, 3);
        return grid;
    }

    /** East door of room 1: two portal cells in the x=6 wall at z=3. */
    private static void eastDoor(HomeFillerTest.Grid grid) {
        grid.door(6, 1, 3);
        grid.door(6, 2, 3);
    }

    private static void plane(HomeFillerTest.Grid grid, int y) {
        for (int x = grid.lx0; x <= grid.lx1; x++) {
            for (int z = grid.lz0; z <= grid.lz1; z++) {
                grid.put(x, y, z);
            }
        }
    }

    @Test
    void aClosedInteriorDoorJoinsTwoRooms() {
        HomeFillerTest.Grid grid = room1();
        grid.shell(6, 0, 0, 12, 4, 6, true); // room 2: interior x7..11
        eastDoor(grid);
        HomeFiller filler = run(grid, 4000, HomeFiller.DoorSettings.defaults());
        HomeRegion region = filler.result();
        assertFalse(region.isBox(), region.reason());
        assertTrue(region.contains(9, 2, 3), "the second room counts");
        assertTrue(region.contains(6, 1, 3), "the door cell itself is part of the home");
        assertEquals(74 + 2 + 75, region.cellCount());
        assertEquals(1, filler.portalStats().total(), "a two-high door is one verdict");
        assertEquals(1, filler.portalStats().interiorRays);
    }

    @Test
    void anExteriorDoorUnderOpenSkyIsAWall() {
        HomeFillerTest.Grid grid = room1();
        grid.skylight = true;
        eastDoor(grid);
        HomeFiller filler = run(grid, 4000, HomeFiller.DoorSettings.defaults());
        HomeRegion region = filler.result();
        assertFalse(region.isBox(), region.reason());
        assertEquals(74, region.cellCount());
        assertFalse(region.contains(7, 1, 3));
        assertFalse(region.contains(6, 1, 3), "an outside door is a wall, not a cell");
        assertEquals(1, filler.portalStats().exteriorOpenAir);
    }

    @Test
    void aDoorOntoAnOpenFieldIsCaughtByTheRays() {
        HomeFillerTest.Grid grid = room1();
        plane(grid, 0); // ground; no skylight, so the open-air step cannot decide
        eastDoor(grid);
        HomeFiller filler = run(grid, 4000, HomeFiller.DoorSettings.defaults());
        assertFalse(filler.result().isBox(), filler.result().reason());
        assertEquals(74, filler.result().cellCount());
        assertEquals(1, filler.portalStats().exteriorRays);
        assertEquals(0, filler.portalStats().exteriorOpenAir);
    }

    @Test
    void anExteriorDoorLeftOpenNoLongerLeaksIntoABoxFallback() {
        // Before the door test an open door let the fill run into the world until the cell cap.
        HomeFillerTest.Grid grid = room1();
        plane(grid, 0);
        eastDoor(grid);
        HomeRegion region = run(grid, 400, HomeFiller.DoorSettings.defaults()).result();
        assertFalse(region.isBox(), region.reason());
        assertEquals(74, region.cellCount());
    }

    @Test
    void anUndergroundDoorIntoAnOpenCaveIsDecidedByTheSubFill() {
        HomeFillerTest.Grid grid = room1();
        plane(grid, 0);
        plane(grid, 4); // a low ceiling over a wide cave: rays are ambiguous
        eastDoor(grid);
        HomeFiller filler = run(grid, 4000, HomeFiller.DoorSettings.defaults());
        HomeRegion region = filler.result();
        assertFalse(region.isBox(), region.reason());
        assertEquals(74, region.cellCount());
        assertFalse(region.contains(7, 1, 3));
        assertEquals(1, filler.portalStats().exteriorFill);
        assertEquals(0, filler.portalStats().exteriorRays);
    }

    @Test
    void aLargeClosedSpaceWithinTheBudgetJoins() {
        HomeFillerTest.Grid grid = room1();
        grid.shell(6, 0, 0, 27, 4, 11, true); // hall interior x7..26, z1..10: 20 x 3 x 10 = 600 cells
        eastDoor(grid);
        HomeFiller filler = run(grid, 4000, HomeFiller.DoorSettings.defaults());
        HomeRegion region = filler.result();
        assertFalse(region.isBox(), region.reason());
        assertTrue(region.contains(20, 2, 5));
        assertEquals(74 + 2 + 600, region.cellCount());
        assertEquals(1, filler.portalStats().interior());
    }

    @Test
    void aSpaceOverTheBudgetIsOutside() {
        HomeFillerTest.Grid grid = room1();
        grid.shell(6, 0, 0, 27, 4, 11, true);
        eastDoor(grid);
        HomeFiller filler = run(grid, 4000, settings(100, 24));
        HomeRegion region = filler.result();
        assertFalse(region.isBox(), region.reason());
        assertFalse(region.contains(20, 2, 5));
        assertEquals(74, region.cellCount());
        assertEquals(1, filler.portalStats().exteriorFill);
    }

    @Test
    void nestedDoorsShareTheBudget() {
        // A -> door -> B (interior x7..15, z1..9: 243 cells) -> door at x16 -> C (interior x17..25: 243 cells)
        HomeFillerTest.Grid grid = room1();
        grid.shell(6, 0, 0, 16, 4, 10, true);
        grid.shell(16, 0, 0, 26, 4, 10, true);
        eastDoor(grid);
        grid.door(16, 1, 5);
        grid.door(16, 2, 5);

        HomeRegion tight = run(grid, 4000, settings(300, 24)).result();
        assertFalse(tight.isBox(), tight.reason());
        assertTrue(tight.contains(10, 2, 5), "B fits the budget");
        assertFalse(tight.contains(20, 2, 5), "C does not fit what B left over");

        HomeRegion roomy = run(grid, 4000, settings(1000, 24)).result();
        assertTrue(roomy.contains(10, 2, 5));
        assertTrue(roomy.contains(20, 2, 5), "with a bigger budget both rooms join");
    }

    @Test
    void maxDoorsCapsTheNumberOfVerdicts() {
        HomeFillerTest.Grid grid = room1();
        grid.shell(6, 0, 0, 12, 4, 6, true); // east room
        grid.shell(-6, 0, 0, 0, 4, 6, true); // west room
        eastDoor(grid);
        grid.door(0, 1, 3);
        grid.door(0, 2, 3);
        HomeFiller filler = run(grid, 4000, settings(800, 1));
        HomeRegion region = filler.result();
        assertFalse(region.isBox(), region.reason());
        boolean east = region.contains(9, 2, 3);
        boolean west = region.contains(-3, 2, 3);
        assertTrue(east ^ west, "exactly one of the two rooms is judged in time");
        assertEquals(1, filler.portalStats().exteriorLimit);
        assertEquals(1, filler.portalStats().interior());

        HomeFiller both = run(grid, 4000, settings(800, 24));
        assertTrue(both.result().contains(9, 2, 3) && both.result().contains(-3, 2, 3));
    }

    @Test
    void aDimensionWithoutSkylightSkipsTheOpenAirStep() {
        HomeFillerTest.Grid grid = room1();
        eastDoor(grid);
        grid.skylight = false;
        HomeFiller dark = run(grid, 4000, HomeFiller.DoorSettings.defaults());
        assertEquals(0, dark.portalStats().exteriorOpenAir);
        assertEquals(1, dark.portalStats().exteriorRays);

        grid.skylight = true;
        HomeFiller lit = run(grid, 4000, HomeFiller.DoorSettings.defaults());
        assertEquals(1, lit.portalStats().exteriorOpenAir);
        assertEquals(0, lit.portalStats().exteriorRays);
    }

    @Test
    void aHatchIntoACellarJoinsTheBasement() {
        // a trapdoor in the floor of an upper room leads down to a cellar
        HomeFillerTest.Grid grid = new HomeFillerTest.Grid();
        grid.shell(0, 5, 0, 6, 9, 6, true); // upper room, floor y5, interior y6..8
        grid.shell(0, 0, 0, 6, 5, 6, true); // cellar, ceiling shared at y5, interior y1..4
        grid.door(2, 5, 2); // the hatch
        grid.put(3, 6, 3);
        HomeFiller filler = new HomeFiller(grid, 3, 6, 3, 4000, 12, 6, 8);
        for (int i = 0; i < 4000 && !filler.step(400); i++) {
            // keep stepping
        }
        HomeRegion region = filler.result();
        assertFalse(region.isBox(), region.reason());
        assertTrue(region.contains(2, 5, 2), "the hatch cell joins");
        assertTrue(region.contains(3, 2, 3), "the cellar counts");
        assertEquals(1, filler.portalStats().interior());
    }

    @Test
    void holdsCoversInteriorBoundaryAndBoxes() {
        HomeRegion region = run(room1(), 4000, HomeFiller.DoorSettings.defaults()).result();
        assertTrue(region.holds(2, 1, 2), "an interior cell");
        assertTrue(region.holds(0, 1, 3), "a wall block touching the interior");
        assertTrue(region.holds(3, 4, 3), "the roof block");
        assertFalse(region.holds(20, 1, 3));
        assertFalse(region.containsBoundary(2, 1, 2));
        HomeRegion box = HomeRegion.box(0, 0, 0, 10, 10, 10, "box");
        assertTrue(box.holds(5, 5, 5));
        assertFalse(box.holds(11, 5, 5));
    }
}
